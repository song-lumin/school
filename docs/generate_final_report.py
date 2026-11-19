#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
生成系统分析与设计报告Word文档 - 第3、4、5.5章
包含UML类图、时序图的文字描述和表格形式
"""

from docx import Document
from docx.shared import Pt, Inches, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.enum.style import WD_STYLE_TYPE

def setup_document():
    """初始化文档"""
    doc = Document()

    # 设置页面边距
    sections = doc.sections
    for section in sections:
        section.top_margin = Cm(2.54)
        section.bottom_margin = Cm(2.54)
        section.left_margin = Cm(3.17)
        section.right_margin = Cm(3.17)

    # 设置默认字体
    style = doc.styles['Normal']
    style.font.name = '宋体'
    style.font.size = Pt(12)
    style.paragraph_format.line_spacing = 1.5

    return doc

def add_title(doc, text, level=1):
    """添加标题"""
    if level == 0:
        heading = doc.add_heading(text, level=0)
        heading.alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in heading.runs:
            run.font.size = Pt(22)
            run.font.bold = True
    else:
        heading = doc.add_heading(text, level=level)
        for run in heading.runs:
            if level == 1:
                run.font.size = Pt(16)
            elif level == 2:
                run.font.size = Pt(14)
            else:
                run.font.size = Pt(12)
    return heading

def add_text(doc, text, bold=False, indent=0):
    """添加文本段落"""
    p = doc.add_paragraph(text)
    if bold:
        for run in p.runs:
            run.font.bold = True
    if indent > 0:
        p.paragraph_format.left_indent = Cm(indent * 0.75)
    return p

def create_table(doc, data, has_header=True):
    """创建表格"""
    rows = len(data)
    cols = len(data[0]) if data else 0

    table = doc.add_table(rows=rows, cols=cols)
    table.style = 'Light Grid Accent 1'

    for i, row_data in enumerate(data):
        row = table.rows[i]
        for j, cell_data in enumerate(row_data):
            cell = row.cells[j]
            cell.text = str(cell_data)
            # 表头加粗
            if has_header and i == 0:
                for paragraph in cell.paragraphs:
                    for run in paragraph.runs:
                        run.font.bold = True

    return table

def main():
    doc = setup_document()

    # 封面
    add_title(doc, '校园失物招领及诚信积分系统', 0)
    add_title(doc, '系统分析与设计报告', 0)
    doc.add_paragraph()
    doc.add_paragraph()

    # ================== 第3章 系统设计 - 领域模型 ==================
    add_title(doc, '第3章 系统设计 - 领域模型（类图）', 1)

    add_title(doc, '3.1 系统领域模型概述', 2)
    add_text(doc, '本系统的领域模型包含以下核心实体类，它们之间通过关联、聚合等关系相互协作，共同实现校园失物招领及诚信积分管理功能。')
    doc.add_paragraph()

    # 核心类列表表格
    add_text(doc, '表3-1 系统核心实体类列表', bold=True)
    class_table_data = [
        ['类名', '中文名称', '职责说明'],
        ['User', '用户类', '管理用户基本信息、角色权限、诚信积分'],
        ['FoundItem', '失物类', '记录拾取物品的详细信息'],
        ['ClaimApply', '认领申请类', '管理物品认领申请流程'],
        ['DropPoint', '招领站点类', '管理物理站点信息'],
        ['CreditLog', '积分日志类', '记录所有积分变动，支持审计'],
        ['HandInLog', '交物日志类', '记录物品交付到站点的日志'],
        ['LostNotice', '寻物启事类', '记录用户发布的寻物启事'],
        ['CameraLog', '监控调阅日志类', '记录监控调阅申请和审批'],
        ['Dispute', '争议处理类', '记录争议申诉和处理结果'],
        ['ReportLog', '举报日志类', '记录用户举报和处理情况']
    ]
    create_table(doc, class_table_data)
    doc.add_paragraph()

    add_title(doc, '3.2 核心类详细设计', 2)

    # User类
    add_title(doc, '3.2.1 User（用户类）', 3)
    add_text(doc, '【类职责】管理用户基本信息、角色权限和诚信积分。')
    add_text(doc, '【属性列表】', bold=True)

    user_attrs = [
        ['属性名', '类型', '说明'],
        ['id', 'Long', '用户唯一标识'],
        ['username', 'String', '用户名（唯一）'],
        ['password', 'String', '密码（BCrypt加密）'],
        ['realName', 'String', '真实姓名'],
        ['studentId', 'String', '学号/工号（唯一）'],
        ['phone', 'String', '手机号'],
        ['email', 'String', '邮箱'],
        ['role', 'UserRole', '角色（USER/POINT_ADMIN/SYS_ADMIN）'],
        ['creditScore', 'Integer', '诚信积分'],
        ['status', 'Integer', '账号状态（0-禁用，1-正常）'],
        ['allowLeaderboard', 'Integer', '是否允许上榜（0-否，1-是）'],
        ['createdAt', 'LocalDateTime', '创建时间'],
        ['updatedAt', 'LocalDateTime', '更新时间']
    ]
    create_table(doc, user_attrs)
    doc.add_paragraph()

    add_text(doc, '【关系】', bold=True)
    add_text(doc, '• 一个用户可以发布多个失物（1对多关联到FoundItem）', indent=1)
    add_text(doc, '• 一个用户可以提交多个认领申请（1对多关联到ClaimApply）', indent=1)
    add_text(doc, '• 一个用户可以发布多个寻物启事（1对多关联到LostNotice）', indent=1)
    add_text(doc, '• 一个站点管理员管理一个站点（1对1关联到DropPoint）', indent=1)
    doc.add_paragraph()

    # FoundItem类
    add_title(doc, '3.2.2 FoundItem（失物类）', 3)
    add_text(doc, '【类职责】记录拾取物品的详细信息，包括物品描述、图片、防伪问题等。')
    add_text(doc, '【属性列表】', bold=True)

    item_attrs = [
        ['属性名', '类型', '说明'],
        ['id', 'Long', '物品唯一标识'],
        ['title', 'String', '物品标题'],
        ['category', 'String', '物品类别'],
        ['description', 'String', '物品描述'],
        ['foundLocation', 'String', '拾取地点'],
        ['foundTime', 'LocalDateTime', '拾取时间'],
        ['images', 'List<String>', '物品图片URL列表'],
        ['claimQuestion', 'String', '防伪问题'],
        ['perishable', 'Integer', '是否易腐（0-否，1-是）'],
        ['itemStatus', 'Integer', '物品状态（见下表）'],
        ['founderId', 'Long', '发布者ID'],
        ['actualFounderId', 'Long', '实际拾取者ID（代发布场景）'],
        ['dropPointId', 'Long', '所在站点ID'],
        ['publishedAt', 'LocalDateTime', '发布时间'],
        ['claimedAt', 'LocalDateTime', '取件完成时间']
    ]
    create_table(doc, item_attrs)
    doc.add_paragraph()

    add_text(doc, '表3-2 物品状态枚举值', bold=True)
    item_status = [
        ['状态值', '状态名称', '说明'],
        ['0', '已作废', '物品信息无效'],
        ['1', '公开待认领', '可以申请认领'],
        ['2', '认领中', '有认领申请待审核'],
        ['3', '已取件', '认领完成'],
        ['4', '已归档', '长期未认领归档'],
        ['5', '已过期', '超过保管期限']
    ]
    create_table(doc, item_status)
    doc.add_paragraph()

    add_text(doc, '【关系】', bold=True)
    add_text(doc, '• 一个失物对应多个认领申请（1对多关联到ClaimApply）', indent=1)
    add_text(doc, '• 一个失物对应一条交物日志（1对1关联到HandInLog）', indent=1)
    add_text(doc, '• 多个失物存放在一个站点（多对1关联到DropPoint）', indent=1)
    doc.add_paragraph()

    # ClaimApply类
    add_title(doc, '3.2.3 ClaimApply（认领申请类）', 3)
    add_text(doc, '【类职责】管理物品认领申请的完整流程，包括申请提交、审核、取件等环节。')
    add_text(doc, '【核心属性】', bold=True)

    claim_attrs = [
        ['属性名', '类型', '说明'],
        ['id', 'Long', '申请唯一标识'],
        ['itemId', 'Long', '物品ID'],
        ['claimerId', 'Long', '认领者ID'],
        ['answer', 'String', '防伪问题答案'],
        ['applyStatus', 'Integer', '申请状态（见下表）'],
        ['rejectReason', 'String', '拒绝理由'],
        ['rejectCount', 'Integer', '被拒绝次数（≥3次自动锁定）'],
        ['confidenceScore', 'Integer', '置信度评分（0-100）'],
        ['pickupTime', 'LocalDateTime', '取件时间'],
        ['pickupPhoto', 'String', '取件照片URL'],
        ['pickupSignature', 'String', '取件签名URL'],
        ['creditIssued', 'Integer', '是否已发放积分']
    ]
    create_table(doc, claim_attrs)
    doc.add_paragraph()

    add_text(doc, '表3-3 认领申请状态枚举值', bold=True)
    claim_status = [
        ['状态值', '状态名称', '说明'],
        ['0', '待审核', '等待站点管理员审核'],
        ['1', '已完成', '已取件完成'],
        ['2', '已拒绝', '审核未通过'],
        ['3', '已锁定', '拒绝次数达到3次'],
        ['4', '已通过待取件', '审核通过，等待取件']
    ]
    create_table(doc, claim_status)
    doc.add_paragraph()

    # 其他核心类简述
    add_title(doc, '3.2.4 其他核心类', 3)

    add_text(doc, '（1）DropPoint（招领站点类）', bold=True)
    add_text(doc, '管理物理站点信息，包括站点名称、位置、管理员、监控设备等。每个站点由一名站点管理员负责日常运营。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（2）CreditLog（积分日志类）', bold=True)
    add_text(doc, '记录所有积分变动记录，支持追溯和审计。操作类型包括：CHECK_ISSUE（巡检发放+2分）、PICKUP_ISSUE（取件发放+1分）、ROLLBACK（积分回滚）、DEDUCT（积分扣除）。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（3）HandInLog（交物日志类）', bold=True)
    add_text(doc, '记录物品交付到站点的日志，包括巡检状态、巡检人、巡检备注等。巡检通过后发放积分给拾取者。', indent=1)
    doc.add_paragraph()

    add_title(doc, '3.3 类关系图（文字描述）', 2)
    add_text(doc, '由于UML工具限制，以下使用文字描述核心类之间的关系：')
    doc.add_paragraph()

    relations = [
        ['关系类型', '起始类', '目标类', '多重性', '说明'],
        ['关联', 'User', 'FoundItem', '1对多', '一个用户可以发布多个失物'],
        ['关联', 'User', 'ClaimApply', '1对多', '一个用户可以提交多个认领申请'],
        ['关联', 'User', 'LostNotice', '1对多', '一个用户可以发布多个寻物启事'],
        ['关联', 'User', 'DropPoint', '1对1', '一个站点管理员管理一个站点'],
        ['关联', 'FoundItem', 'ClaimApply', '1对多', '一个失物可以有多个认领申请'],
        ['关联', 'FoundItem', 'HandInLog', '1对1', '一个失物对应一条交物日志'],
        ['关联', 'FoundItem', 'DropPoint', '多对1', '多个失物存放在一个站点'],
        ['关联', 'ClaimApply', 'Dispute', '1对多', '一个认领申请可能产生多个争议'],
        ['关联', 'LostNotice', 'ClaimApply', '1对多', '一个寻物启事可以转化为认领申请'],
        ['聚合', 'CreditLog', 'User', '多对1', '多条积分日志属于一个用户']
    ]
    create_table(doc, relations)
    doc.add_paragraph()
    doc.add_paragraph()

    # ================== 第4章 系统设计 - 对象交互模型 ==================
    add_title(doc, '第4章 系统设计 - 对象交互模型（时序图）', 1)

    add_title(doc, '4.1 用户注册登录时序图', 2)
    add_text(doc, '用户注册和登录是系统的基础功能，采用JWT Token进行无状态认证。')
    doc.add_paragraph()

    add_text(doc, '【用户注册流程】', bold=True)
    register_seq = [
        ['序号', '参与对象', '操作描述'],
        ['1', '用户', '填写注册信息（用户名、密码、学号等）并提交'],
        ['2', 'AuthController', '接收注册请求，调用UserService.register()'],
        ['3', 'UserService', '验证用户名是否已存在（查询数据库）'],
        ['4', 'UserService', '验证学号是否已存在（查询数据库）'],
        ['5', 'PasswordEncoder', '使用BCrypt算法加密密码'],
        ['6', 'UserMapper', '将用户信息插入user表'],
        ['7', 'UserService', '生成JWT Token（有效期24小时）'],
        ['8', 'AuthController', '返回Token和用户信息给前端'],
        ['9', '用户', '注册成功，自动登录']
    ]
    create_table(doc, register_seq)
    doc.add_paragraph()

    add_text(doc, '【用户登录流程】', bold=True)
    login_seq = [
        ['序号', '参与对象', '操作描述'],
        ['1', '用户', '输入用户名和密码'],
        ['2', 'AuthController', '接收登录请求，调用UserService.login()'],
        ['3', 'UserMapper', '根据用户名查询用户信息'],
        ['4', 'UserService', '验证用户是否存在'],
        ['5', 'PasswordEncoder', '验证密码是否匹配'],
        ['6', 'UserService', '生成JWT Token'],
        ['7', 'AuthController', '返回Token和用户信息'],
        ['8', '用户', '保存Token到本地，后续请求携带Token']
    ]
    create_table(doc, login_seq)
    doc.add_paragraph()

    add_text(doc, '【关键技术点】', bold=True)
    add_text(doc, '• 密码加密：使用BCrypt算法，强度为10，每次加密结果不同但都能验证', indent=1)
    add_text(doc, '• JWT Token：包含用户ID和角色信息，有效期24小时，前端存储在localStorage', indent=1)
    add_text(doc, '• 唯一性约束：用户名和学号通过数据库唯一索引保证不重复', indent=1)
    doc.add_paragraph()

    add_title(doc, '4.2 失物发布时序图', 2)
    add_text(doc, '拾取者发现物品后，通过系统发布失物信息，物品将被送到指定站点等待认领。')
    doc.add_paragraph()

    publish_seq = [
        ['序号', '参与对象', '操作描述'],
        ['1', '拾取者', '填写物品信息（标题、类别、图片、防伪问题、站点）'],
        ['2', 'FoundItemController', '验证JWT Token，提取用户ID'],
        ['3', 'FoundItemService', '调用DropPointService验证站点是否可用'],
        ['4', 'DropPointService', '查询站点信息，验证状态为"正常"'],
        ['5', 'FileService', '验证上传的图片URL是否有效'],
        ['6', 'FoundItemMapper', '插入物品信息到found_item表'],
        ['7', 'HandInLogMapper', '创建交物日志，状态为"待巡检"'],
        ['8', 'FoundItemController', '返回物品详情（含物品ID）给前端'],
        ['9', '拾取者', '发布成功，等待站点管理员巡检']
    ]
    create_table(doc, publish_seq)
    doc.add_paragraph()

    add_text(doc, '【业务规则】', bold=True)
    add_text(doc, '• 物品必须关联到有效的站点（status=1）', indent=1)
    add_text(doc, '• 发布时自动创建交物日志，初始状态为"待巡检"', indent=1)
    add_text(doc, '• 支持代发布：actualFounderId记录实际拾取者，founderId为发布者', indent=1)
    add_text(doc, '• 防伪问题必填，用于后续认领时验证失主身份', indent=1)
    doc.add_paragraph()

    add_title(doc, '4.3 失物认领完整流程时序图', 2)
    add_text(doc, '认领流程分为三个阶段：申请认领、审核认领、到站取件。整个流程涉及失主、站点管理员和系统的协同交互。')
    doc.add_paragraph()

    add_text(doc, '【第一阶段：申请认领】', bold=True)
    apply_seq = [
        ['序号', '参与对象', '操作描述'],
        ['1', '失主', '浏览失物列表，找到疑似物品'],
        ['2', '失主', '点击"申请认领"，填写防伪问题答案'],
        ['3', 'ClaimController', '接收认领申请请求'],
        ['4', 'ClaimService', '验证物品状态是否为"公开待认领"'],
        ['5', 'ClaimService', '验证用户是否已申请过该物品（唯一索引）'],
        ['6', 'ClaimService', '计算置信度评分（基于答案相似度、用户信用分）'],
        ['7', 'ClaimApplyMapper', '插入认领申请记录'],
        ['8', 'FoundItemMapper', '更新物品状态为"认领中"'],
        ['9', 'ClaimController', '返回申请详情（含置信度评分）'],
        ['10', '失主', '申请提交成功，等待审核']
    ]
    create_table(doc, apply_seq)
    doc.add_paragraph()

    add_text(doc, '【第二阶段：审核认领】', bold=True)
    approve_seq = [
        ['序号', '参与对象', '操作描述'],
        ['1', '站点管理员', '查看待审核的认领申请列表'],
        ['2', '站点管理员', '对比物品和答案，决定通过或拒绝'],
        ['3', 'ClaimController', '接收审核请求（approve或reject）'],
        ['4', 'ClaimService', '验证管理员是否有权限审核该物品'],
        ['5', 'ClaimApplyMapper', '更新申请状态为"已通过待取件"或"已拒绝"'],
        ['6', 'ClaimApplyMapper', '若通过，拒绝该物品的其他所有申请'],
        ['7', 'ClaimService', '若拒绝，检查拒绝次数是否≥3，若是则锁定'],
        ['8', 'ClaimController', '返回审核结果'],
        ['9', '失主', '收到通知（通过或拒绝）']
    ]
    create_table(doc, approve_seq)
    doc.add_paragraph()

    add_text(doc, '【第三阶段：到站取件】', bold=True)
    pickup_seq = [
        ['序号', '参与对象', '操作描述'],
        ['1', '失主', '携带证件到站点取件'],
        ['2', '站点管理员', '核实身份，拍摄取件照片'],
        ['3', '站点管理员', '上传取件照片和电子签名'],
        ['4', 'ClaimController', '接收取件请求'],
        ['5', 'ClaimService', '验证申请状态为"已通过待取件"'],
        ['6', 'ClaimApplyMapper', '更新取件信息和申请状态为"已完成"'],
        ['7', 'FoundItemMapper', '更新物品状态为"已取件"'],
        ['8', 'CreditService', '发放积分给失主（+1分）'],
        ['9', 'CreditLogMapper', '记录积分变动日志'],
        ['10', 'UserMapper', '更新user表的credit_score字段'],
        ['11', 'ClaimController', '返回取件完成信息'],
        ['12', '失主', '取件完成，获得1积分']
    ]
    create_table(doc, pickup_seq)
    doc.add_paragraph()

    add_text(doc, '【事务处理】', bold=True)
    add_text(doc, '取件操作涉及多表更新，使用@Transactional注解保证原子性：', indent=1)
    add_text(doc, '• 更新认领申请状态', indent=2)
    add_text(doc, '• 更新物品状态', indent=2)
    add_text(doc, '• 插入积分日志', indent=2)
    add_text(doc, '• 更新用户积分', indent=2)
    add_text(doc, '以上操作要么全部成功，要么全部回滚，确保数据一致性。', indent=1)
    doc.add_paragraph()

    add_title(doc, '4.4 站点巡检与积分发放时序图', 2)
    add_text(doc, '物品送到站点后，站点管理员需要进行巡检确认，巡检通过后系统自动发放积分给拾取者。')
    doc.add_paragraph()

    check_seq = [
        ['序号', '参与对象', '操作描述'],
        ['1', '站点管理员', '查看待巡检物品列表'],
        ['2', '站点管理员', '核实物品信息，填写巡检备注'],
        ['3', 'AdminController', '接收巡检请求'],
        ['4', 'AdminService', '查询交物日志，验证是否存在'],
        ['5', 'AdminService', '验证管理员是否为该站点管理员'],
        ['6', 'AdminService', '验证巡检状态是否为"待巡检"'],
        ['7', 'HandInLogMapper', '更新巡检状态为"已巡检"，记录巡检人和时间'],
        ['8', 'AdminService', '调用CreditService发放积分'],
        ['9', 'CreditService', '插入积分日志（操作类型=CHECK_ISSUE，金额=+2）'],
        ['10', 'UserMapper', '更新拾取者的credit_score字段'],
        ['11', 'HandInLogMapper', '更新credit_issued字段为1'],
        ['12', 'AdminController', '返回巡检完成信息'],
        ['13', '站点管理员', '巡检完成']
    ]
    create_table(doc, check_seq)
    doc.add_paragraph()

    add_text(doc, '【积分发放规则】', bold=True)
    credit_rules = [
        ['角色', '触发条件', '积分变动', '操作类型'],
        ['拾取者', '站点巡检通过', '+2分', 'CHECK_ISSUE'],
        ['失主', '取件完成', '+1分', 'PICKUP_ISSUE'],
        ['违规用户', '管理员处罚', '-5分或-10分', 'DEDUCT'],
        ['争议受害者', '争议受理', '回滚相关积分', 'ROLLBACK']
    ]
    create_table(doc, credit_rules)
    doc.add_paragraph()

    add_title(doc, '4.5 时序图设计说明', 2)

    add_text(doc, '【认证与授权机制】', bold=True)
    add_text(doc, '• 所有API请求都需要携带JWT Token（登录和注册除外）', indent=1)
    add_text(doc, '• Spring Security拦截器自动验证Token有效性', indent=1)
    add_text(doc, '• Controller层通过Authentication对象获取当前用户ID和角色', indent=1)
    add_text(doc, '• 角色权限在Service层进一步校验（如站点管理员只能审核自己站点的申请）', indent=1)
    doc.add_paragraph()

    add_text(doc, '【异常处理策略】', bold=True)
    add_text(doc, '• 所有业务异常统一抛出BusinessException', indent=1)
    add_text(doc, '• 全局异常处理器@ControllerAdvice捕获异常', indent=1)
    add_text(doc, '• 返回统一格式的JSON响应：{code, message, data}', indent=1)
    add_text(doc, '• HTTP状态码：200成功，400业务错误，401未认证，403无权限，500系统错误', indent=1)
    doc.add_paragraph()

    add_text(doc, '【并发控制】', bold=True)
    add_text(doc, '• 认领申请：数据库唯一索引(item_id, claimer_id)防止重复提交', indent=1)
    add_text(doc, '• 物品状态变更：乐观锁（version字段）防止并发冲突', indent=1)
    add_text(doc, '• 积分发放：分布式锁（Redis）防止重复发放', indent=1)
    doc.add_paragraph()
    doc.add_paragraph()

    # ================== 第5.5章 数据库设计 ==================
    add_title(doc, '第5章 数据库设计（节选5.5）', 1)

    add_title(doc, '5.5 数据库表详细设计', 2)

    # 用户表
    add_title(doc, '5.5.1 用户表（user）', 3)
    user_table = [
        ['字段名', '数据类型', '约束', '说明'],
        ['id', 'BIGINT', 'PRIMARY KEY, AUTO_INCREMENT', '用户ID'],
        ['username', 'VARCHAR(50)', 'UNIQUE NOT NULL', '用户名'],
        ['password', 'VARCHAR(255)', 'NOT NULL', '密码（BCrypt加密）'],
        ['real_name', 'VARCHAR(50)', 'NULL', '真实姓名'],
        ['student_id', 'VARCHAR(20)', 'UNIQUE', '学号/工号'],
        ['phone', 'VARCHAR(20)', 'NULL', '手机号'],
        ['email', 'VARCHAR(100)', 'NULL', '邮箱'],
        ['role', 'ENUM', 'NOT NULL DEFAULT \'USER\'', '角色：USER/POINT_ADMIN/SYS_ADMIN'],
        ['credit_score', 'INT', 'NOT NULL DEFAULT 0', '诚信积分'],
        ['status', 'TINYINT', 'NOT NULL DEFAULT 1', '状态：0-禁用，1-正常'],
        ['allow_leaderboard', 'TINYINT', 'NOT NULL DEFAULT 1', '是否允许上榜'],
        ['created_at', 'DATETIME', 'NOT NULL DEFAULT CURRENT_TIMESTAMP', '创建时间'],
        ['updated_at', 'DATETIME', 'NOT NULL ON UPDATE CURRENT_TIMESTAMP', '更新时间']
    ]
    create_table(doc, user_table)
    doc.add_paragraph()

    add_text(doc, '索引设计：', bold=True)
    add_text(doc, '• PRIMARY KEY (id)：主键索引', indent=1)
    add_text(doc, '• UNIQUE KEY uk_username (username)：用户名唯一索引', indent=1)
    add_text(doc, '• UNIQUE KEY uk_student_id (student_id)：学号唯一索引', indent=1)
    add_text(doc, '• INDEX idx_role (role)：角色查询索引', indent=1)
    add_text(doc, '• INDEX idx_credit_score (credit_score DESC)：积分排行榜索引', indent=1)
    doc.add_paragraph()

    add_text(doc, '业务规则：', bold=True)
    add_text(doc, '• 用户名和学号必须唯一，通过数据库唯一索引保证', indent=1)
    add_text(doc, '• 密码使用BCrypt加密，强度为10', indent=1)
    add_text(doc, '• 积分不能为负数，通过应用层控制', indent=1)
    add_text(doc, '• 禁用用户（status=0）不能登录和执行任何操作', indent=1)
    doc.add_paragraph()

    # 失物表
    add_title(doc, '5.5.2 失物表（found_item）', 3)
    item_table = [
        ['字段名', '数据类型', '约束', '说明'],
        ['id', 'BIGINT', 'PRIMARY KEY, AUTO_INCREMENT', '物品ID'],
        ['title', 'VARCHAR(200)', 'NOT NULL', '物品标题'],
        ['category', 'VARCHAR(50)', 'NOT NULL', '物品类别'],
        ['description', 'TEXT', 'NULL', '物品描述'],
        ['found_location', 'VARCHAR(255)', 'NOT NULL', '拾取地点'],
        ['found_time', 'DATETIME', 'NOT NULL', '拾取时间'],
        ['images', 'JSON', 'NULL', '物品图片（JSON数组）'],
        ['claim_question', 'VARCHAR(500)', 'NOT NULL', '防伪问题'],
        ['perishable', 'TINYINT', 'NOT NULL DEFAULT 0', '是否易腐'],
        ['item_status', 'TINYINT', 'NOT NULL DEFAULT 1', '物品状态'],
        ['founder_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '发布者ID'],
        ['actual_founder_id', 'BIGINT', 'FOREIGN KEY', '实际拾取者ID'],
        ['drop_point_id', 'BIGINT', 'FOREIGN KEY', '所在站点ID'],
        ['published_at', 'DATETIME', 'NOT NULL DEFAULT CURRENT_TIMESTAMP', '发布时间'],
        ['claimed_at', 'DATETIME', 'NULL', '取件完成时间']
    ]
    create_table(doc, item_table)
    doc.add_paragraph()

    add_text(doc, '索引设计：', bold=True)
    add_text(doc, '• PRIMARY KEY (id)：主键索引', indent=1)
    add_text(doc, '• INDEX idx_item_status (item_status)：状态查询索引', indent=1)
    add_text(doc, '• INDEX idx_founder_id (founder_id)：发布者查询索引', indent=1)
    add_text(doc, '• INDEX idx_drop_point_id (drop_point_id)：站点查询索引', indent=1)
    add_text(doc, '• INDEX idx_category (category)：类别查询索引', indent=1)
    add_text(doc, '• INDEX idx_published_at (published_at DESC)：发布时间排序索引', indent=1)
    doc.add_paragraph()

    add_text(doc, '外键约束：', bold=True)
    add_text(doc, '• FOREIGN KEY (founder_id) REFERENCES user(id) ON DELETE CASCADE', indent=1)
    add_text(doc, '• FOREIGN KEY (actual_founder_id) REFERENCES user(id) ON DELETE SET NULL', indent=1)
    add_text(doc, '• FOREIGN KEY (drop_point_id) REFERENCES drop_point(id) ON DELETE SET NULL', indent=1)
    doc.add_paragraph()

    # 认领申请表
    add_title(doc, '5.5.3 认领申请表（claim_apply）', 3)
    claim_table = [
        ['字段名', '数据类型', '约束', '说明'],
        ['id', 'BIGINT', 'PRIMARY KEY, AUTO_INCREMENT', '申请ID'],
        ['item_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '物品ID'],
        ['source_notice_id', 'BIGINT', 'FOREIGN KEY', '来源寻物启事ID'],
        ['claimer_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '认领者ID'],
        ['answer', 'VARCHAR(500)', 'NOT NULL', '防伪问题答案'],
        ['apply_status', 'TINYINT', 'NOT NULL DEFAULT 0', '申请状态'],
        ['reject_reason', 'VARCHAR(500)', 'NULL', '拒绝理由'],
        ['reject_count', 'INT', 'NOT NULL DEFAULT 0', '被拒绝次数'],
        ['confidence_score', 'INT', 'NULL', '置信度评分（0-100）'],
        ['pickup_time', 'DATETIME', 'NULL', '取件时间'],
        ['pickup_photo', 'VARCHAR(255)', 'NULL', '取件照片URL'],
        ['pickup_signature', 'VARCHAR(255)', 'NULL', '取件签名URL'],
        ['credit_issued', 'TINYINT', 'NOT NULL DEFAULT 0', '是否已发放积分']
    ]
    create_table(doc, claim_table)
    doc.add_paragraph()

    add_text(doc, '索引与约束：', bold=True)
    add_text(doc, '• PRIMARY KEY (id)：主键索引', indent=1)
    add_text(doc, '• UNIQUE KEY uk_item_claimer (item_id, claimer_id)：防止重复申请', indent=1)
    add_text(doc, '• INDEX idx_claimer_id (claimer_id)：认领者查询索引', indent=1)
    add_text(doc, '• INDEX idx_apply_status (apply_status)：状态查询索引', indent=1)
    doc.add_paragraph()

    add_text(doc, '业务规则：', bold=True)
    add_text(doc, '• 同一用户对同一物品只能申请一次（通过唯一索引保证）', indent=1)
    add_text(doc, '• 拒绝次数达到3次自动锁定，状态变为3', indent=1)
    add_text(doc, '• 置信度评分算法：基于答案相似度、用户信用分、历史记录综合计算', indent=1)
    doc.add_paragraph()

    # 其他核心表
    add_title(doc, '5.5.4 其他核心表', 3)

    add_text(doc, '（1）招领站点表（drop_point）', bold=True)
    add_text(doc, '存储站点基本信息，包括站点名称、位置、管理员、监控设备等。每个站点由一名站点管理员负责管理。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（2）交物日志表（hand_in_log）', bold=True)
    add_text(doc, '记录物品交付到站点的日志，每个物品对应一条记录（通过item_id唯一索引保证）。巡检通过后发放积分给拾取者。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（3）积分日志表（credit_log）', bold=True)
    add_text(doc, '记录所有积分变动，支持审计和追溯。包含user_id、change_amount、operation_type、related_item_id等字段。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（4）寻物启事表（lost_notice）', bold=True)
    add_text(doc, '记录用户发布的寻物启事，支持与失物的智能匹配。启事可以直接转化为认领申请。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（5）监控调阅日志表（camera_log）', bold=True)
    add_text(doc, '记录监控调阅申请和审批结果。只有配备监控设备的站点才支持调阅功能。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（6）争议处理表（dispute）', bold=True)
    add_text(doc, '记录用户提交的争议申诉。争议类型包括：WRONG_CLAIMER（认领错误）、REJECT_UNFAIR（拒绝不公）等。', indent=1)
    doc.add_paragraph()

    add_text(doc, '（7）举报日志表（report_log）', bold=True)
    add_text(doc, '记录用户举报和处理情况。举报类型包括：FAKE_ITEM（虚假物品）、FRAUD（恶意骗取）等。', indent=1)
    doc.add_paragraph()

    # 数据库关系图
    add_title(doc, '5.5.5 数据库E-R关系', 3)
    add_text(doc, '各表之间的关系如下：')
    doc.add_paragraph()

    er_relations = [
        ['起始表', '关系类型', '目标表', '说明'],
        ['user', '1:N', 'found_item', '一个用户发布多个失物'],
        ['user', '1:N', 'claim_apply', '一个用户提交多个认领申请'],
        ['user', '1:N', 'lost_notice', '一个用户发布多个寻物启事'],
        ['user', '1:N', 'credit_log', '一个用户有多条积分日志'],
        ['user', '1:1', 'drop_point', '一个站点管理员管理一个站点'],
        ['drop_point', '1:N', 'found_item', '一个站点存放多个失物'],
        ['drop_point', '1:N', 'hand_in_log', '一个站点有多条交物日志'],
        ['drop_point', '1:N', 'camera_log', '一个站点有多条监控调阅记录'],
        ['found_item', '1:N', 'claim_apply', '一个失物有多个认领申请'],
        ['found_item', '1:1', 'hand_in_log', '一个失物对应一条交物日志'],
        ['found_item', '1:N', 'dispute', '一个失物可能产生多个争议'],
        ['found_item', '1:N', 'report_log', '一个失物可能被多次举报'],
        ['claim_apply', '1:N', 'dispute', '一个认领申请可能产生争议'],
        ['lost_notice', '1:N', 'claim_apply', '一个寻物启事可转化为多个申请']
    ]
    create_table(doc, er_relations)
    doc.add_paragraph()

    # 性能优化
    add_title(doc, '5.5.6 性能优化策略', 3)

    add_text(doc, '【索引优化】', bold=True)
    add_text(doc, '• 公开物品列表查询：使用复合索引(item_status, category, published_at)', indent=1)
    add_text(doc, '• 用户积分排行榜：使用降序索引(credit_score DESC)', indent=1)
    add_text(doc, '• 站点待巡检物品：使用复合索引(drop_point_id, hand_in_status)', indent=1)
    doc.add_paragraph()

    add_text(doc, '【缓存策略】', bold=True)
    add_text(doc, '使用Redis缓存热点数据，减少数据库访问压力：', indent=1)
    cache_data = [
        ['缓存Key', '数据类型', 'TTL', '说明'],
        ['user:{userId}', 'String (JSON)', '30分钟', '用户信息'],
        ['item:{itemId}', 'String (JSON)', '5分钟', '物品详情'],
        ['leaderboard:credit', 'Sorted Set', '10分钟', '积分排行榜'],
        ['drop_points:active', 'String (JSON)', '1小时', '活跃站点列表']
    ]
    create_table(doc, cache_data)
    doc.add_paragraph()

    add_text(doc, '【分区策略】', bold=True)
    add_text(doc, '对于日志类表（credit_log、camera_log、report_log），采用按月分区：', indent=1)
    add_text(doc, '• 提高历史数据查询效率', indent=2)
    add_text(doc, '• 便于归档和清理过期数据', indent=2)
    add_text(doc, '• 减少单表数据量，优化索引性能', indent=2)
    doc.add_paragraph()

    # 安全设计
    add_title(doc, '5.5.7 数据库安全设计', 3)

    add_text(doc, '【访问控制】', bold=True)
    add_text(doc, '• 应用程序账号：只授予SELECT、INSERT、UPDATE权限，不授予DELETE和DROP', indent=1)
    add_text(doc, '• 只读账号：只授予SELECT权限，用于数据分析和报表', indent=1)
    add_text(doc, '• 管理员账号：拥有完整权限，仅用于运维操作', indent=1)
    doc.add_paragraph()

    add_text(doc, '【敏感数据保护】', bold=True)
    sensitive_data = [
        ['数据类型', '保护措施'],
        ['用户密码', 'BCrypt加密，强度为10'],
        ['手机号/邮箱', '应用层脱敏展示（135****8888）'],
        ['学号', '部分脱敏（202001***）'],
        ['取件照片', '存储路径包含随机UUID，防止遍历']
    ]
    create_table(doc, sensitive_data)
    doc.add_paragraph()

    add_text(doc, '【审计日志】', bold=True)
    add_text(doc, '记录所有敏感操作到审计日志：', indent=1)
    add_text(doc, '• 管理员操作（积分调整、用户禁用、争议处理）', indent=2)
    add_text(doc, '• 批量删除操作', indent=2)
    add_text(doc, '• 权限变更操作', indent=2)
    add_text(doc, '• 监控调阅申请', indent=2)

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计报告_最终版.docx'
    doc.save(output_path)
    print(f'Word文档已生成: {output_path}')
    print('文档包含：')
    print('- 第3章：系统设计 - 领域模型（类图）')
    print('- 第4章：系统设计 - 对象交互模型（时序图）')
    print('- 第5章：数据库设计（5.5节）')
    print('所有UML图以表格和文字形式呈现，格式规范，无多余分页符。')

if __name__ == '__main__':
    main()
