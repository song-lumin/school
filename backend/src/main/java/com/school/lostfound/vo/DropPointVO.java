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
    private String adminPhone;
    private String adminEmail;
    /** 站点对外服务说明，如"工作日 9:00-17:00" */
    private String officeHours;
    private Integer hasCamera;
    private String cameraInfo;
    private Integer status;

    public static DropPointVO fromEntity(DropPoint dropPoint, String adminName) {
        return fromEntity(dropPoint, adminName, null, null);
    }

    public static DropPointVO fromEntity(DropPoint dropPoint, String adminName, String adminPhone, String adminEmail) {
        DropPointVO vo = new DropPointVO();
        BeanUtils.copyProperties(dropPoint, vo);
        vo.setAdminName(adminName);
        vo.setAdminPhone(adminPhone);
        vo.setAdminEmail(adminEmail);
        vo.setOfficeHours("工作日 9:00-17:00");
        return vo;
    }
}
