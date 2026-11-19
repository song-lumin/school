package com.school.lostfound.service;

public interface CreditService {
    void issueCredit(Long userId, int amount, String operationType,
                     Long relatedItemId, Long relatedApplyId, String reason, Long operatorId);

    void rollbackCredits(Long itemId, Long applyId, Long claimerId, String reason, Long operatorId);

    /**
     * 幂等发放：同一用户、同一操作类型、同一物品只发一次。
     * 用于物品因误领/冒领被退回并重新公开后，避免同一件物品重复发放奖励。
     */
    void issueCreditOnce(Long userId, int amount, String operationType,
                         Long relatedItemId, Long relatedApplyId, String reason, Long operatorId);

    /**
     * 该用户在这件物品上是否还有一笔「未回滚」的拾获 +3。
     * 用于按「成交」结算：首次发、上次成交被判无效已回滚后，真失主再次取件时补发一次。
     */
    boolean hasLivePickupReward(Long userId, Long itemId);
}
