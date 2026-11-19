package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.HandInLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物品上交流水的数据访问层接口（Mapper），对应数据库表 hand_in_log、实体 HandInLog。
 *
 * <p>作用：负责 hand_in_log 表的增删改查。记录每次"把捡到的物品上交到投放点"的行为
 * （谁上交、交到哪个投放点、对应哪件物品），是物品来源可追溯的关键凭证。
 * 使用方：DropPointServiceImpl（投放点上交登记）、FoundItemServiceImpl（发布招领时记录来源）、
 * AutoInvalidateTask（物品自动作废时的上交关联处理）。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现，无需手写 SQL。</p>
 */
@Mapper
public interface HandInLogMapper extends BaseMapper<HandInLog> {
}
