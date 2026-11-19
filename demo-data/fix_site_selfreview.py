# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag):
    global s
    c = s.count(old)
    print(tag, 'match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + tag)
    s = s.replace(old, new, 1)

rep('''    @Override
    @Transactional
    public void handle(Long id, DisputeHandleRequest request, Long operatorId) {
        Dispute dispute = getDisputeOrThrow(id);
        if (!dispute.getStatus().equals(STATUS_PENDING)) {
            throw new BusinessException(409, "该工单已处理，请勿重复操作");
        }
        assertNoSelfReview(dispute, operatorId);''',
'''    @Override
    @Transactional
    public void handle(Long id, DisputeHandleRequest request, Long operatorId) {
        handleInternal(id, request, operatorId, false);
    }

    /**
     * 定责执行体，管理员定责与站点现场结案共用同一套状态/积分处理。
     * fromSiteCheck=true 表示站点刚做完现场核验直接出结论：这本来就是点位负责人的职责，
     * 此时不做「点位负责人回避」，否则任何工单都无法在本点位结案。
     */
    private void handleInternal(Long id, DisputeHandleRequest request, Long operatorId, boolean fromSiteCheck) {
        Dispute dispute = getDisputeOrThrow(id);
        if (!dispute.getStatus().equals(STATUS_PENDING)) {
            throw new BusinessException(409, "该工单已处理，请勿重复操作");
        }
        if (!fromSiteCheck) {
            assertNoSelfReview(dispute, operatorId);
        }''', 'handle-split')

rep('''            // 复用管理员定责同一套逻辑：物品状态、积分回滚/发放、申请人通知一次到位
            handle(id, handleRequest, operatorId);''',
'''            // 复用管理员定责同一套逻辑：物品状态、积分回滚/发放、申请人通知一次到位
            handleInternal(id, handleRequest, operatorId, true);''', 'call-internal')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
