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
 * 定时任务：
 * 1. 发布满7天未点"已投放"(状态6) → 自动作废(0)，hand_in_log置3
 * 2. 普通物品公开满365天、易腐物品公开满7天 → 自动转超期(5)，关闭认领入口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoInvalidateTask {

    private static final int PENDING_PLACEMENT_TIMEOUT_DAYS = 7;
    private static final int NORMAL_EXPIRE_DAYS = 365;
    private static final int PERISHABLE_EXPIRE_DAYS = 7;

    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void autoInvalidate() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 待投放(6)满7天未投放 → 作废
        LocalDateTime pendingDeadline = now.minusDays(PENDING_PLACEMENT_TIMEOUT_DAYS);
        List<FoundItem> pendingItems = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PENDING_PLACEMENT.getCode())
                        .le(FoundItem::getPublishedAt, pendingDeadline));
        for (FoundItem item : pendingItems) {
            item.setItemStatus(ItemStatus.VOIDED.getCode());
            foundItemMapper.updateById(item);
            HandInLog logRecord = handInLogMapper.selectOne(
                    new LambdaQueryWrapper<HandInLog>().eq(HandInLog::getItemId, item.getId()));
            if (logRecord != null) {
                logRecord.setHandInStatus(3);
                handInLogMapper.updateById(logRecord);
            }
        }

        // 2. 公开/认领中的普通物品满365天、易腐物品满7天 → 超期(5)
        List<FoundItem> activeItems = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .in(FoundItem::getItemStatus,
                                ItemStatus.PUBLIC.getCode(),
                                ItemStatus.CLAIMING.getCode()));
        int expiredCount = 0;
        for (FoundItem item : activeItems) {
            int expireDays = item.getPerishable() != null && item.getPerishable() == 1
                    ? PERISHABLE_EXPIRE_DAYS : NORMAL_EXPIRE_DAYS;
            if (item.getPublishedAt() != null
                    && item.getPublishedAt().plusDays(expireDays).isBefore(now)) {
                item.setItemStatus(ItemStatus.EXPIRED.getCode());
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
