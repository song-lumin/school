package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认领申请实体，对应数据库 claim_apply 表
 *
 * <p>记录「失主对某个招领物品发起的认领」的完整流程数据：
 * 提交申请 → 管理员审核（比对该物品的验证问题答案）→ 通过后凭取件码到站点取件 → 完成。
 * 同时记录审核拒绝原因、匹配置信度、取件凭证照片/签名、信用分发放情况等审计信息。</p>
 *
 * <p>申请状态流转见 {@link com.school.lostfound.enums.ClaimStatus}。</p>
 */
@Data
@TableName("claim_apply")
public class ClaimApply {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请认领的招领物品 ID（关联 found_item.id） */
    private Long itemId;

    /** 来源寻物启事 ID：若申请是从某条寻物启事一键发起的，记录来源便于追溯 */
    private Long sourceNoticeId;

    /** 申请人（失主）用户 ID */
    private Long claimerId;

    /** 失主对验证问题的回答，审核时与物品的 referenceAnswer 比对 */
    private String answer;

    /** 申请状态，见 ClaimStatus 枚举（0 待审核 / 1 已完成 / 2 已拒绝 / 3 已锁定 / 4 已通过待取件） */
    private Integer applyStatus;

    /** 拒绝原因：管理员驳回申请时填写，展示给申请人 */
    private String rejectReason;

    /** 累计被拒绝次数：多次被拒可能触发风控（防止恶意猜答案、骚扰申请） */
    private Integer rejectCount;

    /** 匹配置信度得分（0-100）：由特征匹配算法给出的参考分，辅助管理员判断 */
    private Integer confidenceScore;

    /** 是否低置信度申请（0/1）：低置信度的申请会要求人工重点核验 */
    private Integer lowConfidence;

    /** 置信度说明：解释分数是怎么来的（如分类相同、地点相近、图片相似） */
    private String confidenceReason;

    /** 实际取件时间 */
    private LocalDateTime pickupTime;

    /** 取件凭证照片 URL：取件时拍摄留底，防止纠纷 */
    private String pickupPhoto;

    /** 取件人电子签名：签字确认领取，作为完成凭证 */
    private String pickupSignature;

    /** 是否已发放信用分奖励（0/1）：完成认领后给失主等角色加分的发放标记，防重复发放 */
    private Integer creditIssued;

    /** 是否已回滚信用分（0/1）：已完成认领因争议败诉等原因撤销时回滚奖励 */
    private Integer creditRollback;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
