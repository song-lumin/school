package com.school.lostfound.service;

import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.dto.ClaimRejectRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.impl.ClaimServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimApplyMapper claimApplyMapper;

    @Mock
    private FoundItemMapper foundItemMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DropPointMapper dropPointMapper;

    @Mock
    private CreditService creditService;

    @Mock
    private RiskControlService riskControlService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ClaimServiceImpl claimService;

    private static final Long ITEM_ID = 100L;
    private static final Long FOUNDER_ID = 1L;
    private static final Long CLAIMER_ID = 2L;

    private FoundItem publicItem;

    @BeforeEach
    void setUp() {
        publicItem = new FoundItem();
        publicItem.setId(ITEM_ID);
        publicItem.setFounderId(FOUNDER_ID);
        publicItem.setItemStatus(ItemStatus.PUBLIC.getCode());
        publicItem.setTitle("黑色钱包");
        publicItem.setClaimQuestion("钱包里有什么证件？");
    }

    private ClaimApplyRequest applyRequest(String answer) {
        ClaimApplyRequest req = new ClaimApplyRequest();
        req.setItemId(ITEM_ID);
        req.setAnswer(answer);
        return req;
    }

    // ===== apply =====

    @Test
    void apply_shouldRejectWhenSensitiveWord() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("含敏感词"), CLAIMER_ID));
        assertEquals(400, ex.getCode());
        verify(claimApplyMapper, never()).insert(any(ClaimApply.class));
    }

    @Test
    void apply_shouldRejectWhenItemNotPublic() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        publicItem.setItemStatus(ItemStatus.PICKED_UP.getCode());
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("身份证"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
    }

    @Test
    void apply_shouldRejectSelfClaim() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("身份证"), FOUNDER_ID));
        assertEquals(400, ex.getCode());
    }

    @Test
    void apply_shouldRejectWhenAlreadyPending() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        ClaimApply existing = new ClaimApply();
        existing.setItemId(ITEM_ID);
        existing.setClaimerId(CLAIMER_ID);
        existing.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("身份证"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
        verify(claimApplyMapper, never()).insert(any(ClaimApply.class));
    }

    @Test
    void apply_shouldInsertNewWhenFirstTime() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(claimApplyMapper.selectOne(any())).thenReturn(null);

        User claimer = new User();
        claimer.setId(CLAIMER_ID);
        claimer.setRealName("张三");
        when(userMapper.selectById(CLAIMER_ID)).thenReturn(claimer);

        claimService.apply(applyRequest("身份证"), CLAIMER_ID);

        ArgumentCaptor<ClaimApply> captor = ArgumentCaptor.forClass(ClaimApply.class);
        verify(claimApplyMapper).insert(captor.capture());
        ClaimApply saved = captor.getValue();
        assertEquals(ClaimStatus.PENDING.getCode(), saved.getApplyStatus());
        assertEquals(CLAIMER_ID, saved.getClaimerId());
        assertEquals(0, saved.getRejectCount());
    }

    @Test
    void apply_shouldRejectWhenLocked() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        ClaimApply locked = new ClaimApply();
        locked.setItemId(ITEM_ID);
        locked.setClaimerId(CLAIMER_ID);
        locked.setApplyStatus(ClaimStatus.LOCKED.getCode());
        locked.setUpdatedAt(java.time.LocalDateTime.now());
        when(claimApplyMapper.selectOne(any())).thenReturn(locked);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("再试一次"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
    }

    @Test
    void apply_reapplyAfterReject_shouldKeepAccumulatedRejectCount() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        ClaimApply rejected = new ClaimApply();
        rejected.setId(11L);
        rejected.setItemId(ITEM_ID);
        rejected.setClaimerId(CLAIMER_ID);
        rejected.setApplyStatus(ClaimStatus.REJECTED.getCode());
        rejected.setRejectCount(2);
        when(claimApplyMapper.selectOne(any())).thenReturn(rejected);

        User claimer = new User();
        claimer.setId(CLAIMER_ID);
        claimer.setRealName("张三");
        when(userMapper.selectById(CLAIMER_ID)).thenReturn(claimer);

        claimService.apply(applyRequest("第二次尝试"), CLAIMER_ID);

        // 重新申请保留累计拒绝次数（再被拒1次即达3次锁定）
        assertEquals(2, rejected.getRejectCount());
        assertEquals(ClaimStatus.PENDING.getCode(), rejected.getApplyStatus());
        verify(claimApplyMapper).updateById(rejected);
        verify(claimApplyMapper, never()).insert(any(ClaimApply.class));
    }

    // ===== approve =====

    @Test
    void approve_shouldSetWaitingPickupAndItemClaiming() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        claimService.approve(10L, FOUNDER_ID);

        assertEquals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode(), apply.getApplyStatus());
        assertEquals(ItemStatus.CLAIMING.getCode(), publicItem.getItemStatus());
        verify(claimApplyMapper).updateById(apply);
        verify(foundItemMapper).updateById(publicItem);
    }

    @Test
    void approve_shouldRejectWhenNotFounder() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(userMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.approve(10L, 999L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void approve_shouldRejectWhenAlreadyHandled() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.COMPLETED.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.approve(10L, FOUNDER_ID));
        assertEquals(409, ex.getCode());
    }

    // ===== 管理员审核 =====

    @Test
    void approve_shouldAllowSysAdmin() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        User sysAdmin = new User();
        sysAdmin.setId(50L);
        sysAdmin.setRole(UserRole.SYS_ADMIN);
        when(userMapper.selectById(50L)).thenReturn(sysAdmin);

        claimService.approve(10L, 50L);

        assertEquals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode(), apply.getApplyStatus());
        assertEquals(ItemStatus.CLAIMING.getCode(), publicItem.getItemStatus());
    }

    @Test
    void approve_shouldAllowDropPointAdminOfItemSite() {
        publicItem.setDropPointId(300L);
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        User pointAdmin = new User();
        pointAdmin.setId(60L);
        pointAdmin.setRole(UserRole.POINT_ADMIN);
        when(userMapper.selectById(60L)).thenReturn(pointAdmin);

        DropPoint point = new DropPoint();
        point.setId(300L);
        point.setAdminId(60L);
        when(dropPointMapper.selectById(300L)).thenReturn(point);

        claimService.approve(10L, 60L);

        assertEquals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode(), apply.getApplyStatus());
    }

    @Test
    void approve_shouldRejectPointAdminOfOtherSite() {
        publicItem.setDropPointId(300L);
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        User pointAdmin = new User();
        pointAdmin.setId(61L);
        pointAdmin.setRole(UserRole.POINT_ADMIN);
        when(userMapper.selectById(61L)).thenReturn(pointAdmin);

        DropPoint point = new DropPoint();
        point.setId(300L);
        point.setAdminId(60L);
        when(dropPointMapper.selectById(300L)).thenReturn(point);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.approve(10L, 61L));
        assertEquals(403, ex.getCode());
    }

    // ===== reject: 3 次锁定 =====

    @Test
    void reject_shouldStayRejectedBelowMaxCount() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        apply.setRejectCount(1);
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        ClaimRejectRequest req = new ClaimRejectRequest();
        req.setRejectReason("回答不正确");
        claimService.reject(10L, req, FOUNDER_ID);

        assertEquals(ClaimStatus.REJECTED.getCode(), apply.getApplyStatus());
        assertEquals(2, apply.getRejectCount());
    }

    @Test
    void reject_shouldLockAtThirdRejection() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        apply.setRejectCount(2);
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        ClaimRejectRequest req = new ClaimRejectRequest();
        req.setRejectReason("第三次回答仍不正确");
        claimService.reject(10L, req, FOUNDER_ID);

        assertEquals(3, apply.getRejectCount());
        assertEquals(ClaimStatus.LOCKED.getCode(), apply.getApplyStatus());
    }

    // ===== pickup: 双方积分发放 =====

    @Test
    void pickup_shouldCompleteAndIssueCreditsToBothParties() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        claimService.pickup(10L, "photo.jpg", "sig.png", FOUNDER_ID);

        assertEquals(ClaimStatus.COMPLETED.getCode(), apply.getApplyStatus());
        assertEquals(ItemStatus.PICKED_UP.getCode(), publicItem.getItemStatus());
        assertNotNull(apply.getPickupTime());
        // 发布者 +3，认领者 +1
        verify(creditService).issueCredit(eq(FOUNDER_ID), eq(3), eq("PICKUP_ISSUE"), eq(ITEM_ID), eq(10L), anyString(), eq(FOUNDER_ID));
        verify(creditService).issueCredit(eq(CLAIMER_ID), eq(1), eq("PICKUP_ISSUE"), eq(ITEM_ID), eq(10L), anyString(), eq(FOUNDER_ID));
    }

    @Test
    void pickup_shouldRequirePhotoAndSignature() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.pickup(10L, null, null, FOUNDER_ID));
        assertEquals(400, ex.getCode());
        verify(creditService, never()).issueCredit(anyLong(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyLong());
    }
}
