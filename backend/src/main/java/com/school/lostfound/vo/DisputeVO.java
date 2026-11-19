package com.school.lostfound.vo;

import com.school.lostfound.entity.Dispute;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 争议申诉 VO
 *
 * 功能：封装一条争议/申诉记录的展示数据。用户对认领结果有异议
 * （如物品被错误认领、冒领）时提交争议，VO 在实体 Dispute 基础上
 * 补充了申请人姓名、争议物品标题，供管理员端直接展示。
 *
 * 使用位置：管理员"争议处理"页面、用户"我的申诉"列表。
 */
@Data
public class DisputeVO {
    private Long id;
    /** 发起申诉的用户 ID */
    private Long applicantId;
    /** 申诉人姓名（关联查询补充） */
    private String applicantName;
    /** 关联的认领申请 ID */
    private Long applyId;
    /** 争议物品 ID */
    private Long itemId;
    /** 物品标题（关联查询补充） */
    private String itemTitle;
    /** 争议类型（如错误认领、物品损坏等） */
    private String disputeType;
    /** 争议描述 */
    private String description;
    /** 举证图片列表 */
    private List<String> evidenceImages;
    /** 处理状态：0=待处理 1=已处理 */
    private Integer status;
    /** 管理员处理意见 */
    private String handlerNote;
    /** 处理人 ID */
    private Long handlerId;
    /** 处理时间 */
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    /**
     * 由实体构造 VO 并补充申诉人姓名与物品标题（逐字段赋值，
     * 因实体中没有这两个关联字段，无法用 BeanUtils 自动拷贝）。
     */
    public static DisputeVO fromEntity(Dispute d, String applicantName, String itemTitle) {
        DisputeVO vo = new DisputeVO();
        vo.setId(d.getId());
        vo.setApplicantId(d.getApplicantId());
        vo.setApplicantName(applicantName);
        vo.setApplyId(d.getApplyId());
        vo.setItemId(d.getItemId());
        vo.setItemTitle(itemTitle);
        vo.setDisputeType(d.getDisputeType());
        vo.setDescription(d.getDescription());
        vo.setEvidenceImages(d.getEvidenceImages());
        vo.setStatus(d.getStatus());
        vo.setHandlerNote(d.getHandlerNote());
        vo.setHandlerId(d.getHandlerId());
        vo.setHandledAt(d.getHandledAt());
        vo.setCreatedAt(d.getCreatedAt());
        return vo;
    }
}
