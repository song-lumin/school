package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户的数据访问层接口（Mapper），对应数据库表 user、实体 User。
 *
 * <p>作用：负责 user 表的增删改查。user 表是系统的基础表，存储所有用户
 * （学生、教职工、管理员）的账号、角色、信用分等信息。使用方极为广泛：
 * UserServiceImpl（注册/登录）、JwtAuthenticationFilter（认证时加载用户）、
 * CreditServiceImpl（更新信用分）、AdminServiceImpl（用户管理），以及几乎所有
 * 需要补充"发布者/处理人昵称"等展示信息的 Service 与 Controller。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现，无需手写 SQL。登录认证依赖 Spring Security + JWT，
 * 用户查询由 Service 层通过 LambdaQueryWrapper 按用户名/学号构建。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
