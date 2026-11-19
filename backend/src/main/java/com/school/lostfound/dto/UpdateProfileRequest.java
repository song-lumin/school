package com.school.lostfound.dto;

import lombok.Data;

/**
 * 更新个人资料请求 DTO —— 已登录用户在"个人中心"修改基本资料时提交。
 *
 * 业务场景：用户修改真实姓名、手机号、邮箱后，前端将这些字段提交到
 * 资料更新接口，后端将其映射到当前登录用户（JWT 解析出的身份）的 User 记录。
 *
 * 为什么需要它：资料更新只允许改动这三个"非敏感、可自助修改"的字段，
 * 用户名、角色、信用分等字段不在此 DTO 中，从接口层面杜绝越权修改
 * （Mass Assignment 防护）。
 */
@Data
public class UpdateProfileRequest {
    /** 真实姓名（可选更新）：线下认领核对身份用 */
    private String realName;
    /** 手机号（可选更新）：联系方式 */
    private String phone;
    /** 邮箱（可选更新）：通知渠道 */
    private String email;
}
