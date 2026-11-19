package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 寻物启事实体，对应数据库 lost_notice 表
 *
 * <p>存储「丢失物品」的发布信息：失主描述丢失物品的特征、时间地点与联系方式，
 * 供捡拾者浏览比对，也可被匹配算法用于和招领物品(found_item)做特征匹配。</p>
 *
 * <p>autoResultMap = true：images 字段以 JSON 存储，需要自动生成
 * 带 JacksonTypeHandler 的 resultMap 完成与 List 的互转。</p>
 */
@Data
@TableName(value = "lost_notice", autoResultMap = true)
public class LostNotice {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 启事标题（如「丢失一卡通一张」） */
    private String title;

    /** 物品分类（与招领物品的分类体系一致，用于匹配） */
    private String category;

    /** 物品详细描述（特征、颜色、内含物等） */
    private String description;

    /** 丢失地点 */
    private String lostLocation;

    /** 丢失时间 */
    private LocalDateTime lostTime;

    /** 联系方式（电话/QQ/微信等，仅登录用户可见或按需脱敏） */
    private String contactInfo;

    /** 物品照片 URL 列表，数据库以 JSON 存储 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /** 启事状态（如正常/已下架/已匹配关闭） */
    private Integer status;

    /** 发布者（失主）用户 ID */
    private Long publisherId;

    /** 匹配到的招领物品 ID：系统/人工匹配成功后回填，提示失主去认领 */
    private Long matchedItemId;

    /** 下架原因：管理员处理举报后强制下架时填写 */
    private String takedownReason;

    /** 下架时间 */
    private LocalDateTime takedownAt;

    /** 执行下架的管理员用户 ID */
    private Long takedownBy;

    /** 申诉状态：发布者对下架结果申诉的处理进度 */
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
