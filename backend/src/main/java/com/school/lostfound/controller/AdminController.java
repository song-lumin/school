package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.service.AdminService;
import com.school.lostfound.vo.DashboardVO;
import com.school.lostfound.vo.ExpireWarningVO;
import com.school.lostfound.vo.Result;
import com.school.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<IPage<UserVO>> listUsers(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listUsers(keyword, role, status, page, size));
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        adminService.updateUserRole(id, body.get("role"));
        return Result.success("角色更新成功", null);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestBody Map<String, Integer> body) {
        adminService.updateUserStatus(id, body.get("status"));
        return Result.success("状态更新成功", null);
    }

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(adminService.getDashboard());
    }

    @GetMapping("/expire-warnings")
    public Result<List<ExpireWarningVO>> getExpireWarnings() {
        return Result.success(adminService.getExpireWarnings());
    }

    @PostMapping("/credits/rollback")
    public Result<Void> rollbackCredit(@RequestBody Map<String, Object> request,
                                       Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        adminService.rollbackCredit(request, operatorId);
        return Result.success("积分回滚成功", null);
    }
}
