package com.school.lostfound.vo;

import com.school.lostfound.entity.CreditLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 诚信证书 VO
 *
 * 功能：封装"诚信拾金证书"的展示数据。用户在个人中心可下载/查看一张
 * 电子证书，汇总其全部拾金行为：真实姓名、学号、当前诚信总分、
 * 按加分类型统计的次数、以及逐条加分明细记录。
 *
 * 为什么需要：证书页需要一次请求拿到"汇总 + 明细"两类数据，
 * 直接返回 User/CreditLog 实体会暴露密码等敏感字段且结构分散，
 * 因此用 VO 聚合成证书版式需要的数据结构。
 *
 * 使用位置：个人中心"我的诚信证书"页面、证书导出接口。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateVO {
    /** 持证人真实姓名（证书抬头用） */
    private String realName;
    private String studentId;
    /** 当前诚信总分 */
    private Integer totalScore;
    /** 按加分类型（如拾金上交、协助认领）统计的次数 */
    private Map<String, Long> statsByType;
    /** 加分明细列表 */
    private List<CertificateRecord> records;
    /** 证书开具时间 */
    private LocalDateTime issuedAt;

    /**
     * 证书内嵌明细记录：一条对应一条信用积分变动日志（CreditLog），
     * 只挑出证书需要的字段，避免把整个实体返回给前端。
     */
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
