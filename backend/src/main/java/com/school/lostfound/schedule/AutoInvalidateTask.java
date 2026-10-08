package com.school.lostfound.schedule;

import com.school.lostfound.entity.HandInLog;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.HandInLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.FoundItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 90天未巡检自动作废：状态1(公开)且发布满90天 → 状态0，hand_in_log 同步作废(3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoInvalidateTask {

    private static final int CHECK_TIMEOUT_DAYS = 90;

    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void autoInvalidate() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(CHECK_TIMEOUT_DAYS);

        List<FoundItem> items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode())
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
            log.info("自动作废完成，共处理 {} 条90天未巡检的招领", items.size());
        }
    }
}
