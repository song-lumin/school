package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.CreditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信用分变动日志的数据访问层接口（Mapper），对应数据库表 credit_log、实体 CreditLog。
 *
 * <p>作用：负责 credit_log 表的增删改查。系统采用信用分机制约束用户行为
 * （如按时取回物品加分、虚假发布/恶意认领扣分），每一次加减分都会在本表记录一条流水，
 * 供 CreditServiceImpl（加扣分）、DisputeServiceImpl（争议裁决后回溯）、
 * CreditQueryServiceImpl（用户查询自己的信用流水）使用。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现，无需手写 SQL。</p>
 */
@Mapper
public interface CreditLogMapper extends BaseMapper<CreditLog> {
}
