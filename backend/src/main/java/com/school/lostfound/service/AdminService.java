package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.entity.User;
import com.school.lostfound.vo.DashboardVO;
import com.school.lostfound.vo.ExpireWarningVO;
import com.school.lostfound.vo.UserVO;

import java.util.List;
import java.util.Map;

/**
 * 管理员服务接口 —— 后台管理端的核心能力集合。
 * <p>
 * 业务背景：普通用户与系统自动流转解决了大部分日常场景，
 * 但仍需要管理员承担四类职责：
 * 1. 用户治理：查询、升降角色（USER/ADMIN）、封禁/解封账号；
 * 2. 数据大盘：统计物品量、认领率、用户量等关键指标，掌握平台整体运行状况；
 * 3. 超期管理：查看定时任务标记的"临近超期/已超期"物品并线下处置；
 * 4. 信用仲裁：人工调整信用分、回滚误发的分数、查询用户信用流水。
 * 只有 ADMIN 角色能调用本接口对应的接口（由 Spring Security 注解控制）。
 */
public interface AdminService {
    int l = 0;

    /**
     * 分页查询用户列表，支持关键词与条件筛选。
     *
     * @param keyword 关键词，模糊匹配用户名/学号，null 表示不过滤
     * @param role    角色筛选（USER/ADMIN），null 表示不过滤
     * @param status  账号状态筛选（正常/封禁），null 表示不过滤
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @return 用户分页列表（脱敏 VO，不含密码）
     */
    IPage<UserVO> listUsers(String keyword, String role, Integer status, int page, int size);

    /**
     * 修改用户角色（提升为管理员或降级为普通用户）。
     * 注意：修改后该用户已签发的 JWT 中仍带旧 role，重新登录后才生效。
     *
     * @param userId 目标用户 ID
     * @param role   新角色
     */
    void updateUserRole(Long userId, String role);

    /**
     * 封禁/解封账号。封禁后用户无法登录；已签发的 JWT 因无状态特性
     * 在过期前依然可用，这是无状态认证的固有折衷。
     *
     * @param userId 目标用户 ID
     * @param status 目标状态（1=正常，0=封禁）
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 汇总管理端数据大盘：物品总数/各状态数量、认领成功率、用户数、
     * 今日新增等统计指标，一次接口返回，前端渲染成图表与数字卡片。
     *
     * @return 大盘数据 VO
     */
    DashboardVO getDashboard();

    /**
     * 查询超期预警物品列表（来源：ExpireWarningTask 每天凌晨标记的数据），
     * 管理员据此跟进线下处置，避免物品"悄悄"过期无人知晓。
     *
     * @return 预警物品列表（含剩余天数等提示信息）
     */
    List<ExpireWarningVO> getExpireWarnings();

    /**
     * 回滚一次已发放的信用分（冲正）。用于管理员驳回认领、纠正误判等场景，
     * 内部会委托 CreditService.rollbackCredits 执行并记录流水。
     *
     * @param request    回滚参数（物品 ID、认领申请 ID、原因等，用 Map 承载灵活字段）
     * @param operatorId 操作管理员 ID，便于审计
     */
    void rollbackCredit(Map<String, Object> request, Long operatorId);

    /**
     * 人工将某用户的信用分直接调整为指定值（而非增减）。
     * 用于处理争议后的强制矫正；调整前后差额会写入信用流水，保证可追溯。
     *
     * @param userId   目标用户 ID
     * @param newScore 调整后的分数
     * @param reason   调整原因，必填，展示在流水与用户端通知中
     * @param operatorId 操作管理员 ID
     */
    void adjustCredit(Long userId, Integer newScore, String reason, Long operatorId);

    /**
     * 查询某用户的完整信用流水（不分页，管理端核查用）。
     *
     * @param userId 用户 ID
     * @return 该用户全部信用变动记录，按时间排序
     */
    java.util.List<com.school.lostfound.entity.CreditLog> userCreditLogs(Long userId);
}
