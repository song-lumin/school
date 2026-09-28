package com.school.lostfound.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExpireWarningVO {
    private Long itemId;
    private String title;
    private String founderName;
    private LocalDateTime publishedAt;
    private Integer perishable;
    private Long daysSincePublish;
}
