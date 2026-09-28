package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.LostNoticeRequest;
import com.school.lostfound.dto.ForwardClaimRequest;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.LostNoticeVO;
import com.school.lostfound.vo.MatchResultVO;

import java.util.List;

public interface LostNoticeService {
    LostNoticeVO publish(LostNoticeRequest request, Long currentUserId);

    IPage<LostNoticeVO> list(String keyword, String category, Integer status, int page, int size);

    LostNoticeVO getById(Long id);

    LostNoticeVO close(Long id, Long currentUserId);

    ClaimApplyVO forward(Long id, ForwardClaimRequest request, Long currentUserId);

    List<FoundItem> smartMatch(Long id, Long currentUserId);

    List<MatchResultVO> smartMatchV2(Long id, Long currentUserId);
}
