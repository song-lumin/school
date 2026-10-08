package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.vo.DisputeVO;

public interface DisputeService {
    DisputeVO createDispute(DisputeCreateRequest request, Long currentUserId);

    IPage<DisputeVO> list(String disputeType, Integer status, int page, int size);

    DisputeVO getById(Long id, Long currentUserId);

    IPage<DisputeVO> listMy(Long currentUserId, int page, int size);

    void handle(Long id, DisputeHandleRequest request, Long operatorId);
}
