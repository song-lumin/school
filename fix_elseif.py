f = r'D:\codex\codex-data\schoollose\frontend\src\views\ItemDetailView.vue'
c = open(f, encoding='utf-8').read()
c = c.replace(
    'v-else-if="item.itemStatus === 1 && !userStore.isLoggedIn"',
    'v-if="item.itemStatus === 1 && !userStore.isLoggedIn"'
)
open(f, 'w', encoding='utf-8').write(c)
print('fixed')
