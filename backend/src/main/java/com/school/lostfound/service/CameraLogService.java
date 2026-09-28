package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.CameraLogCreateRequest;
import com.school.lostfound.dto.CameraLogHandleRequest;
import com.school.lostfound.vo.CameraLogVO;

public interface CameraLogService {
    IPage<CameraLogVO> list(Integer status, Long dropPointId, Long itemId, int page, int size);

    CameraLogVO create(CameraLogCreateRequest request, Long applicantId);

    void handle(Long id, CameraLogHandleRequest request, Long handlerId);
}
