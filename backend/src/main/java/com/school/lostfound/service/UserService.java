package com.school.lostfound.service;

import com.school.lostfound.dto.LoginRequest;
import com.school.lostfound.dto.RegisterRequest;
import com.school.lostfound.dto.UpdatePasswordRequest;
import com.school.lostfound.dto.UpdateProfileRequest;
import com.school.lostfound.entity.User;
import com.school.lostfound.vo.LoginVO;
import com.school.lostfound.vo.UserVO;

public interface UserService {
    LoginVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    UserVO getCurrentUser(Long userId);

    UserVO updateProfile(Long userId, UpdateProfileRequest request);

    void updatePassword(Long userId, UpdatePasswordRequest request);

    User getUserById(Long userId);
}
