# -*- coding: utf-8 -*-
import io, os

BASE = r'D:\codex\codex-data\schoollose'

def edit(rel, pairs):
    p = os.path.join(BASE, rel)
    s = io.open(p, encoding='utf-8').read()
    for old, new, tag in pairs:
        c = s.count(old)
        print(os.path.basename(rel), tag, 'match=', c)
        if c != 1:
            raise SystemExit('ABORT ' + tag)
        s = s.replace(old, new, 1)
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
    print('  saved', rel)

# ---------- 1. DTO：核验可选结论 ----------
edit(r'backend\src\main\java\com\school\lostfound\dto\PointCheckRequest.java', [(
'''    @NotBlank(message = "请填写现场核验情况")
    @Size(max = 1000, message = "现场核验最多 1000 字")
    private String note;''',
'''    @NotBlank(message = "请填写现场核验情况")
    @Size(max = 1000, message = "现场核验最多 1000 字")
    private String note;

    /**
     * 现场结论（可选，选了就等于站点当场结案，系统按规则表自动处理状态与积分）：
     * 找不到物品：FOUND 实物在站 / NOT_DELIVERED 从未投放 / STOLEN 被他人拿走；
     * 物品损坏：SITE_FAULT 站点责 / CLAIMER_FAULT 认领人责 / PUBLISHER_FAULT 发布人责；
     * OTHER：现场无法判定，只回填事实并转系统管理员定责。
     */
    @Size(max = 30, message = "结论取值过长")
    private String outcome;''', 'dto')])

# ---------- 2. Service：pointCheck 支持结论 ----------
edit(r'backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java', [(
'''        dispute.setPointCheckNote(request.getNote().trim());
        dispute.setPointCheckerId(operatorId);
        dispute.setPointCheckedAt(LocalDateTime.now());
        disputeMapper.updateById(dispute);''',
'''        dispute.setPointCheckNote(request.getNote().trim());
        dispute.setPointCheckerId(operatorId);
        dispute.setPointCheckedAt(LocalDateTime.now());
        disputeMapper.updateById(dispute);

        // 现场核对完可以直接出结论：本点位负责人（或代看的管理员）按规则表当场结案，
        // 状态与积分由系统自动处理，不必再等系统管理员二次定责。
        String verdict = request.getOutcome() == null ? null : request.getOutcome().trim().toUpperCase();
        boolean hasVerdict = verdict != null && !verdict.isBlank() && !"OTHER".equals(verdict);
        if (hasVerdict) {
            boolean siteCanRule = TYPE_ITEM_NOT_FOUND.equals(dispute.getDisputeType())
                    || TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType());
            if (!siteCanRule) {
                throw new BusinessException(400, "该类型工单由系统管理员定责，站点只回填事实（可留空结论）");
            }
            if (TYPE_ITEM_NOT_FOUND.equals(dispute.getDisputeType())
                    && !(RESULT_FOUND.equals(verdict) || RESULT_NOT_DELIVERED.equals(verdict)
                    || RESULT_STOLEN.equals(verdict))) {
                throw new BusinessException(400, "找不到物品的结论只能是：实物在站 / 从未投放 / 被他人拿走 / 其他");
            }
            if (TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType())
                    && !(FAULT_SITE.equals(verdict) || FAULT_CLAIMER.equals(verdict)
                    || FAULT_PUBLISHER.equals(verdict))) {
                throw new BusinessException(400, "损坏定责只能是：站点 / 认领人 / 发布人 / 其他");
            }
            DisputeHandleRequest handleRequest = new DisputeHandleRequest();
            handleRequest.setApproved(Boolean.TRUE);
            handleRequest.setOutcome(verdict);
            handleRequest.setHandlerNote(StringUtils.hasText(dispute.getPointCheckNote())
                    ? dispute.getPointCheckNote() : "站点现场核验后按结论结案");
            // 复用管理员定责同一套逻辑：物品状态、积分回滚/发放、申请人通知一次到位
            handle(id, handleRequest, operatorId);
            log.info("站点现场结论结案：disputeId={}, verdict={}, operator={}", id, verdict, operatorId);
            return;
        }''', 'svc-verdict'), (
'''        log.info("现场核验回填：disputeId={}, pointId={}, checker={}, standIn={}",
                dispute.getId(), point.getId(), operatorId, !isPointOwner);''',
'''        // 选了「其他」：现场定不了，明确转给系统管理员，别让工单在队列里干等
        if ("OTHER".equals(verdict)) {
            for (User admin : userMapper.selectList(null)) {
                if (admin.getRole() == com.school.lostfound.enums.UserRole.SYS_ADMIN) {
                    messageService.push(admin.getId(), "站点无法定论，请接手定责（工单#" + dispute.getId() + "）",
                            "「" + point.getName() + "」已完成现场核验但结论选了「其他」：" + dispute.getPointCheckNote()
                                    + "。请到「纠纷管理」按规则表定责。",
                            "DISPUTE", dispute.getItemId(), dispute.getApplyId());
                }
            }
        }

        log.info("现场核验回填：disputeId={}, pointId={}, checker={}, standIn={}",
                dispute.getId(), point.getId(), operatorId, !isPointOwner);''', 'svc-other')])

# ---------- 3. 前端 api ----------
edit(r'frontend\src\api\index.ts', [(
'''  /** 站点管理员：回填现场核验事实 */
  pointCheck(id: number | string, note: string) {
    return request.put<any, ApiResponse<void>>(`/disputes/${id}/point-check`, { note })
  },''',
'''  /** 站点管理员：回填现场核验事实；传 outcome 表示当场给出结论，由系统直接结算积分 */
  pointCheck(id: number | string, note: string, outcome?: string) {
    return request.put<any, ApiResponse<void>>(`/disputes/${id}/point-check`, outcome ? { note, outcome } : { note })
  },''', 'api')])

print('backend + api done')
