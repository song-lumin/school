package com.school.lostfound.service;

import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 认领置信度评分器 —— 判断"认领人的回答有多可信"的纯算法工具类。
 * <p>
 * 业务背景：为防止冒领，拾获人发布物品时可设置一道"验证问题"
 * （如"这个水杯是什么颜色的？"），并填写参考答案。认领人答题后，
 * 系统需要一个量化手段来评估答案可信度，辅助发布人决策：
 * 置信度高可以放心通过，置信度低（&lt;40 分）时系统提示"建议人工核验"。
 * <p>
 * 评分模型（满分 100）：
 * 1. 相似度分（最高 60）：答案与"物品标题+描述"以及"参考答案"的
 *    文本重合度 + 答案长度分；
 * 2. 信用分（最高 25）：按当前信用分线性折算（×0.25）；
 * 3. 历史记录（-10 ~ 15）：历史成功认领加正分，被驳回次数扣分。
 * <p>
 * 实现要点：类声明为 final、构造器私有（private 构造防止实例化）、
 * 全部方法 static —— 典型的无状态工具类写法，无任何成员变量，
 * 因此天然线程安全，可被任意并发调用。
 * <p>
 * 算法说明：文本相似度采用 bigram（二元组）重合率——把文本切成相邻两字
 * 的集合，计算答案中有多少个二元组出现在物品描述里。相比逐字比较，
 * bigram 对语序变化更鲁棒，相比分词方案又无需中文分词器依赖，
 * 是中文短文本相似度的轻量实用方案。注意参考答案只加分不判题：
 * 即使与参考答案高度吻合，其他维度过低仍可能是低置信度，
 * 避免参考答案泄露后被"一字不差"抄写绕过。
 */
public final class ClaimConfidenceAnalyzer {

    /** 私有构造器：禁止实例化，本类只提供静态方法 */
    private ClaimConfidenceAnalyzer() {
    }

    /**
     * 无参考答案时的简化评分入口（兼容旧签名）。
     * 等价于参考答案传 null，只按物品描述计算相似度。
     */
    public static Analysis analyze(String answer, String itemText,
                                   int creditScore, int successCount, int rejectCount) {
        return analyze(answer, itemText, null, creditScore, successCount, rejectCount);
    }

    /**
     * 综合评分主入口。
     *
     * @param answer          失主（认领人）回答的验证问题答案
     * @param itemText        物品标题+描述，作为相似度比对的基准文本
     * @param referenceAnswer 发布人填写的参考答案（可空）；仅用于加相似度分，不直接判对错
     * @param creditScore     认领人当前信用分（0-100）
     * @param successCount    认领人历史成功认领次数
     * @param rejectCount     本物品已被驳回的认领次数（次数多说明物品信息可能有误导）
     * @return 评分结果：分数（0-100）、是否低置信度、可读的评分理由
     */
    public static Analysis analyze(String answer, String itemText, String referenceAnswer,
                                   int creditScore, int successCount, int rejectCount) {
        // 统一归一化后再比较，避免大小写/空白差异影响相似度
        String normalizedAnswer = normalize(answer);
        if (normalizedAnswer.isEmpty()) {
            // 空答案无法评估：给 0 分并提示人工核验
            return new Analysis(0, true, "回答为空，建议人工核验");
        }

        // ---------- 第 1 部分：相似度分（满分 60） ----------
        // 把答案与物品描述分别切成 bigram 集合，统计答案的 bigram 在描述中的命中率
        Set<String> answerBigrams = bigrams(normalizedAnswer);
        Set<String> textBigrams = bigrams(normalize(itemText));
        long overlaps = answerBigrams.stream().filter(textBigrams::contains).count();
        // 重合率 = 命中的 bigram 数 / 答案 bigram 总数，衡量答案与物品描述的关联度
        double relevance = answerBigrams.isEmpty() ? 0 : (double) overlaps / answerBigrams.size();
        // 长度分：每 1 个字符得 2 分，封顶 20 —— 鼓励写出有信息量的答案，防敷衍
        int lengthScore = Math.min(20, normalizedAnswer.length() * 2);
        // 相似度总分 = 长度分（≤20）+ 关联度分（关联率×40，≤40），整体封顶 60
        int similarityScore = Math.min(60, lengthScore + (int) Math.round(relevance * 40));

        // 参考答案比对：只加分不判题（即使参考答案存在也不允许它单独决定结果）
        boolean matchesReference = false;
        if (StringUtils.hasText(referenceAnswer)) {
            Set<String> refBigrams = bigrams(normalize(referenceAnswer));
            long refOverlaps = answerBigrams.stream().filter(refBigrams::contains).count();
            double refRelevance = answerBigrams.isEmpty() ? 0 : (double) refOverlaps / answerBigrams.size();
            if (refRelevance >= 0.4) { // 与参考答案重合率达 40% 视为"高度吻合"
                matchesReference = true;
                similarityScore = Math.min(60, similarityScore + 15); // 加 15 分，仍以 60 为上限
            }
        }

        // ---------- 第 2 部分：信用分加权（满分 25） ----------
        // 先把信用分截断到 [0,100] 防脏数据，再乘 0.25 折算成 0~25 分
        int creditComponent = (int) Math.round(Math.max(0, Math.min(100, creditScore)) * 0.25);

        // ---------- 第 3 部分：历史记录（-10 ~ 15） ----------
        int historyComponent = 0;
        // 每次历史成功认领 +3 分，封顶 10：体现"老好人"的可信度
        if (successCount > 0) historyComponent += Math.min(10, successCount * 3);
        // 每次被驳回 -3 分，最多扣 10：驳回多说明该用户的认领行为可疑
        historyComponent -= Math.min(10, rejectCount * 3);
        // 最终截断到 [-10, 15]，保证单项不至于主导总分
        historyComponent = Math.max(-10, Math.min(15, historyComponent));

        // 汇总三部分并截断到 [0,100] 得到最终分数
        int score = Math.max(0, Math.min(100, similarityScore + creditComponent + historyComponent));
        // 低于 40 分判定为低置信度：系统会提示发布人人工重点核验
        boolean lowConfidence = score < 40;

        // 拼装人类可读的评分理由，展示给发布人辅助决策
        StringBuilder reason = new StringBuilder();
        reason.append("答案长度与描述关联度").append(similarityScore >= 40 ? "正常" : "较弱");
        if (matchesReference) reason.append("；与发布人参考答案高度吻合");
        if (creditComponent < 10) reason.append("；信用分较低");
        if (successCount > 0) reason.append("；该用户有").append(successCount).append("次成功认领记录");
        if (rejectCount > 0) reason.append("；本物品已被驳回").append(rejectCount).append("次");
        if (lowConfidence) reason.append("，建议人工重点核验");

        return new Analysis(score, lowConfidence, reason.toString());
    }

    /**
     * 文本归一化：转小写（Locale.ROOT 避免土耳其语等区域的大小写坑）、
     * 去掉所有空白字符；空/空白串统一返回空串。
     */
    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "") : "";
    }

    /**
     * 提取文本的 bigram（相邻两字）集合。
     * 例："蓝色水杯" → {蓝色, 色水, 水杯}。
     * 用 Set 而非 List：相似度只关心"出现过哪些二元组"，不关心出现次数。
     */
    private static Set<String> bigrams(String value) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 1 < value.length(); i++) result.add(value.substring(i, i + 2));
        return result;
    }

    /**
     * 评分结果记录类（Java record：自动生成构造器/访问器/equals/hashCode）。
     *
     * @param score         综合置信度分数（0-100）
     * @param lowConfidence 是否低置信度（<40），前端据此弹出"建议人工核验"提示
     * @param reason        人类可读的评分理由，展示给发布人
     */
    public record Analysis(int score, boolean lowConfidence, String reason) {
    }
}
