package com.school.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功响应 VO
 *
 * 功能：封装登录/注册成功后返回给前端的数据：JWT token + 用户信息。
 * 前端将 token 存入本地（localStorage），后续请求在 Authorization
 * 头中携带；用户信息用于初始化全局状态（头像、角色、诚信分等），
 * 无需登录后再单独请求一次用户接口。
 *
 * 使用位置：登录接口、注册接口的返回值（UserController）。
 */
@Data
@AllArgsConstructor
public class LoginVO {
    /** JWT 访问令牌 */
    private String token;
    /** 当前用户信息（不含密码等敏感字段，见 UserVO） */
    private UserVO user;
}
