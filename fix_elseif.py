"""
一次性修复脚本：修正 ItemDetailView.vue 中的 v-else-if 指令。

用途：前端 ItemDetailView.vue 中原本写的是 v-else-if，
导致该元素与前面的兄弟元素形成 else 链（前面的兄弟不是 v-if 时
Vue 会报错/逻辑不成立）。此脚本把它改成独立的 v-if，
使其不再依赖前面的兄弟元素，始终按自身条件渲染。

输入：旧仓库路径下的 frontend/src/views/ItemDetailView.vue（注意：指向
     D:\codex\codex-data\schoollose 旧目录，非当前仓库，历史遗留脚本）
输出：原文件被覆盖写回（in-place 替换），无备份
说明：用简单的全文字符串替换实现，未用正则；若目标字符串不存在则文件不变
"""

# 目标 Vue 单文件组件的绝对路径（旧项目目录下的路径）
f = r'D:\codex\codex-data\schoollose\frontend\src\views\ItemDetailView.vue'
# 以 UTF-8 读入整个文件内容
c = open(f, encoding='utf-8').read()
# 将 v-else-if 替换为 v-if，切断与前面兄弟元素的 else 链
c = c.replace(
    'v-else-if="item.itemStatus === 1 && !userStore.isLoggedIn"',
    'v-if="item.itemStatus === 1 && !userStore.isLoggedIn"'
)
# 覆盖写回原文件
open(f, 'w', encoding='utf-8').write(c)
# 打印完成标记
print('fixed')

