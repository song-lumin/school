package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.FoundItemVO;


public interface FoundItemService {
    FoundItemVO publish(FoundItemRequest request, Long currentUserId);

    IPage<FoundItemVO> listPublic(ItemQueryRequest request);

    FoundItemVO getById(Long id);

    void invalidate(Long id, Long currentUserId);

    void archive(Long id, Long currentUserId);

    IPage<FoundItemVO> listMy(Long currentUserId, ItemQueryRequest request);

    void deleteByAdmin(Long id);

}
