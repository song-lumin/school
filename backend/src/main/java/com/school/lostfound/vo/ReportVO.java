package com.school.lostfound.vo;

import com.school.lostfound.entity.ReportLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReportVO {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private Long itemId;
    private String itemTitle;
    private String reportType;
    private String description;
    private List<String> evidenceImages;
    private Integer status;
    private String handlerNote;
    private Long handlerId;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

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
