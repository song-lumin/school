package com.school.lostfound.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.HandInLog;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.HandInLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品生命周期自动流转定时任务 —— 系统状态机中"无人操作时兜底推进"的一环。
 * <p>
 * 业务背景：招领物品有两种"卡住不动"的情形，需要系统自动处理，
 * 否则数据会长期滞留在中间状态、占用列表与管理员注意力：
 * <p>
 * 1. 拾获人提交物品后处于"待投放"（状态 6），必须在 7 天内把实物投放到
 *    失物招领点并点击"已投放"。超时未操作视为放弃或 mitweg丢失线索，
 *    物品自动作废（状态 0），同时其上交记录置为"已作废"（handInStatus=3），
 *    保证物品状态与上交流水两处数据一致；
 * 2. 物品公开（状态 1）或认领中（状态 2）超过保存期限仍未闭环，
 *    普通物品满 365 天、易腐物品满 7 天，自动转"超期"（状态 5），
 *    同时关闭认领入口，交由管理员线下处置。
 * <p>
 * 与 AutoExpireTask 的分工：本任务覆盖范围更广（含待投放超时 + 认领中物品，
 * 且会联动更新 hand_in_log），AutoExpireTask 只处理公开物品的超期。
 * 执行时间：每天凌晨 2:30，早于其他两个定时任务，优先清理僵死数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoInvalidateTask {

    /** 待投放状态的最长保留天数：满 7 天未点击"已投放"即作废 */
    private static final int PENDING_PLACEMENT_TIMEOUT_DAYS = 7;
    /** 普通物品公开保存期限：满 365 天转超期 */
    private static final int NORMAL_EXPIRE_DAYS = 365;
    /** 易腐物品公开保存期限：满 7 天转超期 */
    private static final int PERISHABLE_EXPIRE_DAYS = 7;

    /** 招领物品 Mapper：查询与更新物品状态 */
    private final FoundItemMapper foundItemMapper;
    /** 上交流水 Mapper：物品作废时联动更新对应的上交记录状态 */
    private final HandInLogMapper handInLogMapper;

    /**
     * 执行两段式自动流转：待投放超时作废 + 公开物品到期转超期。
     * <p>
     * 事务说明：两个批次的更新在同一事务内执行。特别是作废分支里
     * "物品状态"与"上交流水状态"必须成对修改，事务保证二者要么同时生效、
     * 要么同时回滚，避免出现物品已作废但流水仍显示"待投放"的数据不一致。
     */
    @Scheduled(cron = "0 30 2 * * ?") // 每天 02:30 执行一次
    @Transactional
    public void autoInvalidate() {
        LocalDateTime now = LocalDateTime.now();

        // ---------- 第 1 步：待投放(6)满 7 天未投放 → 作废 ----------
        // 计算超时截止线：发布时间早于"当前时间 - 7 天"即视为超时未投放
        LocalDateTime pendingDeadline = now.minusDays(PENDING_PLACEMENT_TIMEOUT_DAYS);
        List<FoundItem> pendingItems = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PENDING_PLACEMENT.getCode()) // 状态=待投放
                        .le(FoundItem::getPublishedAt, pendingDeadline)); // 发布时间早于截止线 → 超 7 天
        for (FoundItem item : pendingItems) {
            item.setItemStatus(ItemStatus.VOIDED.getCode()); // 物品转"作废"
            foundItemMapper.updateById(item);
            // 联动上交流水：该物品若登记过投放记录，把记录状态置 3（已作废）
            // 使流水与物品状态保持一致，管理端对账时不会出现矛盾数据
            HandInLog logRecord = handInLogMapper.selectOne(
                    new LambdaQueryWrapper<HandInLog>().eq(HandInLog::getItemId, item.getId()));
            if (logRecord != null) {
                logRecord.setHandInStatus(3); // 3 = 已作废
                handInLogMapper.updateById(logRecord);
            }
        }

        // ---------- 第 2 步：公开(1)/认领中(2)的物品到期 → 超期(5) ----------
        // 认领中的物品也在处理范围内：认领流程若长期不闭环，同样不能无限占位
        List<FoundItem> activeItems = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .in(FoundItem::getItemStatus,
                                ItemStatus.PUBLIC.getCode(),
                                ItemStatus.CLAIMING.getCode()));
        int expiredCount = 0;
        for (FoundItem item : activeItems) {
            // 易腐判定带 null 防御：历史数据 perishable 可能为空，视为普通物品处理
            int expireDays = item.getPerishable() != null && item.getPerishable() == 1
                    ? PERISHABLE_EXPIRE_DAYS : NORMAL_EXPIRE_DAYS;
            // 时间判空同理：publishedAt 为空的异常数据直接跳过，不参与流转
            if (item.getPublishedAt() != null
                    && item.getPublishedAt().plusDays(expireDays).isBefore(now)) {
                item.setItemStatus(ItemStatus.EXPIRED.getCode()); // 转"超期待线下处置"，前台认领入口随之关闭
                foundItemMapper.updateById(item);
                expiredCount++;
            }
        }

        if (!pendingItems.isEmpty()) {
            log.info("未投放自动作废完成，共处理 {} 条", pendingItems.size());
        }
        if (expiredCount > 0) {
            log.info("超期自动流转完成，共处理 {} 条物品", expiredCount);
        }
    }
}
