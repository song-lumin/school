package com.school.lostfound.service;

import com.school.lostfound.vo.RiskWarningVO;

import java.util.List;

/**
 * 风控服务接口 —— 平台反作弊与内容安全的统一入口。
 * <p>
 * 业务背景：失物招领平台依赖诚信运转，但存在几类典型风险：
 * 1. 发布内容含敏感/违规词汇（广告、不当言论）；
 * 2. 串通刷分：两个人反复互认对方发布的物品骗取信用分；
 * 3. 发布人"刷单"：大量发布无人认领的物品抬高数据，认领率异常偏低；
 * 4. 盗图：多个物品使用相同/高度相似的图片（重复图片指纹）。
 * 本服务把这些检测规则集中实现，供管理端一键巡检并生成预警列表，
 * 管理员据此人工复核后处罚或驳回。
 */
public interface RiskControlService {
    /**
     * 发布/认领文本敏感词检查。
     *
     * @param text 待检查的文本（物品描述、认领理由等）
     * @return 命中敏感词返回 true，调用方应拒绝提交并提示修改
     */
    boolean containsSensitiveWord(String text);

    /**
     * 串通检测：统计历史完成的认领记录，若"同一发布人 + 同一认领人"
     * 成功完成领取达到 2 次及以上，则生成预警（涉嫌互相刷分）。
     *
     * @return 串通嫌疑预警列表（含涉事用户与次数）
     */
    List<RiskWarningVO> detectCollusion();

    /**
     * 认领率异常检测：某发布人历史发布的物品中，成功被认领的比例过低，
     * 说明可能存在批量发虚假物品刷数据的行为，生成预警供管理员核实。
     *
     * @return 认领率异常预警列表
     */
    List<RiskWarningVO> detectAbnormalClaimRate();

    /**
     * 重复图片检测：比对物品图片的感知哈希指纹，发现不同物品使用了
     * 高度相似（汉明距离小于阈值）的图片时生成预警，用于发现盗图/重复发布。
     *
     * @return 重复图片预警列表
     */
    List<RiskWarningVO> detectDuplicateImages();

    /**
     * 汇总以上全部检测维度，一次性返回所有风控预警，
     * 供管理端"风控巡检"页面统一展示。
     *
     * @return 全部预警的合并列表
     */
    List<RiskWarningVO> getAllWarnings();
}
