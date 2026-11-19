package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.vo.CertificateVO;
import com.school.lostfound.vo.LeaderboardVO;

/**
 * 信用分查询服务接口 —— 信用激励体系的"读"侧。
 * <p>
 * 业务背景：信用分只有能被用户看见、能被比较，激励才有效。
 * 本接口提供：个人分数查询、加分/扣分流水（分页）、信用排行榜、
 * 以及"诚信证书"（把信用记录整理成可展示/可下载的荣誉凭证，
 * 学生可用于综合测评等场景）。
 * <p>
 * 注意隐私开关：排行榜默认按分数排序展示用户昵称，
 * 用户可通过 updateLeaderboardSetting 选择退出排行榜（匿名），
 * 查询端必须尊重该开关，避免隐私争议。
 */
public interface CreditQueryService {
    /**
     * 查询用户当前信用总分。
     *
     * @param userId 用户 ID
     * @return 当前总分；新用户（无任何流水）返回初始分
     */
    Integer getMyCredit(Long userId);

    /**
     * 分页查询用户的信用变动流水。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 流水分页数据（MyBatis-Plus 的 IPage），按时间倒序
     */
    IPage<CreditLog> listMyLogs(Long userId, int page, int size);

    /**
     * 查询信用排行榜（Top N）。
     *
     * @param currentUserId 当前登录用户 ID，用于在榜单中标注"我"的名次
     * @param top           榜单取前几名
     * @return 榜单数据（含排名列表 + 当前用户的名次信息）
     */
    LeaderboardVO getLeaderboard(Long currentUserId, int top);

    /**
     * 修改用户的排行榜隐私设置。
     *
     * @param userId           用户 ID
     * @param allowLeaderboard 是否允许出现在排行榜（1=允许，0=隐藏）
     */
    void updateLeaderboardSetting(Long userId, Integer allowLeaderboard);

    /**
     * 生成用户的诚信证书数据（累计帮助次数、信用分、荣誉称号等汇总）。
     *
     * @param userId 证书对应的用户 ID
     * @return 证书 VO，前端据此渲染可保存/打印的荣誉证书
     */
    CertificateVO getCertificate(Long userId);
}
