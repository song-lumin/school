package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.dto.PointCheckRequest;
import com.school.lostfound.vo.DisputeVO;

public interface DisputeService {
    DisputeVO createDispute(DisputeCreateRequest request, Long currentUserId);

    IPage<DisputeVO> list(String disputeType, Integer status, int page, int size);

    /** 本投放点相关工单（站点管理员视角） */
    IPage<DisputeVO> listForPoint(Long pointAdminId, String disputeType, Integer status, int page, int size);

    /** 站点回填现场核验事实（不改结论、不动积分） */
    void pointCheck(Long id, PointCheckRequest request, Long operatorId);

    DisputeVO getById(Long id, Long currentUserId);

    IPage<DisputeVO> listMy(Long currentUserId, int page, int size);

    void handle(Long id, DisputeHandleRequest request, Long operatorId);

    /**
     * 站点回填 SLA 催办：待处理工单满 24h 仍未回填现场核验的，向系统管理员生成催办任务。
     * 同一工单每满 24h 催一次，最多 3 次；已在 SLA 内回填的不催。
     * @return 本次催办的工单数
     */
    int escalateUnresponsiveSites();
}
