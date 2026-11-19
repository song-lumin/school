package com.school.lostfound.vo;

import com.school.lostfound.entity.FoundItem;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 托管点库存物品 VO
 *
 * 功能：封装托管点管理员的库存清单条目。在物品实体 FoundItem 之外
 * 补充交接流程中的流转信息：上交状态、上交时间、巡检核对时间/备注、
 * 是否已发信用分，以及发布人/认领人姓名。
 *
 * 为什么需要：托管点管理员的工作台需要"物品 + 交接状态"的合成视图，
 * 这些交接字段分散在 HandInLog 等关联表中，由 Service 聚合进本 VO，
 * 管理员一次请求即可看到每件物品处于哪个交接环节。
 *
 * 使用位置：托管点管理员"库存管理"页面（InventoryService）。
 */
@Data
public class InventoryItemVO {
    /** 物品基本信息（直接复用实体） */
    private FoundItem item;
    /** 0=未投放 1=已放置待巡检 2=已核对 3=作废 */
    private Integer handInStatus;
    /** 上交/放置到本托管点的时间 */
    private LocalDateTime handInAt;
    /** 巡检核对完成时间 */
    private LocalDateTime checkedAt;
    /** 巡检核对备注 */
    private String checkNote;
    /** 是否已给拾获人发放信用分：1=已发 */
    private Integer creditIssued;
    /** 发布人（拾获人）姓名 */
    private String founderName;
    /** 认领人姓名 */
    private String claimerName;
    /** 被领取时间 */
    private LocalDateTime pickupTime;
}
