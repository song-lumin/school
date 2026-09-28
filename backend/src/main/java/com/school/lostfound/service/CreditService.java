package com.school.lostfound.service;

public interface CreditService {
    void issueCredit(Long userId, int amount, String operationType,
                     Long relatedItemId, Long relatedApplyId, String reason, Long operatorId);

    void rollbackCredits(Long itemId, Long applyId, Long claimerId, String reason, Long operatorId);
}
