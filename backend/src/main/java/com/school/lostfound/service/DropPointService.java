package com.school.lostfound.service;

import com.school.lostfound.dto.CheckRequest;
import com.school.lostfound.dto.DropPointRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.DropPointVO;

import java.util.List;

public interface DropPointService {
    List<DropPointVO> listAll();

    DropPointVO getById(Long id);

    DropPointVO create(DropPointRequest request);

    DropPointVO update(Long id, DropPointRequest request);

    void delete(Long id);

    List<FoundItem> getPendingItems(Long dropPointId);

    List<ClaimApplyVO> getPendingPickups(Long dropPointId);

    void checkItem(Long dropPointId, CheckRequest request, Long operatorId);
}
