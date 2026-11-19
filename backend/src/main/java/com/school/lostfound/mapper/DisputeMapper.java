package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.Dispute;
import org.apache.ibatis.annotations.Mapper;

/**
 * 认领争议的数据访问层接口（Mapper），对应数据库表 dispute、实体 Dispute。
 *
 * <p>作用：负责 dispute 表的增删改查。当认领被拒绝或双方对物品归属有异议时，用户可发起争议，
 * 由管理员仲裁；仲裁结果会影响信用分（经 CreditLogMapper 记录流水）。
 * 主要使用方为 DisputeServiceImpl（争议发起与裁决）。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现，无需手写 SQL。</p>
 */
@Mapper
public interface DisputeMapper extends BaseMapper<Dispute> {
}
