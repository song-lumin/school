"""
完整版实验报告生成脚本（第3-6章）——检查遗漏接口的提示脚本。

用途：本文件原计划用于生成《系统分析与设计实验报告》第3-6章
（包含全部 14 个 Controller 的完整接口清单），但实际只保留了
通用的 docx 排版工具函数，正文生成逻辑未完成。运行时仅打印
检查提示：列出当时新补的 4 个接口模块（举报管理、内容审核、
归档管理、风控预警）和最终接口总数，提醒作者"先关闭已打开的
Word 文档，再重新运行完整脚本"。

输入：无（不读写文件，只打印提示）
输出：控制台提示文字
为什么要保留：作为报告接口模块清单的备忘/检查清单（checklist）。
"""

from docx import Document
from docx.shared import Pt, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn

def add_heading_custom(doc, text, level=1):
    """
    添加自定义样式的标题段落。

    按标题级别设置黑体、字号（1级16pt / 2级14pt / 3级12pt）并加粗，
    需要通过 qn('w:eastAsia') 设置东亚字体才能对中文生效。
    返回生成的 heading 段落对象。
    """
    heading = doc.add_heading(text, level=level)
    heading.alignment = WD_ALIGN_PARAGRAPH.LEFT
    for run in heading.runs:
        run.font.name = '黑体'
        # 中文属于东亚文字，必须设置 eastAsia 字体才生效
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
        # 一级标题 16pt，二级 14pt，三级 12pt，均加粗
        if level == 1:
            run.font.size = Pt(16)
            run.font.bold = True
        elif level == 2:
            run.font.size = Pt(14)
            run.font.bold = True
        elif level == 3:
            run.font.size = Pt(12)
            run.font.bold = True
    return heading

def add_paragraph_custom(doc, text, font_size=12, bold=False):
    """
    添加自定义样式的正文段落（宋体、可指定字号和加粗）。
    """
    para = doc.add_paragraph(text)
    for run in para.runs:
        run.font.name = '宋体'
        # 设置中文字体（东亚字体属性）
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        run.font.size = Pt(font_size)
        run.font.bold = bold
    return para

def create_table_with_data(doc, data, has_header=True):
    """
    根据二维列表 data 创建表格。

    第一行默认作为表头：居中、加粗、10pt；
    数据行为左对齐、9pt（列数>3 时，避免列太多挤）。
    """
    table = doc.add_table(rows=len(data), cols=len(data[0]))
    table.style = 'Light Grid Accent 1'  # 使用 Word 内置的浅色网格样式
    for i, row_data in enumerate(data):
        row = table.rows[i]
        for j, cell_text in enumerate(row_data):
            cell = row.cells[j]
            cell.text = str(cell_text)
            for para in cell.paragraphs:
                # 表头行居中，其余左对齐
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if (i == 0 and has_header) else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    # 列数多时用小字号防止表格过宽
                    run.font.size = Pt(10 if len(data[0]) <= 3 else 9)
                    run.font.bold = True if (i == 0 and has_header) else False
    return table

# ────────────────────────────────────────────────────────────
# 以下为脚本实际执行部分：仅打印检查提示，不生成文档内容
# ────────────────────────────────────────────────────────────

# 提醒：Word 正占用文件时 python-docx 无法写入/覆盖同名文档
print("请先关闭已打开的Word文档，然后重新运行此脚本")
# 列出在完整报告中需要补齐的新增接口模块（opcode 检查清单）
print("新增接口模块：")
print("1. 举报管理模块（ReportController）- 4个接口")
print("2. 内容审核模块（ModerationController）- 10个接口")
print("3. 归档管理模块（ArchiveController）- 1个接口")
print("4. 风控预警模块（RiskControlController）- 4个接口")
# 提示接口总数：原有接口 + 上面 19 个新增 = 81 个
print("\n总计：原有接口 + 新增19个接口 = 完整的81个接口")
