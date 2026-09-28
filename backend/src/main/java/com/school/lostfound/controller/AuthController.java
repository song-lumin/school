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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getCurrentUser(userId));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(Authentication authentication,
                                        @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(Authentication authentication,
                                       @Valid @RequestBody UpdatePasswordRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updatePassword(userId, request);
        return Result.success("密码修改成功", null);
    }
}
