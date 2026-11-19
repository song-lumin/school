package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.LostNoticeRequest;
import com.school.lostfound.dto.ForwardClaimRequest;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.LostNoticeVO;
import com.school.lostfound.vo.MatchResultVO;

import java.util.List;

/**
 * 失物启事业务接口（Service 层）。
 *
 * <p>作用：定义"失物启事（LostNotice）"模块的全部业务能力。当用户丢失物品后，
 * 会发布一条失物启事描述物品特征；若有人捡到并上交，可通过"转发为认领申请"
 * 流程将启事与拾获物品关联起来。本接口是 Controller 层与具体实现（Impl）之间的契约，
 * 使 Controller 不依赖业务细节，便于测试与替换实现。</p>
 *
 * <p>为什么需要它：失物启事涉及发布校验、状态流转、权限校验（仅发布人可关闭）、
 * 智能匹配等多个业务规则，不应混杂在 Controller 中，因此独立出 Service 层统一封装。</p>
 */
public interface LostNoticeService {

    /**
     * 发布一条失物启事。
     *
     * @param request        启事内容（物品名称、丢失地点、分类、描述、照片等）
     * @param currentUserId  当前登录用户 ID，作为启事的发布人
     * @return 创建成功的启事详情（含生成的 ID 与初始状态）
     */
    LostNoticeVO publish(LostNoticeRequest request, Long currentUserId);

    /**
     * 分页查询失物启事列表，支持按关键词（标题/描述模糊匹配）、分类、状态过滤。
     *
     * @param keyword   搜索关键词，可为空
     * @param category  物品分类，可为空（查全部）
     * @param status    启事状态过滤，可为空
     * @param page      页码（从 1 开始）
     * @param size      每页条数
     * @return 分页结果，封装了当前页启事列表与总数
     */
    IPage<LostNoticeVO> list(String keyword, String category, Integer status, int page, int size);

    /**
     * 按 ID 查询单条启事详情；不存在时抛出业务异常。
     */
    LostNoticeVO getById(Long id);

    /**
     * 关闭启事（例如用户已找回物品）。
     * 业务规则：仅启事的发布人本人可以关闭，否则抛出权限异常。
     */
    LostNoticeVO close(Long id, Long currentUserId);

    /**
     * 管理员删除启事（硬删或逻辑删除由实现决定）。
     * 业务规则：仅管理员角色可调用，普通用户无权删除他人启事。
     */
    void deleteByAdmin(Long id);

    /**
     * 将失物启事"转发"为认领申请：当启事下的匹配物品被确认属于失主时，
     * 以启事发起人的身份对该物品发起认领（ForwardClaimRequest 指定目标物品及凭证）。
     *
     * @param id            失物启事 ID
     * @param request       转发认领请求（目标拾获物品 ID、证明材料等）
     * @param currentUserId 当前用户（须为启事发布人）
     * @return 生成的认领申请详情
     */
    ClaimApplyVO forward(Long id, ForwardClaimRequest request, Long currentUserId);

    /**
     * 智能匹配 V1：根据启事的关键信息（分类、地点、时间窗口）检索
     * 可能对应的拾获物品（FoundItem）列表，按简单相似度排序返回。
     */
    List<FoundItem> smartMatch(Long id, Long currentUserId);

    /**
     * 智能匹配 V2：在 V1 基础上增加打分维度与原因说明，
     * 返回 MatchResultVO（含匹配分数、命中字段），便于用户判断匹配可信度。
     */
    List<MatchResultVO> smartMatchV2(Long id, Long currentUserId);

    /**
     * 查询某条启事已经转发认领过的拾获物品列表，
     * 用于前端展示"该启事已匹配到哪些物品"。
     */
    List<FoundItem> getForwardedItems(Long noticeId);
}
