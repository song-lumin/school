package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.ReportCreateRequest;
import com.school.lostfound.dto.ReportHandleRequest;
import com.school.lostfound.vo.ReportVO;

/**
 * 举报处理业务接口（Service 层）。
 *
 * <p>作用：定义"举报（Report）"模块的业务能力。用户可以对虚假启事、
 * 不当认领、违规内容等发起举报；管理员负责审核处理举报并执行
 * 相应处置（如驳回、下架涉事内容、扣减信用分等）。</p>
 *
 * <p>为什么需要它：举报流程包含"创建 → 管理员审核 → 处置反馈"的完整生命周期，
 * 涉及状态流转与权限校验（举报人/管理员两种视角），独立成 Service 层
 * 可避免业务逻辑散落在 Controller 中，保持分层清晰。</p>
 */
public interface ReportService {

    /**
     * 创建一条举报。
     *
     * @param request    举报内容（举报类型 reportType、被举报对象 ID、举报理由等）
     * @param reporterId 举报发起人的用户 ID
     * @return 创建成功的举报详情（含初始状态，通常为"待处理"）
     */
    ReportVO create(ReportCreateRequest request, Long reporterId);

    /**
     * 管理员分页查询举报列表，可按处理状态与举报类型过滤。
     *
     * @param status     处理状态（如 0 待处理 / 1 已处理），可为空查全部
     * @param reportType 举报类型过滤，可为空
     * @param page       页码
     * @param size       每页条数
     */
    IPage<ReportVO> list(Integer status, String reportType, int page, int size);

    /**
     * 管理员处理一条举报（通过/驳回），并记录处理意见。
     * 业务规则：仅管理员可操作；一条举报通常只能被处理一次，
     * 重复处理应抛出业务异常；处理后举报人可在"我的举报"中看到结果。
     *
     * @param id         举报 ID
     * @param request    处理请求（处理结果、处理说明）
     * @param operatorId 操作的管理员用户 ID，用于记录处理人
     */
    void handle(Long id, ReportHandleRequest request, Long operatorId);

    /**
     * 分页查询"我的举报"：某用户自己发起过的所有举报及其处理进度，
     * 供普通用户查看举报反馈。
     */
    IPage<ReportVO> listMy(Long reporterId, int page, int size);
}
