package com.school.lostfound.vo;

import com.school.lostfound.entity.CameraLog;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/**
 * 监控录像核查申请 VO（View Object，视图对象）
 *
 * 功能：封装一条"调监控核查申请"的完整展示数据返回给前端。
 * 与实体 CameraLog 的区别：实体只存各种外键 ID（dropPointId/itemId/applicantId/handlerId），
 * 而 VO 把关联的托管点名称、物品标题、申请人姓名、处理人姓名一并查出拼装好，
 * 前端拿到即可直接渲染，无需再发多次请求查询关联信息（避免 N+1 问题）。
 *
 * 使用位置：管理员端"监控核查管理"页面列表、申请人"我的核查申请"列表。
 */
@Data
public class CameraLogVO {
    private Long id;
    /** 托管点 ID */
    private Long dropPointId;
    /** 托管点名称（关联查询补充） */
    private String dropPointName;
    /** 关联物品 ID */
    private Long itemId;
    /** 关联物品标题（关联查询补充） */
    private String itemTitle;
    /** 申请理由（说明为何需要调取该时段监控） */
    private String applyReason;
    /** 申请人 ID */
    private Long applicantId;
    /** 申请人姓名（关联查询补充） */
    private String applicantName;
    /** 申请调取的监控时间段起止 */
    private LocalDateTime timeRangeStart;
    private LocalDateTime timeRangeEnd;
    /** 审核状态：0=待审核 1=已通过 2=已驳回 */
    private Integer status;
    /** 处理结果说明（通过时附录像线索，驳回时附原因） */
    private String resultNote;
    /** 处理人 ID */
    private Long handlerId;
    /** 处理人姓名（关联查询补充） */
    private String handlerName;
    /** 处理时间 */
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    /**
     * 由实体构造 VO（字段名相同的属性自动拷贝）。
     * 关联名称字段（dropPointName 等）由 Service 层单独查询后 set 进来。
     */
    public static CameraLogVO fromEntity(CameraLog log) {
        CameraLogVO vo = new CameraLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
