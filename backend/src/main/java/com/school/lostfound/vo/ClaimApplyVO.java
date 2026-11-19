package com.school.lostfound.vo;

import com.school.lostfound.entity.ClaimApply;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/**
 * 认领申请 VO
 *
 * 功能：封装一条认领申请的展示数据。除实体 ClaimApply 本身的字段外，
 * 还补充了关联物品标题、认领问题（验证问题）、申请人姓名等
 * 关联查询信息，前端可直接渲染列表卡片。
 *
 * 使用位置：发布者"处理认领申请"页面、管理员"认领审核"页面、
 * 用户"我的申请"页面。其中的 confidenceScore/lowConfidence/confidenceReason
 * 供管理员快速判断低置信度申请是否需要人工重点核实。
 */
@Data
public class ClaimApplyVO {
    private Long id;
    /** 关联物品 ID */
    private Long itemId;
    /** 申请人基于哪条寻物启事发起的申请 */
    private Long sourceNoticeId;
    /** 物品标题（关联查询补充） */
    private String itemTitle;
    /** 发布者设置的认领验证问题（关联查询补充） */
    private String claimQuestion;
    /** 认领人 ID */
    private Long claimerId;
    /** 认领人姓名（关联查询补充） */
    private String claimerName;
    /** 认领人对验证问题的回答 */
    private String answer;
    /** 申请状态：0=待审核 1=已通过 2=已驳回 */
    private Integer applyStatus;
    /** 驳回原因 */
    private String rejectReason;
    /** 已被驳回的次数（多次驳回可提示可疑） */
    private Integer rejectCount;
    /** 系统自动计算的置信度分数（0-100） */
    private Integer confidenceScore;
    /** 是否低置信度（1=是，需人工重点核实） */
    private Integer lowConfidence;
    /** 置信度评定说明 */
    private String confidenceReason;
    /** 预约/实际领取时间 */
    private LocalDateTime pickupTime;
    /** 领取凭证照片 */
    private String pickupPhoto;
    /** 领取电子签名（图片） */
    private String pickupSignature;
    private LocalDateTime createdAt;

    /**
     * 由实体构造 VO，并补充三个关联字段（物品标题、验证问题、认领人姓名）。
     */
    public static ClaimApplyVO fromEntity(ClaimApply apply, String itemTitle, String claimQuestion, String claimerName) {
        ClaimApplyVO vo = new ClaimApplyVO();
        BeanUtils.copyProperties(apply, vo);
        vo.setItemTitle(itemTitle);
        vo.setClaimQuestion(claimQuestion);
        vo.setClaimerName(claimerName);
        return vo;
    }
}
