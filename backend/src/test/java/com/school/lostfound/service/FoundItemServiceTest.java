package com.school.lostfound.service;

import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.ReportLogMapper;
import com.school.lostfound.service.impl.FoundItemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoundItemServiceTest {
    @Mock
    private FoundItemMapper foundItemMapper;

    @Mock
    private DisputeMapper disputeMapper;

    @Mock
    private ReportLogMapper reportLogMapper;

    @InjectMocks
    private FoundItemServiceImpl foundItemService;

    @Test
    void deleteByAdminShouldRemoveDisputesReportsThenItem() {
        FoundItem item = new FoundItem();
        item.setId(1L);
        when(foundItemMapper.selectById(1L)).thenReturn(item);

        foundItemService.deleteByAdmin(1L);

        InOrder inOrder = inOrder(disputeMapper, reportLogMapper, foundItemMapper);
        inOrder.verify(disputeMapper).delete(any());
        inOrder.verify(reportLogMapper).delete(any());
        inOrder.verify(foundItemMapper).deleteById(1L);
    }
    @Test
    void deleteByAdminShouldThrowWhenItemNotFound() {
        when(foundItemMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> foundItemService.deleteByAdmin(999L));

        assertEquals(404, exception.getCode());
        verify(disputeMapper, never()).delete(any());
        verify(reportLogMapper, never()).delete(any());
        verify(foundItemMapper, never()).deleteById(anyLong());
    }
}
