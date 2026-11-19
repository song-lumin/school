package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeCreditApplyRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.dto.PointCheckRequest;
import com.school.lostfound.service.DisputeService;
import com.school.lostfound.vo.DisputeVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    /** 申诉提交：ITEM_MISMATCH/ITEM_DAMAGED/OTHER 认领人领取后7天内；FALSE_CLAIM 失主发起无时间窗；
     *  ITEM_NOT_FOUND 待取件状态下由认领人申报，交站点核查 */
    @PostMapping
    public Result<DisputeVO> createDispute(@Valid @RequestBody DisputeCreateRequest request,
                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(disputeService.createDispute(request, userId));
    }

    @GetMapping("/my")
    public Result<IPage<DisputeVO>> listMy(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(disputeService.listMy(userId, page, size));
    }

    @GetMapping("/{id}")
    public Result<DisputeVO> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(disputeService.getById(id, userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<IPage<DisputeVO>> list(@RequestParam(required = false) String disputeType,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return Result.success(disputeService.list(disputeType, status, page, size));
    }

    /** 站点管理员视角：本点位相关工单（只读事实，不含定责） */
    @GetMapping("/point")
    @PreAuthorize("hasAnyRole('POINT_ADMIN','SYS_ADMIN')")
    public Result<IPage<DisputeVO>> listForPoint(@RequestParam(required = false) String disputeType,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        return Result.success(disputeService.listForPoint(operatorId, disputeType, status, page, size));
    }

    /** 站点回填现场核验事实：只写事实，不改结论、不动积分 */
    @PutMapping("/{id}/point-check")
    @PreAuthorize("hasAnyRole('POINT_ADMIN','SYS_ADMIN')")
    public Result<Void> pointCheck(@PathVariable Long id,
                                   @Valid @RequestBody PointCheckRequest request,
                                   Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        disputeService.pointCheck(id, request, operatorId);
        return Result.success("现场核验已回填", null);
    }

    /** 手动触发站点回填 SLA 催办（定时任务每小时也会跑一次，这里便于演示与补催） */
    @PostMapping("/escalate")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Integer> escalate() {
        return Result.success("催办任务已生成", disputeService.escalateUnresponsiveSites());
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> handle(@PathVariable Long id,
                               @Valid @RequestBody DisputeHandleRequest request,
                               Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        disputeService.handle(id, request, operatorId);
        return Result.success("处理完成", null);
    }

    /**
     * 积分处置：站点已给结论的工单（状态 4）由系统管理员执行积分。
     * action = EXECUTE 按站点结论执行 / ADJUST 按勾选项执行 / REJECT 打回站点重判。
     */
    @PutMapping("/{id}/credit-apply")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> applyCredit(@PathVariable Long id,
                                    @Valid @RequestBody DisputeCreditApplyRequest request,
                                    Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        disputeService.applyCredit(id, request, operatorId);
        return Result.success("积分处置完成", null);
    }
}
