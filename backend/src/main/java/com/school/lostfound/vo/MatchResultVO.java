package com.school.lostfound.vo;

import com.school.lostfound.entity.FoundItem;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MatchResultVO {
    private Long itemId;
    private String title;
    private String category;
    private String foundLocation;
    private LocalDateTime foundTime;
    private List<String> images;
    /** 匹配分数（0-100） */
    private int score;
    /** 命中的匹配条件说明 */
    private List<String> matchReasons;

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
