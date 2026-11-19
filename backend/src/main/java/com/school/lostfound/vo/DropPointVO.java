package com.school.lostfound.vo;

import com.school.lostfound.entity.DropPoint;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * 托管点（失物收集点）VO
 *
 * 功能：封装托管点的展示数据。托管点是校园内存放拾获物品的物理位置
 * （如各楼栋服务台），VO 在实体 DropPoint 基础上补充了负责管理员姓名。
 *
 * 使用位置：前台"托管点查询"页面（供失主/拾获者就近投放或认领）、
 * 管理员"托管点管理"页面。
 */
@Data
public class DropPointVO {
    private Long id;
    /** 托管点名称，如"图书馆服务台" */
    private String name;
    /** 详细位置描述 */
    private String location;
    /** 负责管理员 ID */
    private Long adminId;
    /** 负责管理员姓名（关联查询补充） */
    private String adminName;
    /** 是否配有监控摄像头：1=有 0=无 */
    private Integer hasCamera;
    /** 摄像头信息描述（如编号、覆盖区域） */
    private String cameraInfo;
    /** 状态：1=开放 0=关闭 */
    private Integer status;

    /**
     * 由实体构造 VO 并补充管理员姓名。
     */
    public static DropPointVO fromEntity(DropPoint dropPoint, String adminName) {
        DropPointVO vo = new DropPointVO();
        BeanUtils.copyProperties(dropPoint, vo);
        vo.setAdminName(adminName);
        return vo;
    }
}
