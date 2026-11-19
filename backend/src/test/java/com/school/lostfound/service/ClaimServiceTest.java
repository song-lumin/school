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

/**
 * 认领申请（ClaimApply）核心流程的单元测试。
 *
 * <p>测试模块：ClaimServiceImpl —— 覆盖认领的完整生命周期：</p>
 * <ul>
 *   <li>apply：提交认领申请（敏感词过滤、物品状态、自认领、重复申请、Redis 防并发锁、累计拒绝次数）</li>
 *   <li>approve：发布者/管理员审批（状态流转 + 权限控制）</li>
 *   <li>reject：拒绝申请（3 次拒绝后锁定，防恶意刷单）</li>
 *   <li>pickup：完成取件（凭证校验 + 双方积分发放）</li>
 * </ul>
 * <p>方式：Mockito 纯单元测试，Mapper/Redis/CreditService 全部 mock，只测 Service 层业务规则。</p>
 */
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
    private CreditService creditService; // 积分发放 mock，pickup 场景验证调用

    @Mock
    private RiskControlService riskControlService; // 敏感词风控 mock

    @Mock
    private RedisTemplate<String, Object> redisTemplate; // 用于防并发锁

    @Mock
    private ValueOperations<String, Object> valueOperations; // Redis 的 value 操作，与上面配套

    @InjectMocks
    private ClaimServiceImpl claimService;

    // 测试常用固定 ID：物品 100、发布者 1、认领者 2
    private static final Long ITEM_ID = 100L;
    private static final Long FOUNDER_ID = 1L;
    private static final Long CLAIMER_ID = 2L;

    // 每个用例共享的"已公开"物品基础数据
    private FoundItem publicItem;

    @BeforeEach
    void setUp() {
        publicItem = new FoundItem();
        publicItem.setId(ITEM_ID);
        publicItem.setFounderId(FOUNDER_ID);
        publicItem.setItemStatus(ItemStatus.PUBLIC.getCode()); // 默认处于可认领的公开状态
        publicItem.setTitle("黑色钱包");
        publicItem.setClaimQuestion("钱包里有什么证件？"); // 认领验证问题
    }

    // 构造一个最小化的认领申请请求（默认回答"身份证"）
    private ClaimApplyRequest applyRequest(String answer) {
        ClaimApplyRequest req = new ClaimApplyRequest();
        req.setItemId(ITEM_ID);
        req.setAnswer(answer);
        return req;
    }

    // ===== apply =====

    /**
     * 验证回答中包含敏感词时申请被拒绝。
     * 预期：抛出 code=400 的 BusinessException，且不插入任何记录。
     * 为什么：内容安全是所有 UGC 入口的第一道闸，必须优先于其他校验。
     */
    @Test
    void apply_shouldRejectWhenSensitiveWord() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(true); // 命中敏感词

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("含敏感词"), CLAIMER_ID));
        assertEquals(400, ex.getCode());
        verify(claimApplyMapper, never()).insert(any(ClaimApply.class)); // 不应有任何写入
    }

    /**
     * 验证物品不处于 PUBLIC 状态时不能申请认领。
     * 预期：状态为 PICKED_UP 的物品抛出 code=409 的异常。
     * 为什么：已被取走的物品再次开放认领会造成重复认领事故。
     */
    @Test
    void apply_shouldRejectWhenItemNotPublic() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        publicItem.setItemStatus(ItemStatus.PICKED_UP.getCode()); // 物品已被取走
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("身份证"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
    }

    /**
     * 验证发布者不能认领自己发布的物品。
     * 预期：FOUNDER_ID 提交申请时抛出 code=400 的异常。
     * 为什么：自认领是明显的刷积分/冒领行为，必须拦截。
     */
    @Test
    void apply_shouldRejectSelfClaim() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("身份证"), FOUNDER_ID)); // 发布者本人申请
        assertEquals(400, ex.getCode());
    }

    /**
     * 验证同一用户对同一物品存在待处理申请时不能重复申请。
     * 预期：已有 PENDING 记录时抛出 code=409 的异常，且不插入新记录。
     * 为什么：防止同一人重复提交造成审核混乱与数据冗余。
     */
    @Test
    void apply_shouldRejectWhenAlreadyPending() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // setIfAbsent 返回 true：模拟成功获取 Redis 防并发锁
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        ClaimApply existing = new ClaimApply();
        existing.setItemId(ITEM_ID);
        existing.setClaimerId(CLAIMER_ID);
        existing.setApplyStatus(ClaimStatus.PENDING.getCode()); // 已有待审核申请
        when(claimApplyMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("身份证"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
        verify(claimApplyMapper, never()).insert(any(ClaimApply.class)); // 不应重复插入
    }

    /**
     * 验证首次申请走"插入新记录"路径，且初始字段正确。
     * 预期：插入的记录状态为 PENDING、认领人正确、拒绝次数从 0 开始。
     * 为什么：这是正常流程的主干路径，初始值错误会影响后续 3 次锁定逻辑。
     */
    @Test
    void apply_shouldInsertNewWhenFirstTime() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(claimApplyMapper.selectOne(any())).thenReturn(null); // 无历史申请记录

        User claimer = new User();
        claimer.setId(CLAIMER_ID);
        claimer.setRealName("张三");
        when(userMapper.selectById(CLAIMER_ID)).thenReturn(claimer); // 认领人存在

        claimService.apply(applyRequest("身份证"), CLAIMER_ID);

        // 用 ArgumentCaptor 捕获实际插入的对象，校验初始字段
        ArgumentCaptor<ClaimApply> captor = ArgumentCaptor.forClass(ClaimApply.class);
        verify(claimApplyMapper).insert(captor.capture());
        ClaimApply saved = captor.getValue();
        assertEquals(ClaimStatus.PENDING.getCode(), saved.getApplyStatus()); // 初始待审核
        assertEquals(CLAIMER_ID, saved.getClaimerId()); // 认领人正确
        assertEquals(0, saved.getRejectCount()); // 拒绝次数从 0 计起
    }

    /**
     * 验证申请已被锁定（LOCKED，累计 3 次拒绝）后不允许再次申请。
     * 预期：抛出 code=409 的异常。
     * 为什么：锁定是针对恶意试答的惩罚机制，若可绕过则形同虚设。
     */
    @Test
    void apply_shouldRejectWhenLocked() {
        when(riskControlService.containsSensitiveWord(anyString())).thenReturn(false);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        ClaimApply locked = new ClaimApply();
        locked.setItemId(ITEM_ID);
        locked.setClaimerId(CLAIMER_ID);
        locked.setApplyStatus(ClaimStatus.LOCKED.getCode()); // 已锁定状态
        locked.setUpdatedAt(java.time.LocalDateTime.now());
        when(claimApplyMapper.selectOne(any())).thenReturn(locked);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.apply(applyRequest("再试一次"), CLAIMER_ID));
        assertEquals(409, ex.getCode());
    }

    /**
     * 验证被拒后重新申请走"更新旧记录"路径，且累计拒绝次数被保留。
     * 预期：不插入新记录而是 updateById；状态回到 PENDING；rejectCount 保持 2
     * （再被拒 1 次达到 3 次即锁定，见 reject 相关用例）。
     * 为什么：若重置次数，攻击者可通过反复重新申请无限次试答。
     */
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
        rejected.setApplyStatus(ClaimStatus.REJECTED.getCode()); // 曾被拒绝
        rejected.setRejectCount(2); // 已累计 2 次拒绝
        when(claimApplyMapper.selectOne(any())).thenReturn(rejected);

        User claimer = new User();
        claimer.setId(CLAIMER_ID);
        claimer.setRealName("张三");
        when(userMapper.selectById(CLAIMER_ID)).thenReturn(claimer);

        claimService.apply(applyRequest("第二次尝试"), CLAIMER_ID);

        // 重新申请保留累计拒绝次数（再被拒1次即达3次锁定）
        assertEquals(2, rejected.getRejectCount());
        assertEquals(ClaimStatus.PENDING.getCode(), rejected.getApplyStatus()); // 状态重置为待审核
        verify(claimApplyMapper).updateById(rejected); // 走更新路径
        verify(claimApplyMapper, never()).insert(any(ClaimApply.class)); // 不插入新记录
    }

    // ===== approve =====

    /**
     * 验证发布者审批通过后的状态流转。
     * 预期：申请变为 APPROVED_WAITING_PICKUP（待取件），物品变为 CLAIMING（认领中），
     * 且两处更新都落库。
     * 为什么：这是审批主流程的状态机核心，双状态必须同步变更。
     */
    @Test
    void approve_shouldSetWaitingPickupAndItemClaiming() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        claimService.approve(10L, FOUNDER_ID); // 由发布者本人审批

        assertEquals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode(), apply.getApplyStatus()); // 申请→待取件
        assertEquals(ItemStatus.CLAIMING.getCode(), publicItem.getItemStatus()); // 物品→认领中
        verify(claimApplyMapper).updateById(apply); // 申请更新落库
        verify(foundItemMapper).updateById(publicItem); // 物品更新落库
    }

    /**
     * 验证非发布者（且非管理员）不能审批。
     * 预期：操作人 999L 查不到用户时抛出 code=403 的异常。
     * 为什么：审批权属于发布者/管理员，防止任意用户越权审批。
     */
    @Test
    void approve_shouldRejectWhenNotFounder() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);
        when(userMapper.selectById(999L)).thenReturn(null); // 操作人不存在（非发布者也非管理员）

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.approve(10L, 999L));
        assertEquals(403, ex.getCode());
    }

    /**
     * 验证已处理完成的申请不能再次审批。
     * 预期：状态为 COMPLETED 时抛出 code=409 的异常。
     * 为什么：保证审批操作幂等，防止重复审批覆盖已有结果。
     */
    @Test
    void approve_shouldRejectWhenAlreadyHandled() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.COMPLETED.getCode()); // 已完成
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.approve(10L, FOUNDER_ID));
        assertEquals(409, ex.getCode());
    }

    // ===== 管理员审核 =====

    /**
     * 验证系统管理员（SYS_ADMIN）可以代替发布者审批。
     * 预期：审批成功，申请与物品状态正常流转。
     * 为什么：发布者可能长期不响应，需要管理员兜底处理积压申请。
     */
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
        sysAdmin.setRole(UserRole.SYS_ADMIN); // 系统管理员角色
        when(userMapper.selectById(50L)).thenReturn(sysAdmin);

        claimService.approve(10L, 50L); // 管理员操作

        assertEquals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode(), apply.getApplyStatus());
        assertEquals(ItemStatus.CLAIMING.getCode(), publicItem.getItemStatus());
    }

    /**
     * 验证物品所在点位的管理员可以审批该物品的申请。
     * 预期：物品属于 300 号点位、该点位 adminId 为 60，60 号用户审批成功。
     * 为什么：点管负责本点位物品的实物交接，应有对应审批权。
     */
    @Test
    void approve_shouldAllowDropPointAdminOfItemSite() {
        publicItem.setDropPointId(300L); // 物品存放在 300 号点位
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        User pointAdmin = new User();
        pointAdmin.setId(60L);
        pointAdmin.setRole(UserRole.POINT_ADMIN); // 点位管理员
        when(userMapper.selectById(60L)).thenReturn(pointAdmin);

        DropPoint point = new DropPoint();
        point.setId(300L);
        point.setAdminId(60L); // 该点位的管理员正是 60 号用户
        when(dropPointMapper.selectById(300L)).thenReturn(point);

        claimService.approve(10L, 60L);

        assertEquals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode(), apply.getApplyStatus());
    }

    /**
     * 验证其他点位的管理员无权审批（越权拦截）。
     * 预期：61 号点管管理的不是物品所在点位，抛出 code=403 的异常。
     * 为什么：点管权限必须限定在本点位内，防止跨点位越权。
     */
    @Test
    void approve_shouldRejectPointAdminOfOtherSite() {
        publicItem.setDropPointId(300L); // 物品属于 300 号点位
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        User pointAdmin = new User();
        pointAdmin.setId(61L);
        pointAdmin.setRole(UserRole.POINT_ADMIN); // 点位管理员，但不是本点位的
        when(userMapper.selectById(61L)).thenReturn(pointAdmin);

        DropPoint point = new DropPoint();
        point.setId(300L);
        point.setAdminId(60L); // 300 号点位的管理员是 60 号，不是 61 号
        when(dropPointMapper.selectById(300L)).thenReturn(point);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.approve(10L, 61L));
        assertEquals(403, ex.getCode());
    }

    // ===== reject: 3 次锁定 =====

    /**
     * 验证未达最大拒绝次数时，拒绝只累计计数不触发锁定。
     * 预期：rejectCount 从 1 变为 2，状态为 REJECTED（而非 LOCKED）。
     * 为什么：给认领者合理的容错空间，锁定只针对反复试答。
     */
    @Test
    void reject_shouldStayRejectedBelowMaxCount() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        apply.setRejectCount(1); // 已被拒 1 次
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        ClaimRejectRequest req = new ClaimRejectRequest();
        req.setRejectReason("回答不正确");
        claimService.reject(10L, req, FOUNDER_ID);

        assertEquals(ClaimStatus.REJECTED.getCode(), apply.getApplyStatus()); // 仅拒绝
        assertEquals(2, apply.getRejectCount()); // 计数 +1
    }

    /**
     * 验证第 3 次拒绝时申请被锁定。
     * 预期：rejectCount 达到 3，状态变为 LOCKED。
     * 为什么：3 次回答错误基本可判定为恶意试答/冒领，必须终止其认领资格。
     */
    @Test
    void reject_shouldLockAtThirdRejection() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        apply.setRejectCount(2); // 已被拒 2 次，本次是第 3 次
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        ClaimRejectRequest req = new ClaimRejectRequest();
        req.setRejectReason("第三次回答仍不正确");
        claimService.reject(10L, req, FOUNDER_ID);

        assertEquals(3, apply.getRejectCount());
        assertEquals(ClaimStatus.LOCKED.getCode(), apply.getApplyStatus()); // 触发锁定
    }

    // ===== pickup: 双方积分发放 =====

    /**
     * 验证取件完成后的状态流转与积分发放。
     * 预期：申请变为 COMPLETED，物品变为 PICKED_UP，记录取件时间；
     * 发布者 +3 分、认领者 +1 分，均以 PICKUP_ISSUE 类型发放。
     * 为什么：取件是整个认领流程的终点，状态与激励必须同时正确落账。
     */
    @Test
    void pickup_shouldCompleteAndIssueCreditsToBothParties() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setClaimerId(CLAIMER_ID);
        apply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode()); // 已批准待取件
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        claimService.pickup(10L, "photo.jpg", "sig.png", FOUNDER_ID); // 带取件照片与签名

        assertEquals(ClaimStatus.COMPLETED.getCode(), apply.getApplyStatus()); // 申请完成
        assertEquals(ItemStatus.PICKED_UP.getCode(), publicItem.getItemStatus()); // 物品已被取走
        assertNotNull(apply.getPickupTime()); // 记录取件时间
        // 发布者 +3，认领者 +1
        verify(creditService).issueCredit(eq(FOUNDER_ID), eq(3), eq("PICKUP_ISSUE"), eq(ITEM_ID), eq(10L), anyString(), eq(FOUNDER_ID));
        verify(creditService).issueCredit(eq(CLAIMER_ID), eq(1), eq("PICKUP_ISSUE"), eq(ITEM_ID), eq(10L), anyString(), eq(FOUNDER_ID));
    }

    /**
     * 验证取件必须提供照片和签名凭证。
     * 预期：两个凭证都为 null 时抛出 code=400 的异常，且不发放任何积分。
     * 为什么：凭证是交接的留痕依据，缺失则无法追溯，也不能激励发放。
     */
    @Test
    void pickup_shouldRequirePhotoAndSignature() {
        ClaimApply apply = new ClaimApply();
        apply.setId(10L);
        apply.setItemId(ITEM_ID);
        apply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode());
        when(claimApplyMapper.selectById(10L)).thenReturn(apply);
        when(foundItemMapper.selectById(ITEM_ID)).thenReturn(publicItem);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> claimService.pickup(10L, null, null, FOUNDER_ID)); // 缺少照片与签名
        assertEquals(400, ex.getCode());
        verify(creditService, never()).issueCredit(anyLong(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyLong()); // 不发积分
    }
}
