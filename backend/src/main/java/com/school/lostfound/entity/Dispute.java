package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "dispute", autoResultMap = true)
public class Dispute {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantId;

    private Long applyId;

    private Long itemId;

    private String disputeType;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> evidenceImages;

    private Integer status;

    private String handlerNote;

    /** 找不到物品工单的核查结论：FOUND / NOT_DELIVERED / STOLEN */
    private String handleResult;

    private Long handlerId;

    private LocalDateTime handledAt;

    /** 站点现场核验陈述：实物是否在架、损坏部位、交付凭证状态等事实描述（只写事实，不写结论） */
    private String pointCheckNote;

    /** 回填现场事实的站点管理员 */
    private Long pointCheckerId;

    private LocalDateTime pointCheckedAt;

    /** 最近一次催办时间：站点超 24h 未回填时由定时任务写入 */
    private LocalDateTime pointEscalatedAt;

    /** 催办次数（最多 3 次，避免持续骚扰） */
    private Integer pointRemindCount;

    /** 定责时站点仍未回填，强制标记 1 = 站点未响应（站点失职留痕） */
    private Integer siteUnresponsive;

    /** 站点未响应标记时间 */
    private LocalDateTime siteUnresponsiveAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
