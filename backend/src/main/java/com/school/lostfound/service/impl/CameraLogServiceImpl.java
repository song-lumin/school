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

/**
 * 监控录像调取服务实现类（CameraLogService 接口的实现）。
 *
 * <p>功能：管理"调取投放点监控录像"的申请全流程——
 * 用户提交调取申请（说明时间范围和理由）-> 管理员审批（批准/拒绝 + 处理说明）。
 *
 * <p>为什么需要它：失物可能被拍进投放点监控，但监控涉及公共区域影像隐私，
 * 不能随意查看。系统通过"申请-审批"流程留痕：谁在什么时间、以什么理由调取了哪个点位的录像、
 * 审批人是谁，全程有 CameraLog 记录可追溯，兼顾寻物效率与隐私合规。
 */
@Service
@RequiredArgsConstructor
public class CameraLogServiceImpl implements CameraLogService {
    private final CameraLogMapper cameraLogMapper;
    private final DropPointMapper dropPointMapper;
    private final FoundItemMapper foundItemMapper;
    private final UserMapper userMapper;

    /**
     * 分页查询监控调取记录列表（管理端）。
     *
     * <p>支持按处理状态、投放点、关联物品三个条件组合过滤；
     * 排序规则：先按状态升序（status=0 待处理的排最前，方便管理员优先处理），
     * 再按创建时间倒序（同状态内最新申请在前）。
     *
     * @param status      处理状态（0 待处理 / 1 已批准 / 2 已拒绝），可空
     * @param dropPointId 投放点 ID，可空
     * @param itemId      关联物品 ID，可空
     * @param page        页码
     * @param size        每页条数
     */
    @Override
    public IPage<CameraLogVO> list(Integer status, Long dropPointId, Long itemId, int page, int size) {
        LambdaQueryWrapper<CameraLog> query = new LambdaQueryWrapper<>();
        // 三个条件均为可选过滤：传了才拼进 WHERE，实现动态组合查询
        if (status != null) query.eq(CameraLog::getStatus, status);
        if (dropPointId != null) query.eq(CameraLog::getDropPointId, dropPointId);
        if (itemId != null) query.eq(CameraLog::getItemId, itemId);
        // 待处理(0) < 已批准(1) < 已拒绝(2)，升序即把待处理置顶
        query.orderByAsc(CameraLog::getStatus).orderByDesc(CameraLog::getCreatedAt);
        IPage<CameraLog> result = cameraLogMapper.selectPage(new Page<>(page, size), query);
        // 实体分页转 VO 分页（保留分页元信息：当前页、页大小、总条数）
        Page<CameraLogVO> output = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        output.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return output;
    }

    /**
     * 用户提交监控调取申请。
     *
     * <p>业务规则（三条前置校验）：
     * 1. 时间范围必须合法（开始时间不能晚于结束时间）；
     * 2. 目标投放点必须存在、处于启用状态且确实配有监控（无摄像头的点位无法调取）；
     * 3. 若关联了物品，物品必须真实存在。
     *
     * <p>新申请初始状态固定为 0（待处理），等待管理员在 handle 中审批。
     *
     * @param request     申请内容（投放点、时间范围、申请理由、可选关联物品）
     * @param applicantId 申请人 ID（从 JWT 中解析，防伪造）
     * @return 创建成功的记录 VO
     */
    @Override
    @Transactional
    public CameraLogVO create(CameraLogCreateRequest request, Long applicantId) {
        // 校验 1：时间范围合法性（开始 > 结束 属于无效区间）
        if (request.getTimeRangeStart().isAfter(request.getTimeRangeEnd())) {
            throw new BusinessException(400, "调取时间范围不正确");
        }
        // 校验 2：投放点必须存在、启用中(status=1)且配有监控(hasCamera=1)
        DropPoint point = dropPointMapper.selectById(request.getDropPointId());
        if (point == null || point.getStatus() != 1 || point.getHasCamera() != 1) {
            throw new BusinessException(400, "投放点不存在、已停用或未配置监控");
        }
        // 校验 3：关联物品可选，但传了就必须真实存在
        if (request.getItemId() != null && foundItemMapper.selectById(request.getItemId()) == null) {
            throw new BusinessException(404, "关联物品不存在");
        }
        CameraLog log = new CameraLog();
        log.setDropPointId(request.getDropPointId());
        log.setItemId(request.getItemId());
        // 申请理由去首尾空白后落库，保证留痕数据整洁
        log.setApplyReason(request.getApplyReason().trim());
        log.setApplicantId(applicantId);
        log.setTimeRangeStart(request.getTimeRangeStart());
        log.setTimeRangeEnd(request.getTimeRangeEnd());
        log.setStatus(0); // 初始状态：0=待处理
        cameraLogMapper.insert(log);
        return toVO(log);
    }

    /**
     * 管理员审批监控调取申请。
     *
     * <p>业务规则：
     * - 记录必须存在且处于待处理(0)状态，已审批的记录不允许二次操作（状态机保护）；
     * - 审批结论只能是 1（已批准）或 2（已拒绝），且处理说明必填——
     *   拒绝时说明理由、批准时留下调取依据，保证影像调取全程可追责。
     *
     * @param id        调取记录 ID
     * @param request   审批内容（status + resultNote）
     * @param handlerId 审批人 ID（从 JWT 中解析）
     */
    @Override
    @Transactional
    public void handle(Long id, CameraLogHandleRequest request, Long handlerId) {
        CameraLog log = cameraLogMapper.selectById(id);
        if (log == null) throw new BusinessException(404, "监控调取记录不存在");
        // 状态机保护：只有待处理(0)记录能被审批，防止重复审批覆盖结果
        if (log.getStatus() != 0) throw new BusinessException(409, "只有待处理记录可以操作");
        // 审批结论必须为 1 或 2，且处理说明不能为空（审批留痕的强制性要求）
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)
                || !StringUtils.hasText(request.getResultNote())) {
            throw new BusinessException(400, "处理状态只能为已批准或已拒绝，且必须填写处理说明");
        }
        log.setStatus(request.getStatus());
        log.setResultNote(request.getResultNote().trim());
        log.setHandlerId(handlerId);          // 记录审批人，便于追责
        log.setHandledAt(LocalDateTime.now()); // 记录审批时间
        cameraLogMapper.updateById(log);
    }

    /**
     * 实体转 VO：补充展示所需的关联名称。
     *
     * <p>CameraLog 实体里只存各类 ID（投放点/物品/申请人/审批人），
     * 前端展示需要名称和标题，这里逐个回查并填充到 VO。
     * 各关联对象均做了空值防御（记录可能未关联物品、尚未被审批）。
     *
     * @param log 调取记录实体
     * @return 携带关联名称的 VO
     */
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
