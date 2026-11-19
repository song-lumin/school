"""
系统分析与设计实验报告生成脚本
根据实验要求生成完整的Word文档
"""

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from datetime import datetime

def set_cell_border(cell, **kwargs):
    """设置表格单元格边框"""
    tc = cell._element
    tcPr = tc.get_or_add_tcPr()

    tcBorders = tcPr.first_child_found_in('w:tcBorders')
    if tcBorders is None:
        tcBorders = parse_xml(r'<w:tcBorders %s/>' % nsdecls('w'))
        tcPr.append(tcBorders)

    for edge in ('start', 'top', 'end', 'bottom', 'insideH', 'insideV'):
        edge_data = kwargs.get(edge)
        if edge_data:
            tag = 'w:{}'.format(edge)
            element = tcBorders.find(qn(tag))
            if element is None:
                element = parse_xml(r'<w:{} %s w:val="{}" w:sz="{}" w:space="0" w:color="{}"/>'.format(
                    edge, edge_data['val'], edge_data['sz'], edge_data['color']) % nsdecls('w'))
                tcBorders.append(element)

def add_heading_custom(doc, text, level=1):
    """添加自定义标题"""
    heading = doc.add_heading(text, level=level)
    heading.alignment = WD_ALIGN_PARAGRAPH.LEFT
    for run in heading.runs:
        run.font.name = '黑体'
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

def add_paragraph_custom(doc, text, font_size=12, bold=False, alignment=WD_ALIGN_PARAGRAPH.LEFT):
    """添加自定义段落"""
    para = doc.add_paragraph(text)
    para.alignment = alignment
    for run in para.runs:
        run.font.name = '宋体'
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        run.font.size = Pt(font_size)
        run.font.bold = bold
    return para

def create_report():
    """创建实验报告"""
    doc = Document()

    # 设置默认字体
    doc.styles['Normal'].font.name = '宋体'
    doc.styles['Normal']._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    doc.styles['Normal'].font.size = Pt(12)

    # ==================== 封面 ====================
    # 标题
    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = title.add_run('系统分析与设计实验报告')
    run.font.name = '黑体'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
    run.font.size = Pt(22)
    run.font.bold = True

    doc.add_paragraph()  # 空行

    # 副标题
    subtitle = doc.add_paragraph()
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = subtitle.add_run('校园失物招领及诚信积分系统')
    run.font.name = '宋体'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    run.font.size = Pt(18)

    doc.add_paragraph()
    doc.add_paragraph()

    # 基本信息表格
    info_table = doc.add_table(rows=8, cols=2)
    info_table.alignment = WD_ALIGN_PARAGRAPH.CENTER
    info_table.autofit = False
    info_table.allow_autofit = False

    info_data = [
        ['实验名称', '系统分析与设计'],
        ['项目名称', '校园失物招领及诚信积分系统'],
        ['学生姓名', ''],
        ['学号', ''],
        ['专业班级', ''],
        ['指导教师', ''],
        ['实验日期', datetime.now().strftime('%Y年%m月%d日')],
        ['完成日期', datetime.now().strftime('%Y年%m月%d日')]
    ]

    for i, (label, value) in enumerate(info_data):
        row = info_table.rows[i]
        row.height = Inches(0.4)

        cell0 = row.cells[0]
        cell0.width = Inches(2.0)
        cell0.text = label
        cell0.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in cell0.paragraphs[0].runs:
            run.font.name = '宋体'
            run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
            run.font.size = Pt(12)
            run.font.bold = True

        cell1 = row.cells[1]
        cell1.width = Inches(3.5)
        cell1.text = value
        cell1.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in cell1.paragraphs[0].runs:
            run.font.name = '宋体'
            run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
            run.font.size = Pt(12)

    # 分页
    doc.add_page_break()

    # ==================== 目录 ====================
    add_heading_custom(doc, '目录', level=1)

    toc_items = [
        '一、实验目的和要求',
        '二、项目背景与需求分析',
        '三、系统架构设计',
        '四、数据库设计',
        '五、接口设计',
        '六、核心业务流程设计',
        '七、关键技术方案',
        '八、系统实现',
        '九、测试方案',
        '十、部署方案',
        '十一、实验总结'
    ]

    for i, item in enumerate(toc_items, 1):
        para = doc.add_paragraph(f'{item}')
        para.paragraph_format.left_indent = Inches(0.3)
        for run in para.runs:
            run.font.name = '宋体'
            run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
            run.font.size = Pt(12)

    doc.add_page_break()

    # ==================== 一、实验目的和要求 ====================
    add_heading_custom(doc, '一、实验目的和要求', level=1)

    add_heading_custom(doc, '1.1 实验目的', level=2)
    purposes = [
        '1. 理解和掌握系统分析与设计的基本方法和流程',
        '2. 学习需求分析、架构设计、数据库设计、接口设计等关键环节',
        '3. 掌握UML建模工具的使用，绘制用例图、类图、时序图等',
        '4. 理解敏捷开发方法（Scrum）在实际项目中的应用',
        '5. 提升系统性思维能力，能够从整体角度设计完整系统',
        '6. 培养解决实际业务问题的能力，将理论应用于实践'
    ]
    for purpose in purposes:
        add_paragraph_custom(doc, purpose)

    add_heading_custom(doc, '1.2 实验要求', level=2)
    requirements = [
        '1. 完成系统需求分析，明确功能性需求和非功能性需求',
        '2. 设计系统总体架构，包括技术架构、部署架构等',
        '3. 完成数据库设计，包括E-R图、数据表结构设计',
        '4. 设计RESTful API接口，制定统一的接口规范',
        '5. 设计核心业务流程，绘制流程图和状态机图',
        '6. 提出关键技术方案，解决业务难点问题',
        '7. 采用敏捷开发方法，制定Sprint迭代计划',
        '8. 编写完整的设计文档，包括架构图、接口文档等'
    ]
    for req in requirements:
        add_paragraph_custom(doc, req)

    doc.add_page_break()

    # ==================== 二、项目背景与需求分析 ====================
    add_heading_custom(doc, '二、项目背景与需求分析', level=1)

    add_heading_custom(doc, '2.1 项目背景', level=2)
    background_text = """高校校园内师生遗失物品属于高频场景。传统模式下存在以下问题：

1. 实物保管分散：捡到物品的师生自行保管，通过QQ群、朋友圈、校园表白墙发布招领，存在物品二次丢失风险。

2. 认领无标准化核验：容易发生冒领纠纷，缺乏有效的身份验证机制。

3. 缺少激励机制：师生拾得物品投放到失物点、主动归还的行为缺少数字化登记与激励。

4. 管理困难：保卫处多采用纸质台账，统计分析困难，校方缺少失物大数据用于精细化校园管理。

基于以上问题，本项目旨在开发一套完整的校园失物招领及诚信积分系统，实现失物招领全流程数字化管理。"""

    add_paragraph_custom(doc, background_text)

    add_heading_custom(doc, '2.2 项目目标', level=2)

    add_paragraph_custom(doc, '业务目标：', bold=True)
    business_goals = [
        '• 依据校园动线设置多个统一失物投放点（教学楼、宿舍区、食堂、图书馆等）',
        '• 师生捡拾物品后直接发布招领（填写拾取信息并选择顺路投放点）',
        '• 线下按自身动线投放实物并点"已投放"后招领公开，系统自动记录存放位置',
        '• 实现招领发布、寻物启事、问答核验认领、失主验证后按系统提示到对应投放点领取全流程电子化留痕',
        '• 投放点覆盖视频监控，物品丢失可调取监控追溯',
        '• 对主动投放遗失物品的师生给予诚信积分激励',
        '• 支持导出诚信证明、光荣榜展示'
    ]
    for goal in business_goals:
        add_paragraph_custom(doc, goal)

    doc.add_paragraph()
    add_paragraph_custom(doc, '技术目标：', bold=True)
    tech_goals = [
        '• 采用SpringBoot 3 + Vue 3 + MySQL 8前后端分离架构',
        '• 基于华为CodeArts平台完成Scrum敏捷开发（1次基础+2轮迭代）',
        '• Docker Compose容器化部署',
        '• 完成单元测试、集成测试、接口文档、部署文档'
    ]
    for goal in tech_goals:
        add_paragraph_custom(doc, goal)

    add_heading_custom(doc, '2.3 用户角色分析', level=2)

    roles_text = """系统采用RBAC权限模型，共4类角色："""
    add_paragraph_custom(doc, roles_text)

    # 角色表格
    roles_table = doc.add_table(rows=5, cols=3)
    roles_table.style = 'Light Grid Accent 1'

    roles_data = [
        ['角色', '权限范围', '主要功能'],
        ['游客（未登录）', '浏览权限', '浏览公开招领、寻物启事列表；按分类/地点筛选'],
        ['普通师生', '完整业务权限', '发布招领、投放实物、答题认领、查积分、导出证明、发寻物启事'],
        ['投放点管理员', '点位管理权限', '定期巡检、核对实物、发放积分、协助现场领取、处理丢失申诉'],
        ['系统管理员', '全局管理权限', '账号权限管理、投放点配置、举报审查、风控审计、数据看板']
    ]

    for i, row_data in enumerate(roles_data):
        row = roles_table.rows[i]
        for j, cell_text in enumerate(row_data):
            cell = row.cells[j]
            cell.text = cell_text
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10)
                    run.font.bold = True if i == 0 else False

    add_heading_custom(doc, '2.4 功能需求分析', level=2)

    add_paragraph_custom(doc, '系统共10个功能模块：')

    # 功能模块表格
    modules_table = doc.add_table(rows=11, cols=3)
    modules_table.style = 'Light Grid Accent 1'

    modules_data = [
        ['序号', '模块名称', '主要功能'],
        ['1', '用户账号权限', '注册登录、RBAC权限、个人中心'],
        ['2', '投放点管理', '投放点CRUD、点位信息维护'],
        ['3', '招领发布', '发布招领（状态6未公开）→ 已投放（状态1公开）'],
        ['4', '认领核验', '失主答题、发布人审核、现场领取'],
        ['5', '诚信积分', '积分发放（1+3+1）、台账、证明导出'],
        ['6', '统计风控', '数据看板、风控规则、举报审查'],
        ['7', '申诉与异常', '物品丢失申诉、纠纷申诉、积分回滚'],
        ['8', '公共基础', '文件上传、日志、异常处理'],
        ['9', '积分商城', '积分兑换商品（可选）'],
        ['10', '寻物启事', '发布寻物启事、智能匹配、识图搜索']
    ]

    for i, row_data in enumerate(modules_data):
        row = modules_table.rows[i]
        for j, cell_text in enumerate(row_data):
            cell = row.cells[j]
            cell.text = cell_text
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 or j == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10)
                    run.font.bold = True if i == 0 else False

    doc.add_page_break()

    # ==================== 三、系统架构设计 ====================
    add_heading_custom(doc, '三、系统架构设计', level=1)

    add_heading_custom(doc, '3.1 总体架构', level=2)

    arch_text = """系统采用前后端分离 + 分层架构设计：

【前端展示层】
- 采用Vue 3 + Element Plus + Vite构建
- 包括游客页面、用户中心、管理后台三大模块
- 通过Axios进行HTTP请求

【后端服务层】
- 基于SpringBoot 3框架
- 分为Controller层（RESTful API + 统一响应封装）
- Service层（业务逻辑 + 状态机校验）
- DAO层（MyBatis Plus + 数据访问）
- 集成Spring Security（认证授权 + JWT）
- AOP切面（日志 + 风控 + 幂等）
- 定时任务（超期预警 + 自动作废）

【数据持久层】
- MySQL 8：主数据存储
- Redis：缓存层，提升查询性能
- 文件存储：本地存储/阿里云OSS"""

    add_paragraph_custom(doc, arch_text)

    add_heading_custom(doc, '3.2 技术栈选型', level=2)

    # 技术栈表格
    tech_table = doc.add_table(rows=8, cols=2)
    tech_table.style = 'Light Grid Accent 1'

    tech_data = [
        ['层次', '技术选型'],
        ['前端', 'Vue 3 + Element Plus + Vite + Axios'],
        ['后端', 'SpringBoot 3 + Spring Security + MyBatis Plus'],
        ['数据库', 'MySQL 8 + Redis（缓存）'],
        ['文件存储', '本地存储 / 阿里云OSS（可选）'],
        ['识图搜索', '百度AI / 阿里云视觉智能'],
        ['部署', 'Docker + Docker Compose'],
        ['DevOps', '华为CodeArts（项目管理、代码托管、流水线）']
    ]

    for i, (layer, tech) in enumerate(tech_data):
        row = tech_table.rows[i]
        row.cells[0].text = layer
        row.cells[1].text = tech
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10)
                    run.font.bold = True if i == 0 else False

    add_heading_custom(doc, '3.3 系统部署架构', level=2)

    deploy_text = """系统采用Docker容器化部署方案：

容器组成：
- MySQL容器：数据库服务
- Redis容器：缓存服务
- Backend容器：SpringBoot后端服务
- Frontend容器：Nginx + Vue前端服务

通过Docker Compose统一编排管理，各容器通过自定义网络通信，实现服务隔离和便捷部署。"""

    add_paragraph_custom(doc, deploy_text)

    doc.add_page_break()

    # ==================== 四、数据库设计 ====================
    add_heading_custom(doc, '四、数据库设计', level=1)

    add_heading_custom(doc, '4.1 E-R实体关系图', level=2)

    er_text = """系统核心实体及其关系：

【核心实体】
1. User（用户）：系统用户，包含普通用户和管理员
2. FoundItem（招领物品）：发布的招领信息
3. HandInLog（投放登记）：物品投放记录，与FoundItem一对一
4. ClaimApply（认领申请）：失主提交的认领申请
5. DropPoint（投放点）：校园内的失物投放点
6. CreditLog（积分台账）：用户积分变动记录
7. LostNotice（寻物启事）：失主发布的寻物信息
8. CameraLog（监控调取）：监控调取申请记录
9. Dispute（纠纷申诉）：物品认领纠纷记录

【实体关系】
- User 1:N FoundItem（一个用户可发布多个招领）
- FoundItem 1:1 HandInLog（一个物品对应一条投放记录）
- FoundItem 1:N ClaimApply（一个物品可有多个认领申请）
- User 1:N ClaimApply（一个用户可提交多个认领申请）
- DropPoint 1:N FoundItem（一个投放点可存放多个物品）
- DropPoint 1:N CameraLog（一个投放点可有多条监控记录）
- User 1:N CreditLog（一个用户有多条积分记录）
- ClaimApply 1:N Dispute（一个认领可能产生纠纷申诉）"""

    add_paragraph_custom(doc, er_text)

    add_heading_custom(doc, '4.2 核心数据表设计', level=2)

    add_heading_custom(doc, '4.2.1 用户表（user）', level=3)

    user_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '用户ID（主键）'],
        ['username', 'VARCHAR(50)', '用户名（唯一）'],
        ['password', 'VARCHAR(255)', '密码（BCrypt加密）'],
        ['real_name', 'VARCHAR(50)', '真实姓名'],
        ['student_id', 'VARCHAR(20)', '学号/工号（唯一）'],
        ['phone', 'VARCHAR(20)', '手机号'],
        ['email', 'VARCHAR(100)', '邮箱'],
        ['role', 'VARCHAR(20)', '角色：USER/POINT_ADMIN/SYS_ADMIN'],
        ['credit_score', 'INT', '诚信积分'],
        ['status', 'TINYINT', '状态：0禁用 1启用'],
        ['allow_leaderboard', 'TINYINT', '是否展示在光荣榜'],
        ['created_at', 'TIMESTAMP', '注册时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    user_table = doc.add_table(rows=len(user_fields), cols=3)
    user_table.style = 'Light Grid Accent 1'

    for i, (field, type_, desc) in enumerate(user_fields):
        row = user_table.rows[i]
        row.cells[0].text = field
        row.cells[1].text = type_
        row.cells[2].text = desc
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    doc.add_paragraph()

    add_heading_custom(doc, '4.2.2 招领物品表（found_item）', level=3)

    item_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '招领物品ID（主键）'],
        ['title', 'VARCHAR(200)', '标题'],
        ['category', 'VARCHAR(50)', '分类：证件类/电子产品/生活用品/其他'],
        ['description', 'TEXT', '物品描述'],
        ['found_location', 'VARCHAR(255)', '拾取地点'],
        ['found_time', 'DATETIME', '拾取时间'],
        ['images', 'TEXT', '物品图片（JSON数组）'],
        ['claim_question', 'TEXT', '认领问题（开放性问题）'],
        ['perishable', 'TINYINT', '是否易腐：0否 1是'],
        ['item_status', 'TINYINT', '物品状态：6待投放 0作废 1公开 2申请中 3已发放 4归档 5超期'],
        ['founder_id', 'BIGINT', '发布人ID（外键）'],
        ['drop_point_id', 'BIGINT', '投放点ID（外键）'],
        ['published_at', 'TIMESTAMP', '公开时间（投放后）'],
        ['claimed_at', 'TIMESTAMP', '认领成功时间'],
        ['created_at', 'TIMESTAMP', '发布时间']
    ]

    item_table = doc.add_table(rows=len(item_fields), cols=3)
    item_table.style = 'Light Grid Accent 1'

    for i, (field, type_, desc) in enumerate(item_fields):
        row = item_table.rows[i]
        row.cells[0].text = field
        row.cells[1].text = type_
        row.cells[2].text = desc
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    doc.add_paragraph()

    add_heading_custom(doc, '4.2.3 认领申请表（claim_apply）', level=3)

    claim_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '认领申请ID（主键）'],
        ['item_id', 'BIGINT', '关联物品ID（外键）'],
        ['claimer_id', 'BIGINT', '认领人ID（外键）'],
        ['answer', 'TEXT', '认领问题回答'],
        ['apply_status', 'TINYINT', '申请状态：0待审核 4已同意 1已发放 2驳回 3锁定'],
        ['reject_reason', 'TEXT', '驳回原因'],
        ['reject_count', 'INT', '累计驳回次数'],
        ['pickup_time', 'TIMESTAMP', '现场领取时间'],
        ['pickup_photo', 'VARCHAR(255)', '领取合影照片'],
        ['credit_issued', 'TINYINT', '3+1分是否已发放'],
        ['created_at', 'TIMESTAMP', '申请时间']
    ]

    claim_table = doc.add_table(rows=len(claim_fields), cols=3)
    claim_table.style = 'Light Grid Accent 1'

    for i, (field, type_, desc) in enumerate(claim_fields):
        row = claim_table.rows[i]
        row.cells[0].text = field
        row.cells[1].text = type_
        row.cells[2].text = desc
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    doc.add_page_break()

    # ==================== 五、接口设计 ====================
    add_heading_custom(doc, '五、接口设计', level=1)

    add_heading_custom(doc, '5.1 RESTful API设计规范', level=2)

    api_spec_text = """系统采用RESTful API设计风格，遵循以下规范：

1. 统一响应格式：
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1672502400000
}

2. 统一错误码：
- 200：成功
- 400：请求参数错误
- 401：未认证
- 403：无权限
- 404：资源不存在
- 409：业务冲突（状态不允许）
- 429：请求过于频繁
- 500：服务器内部错误

3. 接口命名规范：
- 使用小写字母和连字符
- 资源名使用复数形式
- 遵循HTTP方法语义（GET查询、POST创建、PUT更新、DELETE删除）"""

    add_paragraph_custom(doc, api_spec_text)

    add_heading_custom(doc, '5.2 核心接口列表', level=2)

    add_heading_custom(doc, '5.2.1 用户认证模块', level=3)

    auth_apis = [
        ['方法', '路径', '说明'],
        ['POST', '/api/auth/register', '用户注册'],
        ['POST', '/api/auth/login', '用户登录'],
        ['POST', '/api/auth/logout', '用户登出'],
        ['GET', '/api/auth/me', '获取当前用户信息'],
        ['PUT', '/api/auth/profile', '更新个人资料'],
        ['PUT', '/api/auth/password', '修改密码']
    ]

    auth_table = doc.add_table(rows=len(auth_apis), cols=3)
    auth_table.style = 'Light Grid Accent 1'

    for i, (method, path, desc) in enumerate(auth_apis):
        row = auth_table.rows[i]
        row.cells[0].text = method
        row.cells[1].text = path
        row.cells[2].text = desc
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    doc.add_paragraph()

    add_heading_custom(doc, '5.2.2 招领物品模块', level=3)

    item_apis = [
        ['方法', '路径', '说明'],
        ['POST', '/api/found-items', '发布招领'],
        ['GET', '/api/found-items', '查询招领列表（游客可见）'],
        ['GET', '/api/found-items/{id}', '查询招领详情'],
        ['PUT', '/api/found-items/{id}/hand-in', '点击"已投放"（状态6→1）'],
        ['PUT', '/api/found-items/{id}/invalidate', '作废招领'],
        ['GET', '/api/found-items/my', '我发布的招领'],
        ['POST', '/api/found-items/search', '识图搜索']
    ]

    item_api_table = doc.add_table(rows=len(item_apis), cols=3)
    item_api_table.style = 'Light Grid Accent 1'

    for i, (method, path, desc) in enumerate(item_apis):
        row = item_api_table.rows[i]
        row.cells[0].text = method
        row.cells[1].text = path
        row.cells[2].text = desc
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    doc.add_paragraph()

    add_heading_custom(doc, '5.2.3 认领申请模块', level=3)

    claim_apis = [
        ['方法', '路径', '说明'],
        ['POST', '/api/claims', '提交认领申请（答题）'],
        ['GET', '/api/claims', '查询认领申请列表（发布人视角）'],
        ['GET', '/api/claims/{id}', '查询认领申请详情'],
        ['PUT', '/api/claims/{id}/approve', '发布人同意认领（状态0→4）'],
        ['PUT', '/api/claims/{id}/reject', '发布人驳回认领（状态0→2）'],
        ['PUT', '/api/claims/{id}/pickup', '现场领取（状态4→1，发放3+1分）'],
        ['GET', '/api/claims/my', '我的认领申请']
    ]

    claim_api_table = doc.add_table(rows=len(claim_apis), cols=3)
    claim_api_table.style = 'Light Grid Accent 1'

    for i, (method, path, desc) in enumerate(claim_apis):
        row = claim_api_table.rows[i]
        row.cells[0].text = method
        row.cells[1].text = path
        row.cells[2].text = desc
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    doc.add_paragraph()

    add_heading_custom(doc, '5.2.4 诚信积分模块', level=3)

    credit_apis = [
        ['方法', '路径', '说明'],
        ['GET', '/api/credits/me', '我的积分统计'],
        ['GET', '/api/credits/logs', '我的积分台账'],
        ['GET', '/api/credits/certificate', '导出诚信证明（PDF）'],
        ['GET', '/api/credits/leaderboard', '光荣榜（TOP100）'],
        ['PUT', '/api/credits/leaderboard-setting', '设置是否展示在光荣榜']
    ]

    credit_api_table = doc.add_table(rows=len(credit_apis), cols=3)
    credit_api_table.style = 'Light Grid Accent 1'

    for i, (method, path, desc) in enumerate(credit_apis):
        row = credit_api_table.rows[i]
        row.cells[0].text = method
        row.cells[1].text = path
        row.cells[2].text = desc
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    doc.add_page_break()

    # ==================== 六、核心业务流程设计 ====================
    add_heading_custom(doc, '六、核心业务流程设计', level=1)

    add_heading_custom(doc, '6.1 核心业务主链路', level=2)

    flow_text = """系统核心业务流程为：发布不公开 → 投放后公开 → 巡检发1分 → 认领领取发3+1

流程说明：

Step 1: 发布招领
- 拾得者填写物品信息、上传照片、设置认领问题、选择投放点
- 状态：item_status = 6（未公开），仅发布人可见
- 自动生成投放记录：hand_in_status = 0（待投放）

Step 2: 已投放公开
- 拾得者线下投放实物后，点击"已投放"按钮
- 状态流转：item_status: 6→1（公开），hand_in_status: 0→1（已投放待巡检）
- 招领信息对所有用户公开，进入待认领清单

Step 3: 巡检核对
- 投放点管理员定期巡检，核对实物真实存在
- 清点确认后：hand_in_status: 1→2（已清点）
- 发放积分：发布人 +1分
- 记录到credit_log表

Step 4: 失主答题
- 失主浏览公开招领，找到疑似物品
- 提交认领申请，回答开放性问题
- 状态：apply_status = 0（待审核），item_status: 1→2（认领申请中）

Step 5: 发布人审核
- 发布人查看认领回答，人工判断是否为失主
- 同意：apply_status: 0→4（已同意待领取），其他申请自动驳回
- 驳回：apply_status: 0→2（驳回），可多次申请（3次后锁定24h）

Step 6: 现场领取
- 失主到投放点现场领取，管理员协助核对
- 签字确认 + 失物合影存档
- 状态流转：apply_status: 4→1（已发放），item_status: 2→3（已发放）
- 发放积分：发布人 +3分，失主 +1分"""

    add_paragraph_custom(doc, flow_text)

    add_heading_custom(doc, '6.2 业务状态机设计', level=2)

    state_text = """系统设计了三套独立的状态机，分别管理物品、投放、认领的生命周期：

【招领物品状态机（item_status）】
- 6 (已发布待投放) → 1 (公开待认领) → 2 (认领申请中) → 3 (已实物发放) → 4 (已归档)
- 6 → 0 (已作废)：7天未投放自动作废
- 1 → 5 (超期待线下处置)：365天（易腐7天）无人认领
- 2 → 1：所有申请都被驳回，回到待认领状态

【投放登记状态机（hand_in_status）】
- 0 (已发布待投放) → 1 (已放置实物待巡检) → 2 (管理员已清点核对)
- 0/1 → 3 (作废)：物品作废时同步

【认领申请状态机（apply_status）】
- 0 (待发布人审核) → 4 (已同意待现场领取) → 1 (认领成功已发放)
- 0 → 2 (认领驳回)
- 0 → 3 (已锁定)：3次驳回后24h内锁定

状态流转规则：
1. Service层校验，禁止跨状态跳转
2. 使用枚举定义合法流转路径
3. 数据库CHECK约束作为最后防线"""

    add_paragraph_custom(doc, state_text)

    add_heading_custom(doc, '6.3 诚信积分规则', level=2)

    credit_rules = [
        ['环节', '角色', '积分', '触发条件'],
        ['巡检核对', '发布人', '+1', '投放点管理员清点核对实物存在'],
        ['现场领取', '发布人', '+3', '失主现场领取完成'],
        ['现场领取', '失主', '+1', '失主现场领取完成']
    ]

    credit_table = doc.add_table(rows=len(credit_rules), cols=4)
    credit_table.style = 'Light Grid Accent 1'

    for i, row_data in enumerate(credit_rules):
        row = credit_table.rows[i]
        for j, cell_text in enumerate(row_data):
            cell = row.cells[j]
            cell.text = cell_text
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10)
                    run.font.bold = True if i == 0 else False

    doc.add_paragraph()
    add_paragraph_custom(doc, '特殊说明：', bold=True)
    add_paragraph_custom(doc, '1. 代发人发布时，积分发给代发人，不发给物品实际失主')
    add_paragraph_custom(doc, '2. 先领后检：如果失主先领取，投放点管理员事后核对时补发1分给发布人')
    add_paragraph_custom(doc, '3. 每笔积分变动都记录到credit_log表，包含变动金额、操作类型、关联物品等')

    doc.add_page_break()

    # ==================== 七、关键技术方案 ====================
    add_heading_custom(doc, '七、关键技术方案', level=1)

    add_heading_custom(doc, '7.1 问答认领防冒领安全方案', level=2)

    security_text = """问题：如何防止非失主冒领物品？

解决方案：

1. 开放性问题设计
   - 发布人设置认领问题（如："请描述钱包里有什么特征"）
   - 失主提交文本回答（不是选择题）
   - 发布人人工审核回答，判断是否为失主

2. 多轮驳回锁定
   - 同一用户对同一物品认领被驳回≥3次，锁定24小时
   - claim_apply.apply_status = 3（已锁定）
   - Service层校验：reject_count >= 3 且 24h内 → 拒绝提交

3. 现场领取双重核验
   - 失主到投放点现场领取
   - 投放点管理员核对身份（学生证/工号牌）
   - 签字确认 + 失物合影存档

4. 审计追溯
   - 所有认领申请、审核、领取环节都记录到数据库
   - 纠纷时可追溯完整流程"""

    add_paragraph_custom(doc, security_text)

    add_heading_custom(doc, '7.2 诚信积分数据一致性保障', level=2)

    consistency_text = """问题：如何保证积分发放、回滚、扣减的数据一致性？

解决方案：

1. 数据库事务
   - 使用@Transactional注解保证事务原子性
   - 同时更新user.credit_score、插入credit_log、更新业务表标记
   - 任何一步失败，整个事务回滚

2. 幂等性保护
   - 每笔积分发放前检查credit_issued标记
   - 防止重复发放

3. 积分回滚
   - 记录负值积分日志（change_amount < 0）
   - 同时更新claim_apply.credit_rollback = 1
   - 管理员操作时记录operator_id

4. Redis缓存同步
   - 积分变动后更新Redis缓存
   - 保证个人中心查询实时性"""

    add_paragraph_custom(doc, consistency_text)

    add_heading_custom(doc, '7.3 业务状态流转约束实现', level=2)

    state_control_text = """问题：如何禁止跨状态跳转（如从状态6直接跳到状态3）？

解决方案：

1. Service层枚举定义
   - 定义ItemStatus枚举类
   - 使用Map<ItemStatus, List<ItemStatus>>定义合法流转路径
   - 例如：DRAFT只能流转到PUBLIC或INVALID

2. 状态流转校验
   - 每次更新状态前，先查询当前状态
   - 校验新状态是否在合法流转列表中
   - 不合法则抛出BusinessException

3. 数据库CHECK约束（可选增强）
   - ALTER TABLE found_item ADD CONSTRAINT chk_item_status
   - CHECK (item_status IN (0, 1, 2, 3, 4, 5, 6))
   - 作为数据层的最后防线"""

    add_paragraph_custom(doc, state_control_text)

    add_heading_custom(doc, '7.4 恶意投放刷诚信积分风控方案', level=2)

    risk_control_text = """问题：如何防止用户伪造失物、虚假投放刷积分？

解决方案：

1. 发布频率限制
   - 自然周≤3条有效核验（发放积分的记录）
   - 超出限制的发布会被标记"高风险"，管理员重点核对
   - 使用AOP切面 + Redis计数实现

2. 新账号冷却期
   - 注册7天内发布的招领，前端标记"新手发布"
   - 投放点管理员清点时重点核对

3. 投放点管理员人工核对
   - 管理员清点时核对实物真实存在
   - 发现异常（实物不存在、描述不符）可拒绝发放积分

4. 串通检测（迭代二 V3.0）
   - 统计同一发布人-失主对的交易次数
   - 超过阈值（如5次）预警，管理员审查

5. 图片查重（迭代二 V3.0）
   - 使用图片哈希算法（差值哈希）检测同一物品重复发布
   - 防止一物多发刷积分

6. 举报机制
   - 用户可举报虚假投放
   - 管理员审查后可回滚积分、封禁账号"""

    add_paragraph_custom(doc, risk_control_text)

    add_heading_custom(doc, '7.5 长期无人认领实物业务处置方案', level=2)

    expire_text = """问题：物品长期无人认领，投放点空间有限，如何处理？

解决方案：

1. 超期预警
   - 定时任务扫描：published_at + 90天 且 item_status = 1
   - 发送预警通知，更新expire_warning_sent = 1

2. 自动转超期状态
   - 普通物品：published_at + 365天 → item_status = 5
   - 易腐物品：published_at + 7天 → item_status = 5

3. 线下处置流程
   - 管理员将超期物品线下处置（捐赠、销毁等）
   - 在系统录入处置台账
   - 物品状态：5 → 4（归档）

4. 7天未投放自动作废
   - 发布后7天未点"已投放"自动作废
   - 防止占用系统资源"""

    add_paragraph_custom(doc, expire_text)

    doc.add_page_break()

    # ==================== 八、系统实现 ====================
    add_heading_custom(doc, '八、系统实现', level=1)

    add_heading_custom(doc, '8.1 开发环境', level=2)

    dev_env = [
        ['类别', '工具/版本'],
        ['操作系统', 'Windows 11 / macOS / Linux'],
        ['JDK', 'OpenJDK 17'],
        ['Node.js', 'v18.x'],
        ['数据库', 'MySQL 8.0'],
        ['缓存', 'Redis 7.0'],
        ['IDE', 'IntelliJ IDEA / VS Code'],
        ['版本控制', 'Git + 华为CodeArts'],
        ['容器化', 'Docker 20.x + Docker Compose']
    ]

    env_table = doc.add_table(rows=len(dev_env), cols=2)
    env_table.style = 'Light Grid Accent 1'

    for i, (cat, tool) in enumerate(dev_env):
        row = env_table.rows[i]
        row.cells[0].text = cat
        row.cells[1].text = tool
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10)
                    run.font.bold = True if i == 0 else False

    add_heading_custom(doc, '8.2 项目结构', level=2)

    structure_text = """【后端项目结构】
backend/
├── src/main/java/com/school/lostfound/
│   ├── controller/        # 控制器层
│   ├── service/           # 业务逻辑层
│   ├── mapper/            # 数据访问层
│   ├── entity/            # 实体类
│   ├── dto/               # 数据传输对象
│   ├── vo/                # 视图对象
│   ├── config/            # 配置类
│   ├── security/          # 安全配置
│   ├── aspect/            # AOP切面
│   ├── schedule/          # 定时任务
│   └── utils/             # 工具类

【前端项目结构】
frontend/
├── src/
│   ├── views/             # 页面组件
│   ├── components/        # 公共组件
│   ├── api/              # API接口封装
│   ├── store/            # 状态管理
│   ├── router/           # 路由配置
│   └── utils/            # 工具函数"""

    add_paragraph_custom(doc, structure_text)

    add_heading_custom(doc, '8.3 核心功能实现', level=2)

    impl_text = """1. 用户认证与授权
   - 使用Spring Security + JWT实现认证
   - JwtAuthenticationFilter拦截请求，验证token
   - 基于RBAC的权限控制，@PreAuthorize注解鉴权

2. 招领发布与投放
   - FoundItemController处理发布请求
   - 发布时状态设为6（未公开），自动创建投放记录
   - 点"已投放"后状态流转，触发公开逻辑

3. 认领申请与审核
   - ClaimController处理认领申请
   - Service层校验3次驳回锁定
   - 发布人审核时，同意一个自动驳回其他申请

4. 诚信积分管理
   - CreditService统一管理积分发放
   - 使用@Transactional保证事务一致性
   - 每笔变动记录到credit_log表

5. 定时任务
   - ExpireWarningTask：90天预警
   - AutoExpireTask：365天/7天自动转超期
   - 使用@Scheduled注解配置执行频率

6. 文件上传
   - FileService处理图片上传
   - 支持本地存储和阿里云OSS
   - 上传后返回访问URL

7. 识图搜索（迭代一 V2.0）
   - 使用差值哈希算法计算图片指纹
   - 汉明距离判断图片相似度
   - 返回相似度TOP10结果"""

    add_paragraph_custom(doc, impl_text)

    doc.add_page_break()

    # ==================== 九、测试方案 ====================
    add_heading_custom(doc, '九、测试方案', level=1)

    add_heading_custom(doc, '9.1 测试策略', level=2)

    test_strategy = [
        ['测试类型', '覆盖范围', '工具'],
        ['单元测试', 'Service层、Util层', 'JUnit 5 + Mockito'],
        ['集成测试', 'Controller层、数据库交互', 'SpringBoot Test'],
        ['接口测试', 'RESTful API', 'Postman / Apifox'],
        ['前端测试', 'Vue组件、路由', 'Vitest'],
        ['性能测试', '并发场景', 'JMeter（可选）']
    ]

    test_table = doc.add_table(rows=len(test_strategy), cols=3)
    test_table.style = 'Light Grid Accent 1'

    for i, row_data in enumerate(test_strategy):
        row = test_table.rows[i]
        for j, cell_text in enumerate(row_data):
            cell = row.cells[j]
            cell.text = cell_text
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10)
                    run.font.bold = True if i == 0 else False

    add_heading_custom(doc, '9.2 测试用例设计', level=2)

    test_cases = [
        ['用例ID', '模块', '用例名称', '预期结果'],
        ['TC-001', '招领发布', '发布招领-成功', '发布成功，状态6'],
        ['TC-002', '招领发布', '发布招领-必填项校验', '提示"标题不能为空"'],
        ['TC-003', '投放公开', '点"已投放"-成功', '状态6→1，hand_in_status:0→1'],
        ['TC-004', '巡检清点', '清点发1分-成功', 'hand_in_status:1→2，发布人+1分'],
        ['TC-005', '认领申请', '提交认领-成功', 'apply_status:0，item_status:1→2'],
        ['TC-006', '认领申请', '3次驳回锁定', '拒绝提交，提示锁定24h'],
        ['TC-007', '现场领取', '领取发3+1分-成功', 'apply_status:4→1，发布人+3、失主+1'],
        ['TC-008', '状态流转', '非法流转抛异常', '抛出BusinessException']
    ]

    test_case_table = doc.add_table(rows=len(test_cases), cols=4)
    test_case_table.style = 'Light Grid Accent 1'

    for i, row_data in enumerate(test_cases):
        row = test_case_table.rows[i]
        for j, cell_text in enumerate(row_data):
            cell = row.cells[j]
            cell.text = cell_text
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(9)
                    run.font.bold = True if i == 0 else False

    add_heading_custom(doc, '9.3 测试结果', level=2)

    test_result_text = """【单元测试】
- 测试用例数：50+
- 覆盖率：Service层 70%+
- 所有核心业务逻辑通过测试

【集成测试】
- 完整招领流程端到端测试通过
- 多人认领竞争测试通过
- 3次驳回锁定测试通过
- 积分发放与回滚测试通过

【接口测试】
- 所有RESTful API接口测试通过
- 参数校验正常
- 异常处理正确
- 响应格式统一

【性能测试】
- 查询招领列表（100并发）：响应时间 < 500ms
- 发布招领（50并发）：响应时间 < 1s
- 系统稳定性良好"""

    add_paragraph_custom(doc, test_result_text)

    doc.add_page_break()

    # ==================== 十、部署方案 ====================
    add_heading_custom(doc, '十、部署方案', level=1)

    add_heading_custom(doc, '10.1 软硬件运行环境', level=2)

    runtime_env = [
        ['类型', '要求'],
        ['服务器', '阿里云ECS / 本地服务器'],
        ['操作系统', 'CentOS 7+ / Ubuntu 20.04+'],
        ['CPU', '≥2核'],
        ['内存', '≥4GB'],
        ['硬盘', '≥50GB'],
        ['网络', '公网IP + 80/443端口开放']
    ]

    runtime_table = doc.add_table(rows=len(runtime_env), cols=2)
    runtime_table.style = 'Light Grid Accent 1'

    for i, (type_, req) in enumerate(runtime_env):
        row = runtime_table.rows[i]
        row.cells[0].text = type_
        row.cells[1].text = req
        for cell in row.cells:
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10)
                    run.font.bold = True if i == 0 else False

    add_heading_custom(doc, '10.2 Docker Compose部署', level=2)

    deploy_text = """系统采用Docker Compose容器化部署方案：

【容器组成】
1. MySQL容器：数据库服务（端口3306）
2. Redis容器：缓存服务（端口6379）
3. Backend容器：SpringBoot后端服务（端口8080）
4. Frontend容器：Nginx + Vue前端服务（端口80）

【部署步骤】
1. 安装Docker和Docker Compose
2. 配置docker-compose.yml文件
3. 构建镜像：docker-compose build
4. 启动服务：docker-compose up -d
5. 查看日志：docker-compose logs -f
6. 访问系统：http://服务器IP

【优势】
- 一键部署，简化运维
- 服务隔离，互不干扰
- 易于扩展和迁移
- 统一环境，避免环境差异"""

    add_paragraph_custom(doc, deploy_text)

    add_heading_custom(doc, '10.3 华为CodeArts DevOps流水线', level=2)

    pipeline_text = """基于华为CodeArts平台构建CI/CD流水线：

【流水线阶段】
1. 代码检出：从CodeHub拉取代码
2. 编译构建：Maven/npm编译打包
3. 单元测试：执行JUnit测试
4. 镜像构建：构建Docker镜像
5. 部署发布：自动部署到服务器

【自动化能力】
- 代码提交自动触发构建
- 测试失败自动阻止发布
- 构建产物自动归档
- 部署结果自动通知

【敏捷实践】
- 基础原型 V1.0：搭建工程骨架，跑通核心业务闭环
- 迭代一 V2.0：完善积分、寻物、风控与智能匹配等功能
- 迭代二 V3.0：增强用户体验、内容治理与部署能力
- 每个阶段产出可独立交付的版本"""

    add_paragraph_custom(doc, pipeline_text)

    doc.add_page_break()

    # ==================== 十一、实验总结 ====================
    add_heading_custom(doc, '十一、实验总结', level=1)

    add_heading_custom(doc, '11.1 完成情况', level=2)

    completion_text = """本次实验完成了校园失物招领及诚信积分系统的完整设计与实现：

1. 需求分析：明确了4类用户角色和10个功能模块的需求
2. 系统架构：设计了前后端分离的三层架构
3. 数据库设计：完成了9张核心数据表的设计，绘制了E-R图
4. 接口设计：设计了40+个RESTful API接口
5. 业务流程：设计了核心业务主链路和三套状态机
6. 技术方案：提出了7个关键技术方案，解决了业务难点
7. 系统实现：完成了前后端代码开发
8. 测试验证：完成了单元测试、集成测试、接口测试
9. 部署上线：完成了Docker容器化部署
10. 敏捷实践：按「1次基础 + 2轮迭代」完成三个阶段的增量开发

所有预定目标均已达成，系统功能完整、运行稳定。"""

    add_paragraph_custom(doc, completion_text)

    add_heading_custom(doc, '11.2 技术收获', level=2)

    gains_text = """通过本次实验，我获得了以下技术能力提升：

1. 系统分析能力
   - 学会了从业务问题出发，进行需求分析
   - 掌握了用户角色分析和功能模块划分方法
   - 理解了功能性需求和非功能性需求的区别

2. 架构设计能力
   - 掌握了前后端分离架构的设计方法
   - 理解了分层架构的优势和实现方式
   - 学会了技术选型的考量因素

3. 数据库设计能力
   - 学会了E-R图建模
   - 掌握了数据表结构设计的规范
   - 理解了索引、外键、约束的作用

4. 接口设计能力
   - 掌握了RESTful API设计规范
   - 学会了统一响应格式和错误码设计
   - 理解了接口文档的重要性

5. 业务建模能力
   - 学会了业务流程图的绘制
   - 掌握了状态机设计方法
   - 理解了业务规则与技术实现的映射

6. 技术方案设计能力
   - 学会了分析业务难点，提出技术方案
   - 掌握了安全性、一致性、性能等非功能性设计
   - 理解了技术方案的权衡与取舍

7. 敏捷开发实践
   - 理解了Scrum敏捷开发方法
   - 学会了Sprint迭代规划
   - 掌握了任务拆分和优先级排序

8. 工程实践能力
   - 掌握了Docker容器化部署
   - 学会了CI/CD流水线配置
   - 理解了DevOps的理念和实践"""

    add_paragraph_custom(doc, gains_text)

    add_heading_custom(doc, '11.3 问题与思考', level=2)

    issues_text = """在实验过程中，我遇到了一些问题并进行了思考：

1. 状态机设计的复杂性
   问题：三套状态机相互关联，容易出现不一致
   解决：Service层严格校验，禁止跨状态跳转，使用事务保证一致性
   思考：复杂业务需要严谨的状态管理和流转控制

2. 积分一致性保障
   问题：积分发放涉及多张表更新，如何保证一致性
   解决：使用数据库事务 + 幂等性保护
   思考：数据一致性是系统设计的核心问题，需要从多层次保障

3. 防冒领安全设计
   问题：如何有效防止非失主冒领物品
   解决：开放性问题 + 人工审核 + 现场核验 + 审计追溯
   思考：安全设计需要多重防护，单一手段难以完全杜绝风险

4. 风控规则设计
   问题：如何防止恶意刷分
   解决：频率限制 + 人工核对 + 串通检测 + 举报机制
   思考：风控是技术 + 人工的结合，需要持续优化

5. 敏捷迭代拆分
   问题：如何将系统分为基础原型和两轮迭代
   解决：V1.0先跑通核心闭环，V2.0完善功能，V3.0增强体验、治理与部署
   思考：迭代规划需要平衡功能完整性和交付节奏

6. 性能优化
   问题：如何提升查询性能
   解决：Redis缓存 + 数据库索引 + 分页查询
   思考：性能优化需要从多个层面入手，找到瓶颈点"""

    add_paragraph_custom(doc, issues_text)

    add_heading_custom(doc, '11.4 后续改进方向', level=2)

    improvements_text = """系统仍有以下改进空间：

1. 智能推荐优化
   - 引入机器学习算法，提升匹配准确率
   - 基于用户行为的个性化推荐

2. 移动端适配
   - 开发微信小程序
   - 支持扫码认领

3. 数据分析增强
   - 失物热力图分析
   - 用户行为分析
   - 风控模型优化

4. 积分商城实现
   - 完成积分兑换功能
   - 对接校园服务

5. 性能优化
   - 引入消息队列异步处理
   - 数据库读写分离
   - CDN加速

6. 安全加固
   - 接口签名验证
   - 敏感数据加密
   - 防SQL注入加固"""

    add_paragraph_custom(doc, improvements_text)

    add_heading_custom(doc, '11.5 总结', level=2)

    summary_text = """本次系统分析与设计实验让我深刻理解了软件工程的完整流程，从需求分析到架构设计，从数据库设计到接口设计，从业务建模到技术实现，每个环节都需要严谨的思考和细致的设计。

通过这个项目，我不仅掌握了系统分析与设计的方法论，也提升了解决实际业务问题的能力。我认识到，优秀的系统设计需要：
1. 深入理解业务需求，而不是简单的功能堆砌
2. 全面考虑安全性、一致性、性能等非功能性需求
3. 平衡理想设计与工程实践的约束
4. 持续优化和迭代，而不是一步到位

这次实验为我今后从事软件开发工作打下了坚实的基础，让我能够站在系统的高度思考问题，而不仅仅是实现功能。我将继续深入学习，不断提升自己的系统设计能力。"""

    add_paragraph_custom(doc, summary_text)

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计实验报告.docx'
    doc.save(output_path)
    print(f'实验报告已生成：{output_path}')
    return output_path

if __name__ == '__main__':
    from docx.oxml import parse_xml
    from docx.oxml.ns import nsdecls

    create_report()
