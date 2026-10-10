package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.ReportCreateRequest;
import com.school.lostfound.dto.ReportHandleRequest;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.LostNotice;
import com.school.lostfound.entity.ReportLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.LostNoticeMapper;
import com.school.lostfound.mapper.ReportLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.ReportService;
import com.school.lostfound.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    public static final String TYPE_FAKE_PUBLISH = "FAKE_PUBLISH";
    public static final String TYPE_DESC_MISMATCH = "DESC_MISMATCH";
    public static final String TYPE_OTHER = "OTHER";
    private static final Set<String> VALID_TYPES = Set.of(TYPE_FAKE_PUBLISH, TYPE_DESC_MISMATCH, TYPE_OTHER);

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_VALID = 1;
    private static final int STATUS_INVALID = 2;

    private final ReportLogMapper reportLogMapper;
    private final FoundItemMapper foundItemMapper;
    private final LostNoticeMapper lostNoticeMapper;
    private final UserMapper userMapper;
    private final CreditLogMapper creditLogMapper;
    private final CreditService creditService;

    @Override
    @Transactional
    public ReportVO create(ReportCreateRequest request, Long reporterId) {
        if (!VALID_TYPES.contains(request.getReportType())) {
            throw new BusinessException(400, "无效的举报类型");
        }
        String targetType = request.getTargetType() == null ? "ITEM" : request.getTargetType();

        ReportLog report = new ReportLog();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setReportType(request.getReportType());
        report.setDescription(request.getDescription());
        report.setEvidenceImages(request.getEvidenceImages());
        report.setStatus(STATUS_PENDING);

        String targetTitle;
        if ("NOTICE".equals(targetType)) {
            if (request.getNoticeId() == null) {
                throw new BusinessException(400, "被举报启事ID不能为空");
            }
            LostNotice notice = lostNoticeMapper.selectById(request.getNoticeId());
            if (notice == null) {
                throw new BusinessException(404, "被举报启事不存在");
            }
            if (reporterId.equals(notice.getPublisherId())) {
                throw new BusinessException(403, "不能举报自己发布的启事");
            }
            report.setNoticeId(notice.getId());
            targetTitle = notice.getTitle();
        } else {
            if (request.getItemId() == null) {
                throw new BusinessException(400, "被举报物品ID不能为空");
            }
            FoundItem item = foundItemMapper.selectById(request.getItemId());
            if (item == null) {
                throw new BusinessException(404, "被举报物品不存在");
            }
            if (reporterId.equals(item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId())) {
                throw new BusinessException(403, "不能举报自己发布的物品");
            }
            report.setItemId(item.getId());
            targetTitle = item.getTitle();
        }

        reportLogMapper.insert(report);
        log.info("举报已提交：reportId={}, type={}", report.getId(), targetType);
        return toVO(report, null, targetTitle);
    }

    @Override
    public IPage<ReportVO> list(Integer status, String reportType, int page, int size) {
        LambdaQueryWrapper<ReportLog> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ReportLog::getStatus, status);
        }
        if (StringUtils.hasText(reportType)) {
            wrapper.eq(ReportLog::getReportType, reportType);
        }
        wrapper.orderByAsc(ReportLog::getStatus).orderByDesc(ReportLog::getCreatedAt);

        IPage<ReportLog> result = reportLogMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> reporterIds = result.getRecords().stream()
                .map(ReportLog::getReporterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> reporterNames = reporterIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(reporterIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName));

        List<Long> itemIds = result.getRecords().stream()
                .map(ReportLog::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> itemTitles = itemIds.isEmpty() ? Map.of()
                : foundItemMapper.selectBatchIds(itemIds).stream()
                        .collect(Collectors.toMap(FoundItem::getId, FoundItem::getTitle));

        Page<ReportVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(r -> toVO(r, reporterNames.get(r.getReporterId()), itemTitles.get(r.getItemId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    public void handle(Long id, ReportHandleRequest request, Long operatorId) {
        ReportLog report = reportLogMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(404, "举报工单不存在");
        }
        if (!report.getStatus().equals(STATUS_PENDING)) {
            throw new BusinessException(409, "该举报已处理，请勿重复操作");
        }

        if (request.getValid()) {
            report.setStatus(STATUS_VALID);
            handleValid(report, request, operatorId);
        } else {
            report.setStatus(STATUS_INVALID);
        }

        report.setHandlerNote(request.getHandlerNote());
        report.setHandlerId(operatorId);
        report.setHandledAt(LocalDateTime.now());
        reportLogMapper.updateById(report);
    }

    /** 举报成立：物品作废 + 回滚发布人全部相关积分，可选封禁账号 */
    private void handleValid(ReportLog report, ReportHandleRequest request, Long operatorId) {
        String reason = "举报成立（举报#" + report.getId() + "）：" + request.getHandlerNote();

        if ("NOTICE".equals(report.getTargetType())) {
            LostNotice notice = lostNoticeMapper.selectById(report.getNoticeId());
            if (notice == null) {
                throw new BusinessException(404, "关联启事不存在");
            }
            notice.setStatus(3); // 3=管理员下架
            notice.setTakedownReason(reason);
            notice.setTakedownAt(LocalDateTime.now());
            lostNoticeMapper.updateById(notice);
            return;
        }

        FoundItem item = foundItemMapper.selectById(report.getItemId());
        if (item == null) {
            throw new BusinessException(404, "关联物品不存在");
        }

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();

        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), reason, operatorId);
        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), reason, operatorId);

        item.setItemStatus(ItemStatus.VOIDED.getCode());
        item.setTakedownReason(reason);
        item.setTakedownAt(LocalDateTime.now());
        foundItemMapper.updateById(item);

        if (Boolean.TRUE.equals(request.getBanPublisher())) {
            User publisher = userMapper.selectById(publisherId);
            if (publisher != null) {
                publisher.setStatus(0);
                userMapper.updateById(publisher);
                log.info("举报处理：发布者 {} 已被禁用", publisherId);
            }
        }

        log.info("举报成立处理完成：reportId={}, itemId={}", report.getId(), item.getId());
    }

    private void rollbackSingle(Long userId, String operationType, int amount,
                                Long itemId, String reason, Long operatorId) {
        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, operationType)
                .eq(CreditLog::getRelatedItemId, itemId);
        if (creditLogMapper.selectCount(issuedWrapper) == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, -amount);
        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return;
        }

        creditService.issueCredit(userId, -amount, "ROLLBACK", itemId, null, reason, operatorId);
    }


    @Override
    public IPage<ReportVO> listMy(Long reporterId, int page, int size) {
        LambdaQueryWrapper<ReportLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportLog::getReporterId, reporterId)
               .orderByDesc(ReportLog::getCreatedAt);
        IPage<ReportLog> result = reportLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<Long> itemIds = result.getRecords().stream().map(ReportLog::getItemId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> itemTitles = itemIds.isEmpty() ? Map.of()
            : foundItemMapper.selectBatchIds(itemIds).stream().collect(Collectors.toMap(FoundItem::getId, FoundItem::getTitle));
        List<Long> noticeIds = result.getRecords().stream().map(ReportLog::getNoticeId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> noticeTitles = noticeIds.isEmpty() ? Map.of()
            : lostNoticeMapper.selectBatchIds(noticeIds).stream().collect(Collectors.toMap(LostNotice::getId, LostNotice::getTitle));
        Page<ReportVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(r -> {
            String title = r.getItemId() != null ? itemTitles.get(r.getItemId()) : noticeTitles.get(r.getNoticeId());
            return toVO(r, null, title);
        }).collect(Collectors.toList()));
        return voPage;
    }
    private ReportVO toVO(ReportLog r, String reporterName, String itemTitle) {
        return ReportVO.fromEntity(r, reporterName, itemTitle);
    }
}
