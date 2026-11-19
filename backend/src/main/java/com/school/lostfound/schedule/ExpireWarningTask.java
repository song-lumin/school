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
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 超期预警定时任务 —— 在物品"临近超期"时提前打标记，作为超期处置前的提醒环节。
 * <p>
 * 业务背景：失物招领物品不能无限期占仓库位/内存档。系统规定：
 * 易腐物品（如食品、水果）保存期短，公开 7 天即需预警；普通物品 90 天预警。
 * 被本任务标记（expire_warning_sent=1）的物品，前端管理端会用醒目样式提示
 * 管理员尽快联系失主/组织线下处置；随后由 AutoExpireTask / AutoInvalidateTask
 * 在真正到期时把物品转入"超期"状态。预警与到期是先后两个阶段，避免管理员
 * 在物品突然变超期时措手不及。
 * <p>
 * 执行时间：每天凌晨 3 点（cron 秒 分 时 日 月 周），避开白天的业务高峰。
 * 该标记只写一次（expire_warning_sent 0→1），任务天然幂等，重复执行不会重复处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireWarningTask {

    /** 易腐物品（perishable=1）的超期预警提前量：公开满 7 天即预警 */
    private static final int PERISHABLE_WARNING_DAYS = 7;
    /** 普通物品的超期预警提前量：公开满 90 天即预警 */
    private static final int NORMAL_WARNING_DAYS = 90;

    /** 招领物品 Mapper，用于查询与更新物品的预警标记 */
    private final FoundItemMapper foundItemMapper;

    /**
     * 扫描所有"公开待认领"的物品，对已满预警天数的打上预警标记。
     * <p>
     * 筛选条件：物品状态 = 公开（PUBLIC）且尚未预警过（expire_warning_sent=0）。
     * 判断规则：以 publishedAt（发布时间）为基准，计算距离当前已过去的天数，
     * 易腐物品满 7 天、普通物品满 90 天则标记 notify。
     * <p>
     * 事务说明：整个方法包在一个事务里，所有 update 要么全部生效要么全部回滚，
     * 保证批量标记的一致性（不会出现一半标记一半没标记的中间态）。
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天 03:00 执行一次
    @Transactional
    public void sendExpireWarnings() {
        LocalDateTime now = LocalDateTime.now();

        // 只查状态为"公开待认领"且还没发过预警的物品；
        // "未预警"条件保证了任务重跑/补跑时的幂等性，不会对同一物品重复标记
        List<FoundItem> items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode())
                        .eq(FoundItem::getExpireWarningSent, 0));

        int count = 0;
        for (FoundItem item : items) {
            // 按易腐与否选择不同的预警阈值：易腐 7 天，普通 90 天
            int warningDays = item.getPerishable() == 1
                    ? PERISHABLE_WARNING_DAYS : NORMAL_WARNING_DAYS;
            // 用ChronoUnit 计算发布至今的自然日数，达到阈值即触发预警
            if (ChronoUnit.DAYS.between(item.getPublishedAt(), now) >= warningDays) {
                item.setExpireWarningSent(1); // 标记位 0→1，前端据此展示"临近超期"提醒
                foundItemMapper.updateById(item);
                count++;
            }
        }

        if (count > 0) {
            log.info("超期预警完成，共标记 {} 条物品（易腐{}天、普通{}天）",
                    count, PERISHABLE_WARNING_DAYS, NORMAL_WARNING_DAYS);
        }
    }
}
