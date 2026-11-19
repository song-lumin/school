package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 站点现场核验：站点管理员既回填事实，也可以直接给出结论结案。
 * 只填 note = 仅回填事实，工单转「处理中」等系统管理员（旧行为，保留兼容）；
 * 同时填 verdict = 站点结案，工单转「待积分处置」，系统管理员只按结论执行积分。
 */
@Data
public class PointCheckRequest {

    @NotBlank(message = "请填写现场核验情况")
    @Size(max = 1000, message = "现场核验最多 1000 字")
    private String note;

    /**
     * 站点结论码（可选）：
     * 物品不符：MISMATCH_TRUE 误领成立 / REJECT 不成立；
     * 物品损坏：DAMAGE_CLAIMER 认领人责 / DAMAGE_PUBLISHER 发布人责 /
     *          DAMAGE_SITE 站点责 / DAMAGE_MAJEURE 不可抗力；
     * 物品被冒领：FALSE_CLAIM_TRUE 冒领成立 / REJECT 不成立；
     * 找不到物品：NF_FOUND 实物在站 / NF_NOT_DELIVERED 从未投放 / NF_STOLEN 被他人拿走；
     * 其他纠纷：OTHER_TRUE 成立 / REJECT 不成立。
     */
    @Size(max = 30, message = "结论取值过长")
    private String verdict;

    /** 旧字段，等价于 verdict，保留兼容 */
    @Size(max = 30, message = "结论取值过长")
    private String outcome;
}
