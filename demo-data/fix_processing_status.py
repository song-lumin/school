# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag, expect=1):
    global s
    c = s.count(old)
    print(tag, 'match=', c)
    if c != expect:
        raise SystemExit('ABORT ' + tag)
    s = s.replace(old, new)

rep('''    private static final int STATUS_REJECTED = 2;''',
'''    private static final int STATUS_REJECTED = 2;
    /** 处理中：站点已接手、已回填，但还没出最终结论（例如选了「其他」转管理员，或等监控回放） */
    private static final int STATUS_PROCESSING = 3;''', 'const')

rep('''.eq(Dispute::getStatus, STATUS_PENDING));''',
'''.in(Dispute::getStatus, STATUS_PENDING, STATUS_PROCESSING));''', 'dedupe', expect=2)

rep('''        if (!dispute.getStatus().equals(STATUS_PENDING)) {
            throw new BusinessException(409, "该工单已处理，无需再回填现场核验");
        }''',
'''        if (!dispute.getStatus().equals(STATUS_PENDING) && !dispute.getStatus().equals(STATUS_PROCESSING)) {
            throw new BusinessException(409, "该工单已处理完毕，无需再处理");
        }''', 'pointcheck-guard')

rep('''        if (!dispute.getStatus().equals(STATUS_PENDING)) {
            throw new BusinessException(409, "该工单已处理，请勿重复操作");
        }''',
'''        if (!dispute.getStatus().equals(STATUS_PENDING) && !dispute.getStatus().equals(STATUS_PROCESSING)) {
            throw new BusinessException(409, "该工单已处理，请勿重复操作");
        }''', 'handle-guard')

rep('''        if ("OTHER".equals(verdict)) {
            for (User admin : userMapper.selectList(null)) {''',
'''        if ("OTHER".equals(verdict)) {
            dispute.setStatus(STATUS_PROCESSING);
            disputeMapper.updateById(dispute);
            for (User admin : userMapper.selectList(null)) {''', 'mark-processing')

rep('''                            "「" + point.getName() + "」已完成现场核验但结论选了「其他」：" + dispute.getPointCheckNote()''',
'''                            "「" + point.getName() + "」已接手本工单（状态：处理中），结论选了「其他」：" + dispute.getPointCheckNote()''', 'other-msg')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('backend saved')
