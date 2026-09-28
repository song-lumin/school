package com.school.lostfound.vo;

import com.school.lostfound.entity.FoundItem;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class FoundItemVO {
    private Long id;
    private String title;
    private String category;
    private String description;
    private String foundLocation;
    private LocalDateTime foundTime;
    private String claimQuestion;
    private java.util.List<String> images;
    private Integer perishable;
    private Integer itemStatus;
    private Long founderId;
    private String founderName;
    private Long dropPointId;
    private String dropPointName;
    private LocalDateTime publishedAt;
    private LocalDateTime claimedAt;
    /** 发布者注册未满7天（新账号冷却，需重点核对） */
    private Boolean newUser;

    public static FoundItemVO fromEntity(FoundItem item) {
        return fromEntity(item, null, null, null);
    }

    public static FoundItemVO fromEntity(FoundItem item, String founderName, String dropPointName, Boolean newUser) {
        FoundItemVO vo = new FoundItemVO();
        BeanUtils.copyProperties(item, vo);
        vo.setFounderName(founderName);
        vo.setDropPointName(dropPointName);
        vo.setNewUser(newUser);
        return vo;
    }
}
