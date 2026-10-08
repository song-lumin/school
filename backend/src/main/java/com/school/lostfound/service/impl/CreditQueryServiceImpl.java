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

@Service
@RequiredArgsConstructor
public class CreditQueryServiceImpl implements CreditQueryService {

    private final UserMapper userMapper;
    private final CreditLogMapper creditLogMapper;

    @Override
    public Integer getMyCredit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user.getCreditScore();
    }

    @Override
    public IPage<CreditLog> listMyLogs(Long userId, int page, int size) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getCreatedAt);
        return creditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public LeaderboardVO getLeaderboard(Long currentUserId, int top) {
        // 光荣榜仅展示普通用户，管理员不参与积分体系
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, UserRole.USER)
                .eq(User::getAllowLeaderboard, 1)
                .gt(User::getCreditScore, 0)
                .orderByDesc(User::getCreditScore)
                .last("LIMIT " + top);

        List<User> users = userMapper.selectList(wrapper);

        List<LeaderboardVO.Entry> entries = users.stream()
                .map(u -> new LeaderboardVO.Entry(u.getId(), maskName(u.getRealName()), u.getCreditScore()))
                .collect(Collectors.toList());

        Integer myRank = null;
        Integer myScore = 0;

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser != null && currentUser.getRole() == UserRole.USER) {
            myScore = currentUser.getCreditScore();
            if (currentUser.getAllowLeaderboard() == 1) {
                LambdaQueryWrapper<User> rankWrapper = new LambdaQueryWrapper<>();
                rankWrapper.eq(User::getRole, UserRole.USER)
                        .eq(User::getAllowLeaderboard, 1)
                        .gt(User::getCreditScore, myScore);
                long higherCount = userMapper.selectCount(rankWrapper);
                if (myScore > 0) {
                    myRank = (int) higherCount + 1;
                }
            }
        }

        return new LeaderboardVO(entries, myRank, myScore);
    }

    @Override
    @Transactional
    public void updateLeaderboardSetting(Long userId, Integer allowLeaderboard) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setAllowLeaderboard(allowLeaderboard != null && allowLeaderboard == 1 ? 1 : 0);
        userMapper.updateById(user);
    }

    @Override
    public CertificateVO getCertificate(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (user.getRole() != UserRole.USER) {
            throw new BusinessException(403, "管理员不参与诚信积分体系，无需诚信证明");
        }

        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getCreatedAt);
        List<CreditLog> logs = creditLogMapper.selectList(wrapper);

        // 获得方式统计（正负变动分开计数）
        Map<String, Long> statsByType = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getOperationType() + (log.getChangeAmount() >= 0 ? "+" : "-"),
                        Collectors.counting()));

        // 光荣行为记录：仅正向发放（巡检+1、领取+3/+1）
        List<CertificateVO.CertificateRecord> records = logs.stream()
                .filter(log -> log.getChangeAmount() != null && log.getChangeAmount() > 0)
                .sorted(Comparator.comparing(CreditLog::getCreatedAt).reversed())
                .map(CertificateVO.CertificateRecord::fromLog)
                .collect(Collectors.toList());

        return new CertificateVO(user.getRealName(), user.getStudentId(),
                user.getCreditScore(), statsByType, records, LocalDateTime.now());
    }

    private String maskName(String realName) {
        if (realName == null || realName.length() <= 1) {
            return realName;
        }
        return realName.charAt(0) + "*".repeat(realName.length() - 1);
    }
}
