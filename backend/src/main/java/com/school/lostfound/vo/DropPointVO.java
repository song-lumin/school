package com.school.lostfound.vo;

import com.school.lostfound.entity.DropPoint;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class DropPointVO {
    private Long id;
    private String name;
    private String location;
    private Long adminId;
    private String adminName;
    private Integer hasCamera;
    private String cameraInfo;
    private Integer status;

    public static DropPointVO fromEntity(DropPoint dropPoint, String adminName) {
        DropPointVO vo = new DropPointVO();
        BeanUtils.copyProperties(dropPoint, vo);
        vo.setAdminName(adminName);
        return vo;
    }
}
