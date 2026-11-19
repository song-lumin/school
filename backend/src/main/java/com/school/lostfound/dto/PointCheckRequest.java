package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 站点现场核验回填：只描述事实（实物是否在架、损坏部位、交付凭证状态），不写责任结论。
 */
@Data
public class PointCheckRequest {

    @NotBlank(message = "请填写现场核验情况")
    @Size(max = 1000, message = "现场核验最多 1000 字")
    private String note;

    /**
     * 现场结论（可选，选了就等于站点当场结案，系统按规则表自动处理状态与积分）：
     * 找不到物品：FOUND 实物在站 / NOT_DELIVERED 从未投放 / STOLEN 被他人拿走；
     * 物品损坏：SITE_FAULT 站点责 / CLAIMER_FAULT 认领人责 / PUBLISHER_FAULT 发布人责；
     * OTHER：现场无法判定，只回填事实并转系统管理员定责。
     */
    @Size(max = 30, message = "结论取值过长")
    private String outcome;
}
