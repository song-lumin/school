package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 招领物品实体，对应数据库 found_item 表
 *
 * <p>存储「捡到物品」的登记信息，是系统的核心表：捡拾者/站点登记物品，
 * 失主浏览或通过寻物启事匹配后发起认领申请。物品生命周期状态见 {@link com.school.lostfound.enums.ItemStatus}。</p>
 *
 * <p>autoResultMap = true：因为 images 字段是 List 类型、以 JSON 存储在数据库，
 * 需要 MyBatis-Plus 自动生成带 JacksonTypeHandler 的 resultMap 才能正确反序列化。</p>
 */
@Data
@TableName(value = "found_item", autoResultMap = true)
public class FoundItem {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物品标题（如「黑色钱包一个」） */
    private String title;

    /** 物品分类（如 电子产品、证件卡类） */
    private String category;

    /** 物品详细描述 */
    private String description;

    /** 捡到地点 */
    private String foundLocation;

    /** 捡到时间 */
    private LocalDateTime foundTime;

    /** 物品照片 URL 列表，数据库以 JSON 字符串存储，经 JacksonTypeHandler 与 List 互转 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /** 认领验证问题：管理员设置的只有失主能答上的问题（如「钱包里有什么卡」） */
    private String claimQuestion;

    /** 验证问题的标准答案，审核认领时人工比对，不对普通用户展示 */
    private String referenceAnswer;

    /** 是否易腐物品（1 是 / 0 否）：易腐物品（如雨伞外的食物）会更早过期清理 */
    private Integer perishable;

    /** 物品状态，见 ItemStatus 枚举（1 公开待认领 / 2 认领中 / 3 已取件等） */
    private Integer itemStatus;

    /** 登记该物品的捡拾者用户 ID（NULL 表示由站点管理员代登记） */
    private Long founderId;

    /** 实际捡拾者 ID：与 founderId 区分，用于把信用分加给真正捡到东西的人 */
    private Long actualFounderId;

    /** 物品当前存放的失物收集点 ID */
    private Long dropPointId;

    /** 来源寻物启事 ID：若物品是由某条寻物启事「转发认领」生成的，记录其来源 */
    private Long forwardedNoticeId;

    /** 执行转发操作的用户 ID */
    private Long forwarderId;

    /** 发布（公开上架）时间 */
    private LocalDateTime publishedAt;

    /** 被认领完成（取件）时间 */
    private LocalDateTime claimedAt;

    /** 是否已发送过期预警提醒（0/1），配合定时任务 ExpireWarningTask 使用，防止重复提醒 */
    private Integer expireWarningSent;

    /** 下架原因：管理员强制下架（违规、信息有误等）时填写 */
    private String takedownReason;

    /** 下架时间 */
    private LocalDateTime takedownAt;

    /** 执行下架的管理员用户 ID */
    private Long takedownBy;

    /** 申诉状态：被下架的发布者可申诉，记录申诉处理进度 */
    private Integer appealStatus;

    /** 申诉理由 */
    private String appealReason;

    /** 申诉时间 */
    private LocalDateTime appealAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
