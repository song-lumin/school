package com.school.lostfound.vo;

import lombok.Data;

import java.util.Map;

/**
 * 管理员数据看板 VO
 *
 * 功能：一次性聚合系统整体统计数据返回给前端大屏/看板页面，
 * 包括物品总量、各状态数量、用户数、认领情况以及分类分布、
 * 按日认领趋势。
 *
 * 为什么需要：看板需要十几项统计指标，若前端逐个请求会多次往返；
 * 由后端一次查询聚合封装成 VO，接口只需一次调用。
 *
 * 使用位置：管理员后台首页数据看板（AdminService.getDashboard）。
 */
@Data
public class DashboardVO {
    /** 物品总数 */
    private Long totalItems;
    /** 公开展示中的物品数 */
    private Long publicItems;
    /** 已被认领/领取的物品数 */
    private Long pickedUpItems;
    /** 注册用户总数 */
    private Long totalUsers;
    /** 认领申请总数 */
    private Long totalClaims;
    /** 认领成功数 */
    private Long successClaims;
    /** 按物品分类统计数量（key=分类名，value=数量） */
    private Map<String, Long> itemsByCategory;
    /** 按日期统计认领数量（key=日期字符串，value=数量），用于趋势图 */
    private Map<String, Long> claimsByDay;
}
