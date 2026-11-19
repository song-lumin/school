package com.school.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 诚信积分排行榜 VO
 *
 * 功能：封装排行榜页面的完整数据：榜单条目列表 + 当前用户自己的
 * 名次和分数。后端一次返回"榜单 + 我的名次"，前端无需再单独请求
 * "我在第几名"。
 *
 * 为什么需要：榜单条目（Entry）与"我的名次"结构不同，用外层 VO
 * 组合二者；同时尊重用户隐私——allowLeaderboard=false 的用户
 * 不会出现在 entries 中（该过滤在 Service 层完成）。
 *
 * 使用位置：前台"诚信排行榜"页面（UserService/积分排行接口），
 * Redis ZSet 缓存排行数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardVO {
    /** 榜单条目（按分数降序） */
    private List<Entry> entries;
    /** 当前用户的名次（未上榜则为 null） */
    private Integer myRank;
    /** 当前用户的诚信分 */
    private Integer myScore;

    /**
     * 榜单单条记录：展示用户及其诚信分。
     */
    @Data
    @AllArgsConstructor
    public static class Entry {
        private Long userId;
        /** 用户名 */
        private String username;
        /** 真实姓名 */
        private String realName;
        /** 诚信积分 */
        private Integer creditScore;
    }
}
