package com.school.lostfound.schedule;

import com.school.lostfound.service.ClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日清理残留的「临时锁定」认领申请。
 * 统一点：审核通过一人后，同物品其余待审核申请会被置为 LOCKED(3)，
 * 若对方一直不取件、也不取消，这些申请就会长期挂在「锁定中」，用户既不能重新提交也看不到出口。
 * 本任务每天 03:10 扫全表 LOCKED，按物品状态与驳回次数各归各位（与用户重新提交时的懒解锁同一套规则）：
 * - 物品仍在「认领中」且驳回不足 3 次 → 不动（正等取件）；
 * - 物品已回到「公开」且驳回不足 3 次 → 作废（与现场取消、取件成功同一结论，需要就重新提交）；
 * - 物品已取走/作废/丢失/归档等，或驳回满 3 次且过 24h → 关闭为「驳回」（满 3 次清零）并通知原因。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimLockCleanupTask {

    private final ClaimService claimService;

    @Scheduled(cron = "0 10 3 * * ?")
    public void cleanupStaleLocks() {
        try {
            int count = claimService.releaseStaleLocks();
            if (count > 0) {
                log.info("临时锁定清理：本次处理 {} 条过期锁定申请", count);
            }
        } catch (Exception e) {
            // 定时任务不能把异常抛出去影响下一次调度
            log.error("临时锁定清理任务失败", e);
        }
    }
}
