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
    /** 物品所在投放点及其负责人联系方式，供认领人到站前咨询 */
    private Long dropPointId;
    private String dropPointName;
    private String dropPointLocation;
    private String dropPointAdminName;
    private String dropPointAdminPhone;
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
