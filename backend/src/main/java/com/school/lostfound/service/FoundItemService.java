package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.FoundItemVO;


public interface FoundItemService {
    FoundItemVO publish(FoundItemRequest request, Long currentUserId);

    /** 公开列表；viewerId 为 null 表示未登录游客（只能看公开待认领，看不到已领取件） */
    IPage<FoundItemVO> listPublic(ItemQueryRequest request, Long viewerId);

    FoundItemVO getById(Long id);

    void invalidate(Long id, Long currentUserId);

    void markPlaced(Long id, Long currentUserId);

    void forwardToNotice(Long id, Long noticeId, Long currentUserId);

    void archive(Long id, Long currentUserId);

    IPage<FoundItemVO> listMy(Long currentUserId, ItemQueryRequest request);

    void deleteByAdmin(Long id);

}
