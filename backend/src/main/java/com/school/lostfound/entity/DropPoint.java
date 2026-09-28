package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("drop_point")
public class DropPoint {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String location;

    private Long adminId;

    private Integer hasCamera;

    private String cameraInfo;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
