import io

# 1) LayoutView: 顶部显示版本戳
p1 = r'D:\codex\codex-data\schoollose\frontend\src\views\LayoutView.vue'
s = io.open(p1, encoding='utf-8').read()

old1 = '''        <div class="header-right">
          <template v-if="userStore.isLoggedIn">'''
new1 = '''        <div class="header-right">
          <span class="build-tag" title="看到这个标记说明加载的是最新构建">新版 v2</span>
          <template v-if="userStore.isLoggedIn">'''

if old1 in s:
    s = s.replace(old1, new1, 1)
    print('OK header tag')
else:
    print('MISS header tag')

old2 = '''.layout-footer p {'''
new2 = '''.build-tag {
  font-size: 12px;
  line-height: 1;
  color: #5f6b7d;
  background: #eef2f7;
  border: 1px solid #dde4ee;
  border-radius: 10px;
  padding: 5px 9px;
  margin-right: 12px;
  white-space: nowrap;
}

.layout-footer p {'''

if old2 in s:
    s = s.replace(old2, new2, 1)
    print('OK style')
else:
    print('MISS style')

io.open(p1, 'w', encoding='utf-8', newline='').write(s)

# 2) nginx: 构建产物不再长缓存
p2 = r'D:\codex\codex-data\schoollose\frontend\nginx.conf'
n = io.open(p2, encoding='utf-8').read()
old3 = '''        # 带 hash 的构建产物才允许长缓存
        location /assets/ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }'''
new3 = '''        # 演示期：构建产物也不缓存，改完代码刷新即生效
        location /assets/ {
            add_header Cache-Control "no-store, no-cache, must-revalidate";
            expires -1;
        }'''
if old3 in n:
    n = n.replace(old3, new3, 1)
    io.open(p2, 'w', encoding='utf-8', newline='').write(n)
    print('OK nginx')
else:
    print('MISS nginx')
