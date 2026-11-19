package com.school.lostfound.controller;

import com.school.lostfound.dto.CheckRequest;
import com.school.lostfound.dto.DropPointRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.service.DropPointService;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.InventoryItemVO;
import com.school.lostfound.vo.DropPointVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 投放点管理 Controller —— 负责校园投放点（失物收集点）信息的维护与点位日常运营。
 * <p>
 * 为什么需要它：系统采用"投放点"模式管理失物——拾得物品被投放到指定点位，
 * 由点位管理员（POINT_ADMIN）负责保管与核对。本 Controller 同时承担两类职责：
 * 1. 点位信息维护（增删改查），仅系统管理员（SYS_ADMIN）可操作；
 * 2. 点位运营操作（查看待巡检物品、待领取申请、库存，以及巡检打分发积分），
 *    由点位管理员或系统管理员操作。
 * <p>
 * 所有接口统一返回 Result&lt;T&gt; 包装结构，路径前缀为 /api/drop-points。
 */
@RestController
@RequestMapping("/api/drop-points")
@RequiredArgsConstructor
public class DropPointController {

    private final DropPointService dropPointService;

    /**
     * 查询全部投放点列表。
     * 路由：GET /api/drop-points
     * 做什么：返回系统中所有投放点的基本信息（名称、位置、管理员等）。
     * 谁调用：前端地图/列表页，任何已登录用户均可查看。
     */
    @GetMapping
    public Result<List<DropPointVO>> list() {
        return Result.success(dropPointService.listAll());
    }

    /**
     * 查询单个投放点详情。
     * 路由：GET /api/drop-points/{id}
     * 做什么：按 id 返回指定投放点的详细信息。
     * 谁调用：前端投放点详情页。
     */
    @GetMapping("/{id}")
    public Result<DropPointVO> getById(@PathVariable Long id) {
        return Result.success(dropPointService.getById(id));
    }

    /**
     * 新增投放点。
     * 路由：POST /api/drop-points
     * 做什么：根据请求参数创建一个新投放点。参数经 @Valid 校验（@Valid 校验不通过会返回 400）。
     * 谁调用：仅系统管理员 SYS_ADMIN（@PreAuthorize 控制权限）。
     */
    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<DropPointVO> create(@Valid @RequestBody DropPointRequest request) {
        return Result.success(dropPointService.create(request));
    }

    /**
     * 修改投放点信息。
     * 路由：PUT /api/drop-points/{id}
     * 做什么：更新指定投放点的名称、位置、绑定管理员等信息。
     * 谁调用：仅系统管理员 SYS_ADMIN。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<DropPointVO> update(@PathVariable Long id,
                                      @Valid @RequestBody DropPointRequest request) {
        return Result.success(dropPointService.update(id, request));
    }

    /**
     * 删除投放点。
     * 路由：DELETE /api/drop-points/{id}
     * 做什么：删除指定投放点（Service 层会校验点位下是否还有未处理物品）。
     * 谁调用：仅系统管理员 SYS_ADMIN。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        dropPointService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 查询点位下待巡检的物品。
     * 路由：GET /api/drop-points/{id}/items
     * 做什么：返回该投放点下处于待巡检状态的招领物品列表。
     * 谁调用：点位管理员 POINT_ADMIN（巡检自己的点位）或系统管理员 SYS_ADMIN。
     */
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<List<FoundItem>> getPendingItems(@PathVariable Long id) {
        return Result.success(dropPointService.getPendingItems(id));
    }

    /**
     * 查询点位下待领取的认领申请。
     * 路由：GET /api/drop-points/{id}/pending-pickups
     * 做什么：返回该投放点下状态为待现场核验/待领取的认领申请列表。
     * 谁调用：点位管理员 POINT_ADMIN 或系统管理员 SYS_ADMIN（现场核对失主身份）。
     */
    @GetMapping("/{id}/pending-pickups")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<List<ClaimApplyVO>> getPendingPickups(@PathVariable Long id) {
        return Result.success(dropPointService.getPendingPickups(id));
    }

    /**
     * 巡检打分。
     * 路由：PUT /api/drop-points/{id}/check
     * 做什么：点位管理员对本次巡检结果打分（CheckRequest 携带分数等信息），
     *         Service 层根据分数给点位管理员发放信用积分（积分激励机制）。
     * 谁调用：点位管理员 POINT_ADMIN 或系统管理员 SYS_ADMIN。
     */
    @PutMapping("/{id}/check")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<Void> checkItem(@PathVariable Long id,
                                  @Valid @RequestBody CheckRequest request,
                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        dropPointService.checkItem(id, request, userId);
        return Result.success("巡检完成，已发放积分", null);
    }

    /**
     * 查询点位库存。
     * 路由：GET /api/drop-points/{id}/inventory
     * 做什么：返回该投放点当前保管中的物品清单（InventoryItemVO 含物品与状态摘要）。
     * 谁调用：点位管理员 POINT_ADMIN 或系统管理员 SYS_ADMIN。
     */
    @GetMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<List<InventoryItemVO>> getInventory(@PathVariable Long id) {
        return Result.success(dropPointService.getInventory(id));
    }
}