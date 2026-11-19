package com.school.lostfound.vo;

import com.school.lostfound.entity.LostNotice;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 寻物启事 VO
 *
 * 功能：封装一条寻物启事的展示数据。实体 LostNotice 只存 publisherId，
 * VO 补充发布人姓名，并把图片 JSON 字符串解析为 URL 列表，
 * 前端可直接渲染。
 *
 * 使用位置：前台"寻物启事"列表/详情页、个人中心"我发布的寻物启事"、
 * 管理员内容审核页面。
 */
@Data
public class LostNoticeVO {
    private Long id;
    /** 寻物标题 */
    private String title;
    /** 物品分类 */
    private String category;
    /** 物品描述 */
    private String description;
    /** 丢失地点 */
    private String lostLocation;
    /** 丢失时间 */
    private LocalDateTime lostTime;
    /** 联系方式 */
    private String contactInfo;
    /** 图片 URL 列表 */
    private List<String> images;
    /** 状态：0=待审核 1=展示中 2=已找到/关闭 */
    private Integer status;
    /** 发布人 ID */
    private Long publisherId;
    /** 发布人姓名（关联查询补充） */
    private String publisherName;
    /** 发布时间 */
    private LocalDateTime createdAt;

    /**
     * 由实体构造 VO 并补充发布人姓名。
     */
    public static LostNoticeVO fromEntity(LostNotice notice, String publisherName) {
        LostNoticeVO vo = new LostNoticeVO();
        BeanUtils.copyProperties(notice, vo);
        vo.setPublisherName(publisherName);
        return vo;
    }
}
