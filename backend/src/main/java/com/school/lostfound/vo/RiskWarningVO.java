package com.school.lostfound.vo;

import lombok.Data;

import java.util.List;

/**
 * 风险预警 VO
 *
 * 功能：封装一条系统风控预警的展示数据。风控模块定时扫描可疑行为
 * （如发布人与认领人串通、某用户认领率异常偏高等），将预警结果
 * 聚合到本 VO，供管理员重点核查。
 *
 * 为什么需要：一条预警可能涉及多种类型，各自的关键指标不同
 * （串通预警看相似度 similarity、认领率预警看比率 count），
 * 用一个 VO 承载所有类型的字段，由 warningType 区分解读方式，
 * 避免为每种预警单独建类。
 *
 * 使用位置：管理员"风险预警"页面（AdminService 风控接口）。
 */
@Data
public class RiskWarningVO {
    /** 预警类型：COLLUSION=串通, CLAIM_RATE=认领率异常 */
    private String warningType;
    /** 涉及用户ID（串通=双方，认领率=发布人） */
    private List<Long> userIds;
    /** 展示名（脱敏后） */
    private List<String> userNames;
    /** 描述 */
    private String detail;
    /** 关键数值（认领率预警=申请次数或异常比率） */
    private Integer count;
    /** 涉及的物品 ID 列表 */
    private List<Long> itemIds;
    /** 两件物品的图片相似度（串通预警专用，0-100） */
    private Integer similarity;
    /** 涉事物品一标题 */
    private String firstItemTitle;
    /** 涉事物品二标题 */
    private String secondItemTitle;
}
