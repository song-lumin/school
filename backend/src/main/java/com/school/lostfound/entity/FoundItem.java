package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "found_item", autoResultMap = true)
public class FoundItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String description;

    private String foundLocation;

    private LocalDateTime foundTime;

    /** 拾取时间的模糊描述（如「周三傍晚左右」），选填；有值时优先于精确时间展示 */
    private String foundTimeText;

    /**
     * 拾取时间精度：0=精确时刻（历史数据/默认），1=只知道日期，2=上午，3=下午，4=傍晚，5=晚上
     * 模糊精度下 found_time 只代表作日期，具体时分不对外展示
     */
    private Integer foundTimePrecision;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private String claimQuestion;

    private String referenceAnswer;

    private Integer perishable;

    private Integer itemStatus;

    private Long founderId;

    private Long actualFounderId;

    private Long dropPointId;

    private Long forwardedNoticeId;

    private Long forwarderId;

    private LocalDateTime publishedAt;

    private LocalDateTime claimedAt;

    private Integer expireWarningSent;

    private String takedownReason;

    private LocalDateTime takedownAt;

    private Long takedownBy;

    private Integer appealStatus;

    private String appealReason;

    private LocalDateTime appealAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
