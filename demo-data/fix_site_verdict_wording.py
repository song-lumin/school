# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\frontend\src\views\PointAdminView.vue'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag):
    global s
    c = s.count(old)
    print(tag, 'match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + tag)
    s = s.replace(old, new, 1)

rep('''            title="这里只回填现场事实：实物是否在架、有无损坏、交付凭证是否齐全、监控是否可调取。不要写责任结论——定责与积分由系统管理员处理。"''',
'''            title="先回填现场事实（实物是否在架、有无损坏、凭证是否齐全），再选结论：找不到物品可定「实物在站 / 从未投放 / 被他人拿走」，损坏可定「站点责 / 认领人责 / 发布人责」——选中后系统当场按规则表处理物品状态与积分；定不了就选「其他」转系统管理员。"''', 'tab-alert')

rep('''            title="申请由系统管理员审批留痕。批准后你按审批结论到现场调取画面，把看到的事实通过「现场核验」回填，定责仍由系统管理员做。"''',
'''            title="申请由系统管理员审批留痕。批准后你按审批结论到现场调取画面，把看到的事实通过「现场核验」回填；能据此下结论的当场选结论结案，仍定不了的选「其他」转系统管理员。"''', 'camera-alert')

rep('''// ===== 纠纷核验（只回填现场事实，定责由系统管理员） =====''',
'''// ===== 纠纷核验（回填事实 + 可选结论：前三项当场结案，OTHER 转系统管理员） =====''', 'comment')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
