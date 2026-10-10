package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReportCreateRequest {

    /** FAKE_PUBLISH=虚假投放, DESC_MISMATCH=描述不符, OTHER=其他 */
    @NotBlank(message = "举报类型不能为空")
    private String reportType;

    /** ITEM=招领帖, NOTICE=寻物启事 */
    private String targetType = "ITEM";

    private Long itemId;

    private Long noticeId;

    @NotBlank(message = "举报描述不能为空")
    private String description;

    private List<String> evidenceImages;
}
