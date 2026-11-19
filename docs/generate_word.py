#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
生成系统分析与设计报告Word文档
只包含第3、4、5章内容
"""

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn

def set_chinese_font(doc):
    """设置中文字体"""
    doc.styles['Normal'].font.name = '宋体'
    doc.styles['Normal'].font.size = Pt(12)
    # 设置中文字体
    try:
        doc.styles['Normal']._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    except:
        pass  # 如果设置失败则跳过

def add_heading_custom(doc, text, level):
    """添加自定义标题"""
    heading = doc.add_heading(text, level=level)
    heading.paragraph_format.space_before = Pt(12)
    heading.paragraph_format.space_after = Pt(6)
    return heading

def add_paragraph_with_indent(doc, text, indent=0):
    """添加带缩进的段落"""
    p = doc.add_paragraph(text)
    p.paragraph_format.left_indent = Inches(indent * 0.5)
    p.paragraph_format.line_spacing = 1.5
    return p

def add_table_from_data(doc, headers, rows):
    """添加表格"""
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = 'Light Grid Accent 1'

    # 设置表头
    hdr_cells = table.rows[0].cells
    for i, header in enumerate(headers):
        hdr_cells[i].text = header
        hdr_cells[i].paragraphs[0].runs[0].font.bold = True

    # 添加数据行
    for row_data in rows:
        row_cells = table.add_row().cells
        for i, cell_data in enumerate(row_data):
            row_cells[i].text = str(cell_data)

    return table

def main():
    doc = Document()
    set_chinese_font(doc)

    # 标题
    title = doc.add_heading('校园失物招领及诚信积分系统', 0)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER

    subtitle = doc.add_heading('系统分析与设计报告', 0)
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER

    doc.add_page_break()

    # ============================================================
    # 第3章：系统设计 - 领域模型(类图)
    # ============================================================
    add_heading_custom(doc, '3. 系统设计 - 领域模型(类图)', 1)

    add_heading_custom(doc, '3.1 核心领域类图', 2)

    add_paragraph_with_indent(doc, '系统核心领域模型包含以下实体类及其关系：')
    doc.add_paragraph()

    # 核心实体类列表
    doc.add_paragraph('【核心实体类】', style='List Bullet')
    entities = [
        'User (用户类): 管理用户基本信息、角色权限、诚信积分',
        'FoundItem (失物类): 记录拾取物品的详细信息',
        'ClaimApply (认领申请类): 管理物品认领申请流程',
        'DropPoint (招领站点类): 管理物理站点信息',
        'CreditLog (积分日志类): 记录所有积分变动',
        'HandInLog (交物日志类): 记录物品交付到站点的日志',
        'LostNotice (寻物启事类): 记录用户发布的寻物启事',
        'CameraLog (监控调阅日志类): 记录监控调阅申请',
        'Dispute (争议处理类): 记录争议申诉信息',
        'ReportLog (举报日志类): 记录用户举报信息'
    ]
    for entity in entities:
        doc.add_paragraph(entity, style='List Bullet')

    doc.add_paragraph()
    add_paragraph_with_indent(doc, '注：由于Word文档限制，完整的UML类图请参考项目文档中的PlantUML源代码，可使用PlantUML工具生成可视化图表。')

    doc.add_page_break()

    add_heading_custom(doc, '3.2 类图说明', 2)

    add_heading_custom(doc, '3.2.1 核心实体类', 3)

    # User类
    doc.add_paragraph('1. User (用户类)', style='List Number')
    add_paragraph_with_indent(doc, '职责：管理用户基本信息、角色权限、诚信积分', 1)
    add_paragraph_with_indent(doc, '关键属性：', 1)
    doc.add_paragraph('role: 用户角色(普通用户/站点管理员/系统管理员)', style='List Bullet 2')
    doc.add_paragraph('creditScore: 诚信积分,通过拾取和认领物品累积', style='List Bullet 2')
    doc.add_paragraph('status: 账号状态(正常/禁用)', style='List Bullet 2')

    # FoundItem类
    doc.add_paragraph('2. FoundItem (失物类)', style='List Number')
    add_paragraph_with_indent(doc, '职责：记录拾取物品的详细信息', 1)
    add_paragraph_with_indent(doc, '关键属性：', 1)
    doc.add_paragraph('claimQuestion: 防伪问题,用于验证失主身份', style='List Bullet 2')
    doc.add_paragraph('itemStatus: 物品状态(公开待认领/认领中/已取件等)', style='List Bullet 2')
    doc.add_paragraph('actualFounderId: 实际拾取者(支持代发布场景)', style='List Bullet 2')

    # ClaimApply类
    doc.add_paragraph('3. ClaimApply (认领申请类)', style='List Number')
    add_paragraph_with_indent(doc, '职责：管理物品认领申请流程', 1)
    add_paragraph_with_indent(doc, '关键属性：', 1)
    doc.add_paragraph('answer: 防伪问题答案', style='List Bullet 2')
    doc.add_paragraph('confidenceScore: 置信度评分(0-100)', style='List Bullet 2')
    doc.add_paragraph('rejectCount: 拒绝次数,超过3次自动锁定', style='List Bullet 2')
    doc.add_paragraph('pickupPhoto/pickupSignature: 取件凭证', style='List Bullet 2')

    # DropPoint类
    doc.add_paragraph('4. DropPoint (招领站点类)', style='List Number')
    add_paragraph_with_indent(doc, '职责：管理物理站点信息', 1)
    add_paragraph_with_indent(doc, '关键属性：', 1)
    doc.add_paragraph('hasCamera: 是否有监控设备', style='List Bullet 2')
    doc.add_paragraph('adminId: 站点管理员', style='List Bullet 2')

    # CreditLog类
    doc.add_paragraph('5. CreditLog (积分日志类)', style='List Number')
    add_paragraph_with_indent(doc, '职责：记录所有积分变动,支持追溯和审计', 1)
    add_paragraph_with_indent(doc, '关键操作类型：', 1)
    doc.add_paragraph('CHECK_ISSUE: 巡检发放(拾取者获得积分)', style='List Bullet 2')
    doc.add_paragraph('PICKUP_ISSUE: 取件发放(失主获得积分)', style='List Bullet 2')
    doc.add_paragraph('ROLLBACK: 积分回滚', style='List Bullet 2')
    doc.add_paragraph('DEDUCT: 积分扣除', style='List Bullet 2')

    doc.add_page_break()

    add_heading_custom(doc, '3.2.2 业务关系说明', 3)

    relationships = [
        '用户与失物：一个用户可以发布多个失物,一个失物由一个用户发布',
        '失物与认领：一个失物可以有多个认领申请,但最终只有一个通过',
        '用户与站点：一个站点管理员管理一个站点',
        '失物与站点：所有失物必须存放在特定站点',
        '寻物启事与认领：寻物启事可以直接转化为认领申请'
    ]

    for i, rel in enumerate(relationships, 1):
        doc.add_paragraph(f'{i}. {rel}', style='List Number')

    doc.add_page_break()

    # ============================================================
    # 第4章：系统设计 - 对象交互模型(时序图)
    # ============================================================
    add_heading_custom(doc, '4. 系统设计 - 对象交互模型(时序图)', 1)

    add_heading_custom(doc, '4.1 用户注册登录时序图', 2)

    add_paragraph_with_indent(doc, '用户注册登录流程涉及以下关键步骤：')

    doc.add_paragraph()
    doc.add_paragraph('【用户注册流程】', style='Heading 3')
    register_steps = [
        '用户提交注册信息(username, password, studentId)',
        'AuthController接收请求并调用UserService',
        'UserService验证用户名是否已存在',
        'UserService验证学号是否已存在',
        'PasswordEncoder加密密码',
        'UserMapper将用户信息插入数据库',
        'UserService生成JWT Token',
        '返回Token和用户信息给前端'
    ]
    for i, step in enumerate(register_steps, 1):
        doc.add_paragraph(f'{i}. {step}', style='List Number')

    doc.add_paragraph()
    doc.add_paragraph('【用户登录流程】', style='Heading 3')
    login_steps = [
        '用户提交登录信息(username, password)',
        'AuthController调用UserService进行验证',
        'UserService查询数据库获取用户信息',
        'PasswordEncoder验证密码是否匹配',
        'UserService生成JWT Token',
        '返回Token和用户信息给前端'
    ]
    for i, step in enumerate(login_steps, 1):
        doc.add_paragraph(f'{i}. {step}', style='List Number')

    doc.add_paragraph()
    add_paragraph_with_indent(doc, '注：完整的时序图请参考项目文档中的PlantUML源代码。')

    doc.add_page_break()

    add_heading_custom(doc, '4.2 失物发布时序图', 2)

    add_paragraph_with_indent(doc, '失物发布流程说明：')
    publish_steps = [
        '拾取者填写物品信息(标题、类别、图片、防伪问题、站点)',
        'FoundItemController验证JWT Token',
        'FoundItemService调用DropPointService验证站点是否可用',
        'FileService验证上传的图片是否有效',
        'FoundItemMapper将物品信息插入found_item表',
        'HandInLogMapper创建交物日志记录(状态:待巡检)',
        '返回物品详情给前端'
    ]
    for i, step in enumerate(publish_steps, 1):
        doc.add_paragraph(f'{i}. {step}', style='List Number')

    doc.add_paragraph()
    add_paragraph_with_indent(doc, '关键业务规则：')
    doc.add_paragraph('物品必须关联到有效的站点', style='List Bullet')
    doc.add_paragraph('发布时自动创建交物日志,状态为"待巡检"', style='List Bullet')
    doc.add_paragraph('站点管理员巡检通过后才发放积分', style='List Bullet')

    doc.add_page_break()

    add_heading_custom(doc, '4.3 失物认领完整流程时序图', 2)

    add_paragraph_with_indent(doc, '认领流程分为三个阶段：')

    doc.add_paragraph()
    doc.add_paragraph('【第一阶段：申请认领】', style='Heading 3')
    apply_steps = [
        '失主查看失物详情并提交认领申请(itemId, answer)',
        'ClaimService验证物品状态是否为"公开待认领"',
        'ClaimService检查用户是否已经申请过该物品',
        'ClaimService计算置信度评分(0-100分)',
        'ClaimApplyMapper插入认领申请记录',
        'FoundItemMapper更新物品状态为"认领中"',
        '返回申请详情给前端'
    ]
    for i, step in enumerate(apply_steps, 1):
        doc.add_paragraph(f'{i}. {step}', style='List Number')

    doc.add_paragraph()
    doc.add_paragraph('【第二阶段：审核认领】', style='Heading 3')
    approve_steps = [
        '站点管理员查看待审核的认领申请列表',
        '站点管理员审核申请(通过/拒绝)',
        'ClaimService验证管理员权限',
        'ClaimApplyMapper更新申请状态为"已通过待取件"',
        'ClaimApplyMapper拒绝该物品的其他认领申请',
        '通知失主审核结果'
    ]
    for i, step in enumerate(approve_steps, 1):
        doc.add_paragraph(f'{i}. {step}', style='List Number')

    doc.add_paragraph()
    doc.add_paragraph('【第三阶段：到站取件】', style='Heading 3')
    pickup_steps = [
        '失主到站点取件,管理员拍照记录',
        '站点管理员上传取件照片和签名',
        'ClaimService验证申请状态为"已通过待取件"',
        'ClaimApplyMapper更新取件信息和申请状态为"已完成"',
        'FoundItemMapper更新物品状态为"已取件"',
        'CreditService发放积分给失主(+1分)',
        'CreditLogMapper记录积分变动日志',
        '更新user表的credit_score字段'
    ]
    for i, step in enumerate(pickup_steps, 1):
        doc.add_paragraph(f'{i}. {step}', style='List Number')

    doc.add_page_break()

    add_heading_custom(doc, '4.4 站点巡检与积分发放时序图', 2)

    add_paragraph_with_indent(doc, '站点巡检流程说明：')
    check_steps = [
        '站点管理员查看待巡检物品列表',
        '站点管理员核实物品并提交巡检结果',
        'AdminService验证交物日志是否存在',
        'AdminService验证管理员权限',
        'HandInLogMapper更新巡检状态为"已巡检"',
        'CreditService发放积分给拾取者(+2分)',
        'CreditLogMapper记录积分变动',
        '更新user表的credit_score字段'
    ]
    for i, step in enumerate(check_steps, 1):
        doc.add_paragraph(f'{i}. {step}', style='List Number')

    doc.add_paragraph()
    add_paragraph_with_indent(doc, '积分发放规则：')
    doc.add_paragraph('拾取者(巡检通过)：+2分', style='List Bullet')
    doc.add_paragraph('失主(取件完成)：+1分', style='List Bullet')
    doc.add_paragraph('积分变动必须记录到credit_log表', style='List Bullet')
    doc.add_paragraph('积分发放与状态更新在同一事务中完成', style='List Bullet')

    doc.add_page_break()

    add_heading_custom(doc, '4.5 时序图说明', 2)

    add_heading_custom(doc, '4.5.1 关键交互流程', 3)

    doc.add_paragraph('1. 认证与授权', style='List Number')
    add_paragraph_with_indent(doc, '所有API请求都需要通过JWT Token验证', 1)
    add_paragraph_with_indent(doc, 'Spring Security拦截器自动验证Token并提取用户ID', 1)
    add_paragraph_with_indent(doc, 'Controller层通过Authentication对象获取当前用户信息', 1)

    doc.add_paragraph('2. 事务管理', style='List Number')
    add_paragraph_with_indent(doc, '涉及多表操作的业务(如取件、巡检)使用@Transactional保证一致性', 1)
    add_paragraph_with_indent(doc, '积分发放与状态更新必须在同一事务中完成', 1)

    doc.add_paragraph('3. 异常处理', style='List Number')
    add_paragraph_with_indent(doc, '所有业务异常统一抛出BusinessException', 1)
    add_paragraph_with_indent(doc, '全局异常处理器捕获并返回统一格式的错误响应', 1)

    doc.add_paragraph('4. 并发控制', style='List Number')
    add_paragraph_with_indent(doc, '认领申请使用数据库唯一索引防止重复申请', 1)
    add_paragraph_with_indent(doc, '物品状态变更使用乐观锁(版本号)防止并发冲突', 1)

    doc.add_page_break()

    # ============================================================
    # 第5章：数据库设计
    # ============================================================
    add_heading_custom(doc, '5. 数据库设计', 1)

    add_heading_custom(doc, '5.1 数据库E-R图', 2)

    add_paragraph_with_indent(doc, '系统数据库包含10张核心业务表：')
    tables = [
        'user - 用户表',
        'drop_point - 招领站点表',
        'found_item - 失物表',
        'hand_in_log - 交物日志表',
        'claim_apply - 认领申请表',
        'credit_log - 积分日志表',
        'lost_notice - 寻物启事表',
        'camera_log - 监控调阅日志表',
        'dispute - 争议处理表',
        'report_log - 举报日志表'
    ]
    for table in tables:
        doc.add_paragraph(table, style='List Bullet')

    doc.add_paragraph()
    add_paragraph_with_indent(doc, '注：完整的E-R图请参考项目文档中的PlantUML源代码。')

    doc.add_page_break()

    add_heading_custom(doc, '5.2 数据库表详细设计', 2)

    # 用户表
    add_heading_custom(doc, '5.2.1 用户表 (user)', 3)

    user_headers = ['字段名', '数据类型', '约束', '说明']
    user_rows = [
        ['id', 'BIGINT', 'PRIMARY KEY, AUTO_INCREMENT', '用户ID'],
        ['username', 'VARCHAR(50)', 'UNIQUE NOT NULL', '用户名'],
        ['password', 'VARCHAR(255)', 'NOT NULL', '密码(BCrypt加密)'],
        ['real_name', 'VARCHAR(50)', 'NULL', '真实姓名'],
        ['student_id', 'VARCHAR(20)', 'UNIQUE', '学号/工号'],
        ['phone', 'VARCHAR(20)', 'NULL', '手机号'],
        ['email', 'VARCHAR(100)', 'NULL', '邮箱'],
        ['role', 'ENUM', 'NOT NULL DEFAULT USER', '角色'],
        ['credit_score', 'INT', 'NOT NULL DEFAULT 0', '诚信积分'],
        ['status', 'TINYINT', 'NOT NULL DEFAULT 1', '状态'],
        ['allow_leaderboard', 'TINYINT', 'NOT NULL DEFAULT 1', '是否允许上榜'],
        ['created_at', 'DATETIME', 'NOT NULL', '创建时间'],
        ['updated_at', 'DATETIME', 'NOT NULL', '更新时间']
    ]
    add_table_from_data(doc, user_headers, user_rows)

    doc.add_paragraph()
    doc.add_paragraph('索引设计：', style='Heading 4')
    doc.add_paragraph('PRIMARY KEY: id', style='List Bullet')
    doc.add_paragraph('UNIQUE KEY: username, student_id', style='List Bullet')
    doc.add_paragraph('INDEX: role, credit_score DESC', style='List Bullet')

    doc.add_paragraph()
    doc.add_paragraph('业务规则：', style='Heading 4')
    doc.add_paragraph('用户名和学号必须唯一', style='List Bullet')
    doc.add_paragraph('密码使用BCrypt加密,强度为10', style='List Bullet')
    doc.add_paragraph('积分不能为负数,通过应用层控制', style='List Bullet')
    doc.add_paragraph('禁用用户不能登录和操作', style='List Bullet')

    doc.add_page_break()

    # 失物表
    add_heading_custom(doc, '5.2.2 失物表 (found_item)', 3)

    item_headers = ['字段名', '数据类型', '约束', '说明']
    item_rows = [
        ['id', 'BIGINT', 'PRIMARY KEY', '物品ID'],
        ['title', 'VARCHAR(200)', 'NOT NULL', '物品标题'],
        ['category', 'VARCHAR(50)', 'NOT NULL', '物品类别'],
        ['description', 'TEXT', 'NULL', '物品描述'],
        ['found_location', 'VARCHAR(255)', 'NOT NULL', '拾取地点'],
        ['found_time', 'DATETIME', 'NOT NULL', '拾取时间'],
        ['images', 'JSON', 'NULL', '物品图片'],
        ['claim_question', 'VARCHAR(500)', 'NOT NULL', '防伪问题'],
        ['perishable', 'TINYINT', 'NOT NULL DEFAULT 0', '是否易腐'],
        ['item_status', 'TINYINT', 'NOT NULL DEFAULT 1', '物品状态'],
        ['founder_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '发布者ID'],
        ['actual_founder_id', 'BIGINT', 'FOREIGN KEY', '实际拾取者ID'],
        ['drop_point_id', 'BIGINT', 'FOREIGN KEY', '所在站点ID'],
        ['published_at', 'DATETIME', 'NOT NULL', '发布时间'],
        ['claimed_at', 'DATETIME', 'NULL', '取件完成时间']
    ]
    add_table_from_data(doc, item_headers, item_rows)

    doc.add_paragraph()
    doc.add_paragraph('物品状态说明：', style='Heading 4')
    status_table = add_table_from_data(doc, ['状态值', '状态名称', '说明'], [
        ['0', '已作废', '物品信息无效'],
        ['1', '公开待认领', '可以申请认领'],
        ['2', '认领中', '有认领申请待审核'],
        ['3', '已取件', '认领完成'],
        ['4', '已归档', '长期未认领归档'],
        ['5', '已过期', '超过保管期限']
    ])

    doc.add_paragraph()
    doc.add_paragraph('业务规则：', style='Heading 4')
    doc.add_paragraph('物品必须关联到具体站点', style='List Bullet')
    doc.add_paragraph('actual_founder_id用于代发布场景', style='List Bullet')
    doc.add_paragraph('易腐物品过期时间为3天,普通物品为30天', style='List Bullet')

    doc.add_page_break()

    # 认领申请表
    add_heading_custom(doc, '5.2.3 认领申请表 (claim_apply)', 3)

    claim_headers = ['字段名', '数据类型', '约束', '说明']
    claim_rows = [
        ['id', 'BIGINT', 'PRIMARY KEY', '申请ID'],
        ['item_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '物品ID'],
        ['source_notice_id', 'BIGINT', 'FOREIGN KEY', '来源寻物启事ID'],
        ['claimer_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '认领者ID'],
        ['answer', 'VARCHAR(500)', 'NOT NULL', '防伪问题答案'],
        ['apply_status', 'TINYINT', 'NOT NULL DEFAULT 0', '申请状态'],
        ['reject_reason', 'VARCHAR(500)', 'NULL', '拒绝理由'],
        ['reject_count', 'INT', 'NOT NULL DEFAULT 0', '被拒绝次数'],
        ['confidence_score', 'INT', 'NULL', '置信度评分(0-100)'],
        ['pickup_time', 'DATETIME', 'NULL', '取件时间'],
        ['pickup_photo', 'VARCHAR(255)', 'NULL', '取件照片URL'],
        ['pickup_signature', 'VARCHAR(255)', 'NULL', '取件签名URL']
    ]
    add_table_from_data(doc, claim_headers, claim_rows)

    doc.add_paragraph()
    doc.add_paragraph('申请状态说明：', style='Heading 4')
    apply_status_table = add_table_from_data(doc, ['状态值', '状态名称', '说明'], [
        ['0', '待审核', '等待站点管理员审核'],
        ['1', '已完成', '已取件完成'],
        ['2', '已拒绝', '审核未通过'],
        ['3', '已锁定', '拒绝次数达到3次'],
        ['4', '已通过待取件', '审核通过,等待取件']
    ])

    doc.add_paragraph()
    doc.add_paragraph('业务规则：', style='Heading 4')
    doc.add_paragraph('同一用户对同一物品只能申请一次', style='List Bullet')
    doc.add_paragraph('拒绝次数达到3次自动锁定', style='List Bullet')
    doc.add_paragraph('置信度评分基于答案相似度、用户信用分综合计算', style='List Bullet')

    doc.add_page_break()

    # 站点表
    add_heading_custom(doc, '5.2.4 站点表 (drop_point)', 3)

    point_headers = ['字段名', '数据类型', '约束', '说明']
    point_rows = [
        ['id', 'BIGINT', 'PRIMARY KEY', '站点ID'],
        ['name', 'VARCHAR(100)', 'NOT NULL', '站点名称'],
        ['location', 'VARCHAR(255)', 'NOT NULL', '位置描述'],
        ['admin_id', 'BIGINT', 'FOREIGN KEY', '管理员ID'],
        ['has_camera', 'TINYINT', 'NOT NULL DEFAULT 0', '是否有监控'],
        ['camera_info', 'VARCHAR(500)', 'NULL', '监控信息(JSON)'],
        ['status', 'TINYINT', 'NOT NULL DEFAULT 1', '状态']
    ]
    add_table_from_data(doc, point_headers, point_rows)

    doc.add_paragraph()
    doc.add_paragraph('业务规则：', style='Heading 4')
    doc.add_paragraph('一个站点只能有一个管理员', style='List Bullet')
    doc.add_paragraph('管理员必须具有POINT_ADMIN角色', style='List Bullet')
    doc.add_paragraph('停用站点不能接收新物品', style='List Bullet')

    doc.add_page_break()

    # 交物日志表
    add_heading_custom(doc, '5.2.5 交物日志表 (hand_in_log)', 3)

    handin_headers = ['字段名', '数据类型', '约束', '说明']
    handin_rows = [
        ['id', 'BIGINT', 'PRIMARY KEY', '日志ID'],
        ['item_id', 'BIGINT', 'FOREIGN KEY UNIQUE', '物品ID'],
        ['drop_point_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '站点ID'],
        ['hand_in_status', 'TINYINT', 'NOT NULL DEFAULT 1', '巡检状态'],
        ['handed_in_at', 'DATETIME', 'NULL', '登记时间'],
        ['checked_at', 'DATETIME', 'NULL', '巡检时间'],
        ['checked_by', 'BIGINT', 'FOREIGN KEY', '巡检人ID'],
        ['check_note', 'VARCHAR(500)', 'NULL', '巡检备注'],
        ['credit_issued', 'TINYINT', 'NOT NULL DEFAULT 0', '是否已发放积分']
    ]
    add_table_from_data(doc, handin_headers, handin_rows)

    doc.add_paragraph()
    doc.add_paragraph('巡检状态说明：', style='Heading 4')
    doc.add_paragraph('1-待巡检: 物品已提交待核实', style='List Bullet')
    doc.add_paragraph('2-已巡检: 巡检通过,已发放积分', style='List Bullet')
    doc.add_paragraph('3-已作废: 物品信息不实,不发放积分', style='List Bullet')

    doc.add_paragraph()
    doc.add_paragraph('业务规则：', style='Heading 4')
    doc.add_paragraph('一个物品只有一条交物日志', style='List Bullet')
    doc.add_paragraph('巡检通过后发放积分给拾取者(+2分)', style='List Bullet')

    doc.add_page_break()

    # 积分日志表
    add_heading_custom(doc, '5.2.6 积分日志表 (credit_log)', 3)

    credit_headers = ['字段名', '数据类型', '约束', '说明']
    credit_rows = [
        ['id', 'BIGINT', 'PRIMARY KEY', '日志ID'],
        ['user_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '用户ID'],
        ['change_amount', 'INT', 'NOT NULL', '变动积分'],
        ['operation_type', 'VARCHAR(50)', 'NOT NULL', '操作类型'],
        ['related_item_id', 'BIGINT', 'FOREIGN KEY', '关联物品ID'],
        ['related_apply_id', 'BIGINT', 'FOREIGN KEY', '关联认领申请ID'],
        ['reason', 'VARCHAR(500)', 'NULL', '变动原因'],
        ['operator_id', 'BIGINT', 'FOREIGN KEY', '操作人ID']
    ]
    add_table_from_data(doc, credit_headers, credit_rows)

    doc.add_paragraph()
    doc.add_paragraph('操作类型说明：', style='Heading 4')
    op_table = add_table_from_data(doc, ['类型', '说明', '积分变动'], [
        ['CHECK_ISSUE', '巡检发放', '+2(拾取者)'],
        ['PICKUP_ISSUE', '取件发放', '+1(失主)'],
        ['ROLLBACK', '积分回滚', '负数'],
        ['DEDUCT', '积分扣除', '负数'],
        ['ADMIN_ADJUST', '管理员调整', '正负均可']
    ])

    doc.add_paragraph()
    doc.add_paragraph('业务规则：', style='Heading 4')
    doc.add_paragraph('所有积分变动必须记录到此表', style='List Bullet')
    doc.add_paragraph('积分不可直接删除,只能通过回滚操作', style='List Bullet')
    doc.add_paragraph('积分变动后同步更新user.credit_score', style='List Bullet')

    doc.add_page_break()

    # 其他表简述
    add_heading_custom(doc, '5.2.7 其他核心表', 3)

    doc.add_paragraph('寻物启事表 (lost_notice)', style='Heading 4')
    add_paragraph_with_indent(doc, '记录用户发布的寻物启事,支持与失物的智能匹配。启事可以直接转化为认领申请。')

    doc.add_paragraph()
    doc.add_paragraph('监控调阅日志表 (camera_log)', style='Heading 4')
    add_paragraph_with_indent(doc, '记录监控调阅申请,需要管理员审核。只有配备监控的站点才支持调阅功能。')

    doc.add_paragraph()
    doc.add_paragraph('争议处理表 (dispute)', style='Heading 4')
    add_paragraph_with_indent(doc, '记录用户提交的争议申诉,由系统管理员处理。争议类型包括:认领错误、拒绝不公等。')

    doc.add_paragraph()
    doc.add_paragraph('举报日志表 (report_log)', style='Heading 4')
    add_paragraph_with_indent(doc, '记录用户举报的违规行为,如虚假物品、恶意骗取等。举报核实后可能扣除积分或禁用账号。')

    doc.add_page_break()

    add_heading_custom(doc, '5.3 数据库约束与完整性', 2)

    add_heading_custom(doc, '5.3.1 参照完整性约束', 3)

    add_paragraph_with_indent(doc, '外键约束策略：')
    doc.add_paragraph('用户删除时:found_item表采用CASCADE(级联删除)', style='List Bullet')
    doc.add_paragraph('用户删除时:drop_point表采用SET NULL(管理员字段置空)', style='List Bullet')
    doc.add_paragraph('物品删除时:claim_apply表采用CASCADE(级联删除认领申请)', style='List Bullet')
    doc.add_paragraph('物品删除时:hand_in_log表采用CASCADE(级联删除交物日志)', style='List Bullet')

    doc.add_paragraph()
    add_heading_custom(doc, '5.3.2 唯一性约束', 3)

    unique_constraints = [
        'user表: username(用户名唯一), student_id(学号唯一)',
        'claim_apply表: (item_id, claimer_id)联合唯一(同一用户对同一物品只能申请一次)',
        'hand_in_log表: item_id唯一(一个物品只有一条交物日志)'
    ]
    for constraint in unique_constraints:
        doc.add_paragraph(constraint, style='List Bullet')

    doc.add_paragraph()
    add_heading_custom(doc, '5.3.3 业务规则约束', 3)

    add_paragraph_with_indent(doc, '通过应用层逻辑保证以下规则：')
    doc.add_paragraph('积分不能为负数', style='List Bullet')
    doc.add_paragraph('拒绝次数超过3次自动锁定申请', style='List Bullet')
    doc.add_paragraph('物品状态只能按规定流转', style='List Bullet')
    doc.add_paragraph('易腐物品过期时间为3天,普通物品为30天', style='List Bullet')

    doc.add_page_break()

    add_heading_custom(doc, '5.4 数据库性能优化', 2)

    add_heading_custom(doc, '5.4.1 索引优化策略', 3)

    add_paragraph_with_indent(doc, '针对常用查询场景设计索引：')

    doc.add_paragraph()
    doc.add_paragraph('公开物品列表查询', style='Heading 4')
    doc.add_paragraph('使用索引: idx_item_status, idx_category, idx_published_at', style='List Bullet')
    doc.add_paragraph('支持按状态、类别筛选,按发布时间排序', style='List Bullet')

    doc.add_paragraph()
    doc.add_paragraph('用户积分排行榜', style='Heading 4')
    doc.add_paragraph('使用索引: idx_credit_score DESC', style='List Bullet')
    doc.add_paragraph('支持快速查询TOP100用户', style='List Bullet')

    doc.add_paragraph()
    doc.add_paragraph('站点待巡检物品查询', style='Heading 4')
    doc.add_paragraph('使用索引: idx_drop_point_id, idx_hand_in_status', style='List Bullet')
    doc.add_paragraph('支持站点管理员快速查看待处理任务', style='List Bullet')

    doc.add_paragraph()
    add_heading_custom(doc, '5.4.2 缓存策略', 3)

    add_paragraph_with_indent(doc, 'Redis缓存设计：')

    cache_table = add_table_from_data(doc, ['缓存Key', '数据类型', 'TTL', '说明'], [
        ['user:{userId}', 'String (JSON)', '30分钟', '用户信息'],
        ['item:{itemId}', 'String (JSON)', '5分钟', '物品详情'],
        ['leaderboard:credit', 'Sorted Set', '10分钟', '积分排行榜'],
        ['drop_points:active', 'String (JSON)', '1小时', '活跃站点列表'],
        ['jwt:blacklist:{token}', 'String', 'Token有效期', 'JWT黑名单']
    ])

    doc.add_paragraph()
    add_paragraph_with_indent(doc, '缓存更新策略：')
    doc.add_paragraph('用户信息变更: 删除缓存,下次查询时重建', style='List Bullet')
    doc.add_paragraph('物品状态变更: 删除物品缓存和列表缓存', style='List Bullet')
    doc.add_paragraph('积分变更: 异步更新排行榜缓存', style='List Bullet')

    doc.add_page_break()

    add_heading_custom(doc, '5.5 数据库安全设计', 2)

    add_heading_custom(doc, '5.5.1 访问控制', 3)

    add_paragraph_with_indent(doc, '数据库账号权限设计：')

    doc.add_paragraph()
    doc.add_paragraph('应用程序账号 (lostfound_app)', style='Heading 4')
    doc.add_paragraph('权限: SELECT, INSERT, UPDATE', style='List Bullet')
    doc.add_paragraph('不授予DELETE和DROP权限', style='List Bullet')
    doc.add_paragraph('遵循最小权限原则', style='List Bullet')

    doc.add_paragraph()
    doc.add_paragraph('只读账号 (lostfound_readonly)', style='Heading 4')
    doc.add_paragraph('权限: SELECT', style='List Bullet')
    doc.add_paragraph('用于数据分析和报表查询', style='List Bullet')

    doc.add_paragraph()
    add_heading_custom(doc, '5.5.2 敏感数据保护', 3)

    sensitive_table = add_table_from_data(doc, ['数据类型', '保护措施'], [
        ['用户密码', 'BCrypt加密(强度10)'],
        ['手机号/邮箱', '应用层脱敏展示(135****8888)'],
        ['学号', '部分脱敏(202001****)'],
        ['取件照片', '存储路径包含随机UUID,防止遍历']
    ])

    doc.add_paragraph()
    add_heading_custom(doc, '5.5.3 审计日志', 3)

    add_paragraph_with_indent(doc, '记录所有敏感操作到审计日志：')
    doc.add_paragraph('管理员操作(积分调整、用户禁用、争议处理)', style='List Bullet')
    doc.add_paragraph('批量删除操作', style='List Bullet')
    doc.add_paragraph('权限变更操作', style='List Bullet')
    doc.add_paragraph('监控调阅申请', style='List Bullet')

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计报告.docx'
    doc.save(output_path)
    print(f'Word文档已生成: {output_path}')

if __name__ == '__main__':
    main()
