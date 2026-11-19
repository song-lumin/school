package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 诚信积分发放服务实现类（CreditService 接口的实现）。
 *
 * <p>功能：系统内所有积分"写操作"的唯一入口——
 * 1. issueCredit：发放（正数）/ 扣减（负数）积分，同步写积分流水并刷新 Redis 缓存；
 * 2. rollbackCredits：按物品维度回滚已发放的积分（幂等，防止重复扣分）。
 *
 * <p>为什么需要统一入口：积分变动必须同时满足
 * "用户表分值更新 + CreditLog 流水落库 + Redis 缓存刷新"三者一致，
 * 收敛到一个方法内用 @Transactional 保证原子性，
 * 避免各业务（巡检、认领、回滚、管理员调整）散落实现造成账实不符。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 发放/扣减诚信积分（核心写方法，所有积分变动都经过这里）。
     *
     * <p>业务规则：
     * - amount 为正表示奖励，为负表示扣减；
     * - 管理员角色（巡检管理员/系统管理员）不参与积分体系，直接跳过；
     * - 用户不存在时只记录 warn 日志不抛异常（防止上游业务因积分失败而整体回滚）。
     *
     * <p>为什么用 @Transactional：用户表加分和流水表插入必须同成功同失败，
     * 否则会出现"分变了但查不到流水"（无法对账）的脏数据。
     *
     * @param userId        目标用户 ID
     * @param amount        变动分值（正=发放，负=扣减）
     * @param operationType 操作类型（PICKUP_ISSUE / CHECK_ISSUE / ROLLBACK / ADMIN_ADJUST 等）
     * @param relatedItemId 关联物品 ID（可空）
     * @param relatedApplyId 关联认领申请 ID（可空）
     * @param reason        变动原因描述
     * @param operatorId    操作人 ID（系统触发时为空）
     */
    @Override
    @Transactional
    public void issueCredit(Long userId, int amount, String operationType,
                            Long relatedItemId, Long relatedApplyId, String reason, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            // 用户已不存在（如刚被删除）时降级为日志告警，保证主业务流程不被积分环节打断
            log.warn("发放积分失败：用户 {} 不存在", userId);
            return;
        }

        // 管理员不参与诚信积分体系
        if (user.getRole() == UserRole.POINT_ADMIN || user.getRole() == UserRole.SYS_ADMIN) {
            log.info("用户 {} 是管理员，跳过积分发放", userId);
            return;
        }

        // 1. 更新用户表分值（读-改-写：基于刚查出的最新值累加）
        user.setCreditScore(user.getCreditScore() + amount);
        userMapper.updateById(user);

        // 2. 写积分流水：流水是积分体系的"账本"，每一笔变动都可追溯来源
        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(userId);
        creditLog.setChangeAmount(amount);
        creditLog.setOperationType(operationType);
        creditLog.setRelatedItemId(relatedItemId);
        creditLog.setRelatedApplyId(relatedApplyId);
        creditLog.setReason(reason);
        creditLog.setOperatorId(operatorId);
        creditLogMapper.insert(creditLog);

        // 3. 刷新 Redis 缓存（key: credit:user:{id}，TTL 1 小时），
        //    首页/榜单等高频读场景直接命中缓存，减轻数据库压力
        String cacheKey = "credit:user:" + userId;
        redisTemplate.opsForValue().set(cacheKey, user.getCreditScore(), Duration.ofHours(1));
    }

    /**
     * 按物品回滚积分（认领流程异常终止时调用）。
     *
     * <p>场景：失主在巡检通过、发布者已获得 +3/+1 分后取消了认领申请，
     * 系统需要把之前因该物品发放的积分扣回去。
     * 内部委托 {@link #rollbackIfIssued}，带幂等校验，重复调用不会多扣。
     *
     * @param itemId    关联物品 ID
     * @param applyId   关联认领申请 ID（可空）
     * @param claimerId 认领人 ID（其 +1 分由 AdminServiceImpl 的回滚流程单独处理）
     * @param reason    回滚原因
     * @param operatorId 操作人 ID
     */
    @Override
    @Transactional
    public void rollbackCredits(Long itemId, Long applyId, Long claimerId, String reason, Long operatorId) {
        // 回滚金额固定为 1（PICKUP_ISSUE 发布时的发放分值），与发放时一一对应
        rollbackIfIssued(claimerId, 1, itemId, applyId, reason, operatorId);
    }

    /**
     * 幂等回滚：只有"确实发过 PICKUP_ISSUE 分且尚未回滚过"时才执行扣回。
     *
     * <p>两步幂等校验（防重复回滚的核心）：
     * 1. 前置流水校验——查不到该物品的 PICKUP_ISSUE 发放记录，说明根本没发过分，直接返回；
     * 2. 回滚流水校验——已存在该物品的 ROLLBACK 记录，说明已回滚过，再次返回，避免二次扣分。
     *
     * @param userId 被回滚的用户
     * @param amount 发放时的分值（回滚时取负数）
     */
    private void rollbackIfIssued(Long userId, int amount, Long itemId,
                                  Long applyId, String reason, Long operatorId) {
        // 校验 1：是否真的对该用户、该物品发放过 PICKUP_ISSUE 积分
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "PICKUP_ISSUE")
                .eq(CreditLog::getRelatedItemId, itemId);
        if (creditLogMapper.selectCount(wrapper) == 0) {
            return; // 从未发过分，无需回滚
        }

        // 校验 2：是否已经回滚过（存在同物品的 ROLLBACK 流水）——保证幂等
        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId);
        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return; // 已回滚过，直接返回，防止重复扣分
        }

        // 通过两道校验后，走统一的 issueCredit 扣回积分（amount 取负）
        issueCredit(userId, -amount, "ROLLBACK", itemId, applyId, reason, operatorId);
    }
}
