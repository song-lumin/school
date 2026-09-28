package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.LostNoticeRequest;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.dto.ForwardClaimRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.LostNotice;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.LostNoticeMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.LostNoticeService;
import com.school.lostfound.service.ClaimService;
import com.school.lostfound.vo.LostNoticeVO;
import com.school.lostfound.vo.MatchResultVO;
import com.school.lostfound.vo.ClaimApplyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostNoticeServiceImpl implements LostNoticeService {

    /** 拾取时间与丢失时间允许的最大间隔（天） */
    private static final long TIME_WINDOW_DAYS = 3;
    /** 时间相近加分 */
    private static final int SCORE_TIME_NEAR = 30;
    /** 地点相似加分 */
    private static final int SCORE_LOCATION_SIMILAR = 30;
    /** 关键词命中加分 */
    private static final int SCORE_KEYWORD = 20;

    private final LostNoticeMapper lostNoticeMapper;
    private final UserMapper userMapper;
    private final FoundItemMapper foundItemMapper;
    private final ClaimService claimService;

    @Override
    @Transactional
    public LostNoticeVO publish(LostNoticeRequest request, Long currentUserId) {
        LostNotice notice = new LostNotice();
        notice.setTitle(request.getTitle());
        notice.setCategory(request.getCategory());
        notice.setDescription(request.getDescription());
        notice.setLostLocation(request.getLostLocation());
        notice.setLostTime(request.getLostTime());
        notice.setContactInfo(request.getContactInfo());
        notice.setImages(request.getImages());
        notice.setStatus(0);
        notice.setPublisherId(currentUserId);

        lostNoticeMapper.insert(notice);

        User publisher = userMapper.selectById(currentUserId);
        return LostNoticeVO.fromEntity(notice, publisher != null ? publisher.getRealName() : null);
    }

    @Override
    public IPage<LostNoticeVO> list(String keyword, String category, Integer status, int page, int size) {
        LambdaQueryWrapper<LostNotice> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(LostNotice::getTitle, keyword)
                    .or().like(LostNotice::getDescription, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(LostNotice::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(LostNotice::getStatus, status);
        }

        wrapper.orderByDesc(LostNotice::getCreatedAt);

        IPage<LostNotice> result = lostNoticeMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> publisherIds = result.getRecords().stream()
                .map(LostNotice::getPublisherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> publisherNames = publisherIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(publisherIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName));

        Page<LostNoticeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(n -> LostNoticeVO.fromEntity(n, publisherNames.get(n.getPublisherId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public LostNoticeVO getById(Long id) {
        LostNotice notice = getNoticeOrThrow(id);
        User publisher = userMapper.selectById(notice.getPublisherId());
        return LostNoticeVO.fromEntity(notice, publisher != null ? publisher.getRealName() : null);
    }

    @Override
    @Transactional
    public LostNoticeVO close(Long id, Long currentUserId) {
        LostNotice notice = getNoticeOrThrow(id);

        if (!notice.getPublisherId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以关闭启事");
        }
        if (notice.getStatus() != 0) {
            throw new BusinessException(409, "启事当前状态不可关闭");
        }

        notice.setStatus(2);
        lostNoticeMapper.updateById(notice);

        User publisher = userMapper.selectById(currentUserId);
        return LostNoticeVO.fromEntity(notice, publisher != null ? publisher.getRealName() : null);
    }

    @Override
    @Transactional
    public ClaimApplyVO forward(Long id, ForwardClaimRequest request, Long currentUserId) {
        LostNotice notice = getNoticeOrThrow(id);
        if (!notice.getPublisherId().equals(currentUserId)) {
            throw new BusinessException(403, "只有寻物启事发布者可以发起认领");
        }
        if (notice.getStatus() != 0) {
            throw new BusinessException(409, "只有进行中的寻物启事可以发起认领");
        }
        ClaimApplyRequest claimRequest = new ClaimApplyRequest();
        claimRequest.setItemId(request.getItemId());
        claimRequest.setAnswer(request.getAnswer());
        return claimService.applyForward(claimRequest, id, currentUserId);
    }

    @Override
    public List<FoundItem> smartMatch(Long id, Long currentUserId) {
        LostNotice notice = getNoticeOrThrow(id);

        if (!notice.getPublisherId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以查看匹配结果");
        }

        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode());

        if (StringUtils.hasText(notice.getCategory())) {
            wrapper.eq(FoundItem::getCategory, notice.getCategory());
        }
        if (StringUtils.hasText(notice.getTitle())) {
            wrapper.and(w -> w.like(FoundItem::getTitle, notice.getTitle())
                    .or().like(FoundItem::getDescription, notice.getDescription()));
        }

        wrapper.orderByDesc(FoundItem::getPublishedAt).last("LIMIT 20");

        return foundItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public List<MatchResultVO> smartMatchV2(Long id, Long currentUserId) {
        LostNotice notice = getNoticeOrThrow(id);

        if (!notice.getPublisherId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以查看匹配结果");
        }

        // 候选集：分类相同的公开物品
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode());
        if (StringUtils.hasText(notice.getCategory())) {
            wrapper.eq(FoundItem::getCategory, notice.getCategory());
        }
        wrapper.orderByDesc(FoundItem::getPublishedAt).last("LIMIT 200");
        List<FoundItem> candidates = foundItemMapper.selectList(wrapper);

        List<MatchResultVO> results = candidates.stream()
                .map(item -> scoreMatch(notice, item))
                .filter(r -> r.getScore() > 0)
                .sorted(Comparator.comparingInt(MatchResultVO::getScore).reversed())
                .limit(20)
                .collect(Collectors.toList());

        // 把最高分匹配写回 matched_item_id（Sprint2 字段）
        if (!results.isEmpty() && results.get(0).getScore() >= SCORE_KEYWORD) {
            Long bestItemId = results.get(0).getItemId();
            if (!bestItemId.equals(notice.getMatchedItemId())) {
                notice.setMatchedItemId(bestItemId);
                lostNoticeMapper.updateById(notice);
            }
        }

        log.info("智能匹配完成：noticeId={}, 候选={}, 命中={}", id, candidates.size(), results.size());
        return results;
    }

    private MatchResultVO scoreMatch(LostNotice notice, FoundItem item) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // 时间：拾取时间与丢失时间相差 <= 3 天
        if (notice.getLostTime() != null && item.getFoundTime() != null) {
            long diffDays = Math.abs(ChronoUnit.HOURS.between(notice.getLostTime(), item.getFoundTime())) / 24;
            if (diffDays <= TIME_WINDOW_DAYS) {
                score += SCORE_TIME_NEAR;
                reasons.add("时间相近（相差" + diffDays + "天）");
            }
        }

        // 地点：丢失地点与拾取地点文本相似（互为子串即命中）
        if (StringUtils.hasText(notice.getLostLocation()) && StringUtils.hasText(item.getFoundLocation())) {
            String lost = notice.getLostLocation().replaceAll("\\s+", "");
            String found = item.getFoundLocation().replaceAll("\\s+", "");
            if (lost.contains(found) || found.contains(lost)) {
                score += SCORE_LOCATION_SIMILAR;
                reasons.add("地点相近");
            } else {
                // 部分匹配：共享任一 2 字以上片段（简化为按 2 字滑窗）
                if (hasCommonFragment(lost, found)) {
                    score += SCORE_LOCATION_SIMILAR / 2;
                    reasons.add("地点部分相近");
                }
            }
        }

        // 关键词：标题/描述与启事标题/描述互含
        String noticeText = normalize(notice.getTitle()) + normalize(notice.getDescription());
        String itemText = normalize(item.getTitle()) + normalize(item.getDescription());
        if (StringUtils.hasText(notice.getTitle())
                && (itemText.contains(normalize(notice.getTitle())) || normalize(notice.getTitle()).length() >= 2 && itemText.contains(normalize(notice.getTitle())))) {
            score += SCORE_KEYWORD;
            reasons.add("标题关键词命中");
        } else if (StringUtils.hasText(notice.getDescription()) && notice.getDescription().length() >= 4) {
            String[] descTokens = notice.getDescription().split("[，,。.\\s]+");
            long hits = java.util.Arrays.stream(descTokens)
                    .filter(t -> t.length() >= 2)
                    .filter(itemText::contains)
                    .count();
            if (hits > 0) {
                score += SCORE_KEYWORD;
                reasons.add("描述关键词命中");
            }
        }

        return MatchResultVO.fromEntity(item, Math.min(score, 100), reasons);
    }

    private boolean hasCommonFragment(String a, String b) {
        for (int i = 0; i + 2 <= a.length(); i++) {
            if (b.contains(a.substring(i, i + 2))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "");
    }

    private LostNotice getNoticeOrThrow(Long id) {
        LostNotice notice = lostNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(404, "寻物启事不存在");
        }
        return notice;
    }
}
