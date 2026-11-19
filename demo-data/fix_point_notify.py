import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

old = '''        String content = String.format(
                "【%s】%s 就认领物品《%s》提交了工单#%d，说明：%s（认领人电话 %s）。"
                        + "请你到站点现场核对实物现状（是否在架、有无损坏、交付凭证是否齐全），必要时走「监控调取」流程取交付画面，"
                        + "然后在「纠纷核验」中回填现场事实。定责与积分由系统管理员依据你的陈述处理，站点不写结论。",
                label, claimerName, item.getTitle(), dispute.getId(),
                dispute.getDescription() == null ? "未填写" : dispute.getDescription(), claimerPhone);'''

new = '''        // 「不符」和「损坏」是两件事，站点要核的东西不同：不符查「是不是这件」，损坏查「什么时候坏的」。
        // 混成一句会让站点只拍照不回填，定责就缺少依据。
        String checkAction;
        if (TYPE_ITEM_MISMATCH.equals(dispute.getDisputeType())) {
            checkAction = "请按招领填写的品名、颜色、特征逐项比对货架实物，判断是认领人拿错还是招领描述写错，并把实物现状拍照留档；"
                    + "定责后实物会转「待归还」，请勿直接放回公开货架；";
        } else if (TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType())) {
            checkAction = "请到存放区确认损坏部位与程度并拍照，同时核对交接入库时是否已有损坏记录，用于区分保管责任与认领人责任；"
                    + "损坏件定责后作废、不再重新公开，请勿放回公开货架；";
        } else {
            checkAction = "请核对实物现状与交付凭证，必要时走「监控调取」流程取交付画面；";
        }

        String content = String.format(
                "【%s】%s 就认领物品《%s》提交了工单#%d，说明：%s（认领人电话 %s）。"
                        + "%s"
                        + "然后在「纠纷核验」中回填现场事实。定责与积分由系统管理员依据你的陈述处理，站点不写结论。",
                label, claimerName, item.getTitle(), dispute.getId(),
                dispute.getDescription() == null ? "未填写" : dispute.getDescription(), claimerPhone, checkAction);'''

if old in s:
    s = s.replace(old, new, 1)
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
    print('OK replaced')
else:
    print('MISS')
print('contains checkAction:', 'checkAction' in s)
