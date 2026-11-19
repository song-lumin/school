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
    /** 误领次数：从 MISCLAIM_COUNT 计次流水统计，积分明细里不再展示这类 0 分记录 */
    private Integer misclaimCount;
}
