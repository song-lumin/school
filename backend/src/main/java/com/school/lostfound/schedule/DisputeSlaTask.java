package com.school.lostfound.schedule;

import com.school.lostfound.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 站点回填 SLA 催办：每小时扫描一次，待处理工单满 24h 站点仍未回填现场核验的，
 * 给系统管理员生成催办任务（同一工单每满 24h 催一次，最多 3 次）。
 * 定责时若站点仍未回填，由 DisputeServiceImpl.handle 强制标记「站点未响应」并记录时间。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeSlaTask {

    private final DisputeService disputeService;

    @Scheduled(cron = "0 0 * * * ?")
    public void escalateUnresponsiveSites() {
        try {
            int count = disputeService.escalateUnresponsiveSites();
            if (count > 0) {
                log.info("站点回填 SLA 定时催办：本次生成催办任务 {} 条", count);
            }
        } catch (Exception e) {
            // 定时任务不能把异常抛出去影响下一次调度
            log.error("站点回填 SLA 定时催办失败", e);
        }
    }
}
