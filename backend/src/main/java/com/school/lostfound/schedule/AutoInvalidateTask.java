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
 * 7天未投放自动作废：状态6且发布满7天 → 状态0，hand_in_log 同步作废(3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoInvalidateTask {

    private static final int HAND_IN_TIMEOUT_DAYS = 7;

    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void autoInvalidate() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(HAND_IN_TIMEOUT_DAYS);

        List<FoundItem> items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PUBLISHED_NOT_HANDED_IN.getCode())
                        .le(FoundItem::getPublishedAt, deadline));

        for (FoundItem item : items) {
            item.setItemStatus(ItemStatus.VOIDED.getCode());
            foundItemMapper.updateById(item);

            HandInLog logRecord = handInLogMapper.selectOne(
                    new LambdaQueryWrapper<HandInLog>().eq(HandInLog::getItemId, item.getId()));
            if (logRecord != null) {
                logRecord.setHandInStatus(3);
                handInLogMapper.updateById(logRecord);
            }
        }

        if (!items.isEmpty()) {
            log.info("自动作废完成，共处理 {} 条7天未投放的招领", items.size());
        }
    }
}
