package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.ItemImageFingerprint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物品图片指纹的数据访问层接口（Mapper），对应数据库表 item_image_fingerprint、
 * 实体 ItemImageFingerprint。
 *
 * <p>作用：负责 item_image_fingerprint 表的增删改查。该表存储每张物品图片的感知哈希
 * （dhash-64）指纹，是"以图搜图"和相似物品推荐的数据基础。
 * 使用方：ImageFingerprintServiceImpl（生成/索引指纹）、ImageSearchServiceImpl（相似检索）、
 * RiskControlServiceImpl（风控比对）。</p>
 *
 * <p>实现说明：无自定义方法，完全继承 MyBatis-Plus 的 BaseMapper，由框架自动生成
 * 通用 CRUD 实现，无需手写 SQL；按 itemId / imageUrl 的条件查询由 Service 层
 * 通过 LambdaQueryWrapper 构建。相似度计算（汉明距离）在 Java 内存中完成。</p>
 */
@Mapper
public interface ItemImageFingerprintMapper extends BaseMapper<ItemImageFingerprint> {
}
