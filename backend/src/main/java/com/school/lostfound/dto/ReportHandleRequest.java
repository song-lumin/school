package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportHandleRequest {

    /** true=举报成立（物品作废 + 回滚发布人全部积分），false=举报不成立 */
    @NotNull(message = "处理结果不能为空")
    private Boolean valid;

    @NotBlank(message = "处理意见不能为空")
    private String handlerNote;

    /** 举报成立时是否同时封禁发布者账号（可选，默认不封禁） */
    private Boolean banPublisher;
}
