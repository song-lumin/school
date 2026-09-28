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
    private Long applyId;
    private Long itemId;
    private String itemTitle;
    private String disputeType;
    private String description;
    private List<String> evidenceImages;
    private Integer status;
    private String handlerNote;
    private Long handlerId;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

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
