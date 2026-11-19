package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.dto.LoginRequest;
import com.school.lostfound.dto.RegisterRequest;
import com.school.lostfound.dto.UpdatePasswordRequest;
import com.school.lostfound.dto.UpdateProfileRequest;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.UserService;
import com.school.lostfound.utils.JwtUtil;
import com.school.lostfound.vo.LoginVO;
import com.school.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final CreditLogMapper creditLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public LoginVO register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(409, "用户名已存在");
        }

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStudentId, request.getStudentId());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(409, "学号/工号已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStudentId(request.getStudentId());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(UserRole.USER);
        user.setCreditScore(0);
        user.setStatus(1);
        user.setAllowLeaderboard(1);

        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        UserVO userVO = convertToVO(user);

        return new LoginVO(token, userVO);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        UserVO userVO = convertToVO(user);

        return new LoginVO(token, userVO);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = getUserById(userId);
        return convertToVO(user);
    }

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        userMapper.updateById(user);
        return convertToVO(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (user != null && user.getId() != null) {
            // 误领次数单独成字段：MISCLAIM_COUNT 是 0 分计次凭证，已从积分明细里过滤掉，
            // 想展示误领情况只能在这里统计
            long misclaimCount = creditLogMapper.selectCount(new LambdaQueryWrapper<CreditLog>()
                    .eq(CreditLog::getUserId, user.getId())
                    .eq(CreditLog::getOperationType, "MISCLAIM_COUNT"));
            vo.setMisclaimCount((int) misclaimCount);
        }
        return vo;
    }
}
