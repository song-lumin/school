package com.school.lostfound.vo;

import com.school.lostfound.entity.CameraLog;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class CameraLogVO {
    private Long id;
    private Long dropPointId;
    private String dropPointName;
    private Long itemId;
    private String itemTitle;
    private String applyReason;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime timeRangeStart;
    private LocalDateTime timeRangeEnd;
    private Integer status;
    private String resultNote;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    public static CameraLogVO fromEntity(CameraLog log) {
        CameraLogVO vo = new CameraLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
