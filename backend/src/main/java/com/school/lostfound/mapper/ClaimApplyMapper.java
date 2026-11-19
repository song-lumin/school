package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.ClaimApply;
import org.apache.ibatis.annotations.Mapper;

/**
 * 失物认领申请的数据访问层接口（Mapper），对应数据库表 claim_apply、实体 ClaimApply。
 *
 * <p>作用：负责 claim_apply 表的增删改查。用户看到匹配到自己失物的招领信息后，提交认领申请，
 * 管理员/发布者审核通过后物品才能被领走。供 ClaimServiceImpl（申请流转）、
 * AdminServiceImpl（后台审核）、RiskControlServiceImpl（认领风控校验）等使用。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现，无需手写 SQL；复杂的条件查询由 Service 层通过 LambdaQueryWrapper 构建。</p>
 */
@Mapper
public interface ClaimApplyMapper extends BaseMapper<ClaimApply> {
}
