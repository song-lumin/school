package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.User;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public void issueCredit(Long userId, int amount, String operationType,
                            Long relatedItemId, Long relatedApplyId, String reason, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("发放积分失败：用户 {} 不存在", userId);
            return;
        }

        user.setCreditScore(user.getCreditScore() + amount);
        userMapper.updateById(user);

        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(userId);
        creditLog.setChangeAmount(amount);
        creditLog.setOperationType(operationType);
        creditLog.setRelatedItemId(relatedItemId);
        creditLog.setRelatedApplyId(relatedApplyId);
        creditLog.setReason(reason);
        creditLog.setOperatorId(operatorId);
        creditLogMapper.insert(creditLog);

        String cacheKey = "credit:user:" + userId;
        redisTemplate.opsForValue().set(cacheKey, user.getCreditScore(), Duration.ofHours(1));
    }

    @Override
    @Transactional
    public void rollbackCredits(Long itemId, Long applyId, Long claimerId, String reason, Long operatorId) {
        rollbackIfIssued(claimerId, 1, itemId, applyId, reason, operatorId);
    }

    private void rollbackIfIssued(Long userId, int amount, Long itemId,
                                  Long applyId, String reason, Long operatorId) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "PICKUP_ISSUE")
                .eq(CreditLog::getRelatedItemId, itemId);
        if (creditLogMapper.selectCount(wrapper) == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId);
        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return;
        }

        issueCredit(userId, -amount, "ROLLBACK", itemId, applyId, reason, operatorId);
    }
}
