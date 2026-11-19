package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditQueryService;
import com.school.lostfound.vo.CertificateVO;
import com.school.lostfound.vo.LeaderboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 诚信积分查询服务实现类（CreditQueryService 接口的实现）。
 *
 * <p>功能：负责积分体系的所有"读操作"——查个人分值、分页查个人流水、
 * 排行榜（含隐私脱敏与个人排名）、诚信证明（光荣行为证书）生成、排行榜展示开关设置。
 *
 * <p>为什么与 CreditService（写服务）拆分成两个类：
 * 写操作集中在 CreditService 保证事务一致性，读操作（本类）无事务、
 * 以查询组装为主，读写职责分离，符合单一职责原则，也便于答辩时分层讲解。
 */
@Service
@RequiredArgsConstructor
public class CreditQueryServiceImpl implements CreditQueryService {

    private final UserMapper userMapper;
    private final CreditLogMapper creditLogMapper;

    /**
     * 查询指定用户的当前诚信积分。
     *
     * @param userId 用户 ID
     * @return 当前积分值
     * @throws BusinessException 用户不存在时返回 404
     */
    @Override
    public Integer getMyCredit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user.getCreditScore();
    }

    /**
     * 分页查询指定用户的积分流水（个人账本）。
     *
     * <p>按创建时间倒序排列：最新的变动排在最前，符合用户查看账单的习惯。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     */
    @Override
    public IPage<CreditLog> listMyLogs(Long userId, int page, int size) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getCreatedAt);
        return creditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 查询诚信积分排行榜（Top N）+ 当前用户的排名。
     *
     * <p>业务规则（核心是隐私保护）：
     * - 榜单只统计普通用户（USER 角色），管理员不参与积分体系；
     * - 积分为 0 及以下的用户不上榜；
     * - 普通用户只能看到【同意公开】(allowLeaderboard=1) 的用户；管理员可看全部；
     * - 隐私脱敏：非管理员看到的他人姓名做掩码处理（如"张三"显示为"张*"），
     *   管理员可看完整真实姓名；
     * - 个人排名：统计"同意公开且积分比我高"的人数 + 1；积分为 0 或未同意公开则排名为 null。
     *
     * @param currentUserId 当前登录用户 ID（决定可见范围与个人排名）
     * @param top           榜单条数（取前几名）
     */
    @Override
    public LeaderboardVO getLeaderboard(Long currentUserId, int top) {
        // 先查当前用户，用于判断角色（管理员能看到全部用户和真实姓名）
        User me0 = userMapper.selectById(currentUserId);
        boolean isAdmin = me0 != null && me0.getRole() != UserRole.USER;

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, UserRole.USER)   // 只统计普通用户
                .gt(User::getCreditScore, 0);      // 过滤 0 分及以下
        if (!isAdmin) {
            // 普通用户视角：只展示同意上榜的人（隐私开关）
            wrapper.eq(User::getAllowLeaderboard, 1);
        }
        // last("LIMIT n")：直接在 SQL 末尾拼接 LIMIT，只取前 top 名，避免全量查询
        wrapper.orderByDesc(User::getCreditScore).last("LIMIT " + top);

        List<User> users = userMapper.selectList(wrapper);

        // 姓名可见性：管理员看真实姓名，普通用户看脱敏姓名
        boolean canSeeRealName = isAdmin;

        List<LeaderboardVO.Entry> entries = users.stream()
                .map(u -> new LeaderboardVO.Entry(u.getId(), u.getUsername(), canSeeRealName ? u.getRealName() : maskName(u.getRealName()), u.getCreditScore()))
                .collect(Collectors.toList());

        Integer myRank = null;
        Integer myScore = 0;

        User currentUser = me0;
        if (currentUser != null && currentUser.getRole() == UserRole.USER) {
            myScore = currentUser.getCreditScore();
            if (currentUser.getAllowLeaderboard() == 1) {
                // 计算个人排名：统计"同普通用户、同意公开、积分严格大于我"的人数，
                // 排名 = 该人数 + 1（比 selectList 全表再数下标省资源）
                LambdaQueryWrapper<User> rankWrapper = new LambdaQueryWrapper<>();
                rankWrapper.eq(User::getRole, UserRole.USER)
                        .eq(User::getAllowLeaderboard, 1)
                        .gt(User::getCreditScore, myScore);
                long higherCount = userMapper.selectCount(rankWrapper);
                if (myScore > 0) {
                    myRank = (int) higherCount + 1; // 有多少人在我前面，我的名次就是人数+1
                }
            }
        }

        return new LeaderboardVO(entries, myRank, myScore);
    }

    /**
     * 更新用户的排行榜公开展示开关（隐私设置）。
     *
     * <p>业务规则：入参为 1 视为同意公开，其余（含 null）一律按 0（不公开）处理，保守安全。
     *
     * @param userId           用户 ID
     * @param allowLeaderboard 1=同意展示在排行榜，0=不展示
     */
    @Override
    @Transactional
    public void updateLeaderboardSetting(Long userId, Integer allowLeaderboard) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 三目收敛：只接受 1，其余值统一落为 0，避免脏数据入库
        user.setAllowLeaderboard(allowLeaderboard != null && allowLeaderboard == 1 ? 1 : 0);
        userMapper.updateById(user);
    }

    /**
     * 生成用户诚信证明（光荣行为证书）数据。
     *
     * <p>功能：汇总该用户所有正向积分行为（巡检通过 +1、成功帮助失主领取 +3/+1），
     * 输出姓名、学号、当前总分、各行为类型统计和光荣记录明细，前端渲染成证书。
     *
     * <p>业务规则：
     * - 管理员不参与积分体系，直接 403 拒绝生成；
     * - 类型统计把"正负变动分开计数"（如 ROLLBACK+ 与 ROLLBACK- 是两类），便于展示行为构成。
     *
     * @param userId 用户 ID
     * @return 证书 VO（姓名、学号、总分、行为统计、光荣记录列表、生成时间）
     * @throws BusinessException 用户不存在(404)或为管理员(403)
     */
    @Override
    public CertificateVO getCertificate(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (user.getRole() != UserRole.USER) {
            throw new BusinessException(403, "管理员不参与诚信积分体系，无需诚信证明");
        }

        // 拉取该用户全部积分流水（证书需全量统计，不分页）
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getCreatedAt);
        List<CreditLog> logs = creditLogMapper.selectList(wrapper);

        // 获得方式统计（正负变动分开计数）
        // 分组 key = 操作类型 + 变动符号（如 "CHECK_ISSUE+"/"ROLLBACK-"），value = 出现次数
        Map<String, Long> statsByType = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getOperationType() + (log.getChangeAmount() >= 0 ? "+" : "-"),
                        Collectors.counting()));

        // 光荣行为记录：仅正向发放（巡检+1、领取+3/+1）
        // 过滤出加分流水 -> 按时间倒序（最新行为在前）-> 转证书明细 VO
        List<CertificateVO.CertificateRecord> records = logs.stream()
                .filter(log -> log.getChangeAmount() != null && log.getChangeAmount() > 0)
                .sorted(Comparator.comparing(CreditLog::getCreatedAt).reversed())
                .map(CertificateVO.CertificateRecord::fromLog)
                .collect(Collectors.toList());

        return new CertificateVO(user.getRealName(), user.getStudentId(),
                user.getCreditScore(), statsByType, records, LocalDateTime.now());
    }

    /**
     * 姓名脱敏：保留首字符，其余替换为星号（如"张三丰" -> "张**"）。
     *
     * @param realName 真实姓名
     * @return 脱敏后的姓名；单字姓名或空值原样返回
     */
    private String maskName(String realName) {
        if (realName == null || realName.length() <= 1) {
            return realName;
        }
        return realName.charAt(0) + "*".repeat(realName.length() - 1);
    }
}
