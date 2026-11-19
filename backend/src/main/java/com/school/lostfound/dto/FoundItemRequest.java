package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FoundItemRequest {
    @NotBlank(message = "物品标题不能为空")
    private String title;

    @NotBlank(message = "物品类别不能为空")
    private String category;

    private String description;

    @NotBlank(message = "拾取地点不能为空")
    private String foundLocation;

    @NotNull(message = "投放点不能为空")
    private Long dropPointId;

    @NotNull(message = "拾取时间不能为空")
    private LocalDateTime foundTime;

    /** 拾取时间模糊描述（选填，≤60 字），例如「周三傍晚左右」「午休时间」 */
    @Size(max = 60, message = "时间描述最多 60 字")
    private String foundTimeText;

    /** 0=精确时刻 1=只知道日期 2=上午 3=下午 4=傍晚 5=晚上；缺省按 0 处理 */
    private Integer foundTimePrecision;

    private List<String> images;

    @NotBlank(message = "防伪问题不能为空")
    private String claimQuestion;

    private String referenceAnswer;

    private Integer perishable;

    private Long actualFounderId;
}
