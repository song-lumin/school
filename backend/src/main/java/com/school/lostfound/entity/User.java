package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.school.lostfound.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String studentId;

    private String phone;

    private String email;

    private UserRole role;

    private Integer creditScore;

    private Integer status;

    private Integer allowLeaderboard;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
