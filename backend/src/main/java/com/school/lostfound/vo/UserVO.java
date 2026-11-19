package com.school.lostfound.vo;

import com.school.lostfound.enums.UserRole;
import lombok.Data;

/**
 * 用户信息 VO
 *
 * 功能：封装返回给前端的用户信息。与实体 User 最大的区别：
 * 剔除了 password 等敏感字段，是所有用户数据对外展示的"安全外壳"。
 *
 * 为什么需要：直接返回 User 实体会泄露密码哈希、删除标记等
 * 内部字段；接口层统一转成 UserVO，从结构上杜绝敏感信息外泄。
 *
 * 使用位置：登录/注册响应（嵌在 LoginVO 中）、个人中心信息展示、
 * 管理员"用户管理"列表。
 */
@Data
public class UserVO {
    private Long id;
    /** 用户名（登录账号） */
    private String username;
    /** 真实姓名 */
    private String realName;
    /** 学号 */
    private String studentId;
    private String phone;
    private String email;
    /** 角色：STUDENT=学生, ADMIN=管理员, STAFF=托管点工作人员 */
    private UserRole role;
    /** 诚信积分 */
    private Integer creditScore;
    /** 账号状态：1=正常 0=禁用 */
    private Integer status;
    /** 是否允许出现在诚信排行榜：1=允许（隐私开关） */
    private Integer allowLeaderboard;
}
