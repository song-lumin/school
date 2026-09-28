package com.school.lostfound.vo;

import com.school.lostfound.entity.CreditLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateVO {
    private String realName;
    private String studentId;
    private Integer totalScore;
    private Map<String, Long> statsByType;
    private List<CertificateRecord> records;
    private LocalDateTime issuedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateRecord {
        private Integer changeAmount;
        private String operationType;
        private Long relatedItemId;
        private String reason;
        private LocalDateTime createdAt;

        public static CertificateRecord fromLog(CreditLog log) {
            return new CertificateRecord(log.getChangeAmount(), log.getOperationType(),
                    log.getRelatedItemId(), log.getReason(), log.getCreatedAt());
        }
    }
}
