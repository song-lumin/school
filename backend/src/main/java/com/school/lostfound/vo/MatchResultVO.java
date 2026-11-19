package com.school.lostfound.vo;

import com.school.lostfound.entity.FoundItem;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 失物-招领智能匹配结果 VO
 *
 * 功能：封装"系统自动匹配"的单条结果。系统将用户的寻物启事与
 * 库内招领物品按分类、地点、时间、关键词等规则打分，把符合条件的
 * 物品连同匹配分数、命中原因一起返回，帮助失主快速锁定目标物品。
 *
 * 为什么需要：score 与 matchReasons 是算法计算出的临时值，
 * 不存在于任何实体中；匹配页也只需要展示型字段而非完整实体。
 *
 * 使用位置：寻物启事详情页"为您找到疑似物品"推荐区、
 * 匹配推荐接口（ItemService.match）。
 */
@Data
public class MatchResultVO {
    private Long itemId;
    /** 物品标题 */
    private String title;
    /** 分类 */
    private String category;
    /** 拾获地点 */
    private String foundLocation;
    /** 拾获时间 */
    private LocalDateTime foundTime;
    /** 物品图片 URL 列表 */
    private List<String> images;
    /** 匹配分数（0-100） */
    private int score;
    /** 命中的匹配条件说明 */
    private List<String> matchReasons;

    /**
     * 由实体构造 VO：只挑出匹配页需要展示的字段，
     * 并附加算法计算出的 score 与 reasons。
     */
    public static MatchResultVO fromEntity(FoundItem item, int score, List<String> reasons) {
        MatchResultVO vo = new MatchResultVO();
        vo.setItemId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setCategory(item.getCategory());
        vo.setFoundLocation(item.getFoundLocation());
        vo.setFoundTime(item.getFoundTime());
        vo.setImages(item.getImages());
        vo.setScore(score);
        vo.setMatchReasons(reasons);
        return vo;
    }
}
