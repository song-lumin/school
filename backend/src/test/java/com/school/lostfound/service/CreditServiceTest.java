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

/**
 * 信用积分（Credit）服务的单元测试。
 *
 * <p>测试模块：CreditServiceImpl —— 覆盖积分的发放（issueCredit）与回滚（rollbackCredits）：</p>
 * <ul>
 *   <li>issueCredit 正数/负数变动：更新用户分数、写入流水、刷新 Redis 缓存</li>
 *   <li>用户不存在时的静默跳过（不抛异常、不写流水）</li>
 *   <li>rollbackCredits 的幂等与前置条件：未发放过不回滚、已回滚过不重复回滚、
 *       符合条件时写入 ROLLBACK 负流水并扣减分数</li>
 * </ul>
 * <p>方式：Mockito 纯单元测试，Mapper/Redis 全部 mock，只测 Service 层逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private CreditLogMapper creditLogMapper; // 积分流水表 mock

    @Mock
    private UserMapper userMapper; // 用户表 mock（更新分数）

    @Mock
    private RedisTemplate<String, Object> redisTemplate; // Redis 模板 mock

    @Mock
    private ValueOperations<String, Object> valueOperations; // Redis value 操作，与上面配套

    @InjectMocks
    private CreditServiceImpl creditService;

    // 测试用户：id=2，初始信用分 100
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setCreditScore(100);
    }

    /**
     * 验证正向积分发放的三个副作用。
     * 预期：用户分数 100→103；插入一条流水（金额 3、类型 PICKUP_ISSUE、关联物品 100）；
     * Redis 缓存键 credit:user:2 刷新为 103。
     * 为什么：分数、流水、缓存三者必须一致，否则积分数据会出现三种"真相"。
     */
    @Test
    void issueCredit_positive_shouldUpdateScoreInsertLogAndCache() {
        when(userMapper.selectById(2L)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        creditService.issueCredit(2L, 3, "PICKUP_ISSUE", 100L, 10L, "失主取件完成", 1L);

        assertEquals(103, user.getCreditScore()); // 分数增加
        verify(userMapper).updateById(user); // 用户表落库

        // 捕获插入的流水，校验关键字段
        ArgumentCaptor<CreditLog> captor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(captor.capture());
        CreditLog log = captor.getValue();
        assertEquals(3, log.getChangeAmount()); // 流水金额
        assertEquals("PICKUP_ISSUE", log.getOperationType()); // 操作类型
        assertEquals(100L, log.getRelatedItemId()); // 关联物品

        // 缓存同步刷新为新分数
        verify(valueOperations).set(eq("credit:user:2"), eq(103), any(Duration.class));
    }

    /**
     * 验证负向积分变动（扣分）。
     * 预期：分数 100→97，流水金额记录为 -3。
     * 为什么：回滚/处罚场景走的是同一入口，需确认负数被正确累加而非覆盖。
     */
    @Test
    void issueCredit_negative_shouldDecreaseScore() {
        when(userMapper.selectById(2L)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        creditService.issueCredit(2L, -3, "ROLLBACK", 100L, 10L, "举报成立回滚", 1L);

        assertEquals(97, user.getCreditScore()); // 分数扣减
        ArgumentCaptor<CreditLog> captor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(captor.capture());
        assertEquals(-3, captor.getValue().getChangeAmount()); // 流水记录负数
    }

    /**
     * 验证用户不存在时发放操作被静默跳过。
     * 预期：不抛异常，不写流水、不更新用户表。
     * 为什么：积分发放多为异步/事务内调用，用户缺失不应阻塞主流程，也不能留下孤儿流水。
     */
    @Test
    void issueCredit_shouldSkipWhenUserNotExists() {
        when(userMapper.selectById(999L)).thenReturn(null); // 用户不存在

        creditService.issueCredit(999L, 3, "PICKUP_ISSUE", 100L, null, "test", 1L);

        verify(creditLogMapper, never()).insert(any(CreditLog.class)); // 不写流水
        verify(userMapper, never()).updateById(any(User.class)); // 不更新用户
    }

    /**
     * 验证从未发放过取件积分时，回滚操作直接跳过。
     * 预期：流水 count 为 0 → 不插入任何回滚流水。
     * 为什么：没有发放就没有回滚依据，避免凭空产生负流水。
     */
    @Test
    void rollbackCredits_shouldSkipWhenNoPickupIssued() {
        when(creditLogMapper.selectCount(any())).thenReturn(0L); // 无 PICKUP_ISSUE 记录

        creditService.rollbackCredits(100L, 10L, 2L, "纠纷回滚", 1L);

        verify(creditLogMapper, never()).insert(any(CreditLog.class));
    }

    /**
     * 验证回滚操作的幂等性：同一笔申请只能回滚一次。
     * 预期：已存在 ROLLBACK 记录时不再插入新的回滚流水。
     * 为什么：管理员重复处理/并发调用时，重复扣分会错误惩罚用户。
     */
    @Test
    void rollbackCredits_shouldSkipWhenAlreadyRolledBack() {
        // 第一次 count 查询：有 PICKUP_ISSUE 记录；第二次：已有 ROLLBACK 记录
        when(creditLogMapper.selectCount(any())).thenReturn(1L);

        creditService.rollbackCredits(100L, 10L, 2L, "纠纷回滚", 1L);

        verify(creditLogMapper, never()).insert(any(CreditLog.class)); // 不重复回滚
    }

    /**
     * 验证符合条件时回滚正常执行。
     * 预期：有 PICKUP_ISSUE 且无 ROLLBACK 记录时，插入一条金额 -1、类型 ROLLBACK
     * 的流水，用户分数 100→99。
     * 为什么：这是纠纷裁决后回收激励的核心路径，需确认负流水与扣分一致。
     */
    @Test
    void rollbackCredits_shouldIssueNegativeLogWhenEligible() {
        // 第一次 count：有 PICKUP_ISSUE；第二次 count：无 ROLLBACK → 执行回滚
        when(creditLogMapper.selectCount(any()))
                .thenReturn(1L)
                .thenReturn(0L);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        creditService.rollbackCredits(100L, 10L, 2L, "纠纷回滚", 1L);

        // 捕获插入的回滚流水，校验字段与分数扣减
        ArgumentCaptor<CreditLog> captor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(captor.capture());
        List<CreditLog> inserted = captor.getAllValues();
        assertEquals(-1, inserted.get(0).getChangeAmount()); // 负向金额
        assertEquals("ROLLBACK", inserted.get(0).getOperationType()); // 回滚类型
        assertEquals(99, user.getCreditScore()); // 100 - 1
    }
}
