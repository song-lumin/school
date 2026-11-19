package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.CameraLogCreateRequest;
import com.school.lostfound.dto.CameraLogHandleRequest;
import com.school.lostfound.vo.CameraLogVO;

public interface CameraLogService {
    IPage<CameraLogVO> list(Integer status, Long dropPointId, Long itemId, int page, int size);

    /** 站点管理员视角：本人负责点位相关 + 本人发起的调取申请 */
    IPage<CameraLogVO> listMine(Long pointAdminId, Integer status, int page, int size);

    CameraLogVO create(CameraLogCreateRequest request, Long applicantId);

    void handle(Long id, CameraLogHandleRequest request, Long handlerId);
}
