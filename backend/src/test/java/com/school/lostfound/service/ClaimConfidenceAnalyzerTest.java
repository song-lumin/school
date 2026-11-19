package com.school.lostfound.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 认领置信度分析器（ClaimConfidenceAnalyzer）的单元测试。
 *
 * <p>测试模块：ClaimConfidenceAnalyzer#analyze —— 纯函数逻辑，根据认领者的特征描述回答、
 * 物品描述、信用分、历史拒单数等信息，计算认领申请的置信度分数，并判断是否需要人工复核（lowConfidence）。</p>
 * <p>覆盖场景：</p>
 * <ul>
 *   <li>过于简短、泛泛的回答应被标记为低置信度</li>
 *   <li>回答与物品描述相关性越高、信息量越大，分数越高</li>
 *   <li>空回答得到有界的低分，且附带非空的原因说明</li>
 * </ul>
 * <p>方式：无 mock 的纯单元测试，直接调用静态方法验证打分规则。</p>
 */
class ClaimConfidenceAnalyzerTest {

    /**
     * 验证过于简短/泛泛的回答会被标记为需要人工复核。
     * 预期：回答"是"（1 个字，无特征信息）时 lowConfidence() 为 true。
     * 为什么：此类回答无法证明认领者真正了解物品，是冒领的典型信号。
     */
    @Test
    void shortGenericAnswerIsMarkedForReview() {
        // 回答仅一个字"是"，而物品描述包含具体特征（裂纹），应判低置信度
        assertTrue(ClaimConfidenceAnalyzer.analyze("是", "蓝色手机壳，右下角有裂纹", 50, 0, 0).lowConfidence());
    }

    /**
     * 验证回答的信息量与相关性越高，置信度分数越高。
     * 预期：与物品描述呼应的详细回答（"蓝色手机壳，右下角有一道裂纹"）得分
     * 高于简单回答（"蓝色"），且不再被判为低置信度。
     * 为什么：确保打分算法能区分"知道细节的真失主"与"只知道大概的猜测者"。
     */
    @Test
    void informativeAnswerRelatedToDescriptionScoresHigher() {
        // 简短回答：只有一个颜色词
        var brief = ClaimConfidenceAnalyzer.analyze("蓝色", "手机壳是蓝色，右下角有裂纹", 50, 0, 0);
        // 详细回答：包含颜色 + 位置 + 细节，与物品描述高度相关
        var detailed = ClaimConfidenceAnalyzer.analyze("蓝色手机壳，右下角有一道裂纹", "手机壳是蓝色，右下角有裂纹", 50, 0, 0);

        // 详细回答分数必须更高
        assertTrue(detailed.score() > brief.score());
        // 且详细回答不应再触发人工复核
        assertFalse(detailed.lowConfidence());
    }

    /**
     * 验证空回答的兜底处理。
     * 预期：空白回答得分仍在 [0,100] 区间内（分数不会越界）、被判低置信度，
     * 并且 reason 字段给出非空的原因说明（供前端展示复核提示）。
     * 为什么：保证异常输入下打分结果的合法性与可解释性。
     */
    @Test
    void emptyAnswerGetsBoundedLowScoreWithReason() {
        // 输入为纯空白字符串，模拟用户未作答
        var result = ClaimConfidenceAnalyzer.analyze("  ", "校园卡，背面写有学号", 50, 0, 0);

        // 分数必须落在合法区间 [0,100]，不能因空输入越界
        assertTrue(result.score() >= 0 && result.score() <= 100);
        // 空回答必然需要人工复核
        assertTrue(result.lowConfidence());
        // 必须给出非空的原因说明
        assertFalse(result.reason().isBlank());
    }
}
