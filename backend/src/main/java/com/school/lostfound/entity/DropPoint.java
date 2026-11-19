package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失物收集点实体，对应数据库 drop_point 表
 *
 * <p>「失物收集点」是线下实体站点（如图书馆服务台、宿管中心），
 * 捡到的物品统一交到这里保管，失主也到这里取件。本表存储站点的
 * 基础信息、负责管理员及是否配备摄像头（供监控调阅申请用）。</p>
 */
@Data
@TableName("drop_point")
public class DropPoint {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 站点名称（如「图书馆服务台」） */
    private String name;

    /** 站点详细位置（如「图书馆一楼大厅东侧」） */
    private String location;

    /** 该站点的负责管理员用户 ID（角色为 POINT_ADMIN 的用户） */
    private Long adminId;

    /** 是否配备摄像头（0/1）：有摄像头的站点才支持发起监控调阅申请 */
    private Integer hasCamera;

    /** 摄像头补充说明（如摄像头编号、覆盖范围） */
    private String cameraInfo;

    /** 站点状态（如 1 运营中 / 0 已停用） */
    private Integer status;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
