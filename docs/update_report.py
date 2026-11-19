"""
修正版：基于真实系统代码生成第3-6章实验报告（更新报告的核心逻辑未实现）。

用途：本脚本原计划"打开已生成的《系统分析与设计实验报告（第3-6章）.docx》，
在第五章补上遗漏的控制器和接口（举报管理 ReportController、内容审核
ModerationController、归档管理 ArchiveController、风控预警
RiskControlController 等新增 19 个接口）"。

实现说明：由于 python-docx 对已有文档做"段中插入"很繁琐，
最终放弃了直接编辑旧文档的方案（update_report 只打印一句
"重新生成完整报告..."就结束），改为由后续脚本（generate_* 系列）
从零重新生成完整报告。因此本脚本是一次性的过渡/占位脚本。

输入：D:\gdut\school\docs\系统分析与设计实验报告（第3-6章）.docx（读取但未修改）
输出：仅控制台打印，无文件输出
"""

from docx import Document
from docx.shared import Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn

def add_heading_custom(doc, text, level=1):
    """添加自定义样式的标题（黑体，1级16pt / 2级14pt / 3级12pt，均加粗）。"""
    heading = doc.add_heading(text, level=level)
    heading.alignment = WD_ALIGN_PARAGRAPH.LEFT
    for run in heading.runs:
        run.font.name = '黑体'
        # 设置东亚字体，保证中文字体生效
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
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
    """添加自定义样式的正文段落（宋体、可指定字号与加粗）。"""
    para = doc.add_paragraph(text)
    for run in para.runs:
        run.font.name = '宋体'
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        run.font.size = Pt(font_size)
        run.font.bold = bold
    return para

def create_table_with_data(doc, data, has_header=True):
    """
    根据二维列表 data 创建表格。

    表头行居中加粗 10pt，数据行左对齐；列数 > 3 时字号缩小到 9pt。
    """
    table = doc.add_table(rows=len(data), cols=len(data[0]))
    table.style = 'Light Grid Accent 1'  # Word 内置浅色网格样式

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
                    # 列多时缩小字号
                    run.font.size = Pt(10 if len(data[0]) <= 3 else 9)
                    run.font.bold = True if (i == 0 and has_header) else False
    return table

def update_report():
    """读取旧报告并尝试补充缺失接口（未实现，仅打印提示）。"""
    # 打开已存在的实验报告文档（只读，未做任何修改）
    doc = Document('D:\\gdut\\school\\docs\\系统分析与设计实验报告（第3-6章）.docx')

    # 在第五章添加缺失的接口模块
    # 找到第五章的位置并在适当位置插入

    # 由于docx库操作复杂，我们重新生成完整报告
    # （原计划在旧文档中插入段落，python-docx 难以实现，改为整体重生成）
    print("重新生成完整报告...")

# 脚本入口：直接执行更新流程
update_report()
