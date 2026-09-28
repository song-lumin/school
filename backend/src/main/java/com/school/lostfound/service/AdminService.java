package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.entity.User;
import com.school.lostfound.vo.DashboardVO;
import com.school.lostfound.vo.ExpireWarningVO;
import com.school.lostfound.vo.UserVO;

import java.util.List;
import java.util.Map;

public interface AdminService {
    IPage<UserVO> listUsers(String keyword, String role, Integer status, int page, int size);

    void updateUserRole(Long userId, String role);

    void updateUserStatus(Long userId, Integer status);

    DashboardVO getDashboard();

    List<ExpireWarningVO> getExpireWarnings();

    void rollbackCredit(Map<String, Object> request, Long operatorId);
}
