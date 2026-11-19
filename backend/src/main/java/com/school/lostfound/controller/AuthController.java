package com.school.lostfound.controller;

import com.school.lostfound.dto.LoginRequest;
import com.school.lostfound.dto.RegisterRequest;
import com.school.lostfound.dto.UpdatePasswordRequest;
import com.school.lostfound.dto.UpdateProfileRequest;
import com.school.lostfound.service.UserService;
import com.school.lostfound.vo.LoginVO;
import com.school.lostfound.vo.Result;
import com.school.lostfound.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 认证与个人账号接口（/api/auth）—— 系统的"大门"和"个人中心"。
 * <p>
 * 为什么需要它：系统的所有操作都建立在"知道你是谁"的基础上。
 * 本 Controller 负责账号生命周期中最基础的几件事：
 * 1. 注册 / 登录：校验学号唯一性、密码加密（BCrypt），登录成功后签发 JWT 令牌，
 *    此后前端每次请求都在 Header 中携带该令牌，由 JwtAuthenticationFilter 解析身份；
 * 2. 退出登录：JWT 是无状态令牌，服务端不存 session，因此登出只需前端删除本地令牌，
 *    这里返回一个成功响应以保持接口语义完整；
 * 3. 个人资料：查看 / 修改自己的信息、修改密码。
 * <p>
 * 谁调用：前端登录注册页、个人中心页。除 /register、/login 外，
 * 其余接口都需要登录（由 Spring Security 过滤器链统一拦截），
 * 用户身份从 Authentication 中取（JWT 解析结果），而非信任前端传参，防止越权。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册。
     * 路由：POST /api/auth/register（免登录，白名单接口）
     * 做什么：校验学号/用户名唯一、加密存密码、创建账号并直接签发 JWT（注册即登录）。
     * 谁调用：前端注册页。
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    /**
     * 用户登录。
     * 路由：POST /api/auth/login（免登录，白名单接口）
     * 做什么：校验账号密码，成功则签发 JWT 令牌（内含用户 ID 与角色）。
     * 谁调用：前端登录页。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    /**
     * 退出登录。
     * 路由：POST /api/auth/logout
     * 做什么：JWT 无状态，服务端无需销毁 session，直接返回成功，
     * 实际登出动作（清除本地令牌）由前端完成。
     * 谁调用：前端导航栏的"退出"按钮。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }

    /**
     * 查询当前登录用户信息。
     * 路由：GET /api/auth/me（需登录）
     * 做什么：从 JWT 解析出的身份（Authentication）中取用户 ID，返回脱敏后的个人资料；
     * 前端刷新页面后靠它恢复登录态。
     * 谁调用：前端路由守卫 / 顶部用户信息展示。
     */
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getCurrentUser(userId));
    }

    /**
     * 修改个人资料。
     * 路由：PUT /api/auth/profile（需登录）
     * 做什么：更新昵称、头像等资料；用户 ID 取自 JWT 而非请求体，保证只能改自己。
     * 谁调用：前端个人中心-资料编辑页。
     */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(Authentication authentication,
                                        @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.updateProfile(userId, request));
    }

    /**
     * 修改密码。
     * 路由：PUT /api/auth/password（需登录）
     * 做什么：验证旧密码正确后更新为新密码（@Valid 触发 DTO 上的格式校验）。
     * 谁调用：前端个人中心-安全设置页。
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(Authentication authentication,
                                       @Valid @RequestBody UpdatePasswordRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updatePassword(userId, request);
        return Result.success("密码修改成功", null);
    }
}
