package com.school.lostfound.service;

/**
 * 信用分变动服务接口 —— 信用激励体系的"写"侧。
 * <p>
 * 业务背景：为防止恶意认领（乱点认领又不履约）和鼓励诚信行为，
 * 系统设计了信用分机制：完成一次诚信认领加一定分数，
 * 认领后违约/恶意申请则扣分；信用分同时影响排行榜展示与低分用户的权限。
 * <p>
 * 与 CreditQueryService 的分工：本接口只负责"变动"（加分/扣分/回滚），
 * 查询类能力（我的分数、流水、排行榜）在 CreditQueryService 中。
 * 两者分离是因为变动逻辑对事务一致性和并发安全要求更高，
 * 独立出来便于聚焦维护。
 */
public interface CreditService {
    /**
     * 发放（正数）或扣除（负数）信用分，并写入信用流水。
     *
     * @param userId         被变动的用户 ID
     * @param amount         变动分值，正=加分，负=扣分
     * @param operationType  变动类型编码（如 CLAIM_SUCCESS、VIOLATION），用于流水归类与前端图标显示
     * @param relatedItemId  关联的物品 ID（可为 null，如系统奖励不挂物品）
     * @param relatedApplyId 关联的认领申请 ID（可为 null）
     * @param reason         变动原因描述，展示在信用流水中，向用户解释分数增减依据
     * @param operatorId     操作人 ID（系统自动变动时为 null 或系统账号），便于审计追责
     */
    void issueCredit(Long userId, int amount, String operationType,
                     Long relatedItemId, Long relatedApplyId, String reason, Long operatorId);

    /**
     * 信用分回滚 —— 撤销某次认领已完成发放的分数。
     * <p>
     * 业务场景：管理员驳回了一次原本已加分成功的认领（如事后发现是恶意认领），
     * 或认领单被撤销。回滚时按当时的发放记录做反向冲正，
     * 并在流水里记录冲正原因，保证用户总分与流水可逐笔对账。
     *
     * @param itemId    关联物品 ID，用于定位当初的发放流水
     * @param applyId   关联认领申请 ID
     * @param claimerId 认领人 ID（被回滚分数的用户）
     * @param reason    回滚原因，写入流水
     * @param operatorId 执行回滚的管理员 ID，便于审计
     */
    void rollbackCredits(Long itemId, Long applyId, Long claimerId, String reason, Long operatorId);
}
