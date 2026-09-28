package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.dto.ClaimRejectRequest;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.vo.ClaimApplyVO;

public interface ClaimService {
    ClaimApplyVO apply(ClaimApplyRequest request, Long currentUserId);

    ClaimApplyVO applyForward(ClaimApplyRequest request, Long sourceNoticeId, Long currentUserId);

    IPage<ClaimApplyVO> listByItem(Long itemId, Long currentUserId, int page, int size);

    ClaimApplyVO getById(Long id, Long currentUserId);

    void approve(Long id, Long currentUserId);

    void reject(Long id, ClaimRejectRequest request, Long currentUserId);

    ClaimApplyVO pickup(Long id, String pickupPhoto, String pickupSignature, Long currentUserId);

    IPage<ClaimApplyVO> listMy(Long currentUserId, int page, int size);
}
