package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.CameraLogCreateRequest;
import com.school.lostfound.dto.CameraLogHandleRequest;
import com.school.lostfound.vo.CameraLogVO;

/**
 * 监控录像调阅服务接口 —— 管理失物"监控录像拷贝申请"的完整流程。
 * <p>
 * 业务背景：很多物品丢失发生在教室、食堂等有监控的区域，学生个人
 * 无权调看监控，需向保卫处/管理员提交调阅申请。系统把这一线下流程
 * 线上化：申请人（失主）提交申请并关联寻物启事 → 管理员审核处理
 * （同意后告知查看时间地点 / 拒绝并说明原因）→ 全程留痕可追溯。
 * <p>
 * 提供列表筛选（按状态/投放点/物品）、创建申请、处理申请三类能力。
 */
public interface CameraLogService {
    /**
     * 分页查询监控调阅申请，支持多条件筛选。
     *
     * @param status      申请状态筛选（如待处理/已同意/已拒绝），null 表示不过滤
     * @param dropPointId 按投放点/区域筛选，null 表示不过滤
     * @param itemId      按关联物品筛选，null 表示不过滤
     * @param page        页码（从 1 开始）
     * @param size        每页条数
     * @return 申请分页列表（VO 含申请人昵称等关联展示信息）
     */
    IPage<CameraLogVO> list(Integer status, Long dropPointId, Long itemId, int page, int size);

    /**
     * 失主创建一条监控调阅申请。
     *
     * @param request     申请内容（关联物品、事发时间地点、说明等）
     * @param applicantId 申请人 ID（服务端取当前登录用户，防止冒名提交）
     * @return 创建成功后的申请详情 VO
     */
    CameraLogVO create(CameraLogCreateRequest request, Long applicantId);

    /**
     * 管理员处理一条申请（同意并填写调阅安排，或拒绝并给出理由）。
     *
     * @param id        申请记录 ID
     * @param request   处理结果（是否同意、备注原因）
     * @param handlerId 处理人 ID（管理员），写入记录便于审计
     */
    void handle(Long id, CameraLogHandleRequest request, Long handlerId);
}
