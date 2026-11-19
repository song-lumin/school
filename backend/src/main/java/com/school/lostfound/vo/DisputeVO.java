package com.school.lostfound.vo;

import com.school.lostfound.entity.Dispute;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DisputeVO {
    private Long id;
    private Long applicantId;
    private String applicantName;
    private String applicantPhone;
    private Long applyId;
    private Long itemId;
    private String itemTitle;
    private String disputeType;
    private String description;
    private List<String> evidenceImages;
    private Integer status;
    private String handlerNote;
    private String handleResult;
    private Long handlerId;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    /** 站点现场核验陈述（只写事实） */
    private String pointCheckNote;
    private Long pointCheckerId;
    private String pointCheckerName;
    private LocalDateTime pointCheckedAt;

    /** 关联投放点信息，便于管理员核查与回避判断 */
    private Long dropPointId;
    private String dropPointName;
    private String dropPointAdminName;
    private String dropPointAdminPhone;
    /** 定责人是否即该点位负责人（true 表示应回避） */
    private Boolean selfReviewRisk;
    /** 待处理工单已等待小时数：站点不在岗时用来判断是否该由系统侧兜底 */
    private Integer pendingHours;
    /** 现场核验是否由非本点位负责人代填（站点不在岗，由系统管理员电话核实后代填） */
    private Boolean pointCheckStandIn;

    /** 最近一次催办时间（站点超 24h 未回填） */
    private LocalDateTime pointEscalatedAt;
    /** 已催办次数 */
    private Integer pointRemindCount;
    /** 定责时站点仍未回填，强制标记 1 = 站点未响应 */
    private Integer siteUnresponsive;
    /** 站点未响应标记时间 */
    private LocalDateTime siteUnresponsiveAt;
    /** 是否已超过站点回填 SLA（24h）且仍未回填 */
    private Boolean pointSlaOverdue;

    /** 站点结案结论码 */
    private String siteVerdict;
    /** 站点结论的中文说明 */
    private String siteVerdictText;
    /** 站点建议的物品处置 */
    private String siteDispose;
    /** 按结论推导的应回滚项 */
    private String siteSuggestItems;
    /** 需系统复核 */
    private Integer needReview;
    private LocalDateTime siteResolvedAt;
    /** 系统实际执行的回滚项 */
    private String creditActualItems;
    private Long creditHandlerId;
    private LocalDateTime creditHandledAt;

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
        vo.setHandleResult(d.getHandleResult());
        vo.setHandlerId(d.getHandlerId());
        vo.setHandledAt(d.getHandledAt());
        vo.setCreatedAt(d.getCreatedAt());
        vo.setPointCheckNote(d.getPointCheckNote());
        vo.setPointCheckerId(d.getPointCheckerId());
        vo.setPointCheckedAt(d.getPointCheckedAt());
        vo.setPointEscalatedAt(d.getPointEscalatedAt());
        vo.setPointRemindCount(d.getPointRemindCount());
        vo.setSiteUnresponsive(d.getSiteUnresponsive());
        vo.setSiteUnresponsiveAt(d.getSiteUnresponsiveAt());
        vo.setSiteVerdict(d.getSiteVerdict());
        vo.setSiteDispose(d.getSiteDispose());
        vo.setSiteSuggestItems(d.getSiteSuggestItems());
        vo.setNeedReview(d.getNeedReview());
        vo.setSiteResolvedAt(d.getSiteResolvedAt());
        vo.setCreditActualItems(d.getCreditActualItems());
        vo.setCreditHandlerId(d.getCreditHandlerId());
        vo.setCreditHandledAt(d.getCreditHandledAt());
        return vo;
    }
}
