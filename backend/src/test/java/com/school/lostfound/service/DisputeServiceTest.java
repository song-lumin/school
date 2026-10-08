package com.school.lostfound.service;

import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.Dispute;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.impl.DisputeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeMapper disputeMapper;
    @Mock
    private ClaimApplyMapper claimApplyMapper;

    @Mock
    private FoundItemMapper foundItemMapper;

    @Mock
    private CreditLogMapper creditLogMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    private static final Long CLAIMER_ID = 2L;
    private static final Long PUBLISHER_ID = 1L;
    private static final Long ITEM_ID = 100L;
    private static final Long APPLY_ID = 10L;
    private static final Long ADMIN_ID = 9L;

    private FoundItem item;
    private ClaimApply completedApply;

    @BeforeEach
    void setUp() {
        item = new FoundItem();
        item.setId(ITEM_ID);
        item.setFounderId(PUBLISHER_ID);
        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        item.setTitle("黑色钱包");

        completedApply = new ClaimApply();
        completedApply.setId(APPLY_ID);
        completedApply.setItemId(ITEM_ID);
        completedApply.setClaimerId(CLAIMER_ID);
        completedApply.setApplyStatus(ClaimStatus.COMPLETED.getCode());
        completedApply.setPickupTime(LocalDateTime.now().minusHours(1));
    }

    private DisputeCreateRequest disputeRequest(String type) {
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setDisputeType(type);
        req.setApplyId(APPLY_ID);
        req.setDescription("拿到的钱包不是我的");
        return req;
    }

    // ===== 纠纷申诉创建 =====

    @Test
    void createDispute_shouldSucceedWithin24h() {
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);

        disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), CLAIMER_ID);

        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        verify(disputeMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(CLAIMER_ID, captor.getValue().getApplicantId());
    }

    @Test
    void createDispute_shouldRejectAfter24hWindow() {
        completedApply.setPickupTime(LocalDateTime.now().minusHours(25));
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
        verify(disputeMapper, never()).insert(any(Dispute.class));
    }

    @Test
    void createDispute_shouldRejectNonClaimer() {
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), 999L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void createDispute_shouldRejectIncompleteClaim() {
        completedApply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode());
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
    }

    @Test
    void createDispute_shouldRejectInvalidType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("WRONG_TYPE"), CLAIMER_ID));
        assertEquals(400, ex.getCode());
    }

    // ===== 冒领申诉（FALSE_CLAIM）创建 =====

    private DisputeCreateRequest falseClaimRequest() {
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setDisputeType("FALSE_CLAIM");
        req.setItemId(ITEM_ID);
        req.setDescription("这是我的物品，被别人冒领了");
        return req;
    }

    @Test
    void createFalseClaim_shouldLinkLatestCompletedApply() {
        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(claimApplyMapper.selectList(any())).thenReturn(List.of(completedApply));

        disputeService.createDispute(falseClaimRequest(), 999L);

        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        verify(disputeMapper).insert(captor.capture());
        assertEquals("FALSE_CLAIM", captor.getValue().getDisputeType());
        assertEquals(APPLY_ID, captor.getValue().getApplyId());
        assertEquals(ITEM_ID, captor.getValue().getItemId());
        assertEquals(999L, captor.getValue().getApplicantId());
    }

    @Test
    void createFalseClaim_shouldRejectWhenNoCompletedApply() {
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(claimApplyMapper.selectList(any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(falseClaimRequest(), 999L));
        assertEquals(409, ex.getCode());
        verify(disputeMapper, never()).insert(any(Dispute.class));
    }

    @Test
    void createFalseClaim_shouldRejectMissingItemId() {
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setDisputeType("FALSE_CLAIM");
        req.setDescription("test");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(req, 999L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createFalseClaim_shouldAllowLateSubmission() {
        completedApply.setPickupTime(LocalDateTime.now().minusHours(72));
        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(claimApplyMapper.selectList(any())).thenReturn(List.of(completedApply));

        disputeService.createDispute(falseClaimRequest(), 999L);

        verify(disputeMapper).insert(any(Dispute.class));
    }

    // ===== 纠纷申诉处理 =====

    private Dispute pendingDispute(String type) {
        Dispute dispute = new Dispute();
        dispute.setId(50L);
        dispute.setApplicantId(CLAIMER_ID);
        dispute.setApplyId(APPLY_ID);
        dispute.setItemId(ITEM_ID);
        dispute.setDisputeType(type);
        dispute.setStatus(0);
        return dispute;
    }

    @Test
    void handle_approvedDispute_shouldRollbackCreditsAndVoidItem() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        when(disputeMapper.selectById(50L)).thenReturn(dispute);
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        // 每次 rollbackSingle 的两个 count 查询都返回：未发放过→0 跳过；这里直接全部返回 1/0 组合简化：
        when(creditLogMapper.selectCount(any())).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(true);
        req.setHandlerNote("情况属实，同意申诉");

        disputeService.handle(50L, req, ADMIN_ID);

        assertEquals(1, dispute.getStatus());
        // 发布者 PICKUP_ISSUE -3、CHECK_ISSUE -1；认领者 PICKUP_ISSUE -1
        verify(creditService).issueCredit(eq(PUBLISHER_ID), eq(-3), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        verify(creditService).issueCredit(eq(PUBLISHER_ID), eq(-1), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        verify(creditService).issueCredit(eq(CLAIMER_ID), eq(-1), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        assertEquals(ItemStatus.VOIDED.getCode(), item.getItemStatus());
        assertEquals(1, completedApply.getCreditRollback());
    }

    @Test
    void handle_rejectedDispute_shouldNotTouchCredits() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        when(disputeMapper.selectById(50L)).thenReturn(dispute);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(false);
        req.setHandlerNote("证据不足");

        disputeService.handle(50L, req, ADMIN_ID);

        assertEquals(2, dispute.getStatus());
        verify(creditService, never()).issueCredit(anyLong(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyLong());
        assertEquals(ItemStatus.PICKED_UP.getCode(), item.getItemStatus());
    }

    @Test
    void handle_shouldRejectDuplicateHandling() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        dispute.setStatus(1);
        when(disputeMapper.selectById(50L)).thenReturn(dispute);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(false);
        req.setHandlerNote("重复处理");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.handle(50L, req, ADMIN_ID));
        assertEquals(409, ex.getCode());
    }

    @Test
    void handle_approvedFalseClaim_shouldRollbackCreditsAndReopenItem() {
        Dispute dispute = pendingDispute("FALSE_CLAIM");
        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        when(disputeMapper.selectById(50L)).thenReturn(dispute);
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(creditLogMapper.selectCount(any())).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(true);
        req.setHandlerNote("确认冒领");

        disputeService.handle(50L, req, ADMIN_ID);

        assertEquals(1, dispute.getStatus());
        verify(creditService).issueCredit(eq(PUBLISHER_ID), eq(-3), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        verify(creditService).issueCredit(eq(CLAIMER_ID), eq(-1), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        // 物品重新公开而非作废，真正失主可走正常流程认领
        assertEquals(ItemStatus.PUBLIC.getCode(), item.getItemStatus());
        assertEquals(1, completedApply.getCreditRollback());
    }

    @Test
    void getById_shouldAllowAdmin() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        when(disputeMapper.selectById(50L)).thenReturn(dispute);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        User admin = new User();
        admin.setId(ADMIN_ID);
        admin.setRole(UserRole.SYS_ADMIN);
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);

        disputeService.getById(50L, ADMIN_ID);

        verify(foundItemMapper).selectById(ITEM_ID);
    }
}
