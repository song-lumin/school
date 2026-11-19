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

    /**
     * 站点结论码（站点管理员结案时写入）：
     * MISMATCH_TRUE 误领成立 / FALSE_CLAIM_TRUE 冒领成立 / OTHER_TRUE 其他成立；
     * DAMAGE_CLAIMER 损坏·认领人责 / DAMAGE_PUBLISHER 损坏·发布人责 /
     * DAMAGE_SITE 损坏·站点责 / DAMAGE_MAJEURE 损坏·不可抗力；
     * NF_FOUND 实物在站 / NF_NOT_DELIVERED 从未投放 / NF_STOLEN 被他人拿走；
     * REJECT 申诉不成立。
     */
    private String siteVerdict;

    /** 站点建议的物品处置：RETURNING 待归还 / VOIDED 作废 / LOST 丢失待查 / NONE 不动 */
    private String siteDispose;

    /** 按站点结论推导的应回滚项（逗号分隔），供系统管理员核对，不直接执行 */
    private String siteSuggestItems;

    /** 1 = 涉及站点自身责任或不可抗力，需系统复核后执行 */
    private Integer needReview;

    /** 站点提交结论时间 */
    private LocalDateTime siteResolvedAt;

    /** 系统实际执行的回滚项 */
    private String creditActualItems;

    /** 积分处置人 */
    private Long creditHandlerId;

    /** 积分处置时间 */
    private LocalDateTime creditHandledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
