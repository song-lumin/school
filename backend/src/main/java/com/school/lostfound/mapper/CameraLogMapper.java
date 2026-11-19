package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.CameraLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 摄像头识别日志的数据访问层接口（Mapper），对应数据库表 camera_log、实体 CameraLog。
 *
 * <p>作用：负责 camera_log 表的增删改查。该表记录监控摄像头识别到"疑似有人上交/捡到物品"
 * 的事件日志，供 CameraLogServiceImpl 查询、确认后生成拾获记录。</p>
 *
 * <p>实现说明：本接口没有任何自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，
 * 由框架在启动时通过动态代理自动生成实现（insert / deleteById / updateById /
 * selectById / selectList / selectPage 等通用 CRUD 方法），无需手写 SQL。</p>
 */
@Mapper
public interface CameraLogMapper extends BaseMapper<CameraLog> {
}
