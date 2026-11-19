package com.school.lostfound.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.mapper.FoundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动转超期定时任务 —— 把公开超过保存期限的招领物品批量转入"超期待线下处置"状态。
 * <p>
 * 业务背景：物品长期无人认领会持续占用系统资源（列表展示、存储空间），
 * 也有管理规范要求（遗留物品保存一定期限后须转交线下统一处置）。
 * 本系统规定：普通物品公开满 365 天（一年）、易腐物品公开满 7 天后，
 * 状态由"公开待认领"自动变更为"超期"（状态 5）。超期物品不再出现在
 * 前台可认领列表中，由管理员按线下流程（注销/上交/统一处理）收尾。
 * <p>
 * 与 ExpireWarningTask 的关系：预警（90 天/7 天）在前提醒管理员，本任务在后
 * 真正改变状态，二者构成"先预警、后到期"的完整生命周期管理。
 * <p>
 * 执行时间：每天凌晨 3:30（排在 3:00 的预警任务之后，避免同一凌晨窗口资源竞争）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoExpireTask {

    /** 易腐物品保存期限：公开满 7 天即超期 */
    private static final int PERISHABLE_EXPIRE_DAYS = 7;
    /** 普通物品保存期限：公开满 365 天即超期 */
    private static final int NORMAL_EXPIRE_DAYS = 365;

    /** 招领物品 Mapper，用于查询待超期物品与更新状态 */
    private final FoundItemMapper foundItemMapper;

    /**
     * 扫描所有"公开待认领"物品，把超过保存期限的批量置为超期状态。
     * <p>
     * 筛选条件：只要当前状态 = 公开（PUBLIC）就参与判定，是否到期由时间比较决定。
     * 判断规则：publishedAt + 保存天数（易腐 7 / 普通 365）早于当前时间 → 超期。
     * <p>
     * 事务说明：方法整体在一个事务内执行，批量状态变更原子生效。
     * 注意：这里的查询不带"是否已处理"标记，理论上某物品一旦进入 PUBLIC
     * 且到期就会被再次扫描到——但由于本任务会把状态改走（PUBLIC→EXPIRED），
     * 已处理的物品下次查询时状态已不再是 PUBLIC，天然不会被重复处理。
     */
    @Scheduled(cron = "0 30 3 * * ?") // 每天 03:30 执行一次
    @Transactional
    public void autoExpire() {
        LocalDateTime now = LocalDateTime.now();

        // 拉取所有仍处于"公开待认领"状态的物品；已认领/已作废/已超期的不参与判定
        List<FoundItem> items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode()));

        int count = 0;
        for (FoundItem item : items) {
            // 按易腐标记选择保存期限：易腐 7 天，普通 365 天
            int expireDays = item.getPerishable() == 1
                    ? PERISHABLE_EXPIRE_DAYS : NORMAL_EXPIRE_DAYS;
            // 以发布时间为基准加保存天数，若该时间点已早于当前时刻则确认到期
            if (item.getPublishedAt().plusDays(expireDays).isBefore(now)) {
                item.setItemStatus(ItemStatus.EXPIRED.getCode()); // 状态改为"超期待线下处置"
                foundItemMapper.updateById(item);
                count++;
            }
        }

        if (count > 0) {
            log.info("自动转超期完成，共处理 {} 条物品（易腐{}天、普通{}天）",
                    count, PERISHABLE_EXPIRE_DAYS, NORMAL_EXPIRE_DAYS);
        }
    }
}
