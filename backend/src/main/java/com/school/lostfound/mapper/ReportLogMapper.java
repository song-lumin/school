package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.ReportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报记录的数据访问层接口（Mapper），对应数据库表 report_log、实体 ReportLog。
 *
 * <p>作用：负责 report_log 表的增删改查。存储用户对物品或寻物启事的举报
 * （虚假发布、描述不符等）及其处理进度。使用方：ReportServiceImpl（创建与处理举报）、
 * FoundItemServiceImpl（物品状态变更时的举报联动处理）。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现（含分页），无需手写 SQL。</p>
 */
@Mapper
public interface ReportLogMapper extends BaseMapper<ReportLog> {
}
