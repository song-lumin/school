package com.school.lostfound.vo;

import lombok.Data;

import java.util.List;

@Data
public class RiskWarningVO {
    /** 预警类型：COLLUSION=串通, CLAIM_RATE=认领率异常 */
    private String warningType;
    /** 涉及用户ID（串通=双方，认领率=发布人） */
    private List<Long> userIds;
    /** 展示名（脱敏后） */
    private List<String> userNames;
    /** 描述 */
    private String detail;
    /** 关键数值 */
    private Integer count;
    private List<Long> itemIds;
    private Integer similarity;
    private String firstItemTitle;
    private String secondItemTitle;
}
