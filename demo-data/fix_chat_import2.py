# -*- coding: utf-8 -*-
import io

targets = [
    (r'D:\codex\codex-data\schoollose\frontend\src\views\PointAdminView.vue',
     "import { useUserStore } from '@/stores/user'"),
    (r'D:\codex\codex-data\schoollose\frontend\src\views\AdminView.vue',
     "import { DISPUTE_TYPE_MAP, DISPUTE_STATUS_MAP, REPORT_TYPE_MAP, REPORT_STATUS_MAP } from '@/types'"),
]

for path, anchor in targets:
    s = io.open(path, encoding='utf-8').read()
    if "components/ChatDialog.vue" in s:
        print(path, 'already imported')
        continue
    c = s.count(anchor)
    print(path, 'anchor match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + path)
    s = s.replace(anchor, anchor + "\nimport ChatDialog from '@/components/ChatDialog.vue'", 1)
    io.open(path, 'w', encoding='utf-8', newline='').write(s)
    print(path, 'imported')
