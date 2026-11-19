package com.school.lostfound.vo;

import com.school.lostfound.entity.FoundItem;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/**
 * 失物招领物品 VO —— 系统最核心的展示对象
 *
 * 功能：封装一条招领物品的完整展示数据。实体 FoundItem 只保存
 * founderId（发布人ID）、dropPointId（托管点ID）等外键，
 * VO 将发布人姓名、托管点名称等关联信息一并查出拼装好，
 * 前端列表页/详情页一次请求即可渲染完整卡片。
 *
 * 为什么需要：避免前端为每条物品再发请求查发布人和托管点
 * （N+1 问题），同时隔离实体结构——防止把实体里的敏感或
 * 冗余字段直接暴露给接口调用方。
 *
 * 使用位置：首页物品列表、物品详情、管理员物品管理、图搜结果等
 * 几乎所有展示物品的接口。
 */
@Data
public class FoundItemVO {
    private Long id;
    /** 物品标题 */
    private String title;
    /** 分类（如证件、电子设备、钥匙等） */
    private String category;
    private String description;
    /** 拾获地点 */
    private String foundLocation;
    /** 拾获时间 */
    private LocalDateTime foundTime;
    /** 认领验证问题（认领人须答对此问题才可能通过申请） */
    private String claimQuestion;
    /** 物品图片 URL 列表 */
    private java.util.List<String> images;
    /** 是否易腐物品（1=是，需优先处理） */
    private Integer perishable;
    /** 物品状态：0=待审核 1=公开展示 2=已领取 3=已过期 */
    private Integer itemStatus;
    /** 发布人 ID */
    private Long founderId;
    /** 发布人姓名（关联查询补充） */
    private String founderName;
    /** 存放托管点 ID */
    private Long dropPointId;
    /** 托管点名称（关联查询补充） */
    private String dropPointName;
    /** 发布时间 */
    private LocalDateTime publishedAt;
    /** 被领取时间 */
    private LocalDateTime claimedAt;
    /** 发布者注册未满7天（新账号冷却，需重点核对） */
    private Boolean newUser;

    /** 便捷重载：不补充关联名称时使用（如仅比较场景） */
    public static FoundItemVO fromEntity(FoundItem item) {
        return fromEntity(item, null, null, null);
    }

    /**
     * 由实体构造 VO（同名字段 BeanUtils 自动拷贝），
     * 并补充发布人姓名、托管点名称、新账号标记三个关联字段。
     */
    public static FoundItemVO fromEntity(FoundItem item, String founderName, String dropPointName, Boolean newUser) {
        FoundItemVO vo = new FoundItemVO();
        BeanUtils.copyProperties(item, vo);
        vo.setFounderName(founderName);
        vo.setDropPointName(dropPointName);
        vo.setNewUser(newUser);
        return vo;
    }
}
