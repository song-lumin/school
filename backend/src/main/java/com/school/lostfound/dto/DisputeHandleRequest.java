package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DisputeHandleRequest {

    /** true=通过回滚积分；false=驳回 */
    @NotNull(message = "处理结果不能为空")
    private Boolean approved;

    @NotBlank(message = "处理意见不能为空")
    private String handlerNote;
}
