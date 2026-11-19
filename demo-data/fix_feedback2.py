import io
p = r'D:\codex\codex-data\schoollose\frontend\src\views\FeedbackView.vue'
s = io.open(p, encoding='utf-8').read()
old = '<b>纠纷申诉：</b>在「我的纠纷申诉」tab 发起，适用于领取后物品不符、被冒领等'
new = '<b>纠纷申诉：</b>在「我的纠纷申诉」tab 发起，适用于领取后物品不符、损坏、被冒领等'
if old in s:
    s = s.replace(old, new, 1)
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
    print('OK')
else:
    print('MISS')
print('ITEM_NOT_FOUND left:', s.count('ITEM_NOT_FOUND'))
