package com.school.lostfound.vo;

import com.school.lostfound.entity.LostNotice;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LostNoticeVO {
    private Long id;
    private String title;
    private String category;
    private String description;
    private String lostLocation;
    private LocalDateTime lostTime;
    private String contactInfo;
    private List<String> images;
    private Integer status;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime createdAt;

    public static LostNoticeVO fromEntity(LostNotice notice, String publisherName) {
        LostNoticeVO vo = new LostNoticeVO();
        BeanUtils.copyProperties(notice, vo);
        vo.setPublisherName(publisherName);
        return vo;
    }
}
