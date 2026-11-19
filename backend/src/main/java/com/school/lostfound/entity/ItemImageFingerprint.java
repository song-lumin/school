package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品图片指纹实体类，对应数据库表 item_image_fingerprint。
 *
 * <p>业务场景：系统支持"以图搜图"和相似物品推荐。为了让两张"看起来一样"的图片能被
 * 计算机快速比对，本系统在上传物品图片时，为每张图片生成一个感知哈希指纹（dhash-64 算法），
 * 把 64 位哈希以十六进制字符串的形式存入本表。比对两张图片是否相似时，只需比较两个哈希
 * 字符串的汉明距离（不同 bit 的个数），无需再读取原始图片文件，大幅降低计算量。</p>
 *
 * <p>为什么需要它：图片指纹与物品图片是"一对多"关系，且需要记录生成时使用的算法版本
 * （algorithmVersion），以便将来升级算法后能区分新旧指纹、按需重新计算，因此单独建表存储，
 * 而不是把哈希直接塞进物品表的图片 URL 字段里。</p>
 *
 * <p>使用方：ImageFingerprintServiceImpl（生成与查询指纹）、
 * ImageSearchServiceImpl（按指纹汉明距离做以图搜图）、RiskControlServiceImpl（风控比对）。</p>
 */
@Data
@TableName("item_image_fingerprint")
public class ItemImageFingerprint {
    /** 主键，数据库自增（AUTO） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指纹所属的物品 ID，关联 found_item 表的 id */
    private Long itemId;

    /** 该指纹对应的图片的访问 URL（与物品表 images 数组中的一个元素对应） */
    private String imageUrl;

    /** 图片的感知哈希值，dhash-64 算法生成的 64 位哈希（十六进制字符串） */
    private String hashValue;

    /** 生成该指纹时的算法版本号（当前为 "dhash-64-v1"），便于算法升级后区分新旧数据 */
    private String algorithmVersion;

    /** 记录创建时间，插入时由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
