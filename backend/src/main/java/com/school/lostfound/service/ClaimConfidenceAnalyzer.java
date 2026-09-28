package com.school.lostfound.service;

import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ClaimConfidenceAnalyzer {
    private ClaimConfidenceAnalyzer() {
    }

    public static Analysis analyze(String answer, String itemText) {
        String normalizedAnswer = normalize(answer);
        if (normalizedAnswer.isEmpty()) {
            return new Analysis(0, true, "回答为空，建议人工核验");
        }
        Set<String> answerBigrams = bigrams(normalizedAnswer);
        Set<String> textBigrams = bigrams(normalize(itemText));
        long overlaps = answerBigrams.stream().filter(textBigrams::contains).count();
        double relevance = answerBigrams.isEmpty() ? 0 : (double) overlaps / answerBigrams.size();
        int lengthScore = Math.min(40, normalizedAnswer.length() * 2);
        int score = Math.min(100, 20 + lengthScore + (int) Math.round(relevance * 40));
        boolean lowConfidence = normalizedAnswer.length() < 2 || score < 40;
        String reason = lowConfidence ? "回答较短或与物品描述关联较弱，建议人工核验" : "仅供参考：回答长度与描述关联度正常";
        return new Analysis(score, lowConfidence, reason);
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
