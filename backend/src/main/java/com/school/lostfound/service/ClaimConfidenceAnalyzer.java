package com.school.lostfound.service;

import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 认领置信度评分：0-100分
 * 综合：答案与参考答案/物品描述的相似度、用户信用分、历史记录
 */
public final class ClaimConfidenceAnalyzer {

    private ClaimConfidenceAnalyzer() {
    }

    public static Analysis analyze(String answer, String itemText,
                                   int creditScore, int successCount, int rejectCount) {
        return analyze(answer, itemText, null, creditScore, successCount, rejectCount);
    }

    /**
     * @param answer          失主回答
     * @param itemText        物品标题+描述
     * @param referenceAnswer 发布人填写的参考答案（可空）；用于答案相似度比对，不直接判题
     * @param creditScore    用户当前信用分
     * @param successCount    历史成功认领次数
     * @param rejectCount     本物品被驳回次数
     */
    public static Analysis analyze(String answer, String itemText, String referenceAnswer,
                                   int creditScore, int successCount, int rejectCount) {
        String normalizedAnswer = normalize(answer);
        if (normalizedAnswer.isEmpty()) {
            return new Analysis(0, true, "回答为空，建议人工核验");
        }

        // 1. 相似度分（满分60）：答案与物品描述 + 参考答案的重合度
        Set<String> answerBigrams = bigrams(normalizedAnswer);
        Set<String> textBigrams = bigrams(normalize(itemText));
        long overlaps = answerBigrams.stream().filter(textBigrams::contains).count();
        double relevance = answerBigrams.isEmpty() ? 0 : (double) overlaps / answerBigrams.size();
        int lengthScore = Math.min(20, normalizedAnswer.length() * 2);
        int similarityScore = Math.min(60, lengthScore + (int) Math.round(relevance * 40));

        boolean matchesReference = false;
        if (StringUtils.hasText(referenceAnswer)) {
            Set<String> refBigrams = bigrams(normalize(referenceAnswer));
            long refOverlaps = answerBigrams.stream().filter(refBigrams::contains).count();
            double refRelevance = answerBigrams.isEmpty() ? 0 : (double) refOverlaps / answerBigrams.size();
            if (refRelevance >= 0.4) {
                matchesReference = true;
                similarityScore = Math.min(60, similarityScore + 15);
            }
        }

        // 2. 用户信用分加权，满分25
        int creditComponent = (int) Math.round(Math.max(0, Math.min(100, creditScore)) * 0.25);

        // 3. 历史记录，满分15
        int historyComponent = 0;
        if (successCount > 0) historyComponent += Math.min(10, successCount * 3);
        historyComponent -= Math.min(10, rejectCount * 3);
        historyComponent = Math.max(-10, Math.min(15, historyComponent));

        int score = Math.max(0, Math.min(100, similarityScore + creditComponent + historyComponent));
        boolean lowConfidence = score < 40;

        StringBuilder reason = new StringBuilder();
        reason.append("答案长度与描述关联度").append(similarityScore >= 40 ? "正常" : "较弱");
        if (matchesReference) reason.append("；与发布人参考答案高度吻合");
        if (creditComponent < 10) reason.append("；信用分较低");
        if (successCount > 0) reason.append("；该用户有").append(successCount).append("次成功认领记录");
        if (rejectCount > 0) reason.append("；本物品已被驳回").append(rejectCount).append("次");
        if (lowConfidence) reason.append("，建议人工重点核验");

        return new Analysis(score, lowConfidence, reason.toString());
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "") : "";
    }

    private static Set<String> bigrams(String value) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 1 < value.length(); i++) result.add(value.substring(i, i + 2));
        return result;
    }

    public record Analysis(int score, boolean lowConfidence, String reason) {
    }
}
