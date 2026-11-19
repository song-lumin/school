package com.school.lostfound.service;

import com.school.lostfound.dto.CheckRequest;
import com.school.lostfound.dto.DropPointRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.DropPointVO;
import com.school.lostfound.vo.InventoryItemVO;

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

    /** 误领/冒领物品归还入库：待归还(7) → 公开待认领(1)；已定责为「物品损坏」的一律作废(0)，返回 "VOIDED" */
    String returnIn(Long dropPointId, Long itemId, Long operatorId);

    List<InventoryItemVO> getInventory(Long dropPointId);
}
