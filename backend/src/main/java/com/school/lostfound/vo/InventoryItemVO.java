package com.school.lostfound.vo;

import com.school.lostfound.entity.FoundItem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryItemVO {
    private FoundItem item;
    /** 0=未投放 1=已放置待巡检 2=已核对 3=作废 */
    private Integer handInStatus;
    private LocalDateTime handInAt;
    private LocalDateTime checkedAt;
    private String checkNote;
    private Integer creditIssued;
    private String founderName;
    private String claimerName;
    private LocalDateTime pickupTime;
}
