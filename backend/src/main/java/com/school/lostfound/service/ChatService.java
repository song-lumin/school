package com.school.lostfound.service;

import com.school.lostfound.dto.ChatSendRequest;
import com.school.lostfound.vo.ChatMessageVO;
import com.school.lostfound.vo.ChatThreadVO;

import java.util.List;

public interface ChatService {

    /** 读取某张工单（或某个物品）下的沟通记录 */
    List<ChatMessageVO> list(Long disputeId, Long itemId, Long operatorId);

    /** 发一条留言，并把提醒推给这条对话里的其他人 */
    ChatMessageVO send(ChatSendRequest request, Long operatorId);

    /** 「我的沟通」列表：我参与的工单 + 每个工单的最后一条留言 */
    List<ChatThreadVO> listThreads(Long operatorId);
}
