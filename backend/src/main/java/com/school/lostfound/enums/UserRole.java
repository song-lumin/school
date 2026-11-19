package com.school.lostfound.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 *
 * <p>对应 user 表的 role 字段（以枚举名字符串存储）。系统共有三级角色，
 * 权限从低到高：</p>
 * <ul>
 *   <li>USER - 普通用户：发布寻物启事、申请认领、上传图片等日常操作</li>
 *   <li>POINT_ADMIN - 站点管理员：管理本失物收集点的物品登记、审核认领、发放物品</li>
 *   <li>SYS_ADMIN - 系统管理员：全局管理，包括站点管理、用户管理、争议仲裁、风控</li>
 * </ul>
 *
 * <p>该枚举被 Spring Security 用于构造角色权限（ROLE_XXX），
 * SecurityConfig 中的 hasAnyRole("SYS_ADMIN","POINT_ADMIN") 即依赖这些名字。</p>
 */
@Getter
public enum UserRole {
    USER("普通用户"),
    POINT_ADMIN("站点管理员"),
    SYS_ADMIN("系统管理员");

    /** 角色的中文描述，用于前端下拉框展示、日志打印等场景 */
    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}
