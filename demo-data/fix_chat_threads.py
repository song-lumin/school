# -*- coding: utf-8 -*-
import io

# ---------- 1) ChatService 接口加 threads ----------
p1 = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\ChatService.java'
s1 = io.open(p1, encoding='utf-8').read()
old = """import com.school.lostfound.vo.ChatMessageVO;

import java.util.List;"""
new = """import com.school.lostfound.vo.ChatMessageVO;
import com.school.lostfound.vo.ChatThreadVO;

import java.util.List;"""
print('iface-import', s1.count(old))
s1 = s1.replace(old, new, 1)

old2 = """    /** 发一条留言，并把提醒推给这条对话里的其他人 */
    ChatMessageVO send(ChatSendRequest request, Long operatorId);
}"""
new2 = """    /** 发一条留言，并把提醒推给这条对话里的其他人 */
    ChatMessageVO send(ChatSendRequest request, Long operatorId);

    /** 「我的沟通」列表：我参与的工单 + 每个工单的最后一条留言 */
    List<ChatThreadVO> listThreads(Long operatorId);
}"""
print('iface-method', s1.count(old2))
s1 = s1.replace(old2, new2, 1)
io.open(p1, 'w', encoding='utf-8', newline='').write(s1)

# ---------- 2) ChatServiceImpl 追加实现 ----------
p2 = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\ChatServiceImpl.java'
s2 = io.open(p2, encoding='utf-8').read()

s2 = s2.replace(
"""import com.school.lostfound.vo.ChatMessageVO;""",
"""import com.school.lostfound.vo.ChatMessageVO;
import com.school.lostfound.vo.ChatThreadVO;""", 1)

old3 = """    private FoundItem resolveItem(Dispute dispute, Long itemId) {"""
new3 = """    @Override
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

    private FoundItem resolveItem(Dispute dispute, Long itemId) {"""
print('impl-method', s2.count(old3))
s2 = s2.replace(old3, new3, 1)
io.open(p2, 'w', encoding='utf-8', newline='').write(s2)

# ---------- 3) Controller 加 /threads ----------
p3 = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\controller\ChatController.java'
s3 = io.open(p3, encoding='utf-8').read()
s3 = s3.replace(
"""import com.school.lostfound.vo.ChatMessageVO;""",
"""import com.school.lostfound.vo.ChatMessageVO;
import com.school.lostfound.vo.ChatThreadVO;""", 1)
old4 = """    @PostMapping("/messages")"""
new4 = """    /** 我的沟通列表（按工单聚合） */
    @GetMapping("/threads")
    public Result<List<ChatThreadVO>> threads(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(chatService.listThreads(userId));
    }

    @PostMapping("/messages")"""
print('ctrl-threads', s3.count(old4))
s3 = s3.replace(old4, new4, 1)
io.open(p3, 'w', encoding='utf-8', newline='').write(s3)
print('backend extended')
