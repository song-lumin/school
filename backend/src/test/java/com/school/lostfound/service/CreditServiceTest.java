package com.school.lostfound.service;

import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.impl.CreditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private CreditLogMapper creditLogMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CreditServiceImpl creditService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setCreditScore(100);
    }

    @Test
    void issueCredit_positive_shouldUpdateScoreInsertLogAndCache() {
        when(userMapper.selectById(2L)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        creditService.issueCredit(2L, 3, "PICKUP_ISSUE", 100L, 10L, "失主取件完成", 1L);

        assertEquals(103, user.getCreditScore());
        verify(userMapper).updateById(user);

        ArgumentCaptor<CreditLog> captor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(captor.capture());
        CreditLog log = captor.getValue();
        assertEquals(3, log.getChangeAmount());
        assertEquals("PICKUP_ISSUE", log.getOperationType());
        assertEquals(100L, log.getRelatedItemId());

        verify(valueOperations).set(eq("credit:user:2"), eq(103), any(Duration.class));
    }

    @Test
    void issueCredit_negative_shouldDecreaseScore() {
        when(userMapper.selectById(2L)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        creditService.issueCredit(2L, -3, "ROLLBACK", 100L, 10L, "举报成立回滚", 1L);

        assertEquals(97, user.getCreditScore());
        ArgumentCaptor<CreditLog> captor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(captor.capture());
        assertEquals(-3, captor.getValue().getChangeAmount());
    }

    @Test
    void issueCredit_shouldSkipWhenUserNotExists() {
        when(userMapper.selectById(999L)).thenReturn(null);

        creditService.issueCredit(999L, 3, "PICKUP_ISSUE", 100L, null, "test", 1L);

        verify(creditLogMapper, never()).insert(any(CreditLog.class));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void rollbackCredits_shouldSkipWhenNoPickupIssued() {
        when(creditLogMapper.selectCount(any())).thenReturn(0L);

        creditService.rollbackCredits(100L, 10L, 2L, "纠纷回滚", 1L);

        verify(creditLogMapper, never()).insert(any(CreditLog.class));
    }

    @Test
    void rollbackCredits_shouldSkipWhenAlreadyRolledBack() {
        // 第一次 count 查询：有 PICKUP_ISSUE 记录；第二次：已有 ROLLBACK 记录
        when(creditLogMapper.selectCount(any())).thenReturn(1L);

        creditService.rollbackCredits(100L, 10L, 2L, "纠纷回滚", 1L);

        verify(creditLogMapper, never()).insert(any(CreditLog.class));
    }

    @Test
    void rollbackCredits_shouldIssueNegativeLogWhenEligible() {
        // 第一次 count：有 PICKUP_ISSUE；第二次 count：无 ROLLBACK → 执行回滚
        when(creditLogMapper.selectCount(any()))
                .thenReturn(1L)
                .thenReturn(0L);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        creditService.rollbackCredits(100L, 10L, 2L, "纠纷回滚", 1L);

        ArgumentCaptor<CreditLog> captor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(captor.capture());
        List<CreditLog> inserted = captor.getAllValues();
        assertEquals(-1, inserted.get(0).getChangeAmount());
        assertEquals("ROLLBACK", inserted.get(0).getOperationType());
        assertEquals(99, user.getCreditScore());
    }
}
