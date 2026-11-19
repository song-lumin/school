package com.school.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 以图搜物结果 VO
 *
 * 功能：封装"以图搜物"功能的单条结果：匹配到的物品（FoundItemVO）
 * + 相似度百分比。用户上传一张照片，系统计算其与库内物品图片的
 * 相似度并按分数排序返回本 VO 列表。
 *
 * 为什么需要：搜索结果必须同时携带"物品信息"和"相似度分数"两个
 * 维度，且相似度是计算出来的临时值、不存在于任何实体中，
 * 所以用 VO 组合封装。
 *
 * 使用位置：首页"拍照找失物"功能（ItemService 图搜接口）。
 */
@Data
@AllArgsConstructor
public class ImageSearchResultVO {
    /** 匹配到的物品 */
    private FoundItemVO item;
    /** 与上传图片的相似度（0-100，整数） */
    private int similarity;
}
