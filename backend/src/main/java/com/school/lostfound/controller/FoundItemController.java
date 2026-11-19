package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.service.FoundItemService;
import com.school.lostfound.vo.FoundItemVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 招领信息（FoundItem，拾获物品）Controller —— 招领模块的核心入口。
 * <p>
 * 为什么需要它：拾获物品是系统的核心业务对象。拾得者发布招领后，
 * 物品经历"草稿 → 投放点投放 → 确认投放公开 → 被认领/归档"的生命周期，
 * 本 Controller 暴露该生命周期的各个操作入口。
 * 注意：发布接口没有加 @PreAuthorize，权限由 SecurityConfig 中
 * "除白名单外均需登录"的统一规则保证（即任何已登录用户都可发布招领）。
 * <p>
 * 所有接口统一返回 Result&lt;T&gt; 包装结构，路径前缀为 /api/found-items。
 */
@RestController
@RequestMapping("/api/found-items")
@RequiredArgsConstructor
public class FoundItemController {

    private final FoundItemService foundItemService;

    /**
     * 发布招领信息。
     * 路由：POST /api/found-items
     * 做什么：拾得者提交拾获物品信息（描述、图片、投放点等），创建一条招领记录。
     *         从 JWT 解析出当前用户 id，作为 founderId 记录发布者。
     * 谁调用：已登录用户（拾得者）。
     */
    @PostMapping
    public Result<FoundItemVO> publish(@Valid @RequestBody FoundItemRequest request,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(foundItemService.publish(request, userId));
    }

    /**
     * 分页查询公开招领列表。
     * 路由：GET /api/found-items
     * 做什么：按条件（关键词、分类、状态、分页参数等）查询处于公开状态的招领信息，
     *         返回 MyBatis-Plus 的 IPage 分页结果。
     * 谁调用：前端招领大厅/首页列表，任何已登录用户可浏览。
     */
    @GetMapping
    public Result<IPage<FoundItemVO>> list(ItemQueryRequest request) {
        return Result.success(foundItemService.listPublic(request));
    }

    /**
     * 分页查询"我发布的"招领。
     * 路由：GET /api/found-items/my
     * 做什么：按当前登录用户（从 JWT 解析）过滤，返回其本人发布的所有招领记录。
     * 谁调用：前端"我的招领"页面。
     */
    @GetMapping("/my")
    public Result<IPage<FoundItemVO>> listMy(ItemQueryRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(foundItemService.listMy(userId, request));
    }

    /**
     * 查看招领详情。
     * 路由：GET /api/found-items/{id}
     * 做什么：按 id 返回单条招领的完整信息（Service 层控制未公开内容的可见性）。
     * 谁调用：前端招领详情页。
     */
    @GetMapping("/{id}")
    public Result<FoundItemVO> getById(@PathVariable Long id) {
        return Result.success(foundItemService.getById(id));
    }

    /**
     * 作废自己的招领。
     * 路由：PUT /api/found-items/{id}/invalidate
     * 做什么：发布者将状态不对/误发布的招领作废（状态置为 VOIDED）。
     *         Service 层校验当前用户是否为该招领的发布者。
     * 谁调用：该招领的发布者本人。
     */
    @PutMapping("/{id}/invalidate")
    public Result<Void> invalidate(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        foundItemService.invalidate(id, userId);
        return Result.success("作废成功", null);
    }

    /**
     * 确认已投放，招领正式公开。
     * 路由：PUT /api/found-items/{id}/mark-placed
     * 做什么：拾得者确认物品已实际投放到指定投放点，招领状态由"待投放"变为"公开中"，
     *         公开后失主才能浏览并发起认领申请。
     * 谁调用：该招领的发布者本人。
     */
    @PutMapping("/{id}/mark-placed")
    public Result<Void> markPlaced(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        foundItemService.markPlaced(id, userId);
        return Result.success("已确认投放，招领公开", null);
    }

    /**
     * 转发招领到寻物启事。
     * 路由：PUT /api/found-items/{id}/forward
     * 做什么：将招领与某条寻物启事（LostNotice）关联，表示"该物品正是这条启事在找的"，
     *         请求体 body 中的 noticeId 指定目标启事。
     * 谁调用：该招领的发布者本人。
     */
    @PutMapping("/{id}/forward")
    public Result<Void> forwardToNotice(@PathVariable Long id,
                                        @RequestBody java.util.Map<String, Long> body,
                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        foundItemService.forwardToNotice(id, body.get("noticeId"), userId);
        return Result.success("已转发到寻物启事", null);
    }

    /**
     * 归档自己的招领。
     * 路由：PUT /api/found-items/{id}/archive
     * 做什么：物品已被领走或无需继续招领时，发布者将招领归档，不再展示在公开列表。
     * 谁调用：该招领的发布者本人。
     */
    @PutMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        foundItemService.archive(id, userId);
        return Result.success("归档成功", null);
    }
}
