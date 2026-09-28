package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.RiskControlService;
import com.school.lostfound.vo.RiskWarningVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskControlServiceImpl implements RiskControlService {

    /** 串通预警阈值：同一发布人-认领人对完成领取达到该次数 */
    private static final int COLLUSION_THRESHOLD = 2;
    /** 认领率预警阈值：发布 >= 3 条且成功领取数为 0 */
    private static final int CLAIM_RATE_MIN_PUBLISHED = 3;

    private static final List<String> SENSITIVE_WORDS = List.of(
            "刷分", "刷积分", "转账", "加微信", "加QQ", "私下交易",
            "代领", "红包", "返现", "赌博", "贷款", "兼职刷单",
            "色情", "诈骗", "毒品");

    private final FoundItemMapper foundItemMapper;
    private final ClaimApplyMapper claimApplyMapper;
    private final UserMapper userMapper;

    @Override
    public boolean containsSensitiveWord(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.toLowerCase();
        return SENSITIVE_WORDS.stream().anyMatch(normalized::contains);
    }

    @Override
    public List<RiskWarningVO> detectCollusion() {
        List<ClaimApply> completed = claimApplyMapper.selectList(
                new LambdaQueryWrapper<ClaimApply>()
                        .eq(ClaimApply::getApplyStatus, ClaimStatus.COMPLETED.getCode()));

        Map<String, List<ClaimApply>> byPair = completed.stream()
                .filter(a -> a.getItemId() != null && a.getClaimerId() != null)
                .collect(Collectors.groupingBy(a -> a.getItemId() + ":" + a.getClaimerId()));

        Map<Long, String> userNames = nameCache(collectUserIds(byPair.values()));

        return byPair.entrySet().stream()
                .filter(e -> e.getValue().size() >= COLLUSION_THRESHOLD)
                .map(e -> {
                    String[] parts = e.getKey().split(":");
                    Long publisherId = Long.valueOf(parts[0]);
                    Long claimerId = Long.valueOf(parts[1]);
                    RiskWarningVO vo = new RiskWarningVO();
                    vo.setWarningType("COLLUSION");
                    vo.setUserIds(List.of(publisherId, claimerId));
                    vo.setUserNames(List.of(
                            maskName(userNames.get(publisherId)),
                            maskName(userNames.get(claimerId))));
                    vo.setCount(e.getValue().size());
                    vo.setDetail("同一发布人与认领人完成领取 " + e.getValue().size() + " 次，疑似串通刷分，请人工审查");
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskWarningVO> detectAbnormalClaimRate() {
        List<FoundItem> pickedUp = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .eq(FoundItem::getItemStatus, ItemStatus.PICKED_UP.getCode()));
        Map<Long, Long> pickedUpByPublisher = pickedUp.stream()
                .filter(i -> i.getFounderId() != null)
                .collect(Collectors.groupingBy(FoundItem::getFounderId, Collectors.counting()));

        List<FoundItem> finished = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>()
                        .in(FoundItem::getItemStatus,
                                ItemStatus.PICKED_UP.getCode(),
                                ItemStatus.VOIDED.getCode()));
        Map<Long, List<FoundItem>> byPublisher = finished.stream()
                .filter(i -> i.getFounderId() != null)
                .collect(Collectors.groupingBy(FoundItem::getFounderId));

        Map<Long, String> userNames = nameCache(
                byPublisher.values().stream().flatMap(List::stream)
                        .map(FoundItem::getFounderId).filter(Objects::nonNull)
                        .collect(Collectors.toSet()));

        return byPublisher.entrySet().stream()
                .filter(e -> e.getValue().size() >= CLAIM_RATE_MIN_PUBLISHED)
                .filter(e -> pickedUpByPublisher.getOrDefault(e.getKey(), 0L) == 0)
                .map(e -> {
                    RiskWarningVO vo = new RiskWarningVO();
                    vo.setWarningType("CLAIM_RATE");
                    vo.setUserIds(List.of(e.getKey()));
                    vo.setUserNames(List.of(maskName(userNames.get(e.getKey()))));
                    vo.setCount(e.getValue().size());
                    vo.setDetail("发布 " + e.getValue().size() + " 条招领均无成功领取（已作废/已完结），认领率异常，建议审查");
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskWarningVO> getAllWarnings() {
        List<RiskWarningVO> warnings = new ArrayList<>();
        warnings.addAll(detectCollusion());
        warnings.addAll(detectAbnormalClaimRate());
        return warnings;
    }

    private Set<Long> collectUserIds(java.util.Collection<List<ClaimApply>> groups) {
        return groups.stream().flatMap(List::stream)
                .flatMap(a -> java.util.stream.Stream.of(
                        publisherOf(a), a.getClaimerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Long publisherOf(ClaimApply apply) {
        FoundItem item = foundItemMapper.selectById(apply.getItemId());
        return item != null ? item.getFounderId() : null;
    }

    private Map<Long, String> nameCache(java.util.Collection<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct()
                .collect(Collectors.toMap(id -> id, id -> {
                    User u = userMapper.selectById(id);
                    return u != null ? u.getRealName() : "未知";
                }));
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "未知";
        }
        if (name.length() == 1) {
            return name + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
