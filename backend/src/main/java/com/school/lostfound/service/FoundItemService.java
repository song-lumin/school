package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.vo.FoundItemVO;


/**
 * 招领物品服务接口 —— "拾获人发布物品"这条主线的业务定义。
 * <p>
 * 业务背景：FoundItem（招领物品）是系统的核心实体之一：拾获人捡到
 * 物品后在此登记（照片、标题、描述、验证问题、是否易腐等），
 * 之后经历 状态机流转：
 * 待投放(6) → 已投放 → 公开(1) → 认领中(2) → 已领取(3)；
 * 任何阶段都可能被 作废(0) 或转 超期(5，由定时任务自动流转)。
 * <p>
 * 发布环节包含防冒领设计：拾获人可设置验证问题，失主认领时须回答；
 * 图片会上传并建立感知哈希指纹（供以图搜图与盗图检测）。
 * 本接口同时承担前台浏览（列表/详情/筛选）与用户/管理员的
 * 状态操作（作废、归档、投放确认、管理员删除）。
 */
public interface FoundItemService {
    /**
     * 发布一件招领物品。
     * 处理内容：DTO 校验 → 保存图片（上传+指纹索引）→ 写库 →
     * 初始状态为"待投放"，等待拾获人线下投放并确认。
     *
     * @param request       物品信息（标题、描述、分类、图片、验证问题等）
     * @param currentUserId 发布人（拾获人）ID
     * @return 发布成功的物品 VO
     */
    FoundItemVO publish(FoundItemRequest request, Long currentUserId);

    /**
     * 分页查询公开可认领的物品（前台招领大厅），支持关键词、分类、
     * 时间等条件筛选与排序。
     *
     * @param request 查询条件（关键词、分类、页码等）
     * @return 物品分页列表（VO 含首图、发布人昵称等展示字段）
     */
    IPage<FoundItemVO> listPublic(ItemQueryRequest request);

    /**
     * 查询物品详情（含图片列表、发布人信息、验证问题提示等）。
     *
     * @param id 物品 ID
     * @return 物品详情 VO
     */
    FoundItemVO getById(Long id);

    /**
     * 发布人作废自己的物品（如物品已私下还给失主、登记信息有误等）。
     * 作废后物品不再公开展示。
     *
     * @param id            物品 ID
     * @param currentUserId 当前用户 ID（必须是发布人本人）
     */
    void invalidate(Long id, Long currentUserId);

    /**
     * 拾获人确认"已投放"：把实物投放到指定投放点后点击确认，
     * 物品从"待投放"进入后续公开流程；超 7 天未确认会被
     * AutoInvalidateTask 自动作废。
     *
     * @param id            物品 ID
     * @param currentUserId 当前用户 ID（必须是发布人）
     */
    void markPlaced(Long id, Long currentUserId);

    /**
     * 转发为寻物启事：拾获人也可把招领物品关联到某条 LostNotice，
     * 反向匹配失主，打通"招领 ↔ 寻物"两条业务线。
     *
     * @param id            物品 ID
     * @param noticeId      目标寻物启事 ID
     * @param currentUserId 当前用户 ID
     */
    void forwardToNotice(Long id, Long noticeId, Long currentUserId);

    /**
     * 归档物品：流程已闭环（已领取/已超期处理完毕）后的收尾操作，
     * 归档物品从常规列表中隐藏但数据保留，满足存档审计要求。
     *
     * @param id            物品 ID
     * @param currentUserId 当前用户 ID
     */
    void archive(Long id, Long currentUserId);

    /**
     * 分页查询"我发布的"招领物品，供个人中心管理。
     *
     * @param currentUserId 当前用户 ID
     * @param request       查询条件
     * @return 物品分页列表
     */
    IPage<FoundItemVO> listMy(Long currentUserId, ItemQueryRequest request);

    /**
     * 管理员删除违规物品（硬删或逻辑删除，由实现决定），
     * 与发布人自行作废不同，此操作不受"必须是本人"限制。
     *
     * @param id 物品 ID
     */
    void deleteByAdmin(Long id);

}
