package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.CameraLogCreateRequest;
import com.school.lostfound.dto.CameraLogHandleRequest;
import com.school.lostfound.entity.CameraLog;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.CameraLogMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CameraLogService;
import com.school.lostfound.vo.CameraLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CameraLogServiceImpl implements CameraLogService {
    private final CameraLogMapper cameraLogMapper;
    private final DropPointMapper dropPointMapper;
    private final FoundItemMapper foundItemMapper;
    private final UserMapper userMapper;

    @Override
    public IPage<CameraLogVO> list(Integer status, Long dropPointId, Long itemId, int page, int size) {
        LambdaQueryWrapper<CameraLog> query = new LambdaQueryWrapper<>();
        if (status != null) query.eq(CameraLog::getStatus, status);
        if (dropPointId != null) query.eq(CameraLog::getDropPointId, dropPointId);
        if (itemId != null) query.eq(CameraLog::getItemId, itemId);
        query.orderByAsc(CameraLog::getStatus).orderByDesc(CameraLog::getCreatedAt);
        IPage<CameraLog> result = cameraLogMapper.selectPage(new Page<>(page, size), query);
        Page<CameraLogVO> output = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        output.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return output;
    }

    @Override
    @Transactional
    public CameraLogVO create(CameraLogCreateRequest request, Long applicantId) {
        if (request.getTimeRangeStart().isAfter(request.getTimeRangeEnd())) {
            throw new BusinessException(400, "调取时间范围不正确");
        }
        DropPoint point = dropPointMapper.selectById(request.getDropPointId());
        if (point == null || point.getStatus() != 1 || point.getHasCamera() != 1) {
            throw new BusinessException(400, "投放点不存在、已停用或未配置监控");
        }
        if (request.getItemId() != null && foundItemMapper.selectById(request.getItemId()) == null) {
            throw new BusinessException(404, "关联物品不存在");
        }
        CameraLog log = new CameraLog();
        log.setDropPointId(request.getDropPointId());
        log.setItemId(request.getItemId());
        log.setApplyReason(request.getApplyReason().trim());
        log.setApplicantId(applicantId);
        log.setTimeRangeStart(request.getTimeRangeStart());
        log.setTimeRangeEnd(request.getTimeRangeEnd());
        log.setStatus(0);
        cameraLogMapper.insert(log);
        return toVO(log);
    }

    @Override
    @Transactional
    public void handle(Long id, CameraLogHandleRequest request, Long handlerId) {
        CameraLog log = cameraLogMapper.selectById(id);
        if (log == null) throw new BusinessException(404, "监控调取记录不存在");
        if (log.getStatus() != 0) throw new BusinessException(409, "只有待处理记录可以操作");
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)
                || !StringUtils.hasText(request.getResultNote())) {
            throw new BusinessException(400, "处理状态只能为已批准或已拒绝，且必须填写处理说明");
        }
        log.setStatus(request.getStatus());
        log.setResultNote(request.getResultNote().trim());
        log.setHandlerId(handlerId);
        log.setHandledAt(LocalDateTime.now());
        cameraLogMapper.updateById(log);
    }

    private CameraLogVO toVO(CameraLog log) {
        CameraLogVO vo = CameraLogVO.fromEntity(log);
        DropPoint point = dropPointMapper.selectById(log.getDropPointId());
        FoundItem item = log.getItemId() == null ? null : foundItemMapper.selectById(log.getItemId());
        User applicant = userMapper.selectById(log.getApplicantId());
        User handler = log.getHandlerId() == null ? null : userMapper.selectById(log.getHandlerId());
        vo.setDropPointName(point == null ? null : point.getName());
        vo.setItemTitle(item == null ? null : item.getTitle());
        vo.setApplicantName(applicant == null ? null : applicant.getRealName());
        vo.setHandlerName(handler == null ? null : handler.getRealName());
        return vo;
    }
}
