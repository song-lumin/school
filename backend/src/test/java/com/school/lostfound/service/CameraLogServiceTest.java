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

@ExtendWith(MockitoExtension.class)
class CameraLogServiceTest {
    @Mock private CameraLogMapper cameraLogMapper;
    @Mock private DropPointMapper dropPointMapper;
    @Mock private FoundItemMapper foundItemMapper;
    @Mock private UserMapper userMapper;
    @InjectMocks private CameraLogServiceImpl service;

    @Test
    void handlingRequiresResultNote() {
        CameraLog log = new CameraLog();
        log.setId(12L);
        log.setStatus(0);
        when(cameraLogMapper.selectById(12L)).thenReturn(log);
        CameraLogHandleRequest request = new CameraLogHandleRequest();
        request.setStatus(1);
        request.setResultNote("  ");

        assertThrows(BusinessException.class, () -> service.handle(12L, request, 4L));
        verify(cameraLogMapper, never()).updateById(any(CameraLog.class));
    }

    @Test
    void completedDisputeLogCannotBeHandledAgain() {
        CameraLog log = new CameraLog();
        log.setId(12L);
        log.setStatus(3);
        when(cameraLogMapper.selectById(12L)).thenReturn(log);
        CameraLogHandleRequest request = new CameraLogHandleRequest();
        request.setStatus(2);
        request.setResultNote("已处理");

        assertThrows(BusinessException.class, () -> service.handle(12L, request, 4L));
    }
}
