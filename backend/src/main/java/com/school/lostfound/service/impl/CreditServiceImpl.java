package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.MessageService;
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

    /** 诚信积分下限：扣到这里不再继续下降，避免出现 -30、-50 这种没有实际惩戒意义、也无法解释的负数 */
    private static final int MIN_CREDIT_SCORE = -5;
    /** 触底即进入黑名单（账号禁用）：分数能扣到下限，说明累计违规已经足够严重 */
    private static final int BLACKLIST_SCORE = -5;

    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;
    private final FoundItemMapper foundItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean hasLivePickupReward(Long userId, Long itemId) {
        if (userId == null || itemId == null) {
            return false;
        }
        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "PICKUP_ISSUE")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, 3);
        long issued = creditLogMapper.selectCount(issuedWrapper);

        LambdaQueryWrapper<CreditLog> rolledWrapper = new LambdaQueryWrapper<>();
        rolledWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, -3);
        long rolled = creditLogMapper.selectCount(rolledWrapper);
        return issued - rolled > 0;
    }
    private final MessageService messageService;

    @Override
    @Transactional
    public void issueCredit(Long userId, int amount, String operationType,
                            Long relatedItemId, Long relatedApplyId, String reason, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("发放积分失败：用户 {} 不存在", userId);
            return;
        }

        // 管理员不参与诚信积分体系
        if (user.getRole() == UserRole.POINT_ADMIN || user.getRole() == UserRole.SYS_ADMIN) {
            log.info("用户 {} 是管理员，跳过积分发放", userId);
            return;
        }

        // 原子自增：避免「读-改-写」在并发（同时取件/同时定责）下丢分；credit_score 为空按 0 起算
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("credit_score = COALESCE(credit_score, 0) + (" + amount + ")"));

        User updated = userMapper.selectById(userId);
        int rawScore = updated == null || updated.getCreditScore() == null ? 0 : updated.getCreditScore();

        // 触底处理：扣到下限就停住，分数不再继续变负（流水仍按真实扣减金额记录，便于审计）
        int currentScore = rawScore;
        boolean hitFloor = rawScore < MIN_CREDIT_SCORE;
        if (hitFloor) {
            userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .setSql("credit_score = " + MIN_CREDIT_SCORE));
            currentScore = MIN_CREDIT_SCORE;
        }

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
        redisTemplate.opsForValue().set(cacheKey, currentScore, Duration.ofHours(1));

        // 积分每笔变动都给当事人发站内消息，回滚自动带上业务原因
        String title = amount >= 0
                ? "积分发放 +" + amount
                : "积分回滚 " + amount;
        String content = (amount >= 0 ? "你的诚信积分 +" : "你的诚信积分 ")
                + amount + "，原因：" + (reason == null ? "系统调整" : reason)
                + "；变动后积分 " + currentScore + "。"
                + (hitFloor ? "（积分已到达下限 " + MIN_CREDIT_SCORE + "，不再继续下降）" : "");
        messageService.push(userId, title, content, "CREDIT", relatedItemId, relatedApplyId);

        // 触底即黑名单：账号置为禁用（status=0），其尚未交付的招领一并下架，
        // 否则东西还挂在公开池里、发布人却登不进来审核，认领人只能干等
        if (currentScore <= BLACKLIST_SCORE
                && user.getStatus() != null && user.getStatus() == 1) {
            userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getStatus, 0));

            // 只下架「公开待认领」与「待投放」：认领中(2) 的交由站点管理员继续审核收尾，
            // 已取件/待归还等后置状态本来就与发布人操作无关
            int offShelf = foundItemMapper.update(null, new LambdaUpdateWrapper<FoundItem>()
                    .eq(FoundItem::getFounderId, userId)
                    .in(FoundItem::getItemStatus,
                            ItemStatus.PUBLIC.getCode(), ItemStatus.PENDING_PLACEMENT.getCode())
                    .set(FoundItem::getItemStatus, ItemStatus.VOIDED.getCode())
                    .set(FoundItem::getTakedownReason, "发布人账号进入黑名单，招领自动下架"));

            messageService.push(userId, "账号已进入黑名单 · 暂停使用",
                    "你的诚信积分已降至 " + currentScore + "（下限 " + MIN_CREDIT_SCORE + "），"
                            + "系统已暂停该账号的登录与认领权限，且"
                            + (offShelf > 0 ? "已下架你名下 " + offShelf + " 条尚未交付的公开招领；" : "你名下已无处于公开状态的招领；")
                            + "处于「认领中」的物品由站点管理员继续处理，不会再接收你的新操作。"
                            + "如认为处理有误，可向系统管理员申诉；管理员核实后解封，积分将清零重新计算（已下架的招领需重新发布）。",
                    "CREDIT", relatedItemId, relatedApplyId);
            log.warn("诚信积分触底，账号已禁用：user={}, score={}, 下架招领={}", userId, currentScore, offShelf);
        }
    }

    @Override
    @Transactional
    public void rollbackCredits(Long itemId, Long applyId, Long claimerId, String reason, Long operatorId) {
        rollbackIfIssued(claimerId, 1, itemId, applyId, reason, operatorId);
    }

    @Override
    @Transactional
    public void issueCreditOnce(Long userId, int amount, String operationType,
                                Long relatedItemId, Long relatedApplyId, String reason, Long operatorId) {
        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, operationType);
        if (relatedItemId != null) {
            issuedWrapper.eq(CreditLog::getRelatedItemId, relatedItemId);
        }
        // 幂等粒度按「认领周期」：同一笔认领申请只结算一次；
        // 误领退回后物品重新公开、由他人重新认领属于新的一笔，上一笔积分已被回滚，应当重新结算
        if (relatedApplyId != null) {
            issuedWrapper.eq(CreditLog::getRelatedApplyId, relatedApplyId);
        }
        if (creditLogMapper.selectCount(issuedWrapper) > 0) {
            log.info("积分已发放过，跳过重复发放：user={}, type={}, item={}", userId, operationType, relatedItemId);
            return;
        }
        issueCredit(userId, amount, operationType, relatedItemId, relatedApplyId, reason, operatorId);
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
