# -*- coding: utf-8 -*-
import io

p = r'D:\codex\codex-data\schoollose\.gitignore'
s = io.open(p, encoding='utf-8').read()
add = """
# 本地中间产物与大数据备份（不入库）
.docx_work/
.report_extract.md
demo-data/uploads-old/
demo-data/images/
demo-data/*.jpg
demo-data/*.jpeg
demo-data/*.png
demo-data/*.json
demo-data/backup-*.sql
scripts/*.log
"""
if 'demo-data/backup-*.sql' not in s:
    s = s.rstrip('\n') + '\n' + add
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
    print('gitignore updated')
else:
    print('gitignore already has entries')
print(s[-260:])
