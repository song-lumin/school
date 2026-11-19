package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.DropPoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 失物投放点的数据访问层接口（Mapper），对应数据库表 drop_point、实体 DropPoint。
 *
 * <p>作用：负责 drop_point 表的增删改查。投放点是校园内实际存在的失物收集点
 * （如"图书馆一楼服务台"），摄像头识别、捡到物品上交、认领取件都围绕投放点进行。
 * 使用方非常广泛：CameraLogServiceImpl（识别日志关联投放点）、DropPointServiceImpl
 * （投放点管理与上交登记）、ClaimServiceImpl（告知用户到哪个投放点取件）、
 * ImageSearchServiceImpl / ArchiveController / ModerationController（展示与归档）等。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现，无需手写 SQL。</p>
 */
@Mapper
public interface DropPointMapper extends BaseMapper<DropPoint> {
}
