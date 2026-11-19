package com.school.lostfound.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品过期预警 VO
 *
 * 功能：封装一条"物品即将过期"预警的展示数据。自动物品过期任务
 * （ExpireWarningTask）定时扫描即将超期（默认保管期7天）的招领物品，
 * 生成预警列表供管理员催促处理/下架。
 *
 * 为什么需要：单独建 VO 而不复用 FoundItemVO，是因为预警页只需
 * 少数几个字段（标题、发布人、已发布天数等），精简的 VO 减少
 * 数据传输量，也让定时任务组件与应用层解耦（task 包不依赖完整的
 * 物品 VO 装配逻辑）。
 *
 * 使用位置：管理员"过期预警"列表（AdminService 修改场景）。
 */
@Data
public class ExpireWarningVO {
    /** 临期物品 ID */
    private Long itemId;
    /** 物品标题 */
    private String title;
    /** 拾获发布人姓名 */
    private String founderName;
    /** 发布时间 */
    private LocalDateTime publishedAt;
    /** 是否易腐物品（1=是，需优先处理） */
    private Integer perishable;
    /** 距发布已过的天数 */
    private Long daysSincePublish;
}
