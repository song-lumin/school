package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求 DTO —— 已登录用户在"个人中心-安全设置"中修改密码时提交。
 *
 * 业务场景：前端将旧密码与新密码 PUT/POST 到修改密码接口，后端先校验
 * 旧密码是否与数据库中 BCrypt 密文匹配，匹配成功才更新为新密码的加密结果。
 *
 * 为什么需要它：密码修改必须验证旧密码以防会话被劫持后恶意改密，
 * 用独立 DTO 明确"旧 + 新"两个字段的语义，且确保密码字段不会在其他
 * 接口中被意外复用或泄露。
 */
@Data
public class UpdatePasswordRequest {
    /** 旧密码（必填）：用于验证当前操作者是账号本人 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码（必填）：后端加密后覆盖原密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
