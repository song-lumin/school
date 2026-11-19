package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.LostNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 寻物启事的数据访问层接口（Mapper），对应数据库表 lost_notice、实体 LostNotice。
 *
 * <p>作用：负责 lost_notice 表的增删改查。用户丢失物品后发布寻物启事，系统可将其与
 * 招领物品做匹配提醒。使用方：LostNoticeServiceImpl（启事发布/检索/状态管理）、
 * FoundItemServiceImpl（发布招领物品时自动匹配寻物启事）、ReportServiceImpl（举报启事）、
 * ModerationController（内容审核）。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现（含分页），无需手写 SQL。</p>
 */
@Mapper
public interface LostNoticeMapper extends BaseMapper<LostNotice> {
}
