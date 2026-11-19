package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布失物启事请求 DTO —— 用户登记"我丢了东西"的启事信息。
 *
 * 业务场景：用户在前端"发布失物启事"页面填写表单并提交，
 * 后端据此创建一条 LostNotice（失物启事）记录，用于与拾获物品（FoundItem）进行匹配。
 *
 * 为什么需要它：失物启事与物品实体字段不完全一致（如联系方式、丢失时间为启事特有），
 * 用独立 DTO 接收请求可避免直接暴露 Entity，同时集中承载参数校验规则。
 */
@Data
public class LostNoticeRequest {
    /** 启事标题（必填）：简要概括丢失物品，如"黑色钱包" */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 物品类别（必填）：用于与拾获物品按类别自动匹配 */
    @NotBlank(message = "物品类别不能为空")
    private String category;

    /** 物品详细描述（可选）：品牌、颜色、内含物品等特征 */
    private String description;

    /** 丢失地点（必填）：帮助缩小匹配范围 */
    @NotBlank(message = "丢失地点不能为空")
    private String lostLocation;

    /** 丢失时间（必填）：用于时间线匹配与过期提醒 */
    @NotNull(message = "丢失时间不能为空")
    private LocalDateTime lostTime;

    /** 联系方式（必填）：拾到者/管理员联系失主用 */
    @NotBlank(message = "联系方式不能为空")
    private String contactInfo;

    /** 启事图片 URL 列表（可选）：物品参考图，便于比对 */
    private List<String> images;
}
