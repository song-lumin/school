package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.school.lostfound.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应数据库 user 表
 *
 * <p>存储所有注册用户（普通用户/站点管理员/系统管理员）的账号信息。
 * 登录认证、权限控制、信用分体系都以此表为数据来源。</p>
 *
 * <p>@Data（Lombok）自动生成 getter/setter/toString 等方法；
 * @TableName 声明实体与数据库表的映射关系。</p>
 */
@Data
@TableName("user")
public class User {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名，唯一 */
    private String username;

    /** 登录密码（BCrypt 加密后的密文，绝不存明文） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 学号（校园场景的实名凭证） */
    private String studentId;

    /** 联系手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 角色：USER 普通用户 / POINT_ADMIN 站点管理员 / SYS_ADMIN 系统管理员 */
    private UserRole role;

    /** 信用分：认领成功加分、恶意认领/违约扣分，分数影响能否发起认领 */
    private Integer creditScore;

    /** 账号状态：如 1 正常、0 被禁用 */
    private Integer status;

    /** 是否允许出现在信用排行榜（用户隐私开关，0/1） */
    private Integer allowLeaderboard;

    /** 创建时间，插入时由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
