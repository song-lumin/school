package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户注册请求 DTO —— 校园身份注册时提交的参数。
 *
 * 业务场景：用户在"注册"页面填写账号信息并提交，后端据此创建 User 记录
 * （默认普通用户角色），密码经 BCrypt 加密后落库。
 *
 * 为什么需要它：注册请求只包含账号相关的部分字段，且需要携带校验规则
 * （如手机号正则），不能直接用 User Entity 接收，否则会暴露密码加密、
 * 角色、信用分等不该由前端传入的字段。
 */
@Data
public class RegisterRequest {
    /** 登录用户名（必填，唯一） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码（必填，明文传入，后端加密存储） */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 真实姓名（必填）：用于线下认领时的身份核对 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 学号/工号（必填）：校园身份标识，用于核实校内人员身份 */
    @NotBlank(message = "学号/工号不能为空")
    private String studentId;

    /** 手机号：正则校验国内 11 位手机号格式 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（可选）：通知渠道之一 */
    private String email;
}
