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
 * 超期预警：易腐7天、普通90天，公开待认领物品标记 expire_warning_sent=1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireWarningTask {

    private static final int PERISHABLE_WARNING_DAYS = 7;
    private static final int NORMAL_WARNING_DAYS = 90;

    private final FoundItemMapper foundItemMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void sendExpireWarnings() {
        LocalDateTime now = LocalDateTime.now();

        List<FoundItem> items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode())
                        .eq(FoundItem::getExpireWarningSent, 0));

        int count = 0;
        for (FoundItem item : items) {
            int warningDays = item.getPerishable() == 1
                    ? PERISHABLE_WARNING_DAYS : NORMAL_WARNING_DAYS;
            if (ChronoUnit.DAYS.between(item.getPublishedAt(), now) >= warningDays) {
                item.setExpireWarningSent(1);
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
