package com.school.lostfound.vo;

import com.school.lostfound.enums.UserRole;
import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String studentId;
    private String phone;
    private String email;
    private UserRole role;
    private Integer creditScore;
    private Integer status;
    private Integer allowLeaderboard;
}
