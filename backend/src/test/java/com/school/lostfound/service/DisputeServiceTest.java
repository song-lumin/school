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

/**
 * 纠纷处理（Dispute）服务的单元测试。
 *
 * <p>测试模块：DisputeServiceImpl —— 覆盖两类纠纷的创建与处理：</p>
 * <ul>
 *   <li>ITEM_MISMATCH（物品不符）：认领者取件后 24 小时内申诉</li>
 *   <li>FALSE_CLAIM（冒领申诉）：物品发布者主张被他人冒领，不受 24 小时窗口限制</li>
 *   <li>handle：管理员裁决——通过则回滚双方积分并作废/重开物品；驳回则不动积分</li>
 * </ul>
 * <p>方式：Mockito 纯单元测试，Mapper/CreditService 全部 mock，只测 Service 层业务规则。</p>
 */
@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeMapper disputeMapper; // 纠纷工单表 mock
    @Mock
    private ClaimApplyMapper claimApplyMapper; // 认领申请表 mock

    @Mock
    private FoundItemMapper foundItemMapper; // 物品表 mock

    @Mock
    private CreditLogMapper creditLogMapper; // 积分流水表 mock（判断是否已发放/已回滚）

    @Mock
    private UserMapper userMapper; // 用户表 mock（权限判断）

    @Mock
    private CreditService creditService; // 积分回滚 mock

    @InjectMocks
    private DisputeServiceImpl disputeService;

    // 测试常用固定 ID：认领者 2、发布者 1、物品 100、申请 10、管理员 9
    private static final Long CLAIMER_ID = 2L;
    private static final Long PUBLISHER_ID = 1L;
    private static final Long ITEM_ID = 100L;
    private static final Long APPLY_ID = 10L;
    private static final Long ADMIN_ID = 9L;

    private FoundItem item;          // 已被取走的物品
    private ClaimApply completedApply; // 已完成的认领申请

    @BeforeEach
    void setUp() {
        item = new FoundItem();
        item.setId(ITEM_ID);
        item.setFounderId(PUBLISHER_ID);
        item.setItemStatus(ItemStatus.PICKED_UP.getCode()); // 默认已被取走
        item.setTitle("黑色钱包");

        completedApply = new ClaimApply();
        completedApply.setId(APPLY_ID);
        completedApply.setItemId(ITEM_ID);
        completedApply.setClaimerId(CLAIMER_ID);
        completedApply.setApplyStatus(ClaimStatus.COMPLETED.getCode()); // 已完成
        completedApply.setPickupTime(LocalDateTime.now().minusHours(1)); // 1 小时前取件（24h 窗口内）
    }

    // 构造物品不符纠纷请求（默认类型 ITEM_MISMATCH）
    private DisputeCreateRequest disputeRequest(String type) {
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setDisputeType(type);
        req.setApplyId(APPLY_ID);
        req.setDescription("拿到的钱包不是我的");
        return req;
    }

    // ===== 纠纷申诉创建 =====

    /**
     * 验证 24 小时窗口内的正常申诉可以创建。
     * 预期：插入一条 status=0（待处理）的工单，且申诉人为认领者本人。
     * 为什么：这是纠纷入口的主干路径，初始状态与申请人必须正确。
     */
    @Test
    void createDispute_shouldSucceedWithin24h() {
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);

        disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), CLAIMER_ID);

        // 捕获插入的工单，校验初始状态与申诉人
        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        verify(disputeMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getStatus()); // 0 = 待处理
        assertEquals(CLAIMER_ID, captor.getValue().getApplicantId()); // 申请人正确
    }

    /**
     * 验证超过 24 小时后不能发起物品不符申诉。
     * 预期：取件 25 小时后提交抛出 code=409 的异常，不落库。
     * 为什么：防止物品被使用多日后恶意翻案，纠纷必须有申诉时限。
     */
    @Test
    void createDispute_shouldRejectAfter24hWindow() {
        completedApply.setPickupTime(LocalDateTime.now().minusHours(25)); // 超过 24h 窗口
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
        verify(disputeMapper, never()).insert(any(Dispute.class)); // 不应创建工单
    }

    /**
     * 验证非该申请的认领者不能发起申诉。
     * 预期：999L（与申请无关的用户）提交抛出 code=403 的异常。
     * 为什么：物品不符申诉只能由实际取件的认领者发起，防止他人代诉。
     */
    @Test
    void createDispute_shouldRejectNonClaimer() {
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), 999L)); // 与申请无关的用户
        assertEquals(403, ex.getCode());
    }

    /**
     * 验证未完成取件的申请不能发起纠纷。
     * 预期：状态为 APPROVED_WAITING_PICKUP 时抛出 code=409 的异常。
     * 为什么：纠纷针对"已交接但有问题"的场景，未取件时应走取消/拒绝而非纠纷。
     */
    @Test
    void createDispute_shouldRejectIncompleteClaim() {
        completedApply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode()); // 尚未取件完成
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("ITEM_MISMATCH"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
    }

    /**
     * 验证非法的纠纷类型被拒绝。
     * 预期：类型 "WRONG_TYPE" 抛出 code=400 的异常（且在查询申请之前就拦截）。
     * 为什么：类型白名单是第一层校验，未定义的类型无法被后续流程处理。
     */
    @Test
    void createDispute_shouldRejectInvalidType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(disputeRequest("WRONG_TYPE"), CLAIMER_ID));
        assertEquals(400, ex.getCode());
    }

    // ===== 冒领申诉（FALSE_CLAIM）创建 =====

    // 构造冒领申诉请求：由发布者提交，只需物品 ID，不需申请 ID
    private DisputeCreateRequest falseClaimRequest() {
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setDisputeType("FALSE_CLAIM");
        req.setItemId(ITEM_ID);
        req.setDescription("这是我的物品，被别人冒领了");
        return req;
    }

    /**
     * 验证冒领申诉自动关联该物品最近一次已完成的认领申请。
     * 预期：工单的 disputeType=FALSE_CLAIM、applyId 指向已完成申请、itemId 与申请人（发布者）正确。
     * 为什么：裁决时要按申请回滚积分，必须先正确建立工单与申请的关联。
     */
    @Test
    void createFalseClaim_shouldLinkLatestCompletedApply() {
        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(claimApplyMapper.selectList(any())).thenReturn(List.of(completedApply)); // 查到已完成申请

        disputeService.createDispute(falseClaimRequest(), 999L); // 发布者以外用户（模拟真失主场景）提交

        // 校验工单与申请/物品/申诉人的关联字段
        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        verify(disputeMapper).insert(captor.capture());
        assertEquals("FALSE_CLAIM", captor.getValue().getDisputeType());
        assertEquals(APPLY_ID, captor.getValue().getApplyId()); // 自动关联到已完成申请
        assertEquals(ITEM_ID, captor.getValue().getItemId());
        assertEquals(999L, captor.getValue().getApplicantId()); // 申诉人为提交者
    }

    /**
     * 验证物品从未被成功认领时不能发起冒领申诉。
     * 预期：无已完成申请时抛出 code=409 的异常，不落库。
     * 为什么：没有"被冒领"的对象（无人取走），申诉不成立。
     */
    @Test
    void createFalseClaim_shouldRejectWhenNoCompletedApply() {
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(claimApplyMapper.selectList(any())).thenReturn(List.of()); // 无已完成申请

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(falseClaimRequest(), 999L));
        assertEquals(409, ex.getCode());
        verify(disputeMapper, never()).insert(any(Dispute.class));
    }

    /**
     * 验证冒领申诉必须提供 itemId。
     * 预期：缺少 itemId 抛出 code=400 的异常。
     * 为什么：冒领申诉按物品定位认领记录，缺 itemId 无法定位目标。
     */
    @Test
    void createFalseClaim_shouldRejectMissingItemId() {
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setDisputeType("FALSE_CLAIM");
        req.setDescription("test"); // 未设置 itemId

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.createDispute(req, 999L));
        assertEquals(400, ex.getCode());
    }

    /**
     * 验证冒领申诉不受 24 小时窗口限制。
     * 预期：取件 72 小时后提交仍能成功落库。
     * 为什么：与物品不符申诉不同，冒领可能事后才发现，真失主的救济渠道不应有时限。
     */
    @Test
    void createFalseClaim_shouldAllowLateSubmission() {
        completedApply.setPickupTime(LocalDateTime.now().minusHours(72)); // 已过 24h 很久
        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(claimApplyMapper.selectList(any())).thenReturn(List.of(completedApply));

        disputeService.createDispute(falseClaimRequest(), 999L);

        verify(disputeMapper).insert(any(Dispute.class)); // 仍可创建工单
    }

    // ===== 纠纷申诉处理 =====

    // 构造一条待处理（status=0）的纠纷工单
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

    /**
     * 验证物品不符申诉裁决通过后的完整副作用。
     * 预期：工单 status=1（已通过）；回滚双方积分——发布者 -3（PICKUP_ISSUE）
     * 与 -1（CHECK_ISSUE）、认领者 -1（PICKUP_ISSUE），备注含工单号；
     * 物品状态变为 VOIDED（作废）；申请标记 creditRollback=1。
     * 为什么：申诉成立意味着本次交接无效，激励与物品状态都必须完整撤销。
     */
    @Test
    void handle_approvedDispute_shouldRollbackCreditsAndVoidItem() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        when(disputeMapper.selectById(50L)).thenReturn(dispute);
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        // 每笔回滚前的两个 count 查询返回"已发放过、未回滚过"，使回滚条件成立
        when(creditLogMapper.selectCount(any())).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(true); // 裁决通过
        req.setHandlerNote("情况属实，同意申诉");

        disputeService.handle(50L, req, ADMIN_ID);

        assertEquals(1, dispute.getStatus()); // 工单标记为已通过
        // 发布者 PICKUP_ISSUE -3、CHECK_ISSUE -1；认领者 PICKUP_ISSUE -1
        verify(creditService).issueCredit(eq(PUBLISHER_ID), eq(-3), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        verify(creditService).issueCredit(eq(PUBLISHER_ID), eq(-1), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        verify(creditService).issueCredit(eq(CLAIMER_ID), eq(-1), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        assertEquals(ItemStatus.VOIDED.getCode(), item.getItemStatus()); // 物品作废
        assertEquals(1, completedApply.getCreditRollback()); // 申请标记已回滚
    }

    /**
     * 验证裁决驳回时不产生任何积分变动、物品状态不变。
     * 预期：工单 status=2（已驳回）；creditService 零调用；物品仍是 PICKED_UP。
     * 为什么：驳回 = 维持原交接结果，任何副作用都是对用户的错误惩罚。
     */
    @Test
    void handle_rejectedDispute_shouldNotTouchCredits() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        when(disputeMapper.selectById(50L)).thenReturn(dispute);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(false); // 裁决驳回
        req.setHandlerNote("证据不足");

        disputeService.handle(50L, req, ADMIN_ID);

        assertEquals(2, dispute.getStatus()); // 工单标记为已驳回
        // 不应有任何积分变动
        verify(creditService, never()).issueCredit(anyLong(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyLong());
        assertEquals(ItemStatus.PICKED_UP.getCode(), item.getItemStatus()); // 物品状态不变
    }

    /**
     * 验证已处理的工单不能重复处理。
     * 预期：status=1 的工单再次 handle 抛出 code=409 的异常。
     * 为什么：重复裁决会二次回滚积分，造成重复惩罚，必须幂等拦截。
     */
    @Test
    void handle_shouldRejectDuplicateHandling() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        dispute.setStatus(1); // 已处理通过
        when(disputeMapper.selectById(50L)).thenReturn(dispute);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(false);
        req.setHandlerNote("重复处理");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.handle(50L, req, ADMIN_ID));
        assertEquals(409, ex.getCode());
    }

    /**
     * 验证冒领申诉裁决通过后的处理方式：重开物品而非作废。
     * 预期：回滚发布者 -3 与认领者 -1 积分；物品状态回到 PUBLIC（重新公开）；
     * 申请标记 creditRollback=1。
     * 为什么：与物品不符不同，物品本身是真的——作废会埋没失物，
     * 重开才能让真正失主重新走正常认领流程。
     */
    @Test
    void handle_approvedFalseClaim_shouldRollbackCreditsAndReopenItem() {
        Dispute dispute = pendingDispute("FALSE_CLAIM");
        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        when(disputeMapper.selectById(50L)).thenReturn(dispute);
        when(claimApplyMapper.selectById(APPLY_ID)).thenReturn(completedApply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        // 每笔回滚前的两个 count 查询返回"已发放过、未回滚过"
        when(creditLogMapper.selectCount(any())).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L).thenReturn(1L).thenReturn(0L);

        DisputeHandleRequest req = new DisputeHandleRequest();
        req.setApproved(true);
        req.setHandlerNote("确认冒领");

        disputeService.handle(50L, req, ADMIN_ID);

        assertEquals(1, dispute.getStatus());
        // 回滚发布者与冒领者的积分
        verify(creditService).issueCredit(eq(PUBLISHER_ID), eq(-3), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        verify(creditService).issueCredit(eq(CLAIMER_ID), eq(-1), eq("ROLLBACK"), eq(ITEM_ID), eq(APPLY_ID), contains("工单#50"), eq(ADMIN_ID));
        // 物品重新公开而非作废，真正失主可走正常流程认领
        assertEquals(ItemStatus.PUBLIC.getCode(), item.getItemStatus());
        assertEquals(1, completedApply.getCreditRollback());
    }

    /**
     * 验证管理员可以查看纠纷详情。
     * 预期：SYS_ADMIN 调用 getById 不抛异常，且触发了物品信息的查询（组装详情）。
     * 为什么：处理纠纷前必须能查看完整上下文，管理员拥有查看权。
     */
    @Test
    void getById_shouldAllowAdmin() {
        Dispute dispute = pendingDispute("ITEM_MISMATCH");
        when(disputeMapper.selectById(50L)).thenReturn(dispute);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(item);
        User admin = new User();
        admin.setId(ADMIN_ID);
        admin.setRole(UserRole.SYS_ADMIN); // 系统管理员
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);

        disputeService.getById(50L, ADMIN_ID);

        // 管理员可查看：详情查询确实取到了物品信息
        verify(foundItemMapper).selectById(ITEM_ID);
    }
}
