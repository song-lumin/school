package com.school.lostfound.service;

import com.school.lostfound.dto.CameraLogHandleRequest;
import com.school.lostfound.entity.CameraLog;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.CameraLogMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.impl.CameraLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 摄像头日志（CameraLog）处理功能的单元测试。
 *
 * <p>测试模块：CameraLogServiceImpl#handle —— 监控摄像头上报捡获日志后的人工处理流程。</p>
 * <p>覆盖场景：</p>
 * <ul>
 *   <li>处理结果必须填写备注（resultNote），否则拒绝</li>
 *   <li>已处理完成的日志不允许再次处理（幂等/状态机保护）</li>
 * </ul>
 * <p>方式：Mockito 纯单元测试，Mapper 全部 mock，只测 Service 层业务规则。</p>
 */
@ExtendWith(MockitoExtension.class)
class CameraLogServiceTest {
    // mock 各依赖 Mapper，注入到被测的 CameraLogServiceImpl
    @Mock private CameraLogMapper cameraLogMapper;
    @Mock private DropPointMapper dropPointMapper;
    @Mock private FoundItemMapper foundItemMapper;
    @Mock private UserMapper userMapper;
    @InjectMocks private CameraLogServiceImpl service;

    /**
     * 验证处理日志时必须填写处理结果备注。
     * 预期：resultNote 为空白字符串时抛出 BusinessException，且不执行任何数据库更新。
     * 为什么：备注是"物品交到哪个点位/如何处理"的留痕依据，缺失会导致流程不可追溯。
     */
    @Test
    void handlingRequiresResultNote() {
        CameraLog log = new CameraLog();
        log.setId(12L);
        log.setStatus(0); // 待处理状态
        when(cameraLogMapper.selectById(12L)).thenReturn(log); // mock 查到一条待处理日志
        CameraLogHandleRequest request = new CameraLogHandleRequest();
        request.setStatus(1);
        request.setResultNote("  "); // 空白备注，应被校验拒绝

        // 预期抛出业务异常
        assertThrows(BusinessException.class, () -> service.handle(12L, request, 4L));
        // 验证没有发生任何更新操作（失败请求不应产生副作用）
        verify(cameraLogMapper, never()).updateById(any(CameraLog.class));
    }

    /**
     * 验证已处理完成的日志不能被重复处理。
     * 预期：日志状态为 3（已处理）时再次 handle 抛出 BusinessException。
     * 为什么：防止同一日志被多次处理导致重复通知/重复流转等状态混乱。
     */
    @Test
    void completedDisputeLogCannotBeHandledAgain() {
        CameraLog log = new CameraLog();
        log.setId(12L);
        log.setStatus(3); // 已处理完成的状态
        when(cameraLogMapper.selectById(12L)).thenReturn(log); // mock 一条已完成日志
        CameraLogHandleRequest request = new CameraLogHandleRequest();
        request.setStatus(2);
        request.setResultNote("已处理"); // 备注合法，仅状态不允许

        // 预期因状态不合法而抛出业务异常
        assertThrows(BusinessException.class, () -> service.handle(12L, request, 4L));
    }
}
