"""Export the revised three-stage report without duplicating its iteration chapter."""
from pathlib import Path
from docx import Document

SOURCE = Path(r"D:\gdut\school\docs\系统分析与设计报告第二次提交.docx")
OUTPUT = Path(r"D:\gdut\school\docs\系统分析与设计实验报告_最终版_含Sprint.docx")
REQUIRED_HEADINGS = (
    "7.1 基础原型 V1.0",
    "7.2 迭代一 V2.0",
    "7.3 迭代二 V3.0",
)

doc = Document(SOURCE)
headings = [p.text.strip() for p in doc.paragraphs]
missing = [heading for heading in REQUIRED_HEADINGS if not any(text.startswith(heading) for text in headings)]
if missing:
    raise ValueError(f"正式报告缺少迭代章节：{', '.join(missing)}")

doc.save(OUTPUT)
print(f"已导出三阶段迭代报告：{OUTPUT}")
