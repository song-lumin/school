package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.HandInLog;
import com.school.lostfound.entity.ReportLog;
import com.school.lostfound.entity.Dispute;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.HandInLogMapper;
import com.school.lostfound.mapper.LostNoticeMapper;
import com.school.lostfound.mapper.ReportLogMapper;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.FoundItemService;
import com.school.lostfound.service.ImageFingerprintService;
import com.school.lostfound.service.RiskControlService;
import com.school.lostfound.vo.FoundItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoundItemServiceImpl implements FoundItemService {

    private static final int MAX_WEEKLY_PUBLISH = 3;
    /** 未公开草稿（待投放）不占每周公开额度，但同时最多挂 5 条，防批量灌数据 */
    private static final int MAX_PENDING_DRAFTS = 5;
    private static final int NEW_USER_DAYS = 7;

    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;
    private final UserMapper userMapper;
    private final DropPointMapper dropPointMapper;
    private final RiskControlService riskControlService;
    private final ImageFingerprintService imageFingerprintService;
    private final DisputeMapper disputeMapper;
    private final ReportLogMapper reportLogMapper;
    private final LostNoticeMapper lostNoticeMapper;

    @Override
    @Transactional
    public FoundItemVO publish(FoundItemRequest request, Long currentUserId) {
        checkWeeklyLimit(currentUserId);

        if (riskControlService.containsSensitiveWord(request.getTitle())
                || riskControlService.containsSensitiveWord(request.getDescription())
                || riskControlService.containsSensitiveWord(request.getFoundTimeText())) {
            throw new BusinessException(400, "内容包含敏感词，请修改后重新发布");
        }

        // 拾取时间允许模糊：精度只接受 0-5，模糊描述最长 60 字（DTO 已限长，这里兜住空白串）
        Integer precision = request.getFoundTimePrecision();
        if (precision != null && (precision < 0 || precision > 5)) {
            throw new BusinessException(400, "拾取时间精度不合法");
        }
        if (request.getFoundTime() != null && request.getFoundTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "拾取时间不能晚于当前时间");
        }

        User founder = userMapper.selectById(currentUserId);
        if (founder == null) {
            throw new BusinessException(404, "用户不存在");
        }

        DropPoint dropPoint = dropPointMapper.selectById(request.getDropPointId());
        if (dropPoint == null || dropPoint.getStatus() != 1) {
            throw new BusinessException(404, "投放点不存在或已停用");
        }

        FoundItem item = new FoundItem();
        item.setTitle(request.getTitle());
        item.setCategory(request.getCategory());
        item.setDescription(request.getDescription());
        item.setFoundLocation(request.getFoundLocation());
        item.setFoundTime(request.getFoundTime());
        // 模糊时间：precision 缺省按 0（精确）落库，描述为空则存 NULL，前端无描述时按日期+时段词展示
        item.setFoundTimePrecision(request.getFoundTimePrecision() == null ? 0 : request.getFoundTimePrecision());
        String foundTimeText = request.getFoundTimeText() == null ? null : request.getFoundTimeText().trim();
        item.setFoundTimeText(foundTimeText == null || foundTimeText.isEmpty() ? null : foundTimeText);
        item.setImages(request.getImages());
        item.setClaimQuestion(request.getClaimQuestion());
        item.setReferenceAnswer(request.getReferenceAnswer());
        item.setPerishable(request.getPerishable() != null ? request.getPerishable() : 0);
        // 发布后先为"已发布待投放"(6)，不对外公开；发布人线下把实物放入投放点后点"已投放"才变公开(1)
        item.setItemStatus(ItemStatus.PENDING_PLACEMENT.getCode());
        item.setFounderId(currentUserId);
        item.setDropPointId(request.getDropPointId());

        if (request.getActualFounderId() != null && !request.getActualFounderId().equals(currentUserId)) {
            User actualFounder = userMapper.selectById(request.getActualFounderId());
            if (actualFounder == null) {
                throw new BusinessException(404, "实际拾取者不存在");
            }
            item.setActualFounderId(request.getActualFounderId());
        }

        item.setPublishedAt(LocalDateTime.now());
        foundItemMapper.insert(item);

        // hand_in_log 初始状态0=实物未投放（发布人尚未把实物放入投放点）
        HandInLog log = new HandInLog();
        log.setItemId(item.getId());
        log.setDropPointId(request.getDropPointId());
        log.setHandInStatus(0);
        handInLogMapper.insert(log);

        try {
            imageFingerprintService.indexItemImages(item);
        } catch (RuntimeException exception) {
            org.slf4j.LoggerFactory.getLogger(FoundItemServiceImpl.class)
                    .warn("物品图片指纹建立失败: itemId={}", item.getId(), exception);
        }

        return FoundItemVO.fromEntity(item, founder.getRealName(), null,
                isNewUser(founder, item));
    }

    @Override
    public IPage<FoundItemVO> listPublic(ItemQueryRequest request, Long viewerId) {
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        // 公开列表只对外暴露两种状态：公开待认领(1)、已领取(3，供被冒领人核对)
        // 其余状态（作废0/丢失8/待投放6/归还中7/超期5/归档4）一律不对外，
        // 否则未登录用户传 itemStatus 就能枚举全部下架件与内部处理中的物品
        int queryStatus = request.getItemStatus() == null
                ? ItemStatus.PUBLIC.getCode() : request.getItemStatus();
        if (queryStatus != ItemStatus.PUBLIC.getCode()
                && queryStatus != ItemStatus.PICKED_UP.getCode()) {
            queryStatus = ItemStatus.PUBLIC.getCode();
        }

        // 「已领取」只给本人核对用：游客一律返回空（不能匿名枚举别人领走了哪些东西）；
        // 登录用户也只能看到与自己相关的记录 —— 自己发布的、或自己认领过的
        if (queryStatus == ItemStatus.PICKED_UP.getCode()) {
            if (viewerId == null) {
                return new Page<>(request.getPage(), request.getSize());
            }
            wrapper.eq(FoundItem::getItemStatus, queryStatus)
                    .and(w -> w.eq(FoundItem::getFounderId, viewerId)
                            .or().apply("EXISTS (SELECT 1 FROM claim_apply ca "
                                    + "WHERE ca.item_id = found_item.id AND ca.claimer_id = {0})", viewerId));
        } else {
            wrapper.eq(FoundItem::getItemStatus, queryStatus);
        }

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(FoundItem::getTitle, request.getKeyword())
                    .or().like(FoundItem::getDescription, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(FoundItem::getCategory, request.getCategory());
        }
        if (request.getDropPointId() != null) {
            wrapper.eq(FoundItem::getDropPointId, request.getDropPointId());
        }

        wrapper.orderByDesc(FoundItem::getPublishedAt);

        IPage<FoundItem> page = foundItemMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);

        return convertPage(page);
    }

    @Override
    public FoundItemVO getById(Long id) {
        FoundItem item = getItemOrThrow(id);
        return convertWithNames(List.of(item)).get(0);
    }

    @Override
    @Transactional
    public void invalidate(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);

        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以作废");
        }
        // 允许在待投放(6)或公开(1)状态下作废
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())
                && !item.getItemStatus().equals(ItemStatus.PENDING_PLACEMENT.getCode())) {
            throw new BusinessException(409, "物品当前状态不可作废");
        }

        item.setItemStatus(ItemStatus.VOIDED.getCode());
        foundItemMapper.updateById(item);

        LambdaQueryWrapper<HandInLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandInLog::getItemId, id);
        HandInLog log = handInLogMapper.selectOne(wrapper);
        if (log != null) {
            log.setHandInStatus(3);
            handInLogMapper.updateById(log);
        }
    }

    @Override
    @Transactional
    public void markPlaced(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);

        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以确认已投放");
        }
        if (!item.getItemStatus().equals(ItemStatus.PENDING_PLACEMENT.getCode())) {
            throw new BusinessException(409, "只有待投放状态的物品可以标记已投放");
        }

        // 草稿不占额度，但「点已投放」这一步就是真正占用每周公开额度的时刻：
        // 不公开不审核也不受限，一旦要公开就必须过 3 条上限
        long publishedThisWeek = foundItemMapper.selectCount(new LambdaQueryWrapper<FoundItem>()
                .eq(FoundItem::getFounderId, currentUserId)
                .in(FoundItem::getItemStatus,
                        ItemStatus.PUBLIC.getCode(),
                        ItemStatus.CLAIMING.getCode(),
                        ItemStatus.PICKED_UP.getCode(),
                        ItemStatus.RETURNING.getCode(),
                        ItemStatus.EXPIRED.getCode(),
                        // 作废件同样占额度：否则「投放→作废→再发」可以无限循环绕过周限额
                        ItemStatus.VOIDED.getCode())
                .ge(FoundItem::getPublishedAt, LocalDateTime.now().minus(7, ChronoUnit.DAYS)));
        if (publishedThisWeek >= MAX_WEEKLY_PUBLISH) {
            throw new BusinessException(429,
                    "本周已公开 3 条招领，额度已用完。草稿可留在「待投放」不占额度，下周再点已投放");
        }

        // 物品转为公开待认领
        item.setItemStatus(ItemStatus.PUBLIC.getCode());
        foundItemMapper.updateById(item);

        // hand_in_log: 0(未投放) → 1(已放置实物待巡检)
        LambdaQueryWrapper<HandInLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandInLog::getItemId, id);
        HandInLog log = handInLogMapper.selectOne(wrapper);
        if (log != null) {
            log.setHandInStatus(1);
            log.setHandedInAt(LocalDateTime.now());
            handInLogMapper.updateById(log);
        }
    }

    @Override
    @Transactional
    public void forwardToNotice(Long id, Long noticeId, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())
                && !item.getItemStatus().equals(ItemStatus.CLAIMING.getCode())) {
            throw new BusinessException(409, "只有公开中的招领可以转发");
        }
        if (lostNoticeMapper.selectById(noticeId) == null) {
            throw new BusinessException(404, "寻物启事不存在");
        }
        item.setForwardedNoticeId(noticeId);
        item.setForwarderId(currentUserId);
        foundItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void archive(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);
        int status = item.getItemStatus();

        if (status == ItemStatus.PICKED_UP.getCode()) {
            // 3→4：发布者手动归档已取件物品
            if (!item.getFounderId().equals(currentUserId)) {
                throw new BusinessException(403, "只有发布者可以归档");
            }
        } else if (status == ItemStatus.EXPIRED.getCode()) {
            // 5→4：超期物品经线下处置后由管理员归档
            User operator = userMapper.selectById(currentUserId);
            if (operator == null || operator.getRole() != com.school.lostfound.enums.UserRole.SYS_ADMIN) {
                throw new BusinessException(403, "只有系统管理员可以归档超期物品");
            }
        } else {
            throw new BusinessException(409, "物品当前状态不可归档");
        }

        item.setItemStatus(ItemStatus.ARCHIVED.getCode());
        foundItemMapper.updateById(item);
    }

    @Override
    public IPage<FoundItemVO> listMy(Long currentUserId, ItemQueryRequest request) {
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getFounderId, currentUserId);

        if (request.getItemStatus() != null) {
            wrapper.eq(FoundItem::getItemStatus, request.getItemStatus());
        }

        wrapper.orderByDesc(FoundItem::getPublishedAt);

        IPage<FoundItem> page = foundItemMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);

        return convertPage(page);
    }

    /**
     * 发布时的额度检查，按「未公开不占额度」口径分两层：
     * 1）每周公开额度 3 条：只统计真正进过公开池的（公开/认领中/已领取/归还中/超期）；
     * 2）草稿上限 5 条：待投放(6)不算公开、不需审核，但同时挂太多要拦，防批量灌数据。
     */
    private void checkWeeklyLimit(Long userId) {
        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getFounderId, userId)
                .in(FoundItem::getItemStatus,
                        ItemStatus.PUBLIC.getCode(),
                        ItemStatus.CLAIMING.getCode(),
                        ItemStatus.PICKED_UP.getCode(),
                        ItemStatus.RETURNING.getCode(),
                        ItemStatus.EXPIRED.getCode(),
                        // 作废件同样占额度：否则「投放→作废→再发」可以无限循环绕过周限额
                        ItemStatus.VOIDED.getCode())
                .ge(FoundItem::getPublishedAt, weekAgo);
        if (foundItemMapper.selectCount(wrapper) >= MAX_WEEKLY_PUBLISH) {
            throw new BusinessException(429, "每周最多公开 3 条招领（待投放草稿不占额度），请下周再试");
        }

        LambdaQueryWrapper<FoundItem> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(FoundItem::getFounderId, userId)
                .eq(FoundItem::getItemStatus, ItemStatus.PENDING_PLACEMENT.getCode());
        if (foundItemMapper.selectCount(draftWrapper) >= MAX_PENDING_DRAFTS) {
            throw new BusinessException(429,
                    "待投放草稿最多 5 条，请先把实物投放并点「已投放」，或作废不再需要的草稿");
        }
    }

    private IPage<FoundItemVO> convertPage(IPage<FoundItem> page) {
        List<FoundItemVO> voList = convertWithNames(page.getRecords());
        Page<FoundItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private List<FoundItemVO> convertWithNames(List<FoundItem> items) {
        List<Long> founderIds = items.stream()
                .map(FoundItem::getFounderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> dropPointIds = items.stream()
                .map(FoundItem::getDropPointId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<User> founders = founderIds.isEmpty() ? List.of()
                : userMapper.selectBatchIds(founderIds);

        Map<Long, String> founderNames = founders.stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getRealName() == null ? "" : u.getRealName(), (a, b) -> a));

        Map<Long, User> foundersById = founders.stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a,b) -> a));

        List<DropPoint> dropPoints = dropPointIds.isEmpty() ? List.of()
                : dropPointMapper.selectBatchIds(dropPointIds);

        Map<Long, String> dropPointNames = dropPoints.stream()
                .collect(Collectors.toMap(DropPoint::getId, DropPoint::getName));

        List<Long> pointAdminIds = dropPoints.stream()
                .map(DropPoint::getAdminId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> pointAdmins = pointAdminIds.isEmpty() ? java.util.Collections.emptyMap()
                : userMapper.selectBatchIds(pointAdminIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
        Map<Long, Long> pointAdminIdByPoint = dropPoints.stream()
                .filter(dp -> dp.getAdminId() != null)
                .collect(Collectors.toMap(DropPoint::getId, DropPoint::getAdminId, (a, b) -> a));

        return items.stream()
                .map(item -> {
                    FoundItemVO vo = FoundItemVO.fromEntity(
                            item,
                            founderNames.get(item.getFounderId()),
                            dropPointNames.get(item.getDropPointId()),
                            isNewUser(foundersById.get(item.getFounderId()), item));
                    Long adminId = pointAdminIdByPoint.get(item.getDropPointId());
                    User admin = adminId == null ? null : pointAdmins.get(adminId);
                    if (admin != null) {
                        vo.setDropPointAdminName(admin.getRealName());
                        vo.setDropPointAdminPhone(admin.getPhone());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private boolean isNewUser(User founder, FoundItem item) {
        return founder != null && founder.getCreatedAt() != null && item.getPublishedAt() != null
                && item.getPublishedAt().isBefore(founder.getCreatedAt().plusDays(NEW_USER_DAYS));
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long id) {
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }

        // dispute/report_log 外键无级联，需先删，否则删除被阻塞
        disputeMapper.delete(new LambdaQueryWrapper<Dispute>().eq(Dispute::getItemId, id));
        reportLogMapper.delete(new LambdaQueryWrapper<ReportLog>().eq(ReportLog::getItemId, id));
        foundItemMapper.deleteById(id);
    }

    private FoundItem getItemOrThrow(Long id) {
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        return item;
    }
}
