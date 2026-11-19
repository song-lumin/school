package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.FoundItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 招领物品（捡到的物品）的数据访问层接口（Mapper），对应数据库表 found_item、实体 FoundItem。
 *
 * <p>作用：负责 found_item 表的增删改查。found_item 是整个系统的核心表，记录每一件
 * 被捡到/上交的物品及其状态流转（待认领、已认领、已失效、已下架等）。使用方覆盖
 * FoundItemServiceImpl（发布/检索/状态流转）、ClaimServiceImpl、ReportServiceImpl、
 * 各定时任务（AutoExpireTask 自动过期、AutoInvalidateTask 自动作废、ExpireWarningTask
 * 过期预警）、ModerationController（内容审核）、ArchiveController（归档统计）等。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现（含分页查询 selectPage），无需手写 SQL；多条件检索由 Service 层
 * 通过 LambdaQueryWrapper 动态构建。</p>
 */
@Mapper
public interface FoundItemMapper extends BaseMapper<FoundItem> {
}
