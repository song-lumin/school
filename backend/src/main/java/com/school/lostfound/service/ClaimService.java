package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.dto.ClaimRejectRequest;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.vo.ClaimApplyVO;

/**
 * 认领服务接口 —— 平台最核心的闭环业务："失主申请 → 发布人审核 →
 * 领取确认 → 信用分结算"。
 * <p>
 * 业务背景：拾获人发布招领后，真正的失主需要通过"认领申请"证明物品
 * 是自己的。为防冒领，流程设计了多重关卡：
 * 1. 验证问题：认领人回答拾获人预设的问题，系统用 ClaimConfidenceAnalyzer
 *    打出置信度分辅助决策；
 * 2. 审核把关：发布人（或超时后管理员介入）人工审核申请；
 * 3. 凭证留痕：领取时需拍照/签字，形成不可抵赖的交接记录；
 * 4. 信用结算：审核通过发放信用分，被驳回或撤销则扣分/回滚。
 * <p>
 * 状态机（认领申请）：待审核 → 已通过 → 已领取（成功终态）；
 * 待审核 → 已驳回。此后可由争议流程（DisputeService）补救。
 * 注意本文件第 6 行重复 import 的 ClaimApplyRequest 系原有代码，未予改动。
 */
public interface ClaimService {
    /**
     * 提交认领申请（正常入口）。校验：不能认领自己的发布、物品须处于
     * 公开/认领中状态、同一人对同一物品不可重复申请；申请中包含验证问题答案。
     *
     * @param request       申请内容（物品 ID、验证答案、说明等）
     * @param currentUserId 认领人 ID（服务端取当前登录用户）
     * @return 创建成功的申请详情 VO（含本次置信度评分）
     */
    ClaimApplyVO apply(ClaimApplyRequest request, Long currentUserId);

    /**
     * 从"寻物启事"跨流程转发申请认领：失主先发布了 LostNotice（寻物启事），
     * 后来在招领列表发现了疑似物品，可携带 sourceNoticeId 直接关联发起认领，
     * 系统会打通"寻物启事 ↔ 招领物品"两边的记录。
     *
     * @param request        申请内容
     * @param sourceNoticeId 来源寻物启事 ID，用于关联追踪
     * @param currentUserId  认领人 ID
     * @return 创建成功的申请详情 VO
     */
    ClaimApplyVO applyForward(ClaimApplyRequest request, Long sourceNoticeId, Long currentUserId);

    /**
     * 分页查看某物品下的全部认领申请（发布人审核用），
     * 会顺带校验当前用户是否为该物品发布人（防越权窥视他人申请）。
     *
     * @param itemId        物品 ID
     * @param currentUserId 当前用户 ID，用于权限判断
     * @param page          页码
     * @param size          每页条数
     * @return 申请分页列表（含认领人昵称、置信度分等展示信息）
     */
    IPage<ClaimApplyVO> listByItem(Long itemId, Long currentUserId, int page, int size);

    /**
     * 查看单条认领申请详情（发布人或申请人本人可见）。
     *
     * @param id            申请 ID
     * @param currentUserId 当前用户 ID，用于越权校验
     * @return 申请详情 VO
     */
    ClaimApplyVO getById(Long id, Long currentUserId);

    /**
     * 发布人通过一条认领申请。系统在此处发放信用分（奖励拾获人/认领人
     * 的诚信行为），并把物品状态流转为"认领中"，等待实物交接。
     *
     * @param id            申请 ID
     * @param currentUserId 当前用户 ID（必须是物品发布人）
     */
    void approve(Long id, Long currentUserId);

    /**
     * 发布人驳回一条认领申请。
     *
     * @param id            申请 ID
     * @param request       驳回申请（含驳回理由，理由会通知认领人）
     * @param currentUserId 当前用户 ID（必须是物品发布人）
     */
    void reject(Long id, ClaimRejectRequest request, Long currentUserId);

    /**
     * 确认领取（交接环节）：认领人线下拿到物品后，上传领取照片与
     * 签名图片作为交接凭证，申请进入"已领取"成功终态。
     *
     * @param id              申请 ID（必须是已通过状态）
     * @param pickupPhoto     领取现场照片 URL
     * @param pickupSignature 签名图片 URL（电子签名，留证）
     * @param currentUserId   认领人 ID（必须为申请人本人）
     * @return 更新后的申请 VO
     */
    ClaimApplyVO pickup(Long id, String pickupPhoto, String pickupSignature, Long currentUserId);

    /**
     * 撤销领取确认：领取后发现物品不对/认领人反悔，可撤销，申请回到
     * 审核环节或物品重新公开；相关信用分会按规则冲正回滚。
     *
     * @param id            申请 ID
     * @param currentUserId 当前用户 ID（申请人本人或管理员）
     */
    void cancelPickup(Long id, Long currentUserId);

    /**
     * 分页查询"我提交的认领申请"，供认领人跟踪进度。
     *
     * @param currentUserId 当前用户 ID
     * @param page          页码
     * @param size          每页条数
     * @return 申请分页列表
     */
    IPage<ClaimApplyVO> listMy(Long currentUserId, int page, int size);

    /**
     * 分页查询"需要我审核"的申请（面向发布人/管理员的工作台）。
     *
     * @param currentUserId 当前用户 ID
     * @param status        状态筛选（待审核/已通过/已驳回），null 表示不过滤
     * @param page          页码
     * @param size          每页条数
     * @return 申请分页列表
     */
    IPage<ClaimApplyVO> listForReview(Long currentUserId, Integer status, int page, int size);
}
