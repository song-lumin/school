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
 * 自动转超期：公开待认领物品，普通365天、易腐7天 → 状态5（超期待线下处置）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoExpireTask {

    private static final int PERISHABLE_EXPIRE_DAYS = 7;
    private static final int NORMAL_EXPIRE_DAYS = 365;

    private final FoundItemMapper foundItemMapper;

    @Scheduled(cron = "0 30 3 * * ?")
    @Transactional
    public void autoExpire() {
        LocalDateTime now = LocalDateTime.now();

        List<FoundItem> items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode()));

        int count = 0;
        for (FoundItem item : items) {
            int expireDays = item.getPerishable() == 1
                    ? PERISHABLE_EXPIRE_DAYS : NORMAL_EXPIRE_DAYS;
            if (item.getPublishedAt().plusDays(expireDays).isBefore(now)) {
                item.setItemStatus(ItemStatus.EXPIRED.getCode());
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
