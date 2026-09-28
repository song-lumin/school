package com.school.lostfound.vo;

import com.school.lostfound.entity.ClaimApply;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class ClaimApplyVO {
    private Long id;
    private Long itemId;
    private Long sourceNoticeId;
    private String itemTitle;
    private String claimQuestion;
    private Long claimerId;
    private String claimerName;
    private String answer;
    private Integer applyStatus;
    private String rejectReason;
    private Integer rejectCount;
    private Integer confidenceScore;
    private Integer lowConfidence;
    private String confidenceReason;
    private LocalDateTime pickupTime;
    private String pickupPhoto;
    private String pickupSignature;
    private LocalDateTime createdAt;

    public static ClaimApplyVO fromEntity(ClaimApply apply, String itemTitle, String claimQuestion, String claimerName) {
        ClaimApplyVO vo = new ClaimApplyVO();
        BeanUtils.copyProperties(apply, vo);
        vo.setItemTitle(itemTitle);
        vo.setClaimQuestion(claimQuestion);
        vo.setClaimerName(claimerName);
        return vo;
    }
}
