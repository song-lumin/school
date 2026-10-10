package com.school.lostfound.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimConfidenceAnalyzerTest {
    @Test
    void shortGenericAnswerIsMarkedForReview() {
        assertTrue(ClaimConfidenceAnalyzer.analyze("是", "蓝色手机壳，右下角有裂纹", 50, 0, 0).lowConfidence());
    }

    @Test
    void informativeAnswerRelatedToDescriptionScoresHigher() {
        var brief = ClaimConfidenceAnalyzer.analyze("蓝色", "手机壳是蓝色，右下角有裂纹", 50, 0, 0);
        var detailed = ClaimConfidenceAnalyzer.analyze("蓝色手机壳，右下角有一道裂纹", "手机壳是蓝色，右下角有裂纹", 50, 0, 0);

        assertTrue(detailed.score() > brief.score());
        assertFalse(detailed.lowConfidence());
    }

    @Test
    void emptyAnswerGetsBoundedLowScoreWithReason() {
        var result = ClaimConfidenceAnalyzer.analyze("  ", "校园卡，背面写有学号", 50, 0, 0);

        assertTrue(result.score() >= 0 && result.score() <= 100);
        assertTrue(result.lowConfidence());
        assertFalse(result.reason().isBlank());
    }
}
