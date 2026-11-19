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

/**
 * 管理员后台接口（/api/admin）—— 给管理员角色用的"管理控制台"。
 * <p>
 * 为什么需要它：普通用户的日常操作（发布、认领）之外，平台还需要一个
 * 人工治理入口，本 Controller 集中了管理端四类能力：
 * 1. 用户治理：分页检索用户、调整角色（USER/ADMIN）、封禁/解封账号；
 * 2. 数据大盘：物品量、认领成功率等统计指标，供图表/数字卡片展示；
 * 3. 超期预警：查看定时任务（ExpireWarningTask）标记的临期/超期物品，跟进线下处置；
 * 4. 信用仲裁：人工调整用户信用分、回滚误发的分数、查询某用户全部信用流水。
 * <p>
 * 权限：接口级权限由 SecurityConfig 的过滤器链 + @PreAuthorize 控制，
 * 非管理员访问返回 403。用户身份一律从 JWT（Authentication）解析，不信任前端传参。
 * <p>
 * 谁调用：前端管理员后台（用户管理、数据大盘、超期预警、信用管理等页面）。
 */
@RestController    //类里面所有接口方法，返回的东西都会自动转成 JSON 给前端
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 分页查询用户列表。
     * 路由：GET /api/admin/users（需管理员）
     * 做什么：按关键词（用户名/学号模糊匹配）、角色、账号状态筛选用户，分页返回（脱敏 VO）。
     * 谁调用：前端后台-用户管理页的列表与搜索。
     */
    @GetMapping("/users")
    public Result<IPage<UserVO>> listUsers(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listUsers(keyword, role, status, page, size));
    }

    /**
     * 修改用户角色。
     * 路由：PUT /api/admin/users/{id}/role（需管理员）
     * 做什么：把目标用户升为 ADMIN 或降为 USER；注意该用户旧 JWT 里仍带原角色，
     * 重新登录后才生效（JWT 无状态）。
     * 谁调用：前端后台-用户管理页的角色下拉操作。
     */
    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        adminService.updateUserRole(id, body.get("role"));
        return Result.success("角色更新成功", null);
    }

    /**
     * 封禁 / 解封账号。
     * 路由：PUT /api/admin/users/{id}/status（需管理员）
     * 做什么：status=0 封禁（禁止登录）、1 解封；用于处理违规行为。
     * 谁调用：前端后台-用户管理页的封禁/解封按钮。
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestBody Map<String, Integer> body) {
        adminService.updateUserStatus(id, body.get("status"));
        return Result.success("状态更新成功", null);
    }

    /**
     * 管理端数据大盘。
     * 路由：GET /api/admin/dashboard（需管理员）
     * 做什么：一次性返回物品总数/各状态数量、认领成功率、用户数、今日新增等统计指标。
     * 谁调用：前端后台首页的图表与数字卡片。
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(adminService.getDashboard());
    }

    /**
     * 超期预警列表。
     * 路由：GET /api/admin/expire-warnings（需管理员）
     * 做什么：返回定时任务 ExpireWarningTask 每日标记的"临近超期/已超期"物品，
     * 提醒管理员跟进线下处置（催促领取、转投放点保管等）。
     * 谁调用：前端后台-超期预警页面。
     */
    @GetMapping("/expire-warnings")
    public Result<List<ExpireWarningVO>> getExpireWarnings() {
        return Result.success(adminService.getExpireWarnings());
    }

    /**
     * 人工调整用户信用分。
     * 路由：PUT /api/admin/users/{id}/credit（需管理员）
     * 做什么：把目标用户信用分直接调整为 newScore（非增减），用于争议仲裁后的强制矫正；
     * 调整差额会写入 CreditLog 流水，记录操作人便于审计。
     * 谁调用：前端后台-信用管理页。
     */
    @PutMapping("/users/{id}/credit")
    public Result<Void> adjustCredit(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        Integer newScore = body.get("newScore") == null ? null : Integer.valueOf(body.get("newScore").toString());
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        adminService.adjustCredit(id, newScore, reason, operatorId);
        return Result.success("积分调整成功", null);
    }

    /**
     * 查询某用户的信用流水。
     * 路由：GET /api/admin/users/{id}/credit-logs（需管理员）
     * 做什么：返回该用户全部信用变动记录（加分/扣分/调整/回滚），供核查争议时追溯。
     * 谁调用：前端后台-用户信用详情弹窗。
     */
    @GetMapping("/users/{id}/credit-logs")
    public Result<java.util.List<com.school.lostfound.entity.CreditLog>> userCreditLogs(@PathVariable Long id) {
        return Result.success(adminService.userCreditLogs(id));
    }

    /**
     * 信用分回滚（冲正）。
     * 路由：POST /api/admin/credits/rollback（需管理员）
     * 做什么：撤销一次已发放的信用分，用于驳回认领、纠正误判等场景；
     * 记录操作管理员 ID，保证每次冲正可审计。
     * 谁调用：前端后台-信用管理页的"回滚"操作。
     */
    @PostMapping("/credits/rollback")
    public Result<Void> rollbackCredit(@RequestBody Map<String, Object> request,
                                       Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        adminService.rollbackCredit(request, operatorId);
        return Result.success("积分回滚成功", null);
    }
}
