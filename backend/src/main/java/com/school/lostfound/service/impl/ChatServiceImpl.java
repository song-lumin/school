package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.dto.ChatSendRequest;
import com.school.lostfound.entity.ChatMessage;
import com.school.lostfound.entity.Dispute;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ChatMessageMapper;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.ChatService;
import com.school.lostfound.service.MessageService;
import com.school.lostfound.vo.ChatMessageVO;
import com.school.lostfound.vo.ChatThreadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTENT = 500;

    private final ChatMessageMapper chatMessageMapper;
    private final DisputeMapper disputeMapper;
    private final FoundItemMapper foundItemMapper;
    private final DropPointMapper dropPointMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Override
    public List<ChatMessageVO> list(Long disputeId, Long itemId, Long operatorId) {
        Dispute dispute = disputeId == null ? null : disputeMapper.selectById(disputeId);
        if (disputeId != null && dispute == null) {
            throw new BusinessException(404, "工单不存在");
        }
        FoundItem item = resolveItem(dispute, itemId);
        assertMember(dispute, item, operatorId);

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        if (dispute != null) {
            wrapper.eq(ChatMessage::getDisputeId, dispute.getId());
        } else {
            wrapper.eq(ChatMessage::getItemId, item.getId());
        }
        wrapper.orderByAsc(ChatMessage::getCreatedAt).orderByAsc(ChatMessage::getId);

        List<ChatMessage> rows = chatMessageMapper.selectList(wrapper);
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> senderIds = rows.stream().map(ChatMessage::getSenderId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = senderIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(senderIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return rows.stream().map(row -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(row.getId());
            vo.setDisputeId(row.getDisputeId());
            vo.setItemId(row.getItemId());
            vo.setSenderId(row.getSenderId());
            User sender = users.get(row.getSenderId());
            vo.setSenderName(sender == null ? "用户" : displayName(sender));
            vo.setSenderRole(row.getSenderRole());
            vo.setMine(row.getSenderId() != null && row.getSenderId().equals(operatorId));
            vo.setContent(row.getContent());
            vo.setCreatedAt(row.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessageVO send(ChatSendRequest request, Long operatorId) {
        if (request.getDisputeId() == null && request.getItemId() == null) {
            throw new BusinessException(400, "请指定要沟通的工单或物品");
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "留言内容不能为空");
        }
        if (content.length() > MAX_CONTENT) {
            throw new BusinessException(400, "留言最多 " + MAX_CONTENT + " 字");
        }

        Dispute dispute = request.getDisputeId() == null ? null : disputeMapper.selectById(request.getDisputeId());
        if (request.getDisputeId() != null && dispute == null) {
            throw new BusinessException(404, "工单不存在");
        }
        FoundItem item = resolveItem(dispute, request.getItemId());
        assertMember(dispute, item, operatorId);

        User sender = userMapper.selectById(operatorId);
        if (sender == null) {
            throw new BusinessException(403, "账号不存在");
        }

        ChatMessage row = new ChatMessage();
        row.setDisputeId(dispute == null ? null : dispute.getId());
        row.setItemId(item == null ? null : item.getId());
        row.setSenderId(operatorId);
        row.setSenderRole(roleCode(sender.getRole()));
        row.setContent(content);
        row.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(row);

        List<Long> receivers = new ArrayList<>(participants(dispute, item));
        String title = dispute == null
                ? "物品沟通 · 有新留言"
                : "工单#" + dispute.getId() + " · 有新留言";
        String preview = content.length() > 60 ? content.substring(0, 60) + "…" : content;
        int pushed = 0;
        for (Long receiver : receivers) {
            if (receiver == null || receiver.equals(operatorId)) {
                continue;
            }
            messageService.push(receiver, title,
                    displayName(sender) + "（" + roleLabel(sender.getRole()) + "）：" + preview
                            + "。可在对应工单点「沟通」回复。",
                    "CHAT", item == null ? null : item.getId(),
                    dispute == null ? null : dispute.getApplyId());
            pushed++;
        }

        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(row.getId());
        vo.setDisputeId(row.getDisputeId());
        vo.setItemId(row.getItemId());
        vo.setSenderId(operatorId);
        vo.setSenderName(displayName(sender));
        vo.setSenderRole(row.getSenderRole());
        vo.setMine(true);
        vo.setContent(content);
        vo.setCreatedAt(row.getCreatedAt());
        log.info("沟通留言：disputeId={}, sender={}, 通知 {} 人", row.getDisputeId(), operatorId, pushed);
        return vo;
    }

    @Override
    public List<ChatThreadVO> listThreads(Long operatorId) {
        User me = userMapper.selectById(operatorId);
        if (me == null) {
            throw new BusinessException(403, "账号不存在");
        }
        List<Dispute> disputes = myDisputes(me);
        if (disputes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = disputes.stream().map(Dispute::getId).toList();

        LambdaQueryWrapper<ChatMessage> cw = new LambdaQueryWrapper<>();
        cw.in(ChatMessage::getDisputeId, ids).orderByDesc(ChatMessage::getId);
        List<ChatMessage> all = chatMessageMapper.selectList(cw);
        java.util.Map<Long, ChatMessage> latest = new java.util.HashMap<>();
        for (ChatMessage m : all) {
            latest.putIfAbsent(m.getDisputeId(), m);
        }

        List<Long> itemIds = disputes.stream().map(Dispute::getItemId).filter(Objects::nonNull).distinct().toList();
        java.util.Map<Long, FoundItem> items = itemIds.isEmpty() ? java.util.Map.of()
                : foundItemMapper.selectBatchIds(itemIds).stream()
                        .collect(Collectors.toMap(FoundItem::getId, i -> i, (a, b) -> a));

        List<ChatThreadVO> threads = new ArrayList<>();
        for (Dispute d : disputes) {
            ChatThreadVO vo = new ChatThreadVO();
            vo.setDisputeId(d.getId());
            vo.setItemId(d.getItemId());
            FoundItem item = items.get(d.getItemId());
            vo.setItemTitle(item == null ? null : item.getTitle());
            vo.setDisputeType(d.getDisputeType());
            vo.setStatus(d.getStatus());
            ChatMessage last = latest.get(d.getId());
            if (last == null) {
                vo.setHasMessage(false);
            } else {
                vo.setHasMessage(true);
                vo.setLastContent(last.getContent());
                vo.setLastAt(last.getCreatedAt());
                vo.setLastSenderRole(last.getSenderRole());
                User sender = userMapper.selectById(last.getSenderId());
                vo.setLastSender(sender == null ? "用户" : displayName(sender));
            }
            threads.add(vo);
        }
        // 有留言的排前面，其次按最后留言/工单时间
        threads.sort((a, b) -> {
            if (Boolean.TRUE.equals(a.getHasMessage()) != Boolean.TRUE.equals(b.getHasMessage())) {
                return Boolean.TRUE.equals(a.getHasMessage()) ? -1 : 1;
            }
            if (a.getLastAt() != null && b.getLastAt() != null) {
                return b.getLastAt().compareTo(a.getLastAt());
            }
            return Long.compare(b.getDisputeId(), a.getDisputeId());
        });
        return threads;
    }

    /** 我参与的工单：管理员看全部、点位负责人看自己点位、用户看自己申诉的 */
    private List<Dispute> myDisputes(User me) {
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        if (me.getRole() == UserRole.SYS_ADMIN) {
            wrapper.orderByDesc(Dispute::getCreatedAt).last("limit 100");
            return disputeMapper.selectList(wrapper);
        }
        if (me.getRole() == UserRole.POINT_ADMIN) {
            List<DropPoint> points = dropPointMapper.selectList(
                    new LambdaQueryWrapper<DropPoint>().eq(DropPoint::getAdminId, me.getId()));
            Set<Long> pointIds = new LinkedHashSet<>();
            for (DropPoint p : points) {
                if (p.getId() != null) {
                    pointIds.add(p.getId());
                }
            }
            if (pointIds.isEmpty()) {
                return List.of();
            }
            List<FoundItem> items = foundItemMapper.selectList(
                    new LambdaQueryWrapper<FoundItem>().in(FoundItem::getDropPointId, pointIds));
            Set<Long> itemIds = new LinkedHashSet<>();
            for (FoundItem i : items) {
                if (i.getId() != null) {
                    itemIds.add(i.getId());
                }
            }
            if (itemIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(Dispute::getItemId, itemIds).orderByDesc(Dispute::getCreatedAt).last("limit 100");
            return disputeMapper.selectList(wrapper);
        }
        wrapper.eq(Dispute::getApplicantId, me.getId()).orderByDesc(Dispute::getCreatedAt).last("limit 100");
        return disputeMapper.selectList(wrapper);
    }

    private FoundItem resolveItem(Dispute dispute, Long itemId) {
        Long targetId = dispute != null ? dispute.getItemId() : itemId;
        if (targetId == null) {
            return null;
        }
        FoundItem item = foundItemMapper.selectById(targetId);
        if (dispute != null && item == null) {
            throw new BusinessException(404, "工单关联的物品不存在");
        }
        return item;
    }

    private Set<Long> participants(Dispute dispute, FoundItem item) {
        Set<Long> ids = new LinkedHashSet<>();
        if (dispute != null && dispute.getApplicantId() != null) {
            ids.add(dispute.getApplicantId());
        }
        Long pointAdminId = pointAdminIdOf(item);
        if (pointAdminId != null) {
            ids.add(pointAdminId);
        }
        for (User u : userMapper.selectList(null)) {
            if (u.getRole() == UserRole.SYS_ADMIN) {
                ids.add(u.getId());
            }
        }
        return ids;
    }

    private void assertMember(Dispute dispute, FoundItem item, Long operatorId) {
        User operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException(403, "账号不存在");
        }
        if (operator.getRole() == UserRole.SYS_ADMIN) {
            return;
        }
        if (dispute != null && dispute.getApplicantId() != null && dispute.getApplicantId().equals(operatorId)) {
            return;
        }
        Long pointAdminId = pointAdminIdOf(item);
        if (pointAdminId != null && pointAdminId.equals(operatorId)) {
            return;
        }
        if (dispute == null && item != null) {
            Long founder = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
            if (founder != null && founder.equals(operatorId)) {
                return;
            }
        }
        throw new BusinessException(403, "你不在本工单的沟通范围内（仅申诉人、该点位负责人、系统管理员可参与）");
    }

    private Long pointAdminIdOf(FoundItem item) {
        if (item == null || item.getDropPointId() == null) {
            return null;
        }
        DropPoint point = dropPointMapper.selectById(item.getDropPointId());
        return point == null ? null : point.getAdminId();
    }

    private String roleCode(UserRole role) {
        if (role == UserRole.SYS_ADMIN) {
            return "SYS_ADMIN";
        }
        if (role == UserRole.POINT_ADMIN) {
            return "POINT_ADMIN";
        }
        return "USER";
    }

    private String roleLabel(UserRole role) {
        if (role == UserRole.SYS_ADMIN) {
            return "系统管理员";
        }
        if (role == UserRole.POINT_ADMIN) {
            return "站点负责人";
        }
        return "用户";
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }
}
