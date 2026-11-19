package com.school.lostfound.vo;

import com.school.lostfound.entity.ReportLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报 VO
 *
 * 功能：封装一条用户举报记录的展示数据。用户可举报虚假招领、
 * 冒领行为等，VO 在实体 ReportLog 基础上补充举报人姓名与
 * 被举报物品标题，供管理员直接审核处理。
 *
 * 使用位置：管理员"举报处理"页面、用户"我的举报"列表。
 */
@Data
public class ReportVO {
    private Long id;
    /** 举报人 ID */
    private Long reporterId;
    /** 举报人姓名（关联查询补充） */
    private String reporterName;
    /** 被举报物品 ID */
    private Long itemId;
    /** 被举报物品标题（关联查询补充） */
    private String itemTitle;
    /** 举报类型（如虚假信息、冒领） */
    private String reportType;
    /** 举报描述 */
    private String description;
    /** 举证图片列表 */
    private List<String> evidenceImages;
    /** 处理状态：0=待处理 1=已处理 */
    private Integer status;
    /** 管理员处理意见 */
    private String handlerNote;
    /** 处理人 ID */
    private Long handlerId;
    /** 处理时间 */
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    /**
     * 由实体构造 VO 并补充举报人姓名与物品标题（逐字段赋值，
     * 因实体中没有这两个关联字段）。
     */
    public static ReportVO fromEntity(ReportLog r, String reporterName, String itemTitle) {
        ReportVO vo = new ReportVO();
        vo.setId(r.getId());
        vo.setReporterId(r.getReporterId());
        vo.setReporterName(reporterName);
        vo.setItemId(r.getItemId());
        vo.setItemTitle(itemTitle);
        vo.setReportType(r.getReportType());
        vo.setDescription(r.getDescription());
        vo.setEvidenceImages(r.getEvidenceImages());
        vo.setStatus(r.getStatus());
        vo.setHandlerNote(r.getHandlerNote());
        vo.setHandlerId(r.getHandlerId());
        vo.setHandledAt(r.getHandledAt());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}
