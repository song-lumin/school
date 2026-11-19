#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
生成系统分析与设计报告 - 完善版
优化5.5数据库设计部分，增加表格和类关系图
"""

from docx import Document
from docx.shared import Pt, Inches, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
import requests
import zlib

def setup_document():
    """初始化文档并设置样式"""
    doc = Document()

    # 设置页面边距
    sections = doc.sections
    for section in sections:
        section.top_margin = Cm(2.54)
        section.bottom_margin = Cm(2.54)
        section.left_margin = Cm(3.17)
        section.right_margin = Cm(3.17)

    # 设置正文样式
    style = doc.styles['Normal']
    style.font.name = '宋体'
    style.font.size = Pt(12)
    style.font.bold = False
    style.paragraph_format.line_spacing = 1.5
    style.paragraph_format.space_after = Pt(6)

    # 设置标题样式
    style = doc.styles['Heading 1']
    style.font.name = '黑体'
    style.font.size = Pt(18)
    style.font.bold = True
    style.paragraph_format.space_before = Pt(24)
    style.paragraph_format.space_after = Pt(12)

    style = doc.styles['Heading 2']
    style.font.name = '黑体'
    style.font.size = Pt(16)
    style.font.bold = True
    style.paragraph_format.space_before = Pt(18)
    style.paragraph_format.space_after = Pt(10)

    style = doc.styles['Heading 3']
    style.font.name = '黑体'
    style.font.size = Pt(14)
    style.font.bold = True
    style.paragraph_format.space_before = Pt(12)
    style.paragraph_format.space_after = Pt(8)

    return doc

def add_paragraph(doc, text):
    """添加段落并确保格式统一"""
    p = doc.add_paragraph()
    run = p.add_run(text)
    run.font.name = '宋体'
    run.font.size = Pt(12)
    run.font.bold = False
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    return p

def add_figure(doc, image_path, width=6.0):
    """添加图片"""
    try:
        paragraph = doc.add_paragraph()
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = paragraph.add_run()
        run.add_picture(image_path, width=Inches(width))
    except Exception as e:
        add_paragraph(doc, '[图片加载失败]')

def create_table(doc, data):
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
            if i == 0:
                for paragraph in cell.paragraphs:
                    for run in paragraph.runs:
                        run.font.bold = True
                        run.font.name = '宋体'

    return table

def plantuml_encode(plantuml_text):
    """编码PlantUML文本"""
    zlibbed_str = zlib.compress(plantuml_text.encode('utf-8'))
    compressed_string = zlibbed_str[2:-4]
    plantuml_alphabet = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_'
    encoded = ''
    for i in range(0, len(compressed_string), 3):
        if i + 2 == len(compressed_string):
            b1 = compressed_string[i] & 0xFF
            b2 = compressed_string[i + 1] & 0xFF
            encoded += plantuml_alphabet[b1 >> 2]
            encoded += plantuml_alphabet[((b1 & 0x3) << 4) | (b2 >> 4)]
            encoded += plantuml_alphabet[(b2 & 0xF) << 2]
        elif i + 1 == len(compressed_string):
            b1 = compressed_string[i] & 0xFF
            encoded += plantuml_alphabet[b1 >> 2]
            encoded += plantuml_alphabet[(b1 & 0x3) << 4]
        else:
            b1 = compressed_string[i] & 0xFF
            b2 = compressed_string[i + 1] & 0xFF
            b3 = compressed_string[i + 2] & 0xFF
            encoded += plantuml_alphabet[b1 >> 2]
            encoded += plantuml_alphabet[((b1 & 0x3) << 4) | (b2 >> 4)]
            encoded += plantuml_alphabet[((b2 & 0xF) << 2) | (b3 >> 6)]
            encoded += plantuml_alphabet[b3 & 0x3F]
    return encoded

def generate_plantuml_image(plantuml_code, filename):
    """生成PlantUML图片"""
    try:
        encoded = plantuml_encode(plantuml_code)
        url = f'http://www.plantuml.com/plantuml/png/{encoded}'
        print(f'正在生成: {filename}...')
        response = requests.get(url, timeout=30)
        if response.status_code == 200:
            with open(filename, 'wb') as f:
                f.write(response.content)
            print(f'✓ {filename}')
            return True
        else:
            print(f'✗ {filename} 失败')
            return False
    except Exception as e:
        print(f'✗ {filename} 错误: {str(e)}')
        return False

def main():
    import os
    image_dir = 'D:\\gdut\\school\\docs\\images'
    os.makedirs(image_dir, exist_ok=True)

    print('=' * 60)
    print('开始生成UML图...')
    print('=' * 60)

    # 生成类图
    class_diagram = '''@startuml
skinparam classAttributeIconSize 0
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 10
skinparam defaultFontSize 12
skinparam defaultFontName SimHei

skinparam class {
    BackgroundColor #E8F4F8
    BorderColor #2C7BB6
    BorderThickness 2
    ArrowColor #2C7BB6
    FontSize 12
    FontStyle bold
}

class "用户类" as User {
    -id : Long
    -username : String
    -password : String
    -realName : String
    -studentId : String
    -role : UserRole
    -creditScore : Integer
    -status : Integer
}

class "失物类" as FoundItem {
    -id : Long
    -title : String
    -category : String
    -foundLocation : String
    -foundTime : LocalDateTime
    -images : List<String>
    -claimQuestion : String
    -itemStatus : Integer
    -founderId : Long
    -dropPointId : Long
}

class "认领申请类" as ClaimApply {
    -id : Long
    -itemId : Long
    -claimerId : Long
    -answer : String
    -applyStatus : Integer
    -confidenceScore : Integer
    -rejectCount : Integer
    -pickupTime : LocalDateTime
}

class "招领站点类" as DropPoint {
    -id : Long
    -name : String
    -location : String
    -adminId : Long
    -hasCamera : Integer
}

class "积分日志类" as CreditLog {
    -id : Long
    -userId : Long
    -changeAmount : Integer
    -operationType : String
}

class "交物日志类" as HandInLog {
    -id : Long
    -itemId : Long
    -dropPointId : Long
    -handInStatus : Integer
    -checkedBy : Long
}

class "寻物启事类" as LostNotice {
    -id : Long
    -title : String
    -lostLocation : String
    -status : Integer
    -publisherId : Long
}

User "1" -- "0..*" FoundItem : 发布 >
User "1" -- "0..*" ClaimApply : 认领 >
User "1" -- "0..*" LostNotice : 发布 >
User "1" -- "0..*" CreditLog : 积分记录 >
User "1" -- "0..1" DropPoint : 管理 >

FoundItem "1" -- "0..*" ClaimApply : 被认领 >
FoundItem "1" -- "0..1" HandInLog : 交物日志 >
FoundItem "*" -- "1" DropPoint : 存放于 >

ClaimApply "*" -- "0..1" LostNotice : 来源于 >

@enduml'''

    class_diagram_file = f'{image_dir}\\class_diagram.png'
    generate_plantuml_image(class_diagram, class_diagram_file)

    # 生成类关系图（简化版，更清晰）
    class_relation_diagram = '''@startuml
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontSize 13
skinparam defaultFontName SimHei

skinparam class {
    BackgroundColor #E8F4F8
    BorderColor #2C7BB6
    BorderThickness 2
    ArrowColor #2C7BB6
    ArrowThickness 2
    FontSize 13
    FontStyle bold
}

class "用户类" as User
class "失物类" as Item
class "认领申请类" as Claim
class "招领站点类" as Point
class "交物日志类" as HandIn
class "积分日志类" as Credit
class "寻物启事类" as Notice

User "1" -down-> "0..*" Item : 发布
User "1" -down-> "0..*" Claim : 认领
User "1" -down-> "0..*" Notice : 发布启事
User "1" --> "0..*" Credit : 积分记录
User "1" --> "0..1" Point : 管理

Item "1" --> "0..*" Claim : 被认领
Item "1" --> "0..1" HandIn : 交物日志
Item "*" --> "1" Point : 存放于

Claim "*" --> "0..1" Notice : 来源于启事

note right of User : 系统核心实体\\n管理用户信息和积分
note right of Item : 失物信息\\n包含防伪问题
note bottom of Claim : 认领流程管理\\n包含置信度评分

@enduml'''

    class_relation_file = f'{image_dir}\\class_relation.png'
    generate_plantuml_image(class_relation_diagram, class_relation_file)

    # 生成其他时序图（复用之前的）
    auth_seq_file = f'{image_dir}\\auth_sequence.png'
    publish_seq_file = f'{image_dir}\\publish_sequence.png'
    claim_seq_file = f'{image_dir}\\claim_sequence.png'
    pickup_seq_file = f'{image_dir}\\pickup_sequence.png'
    check_seq_file = f'{image_dir}\\check_sequence.png'
    er_file = f'{image_dir}\\er_diagram.png'

    print('=' * 60)
    print('UML图生成完成！')
    print('=' * 60)

    doc = setup_document()

    # 封面
    title = doc.add_heading('校园失物招领及诚信积分系统', 0)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    for run in title.runs:
        run.font.name = '黑体'
        run.font.size = Pt(26)

    subtitle = doc.add_paragraph()
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle_run = subtitle.add_run('系统分析与设计报告')
    subtitle_run.font.name = '黑体'
    subtitle_run.font.size = Pt(22)
    subtitle_run.font.bold = True

    doc.add_paragraph('\n\n\n')

    # ================== 第3章 ==================
    doc.add_heading('第3章 系统设计 - 领域模型（类图）', 1)

    doc.add_heading('3.1 核心领域模型', 2)
    add_paragraph(doc, '本系统的领域模型包含7个核心实体类，它们之间通过关联、聚合等关系相互协作，共同实现校园失物招领及诚信积分管理功能。系统采用面向对象的设计思想，将业务逻辑封装在各个实体类中，确保系统的高内聚、低耦合。')

    add_figure(doc, class_diagram_file, 6.5)

    doc.add_heading('3.2 核心类详细说明', 2)

    doc.add_heading('3.2.1 用户类', 3)
    add_paragraph(doc, '用户类是系统的核心实体之一，负责管理用户的基本信息、角色权限和诚信积分。系统支持三种角色：普通用户负责发布失物和认领物品，站点管理员负责管理站点和审核认领申请，系统管理员负责系统全局管理和争议处理。')
    add_paragraph(doc, '核心属性包括：用户ID、用户名、密码、真实姓名、学号或工号、角色、诚信积分、账号状态等。用户通过拾取和认领物品可以累积诚信积分，积分达到一定数量可以上榜展示，激励拾金不昧的行为。')

    doc.add_heading('3.2.2 失物类', 3)
    add_paragraph(doc, '失物类记录拾取物品的详细信息，是系统业务流程的核心。每个失物包含标题、类别、描述、拾取地点、拾取时间、物品图片、防伪问题等属性。防伪问题是系统的特色功能，用于在认领环节验证失主身份，防止恶意认领。')
    add_paragraph(doc, '物品状态包括：公开待认领、认领中、已取件、已归档、已过期、已作废。物品从发布到认领完成的整个生命周期由状态字段管理。系统支持易腐物品标记，易腐物品的过期时间为3天，普通物品为30天。')

    doc.add_heading('3.2.3 认领申请类', 3)
    add_paragraph(doc, '认领申请类管理物品认领的完整流程。失主申请认领时需要回答防伪问题，系统自动计算置信度评分，评分范围为0到100分，基于答案相似度、用户信用分和历史记录综合计算。低置信度申请会被标记，提醒管理员重点审核。')
    add_paragraph(doc, '申请状态包括：待审核、已完成、已拒绝、已锁定、已通过待取件。为防止恶意认领，系统记录拒绝次数，当同一申请被拒绝3次后自动锁定，不能再次申请。取件时需要拍摄照片和电子签名作为凭证。')

    doc.add_heading('3.2.4 其他核心类', 3)
    add_paragraph(doc, '招领站点类管理物理站点信息，包括站点名称、位置、管理员、监控设备等。每个站点由一名站点管理员负责日常运营，包括物品巡检和认领审核。')
    add_paragraph(doc, '积分日志类记录所有积分变动，支持审计和追溯。操作类型包括巡检发放、取件发放、积分回滚、积分扣除等。')
    add_paragraph(doc, '交物日志类记录物品交付到站点的日志，一个物品对应一条记录。巡检状态包括待巡检、已巡检、已作废。')
    add_paragraph(doc, '寻物启事类记录用户发布的寻物启事，支持与失物的智能匹配，启事可以直接转化为认领申请。')

    doc.add_heading('3.3 类关系图', 2)
    add_paragraph(doc, '为了更清晰地展示系统各实体类之间的关联关系，下图以简化的方式描述了核心类之间的依赖和关联。')

    add_figure(doc, class_relation_file, 6.5)

    add_paragraph(doc, '系统各实体类之间通过关联关系形成完整的业务模型。用户类作为核心实体，与失物类、认领申请类、寻物启事类、积分日志类和站点类都有关联。失物类与认领申请类、交物日志类和站点类关联。认领申请类可以来源于寻物启事类。所有关系都通过外键约束保证数据的参照完整性。')

    # ================== 第4章 ==================
    doc.add_heading('第4章 系统设计 - 对象交互模型（时序图）', 1)

    doc.add_heading('4.1 用户注册登录时序图', 2)
    add_paragraph(doc, '用户注册和登录是系统的基础功能，采用JWT Token进行无状态认证。注册时系统验证用户名和学号的唯一性，使用BCrypt算法加密密码，强度为10，将用户信息插入数据库后生成JWT Token返回给前端。Token有效期为24小时，前端将Token存储在本地存储中，后续所有API请求都需要在请求头中携带Token。')

    add_figure(doc, auth_seq_file, 6.5)

    add_paragraph(doc, '登录流程验证用户名和密码的正确性，若验证通过则生成JWT Token。Spring Security拦截器会自动验证Token的有效性，并提取用户ID和角色信息，控制器层通过认证对象获取当前用户信息，实现了认证与业务逻辑的分离。')

    doc.add_heading('4.2 失物发布时序图', 2)
    add_paragraph(doc, '拾取者发现物品后，通过系统发布失物信息。发布时需要填写物品标题、类别、描述、拾取地点和时间，上传物品图片，设置防伪问题，并选择将物品送到哪个站点。系统验证站点的有效性，站点状态必须为正常，插入失物记录到数据库表，同时自动创建交物日志到另一个数据库表，初始状态为待巡检。')

    add_figure(doc, publish_seq_file, 6.5)

    add_paragraph(doc, '防伪问题是系统的核心功能，用于验证失主身份。问题应该只有真正的失主才知道答案，例如书包里有什么颜色的笔记本、钱包里有多少张银行卡等。站点管理员收到物品后进行巡检，巡检通过后系统自动发放积分给拾取者。')

    doc.add_heading('4.3 失物认领时序图', 2)
    add_paragraph(doc, '认领流程分为三个阶段：申请认领、审核认领和到站取件。整个流程涉及失主、站点管理员和系统的协同交互。')

    doc.add_heading('4.3.1 申请认领阶段', 3)
    add_paragraph(doc, '失主浏览失物列表，找到疑似物品后点击申请认领，填写防伪问题答案并提交。系统首先验证物品状态是否为公开待认领，然后检查用户是否已经申请过该物品，通过数据库唯一索引保证同一用户对同一物品只能申请一次。')

    add_figure(doc, claim_seq_file, 6.5)

    add_paragraph(doc, '系统计算置信度评分，范围为0到100分，评分算法综合考虑答案相似度、用户信用分和历史认领成功率。低置信度申请，分数小于40分的申请会被标记，提醒管理员重点审核。插入认领申请记录后，系统将物品状态更新为认领中，防止其他用户申请。')

    doc.add_heading('4.3.2 审核认领阶段', 3)
    add_paragraph(doc, '站点管理员查看待审核的认领申请列表，对比物品和失主的答案，决定通过或拒绝。系统验证管理员是否有权限审核该物品，只能审核自己站点的物品。若审核通过，系统将申请状态更新为已通过待取件，同时拒绝该物品的其他所有申请；若拒绝，系统记录拒绝理由和拒绝次数，若拒绝次数达到3次则自动锁定该申请。')

    doc.add_heading('4.3.3 到站取件阶段', 3)
    add_paragraph(doc, '失主携带证件到站点取件，站点管理员核实身份后拍摄取件照片和电子签名，上传到系统。系统验证申请状态为已通过待取件，更新认领申请状态为已完成，更新物品状态为已取件，发放积分给失主，积分增加1分，记录积分变动日志。')

    add_figure(doc, pickup_seq_file, 6.5)

    add_paragraph(doc, '取件操作涉及多张表的更新，包括认领申请表、失物表、积分日志表和用户表，系统使用事务注解保证事务的原子性，要么全部成功，要么全部回滚，确保数据一致性。')

    doc.add_heading('4.4 站点巡检与积分发放时序图', 2)
    add_paragraph(doc, '物品送到站点后，站点管理员需要进行巡检确认，核实物品信息是否真实有效。巡检通过后，系统自动发放积分给拾取者，积分增加2分，激励拾金不昧的行为。巡检是积分发放的前置条件，未巡检的物品不会发放积分，防止虚假发布骗取积分。')

    add_figure(doc, check_seq_file, 6.5)

    add_paragraph(doc, '系统验证管理员是否为该站点的管理员，验证巡检状态是否为待巡检，防止重复巡检，更新巡检状态为已巡检并记录巡检人和时间。积分发放包括插入积分日志、更新用户积分、更新交物日志的积分发放标识字段，确保积分只发放一次。')

    doc.add_heading('4.5 时序图设计说明', 2)

    doc.add_heading('4.5.1 认证与授权机制', 3)
    add_paragraph(doc, '系统采用JWT Token进行无状态认证，所有API请求，登录和注册除外，都需要在请求头中携带Token。Spring Security拦截器自动验证Token的有效性，若Token无效或已过期则返回401未授权错误。控制器层通过认证对象获取当前用户ID和角色，服务层根据角色进行权限校验，例如站点管理员只能审核自己站点的认领申请，系统管理员才能处理争议和举报。')

    doc.add_heading('4.5.2 事务管理策略', 3)
    add_paragraph(doc, '涉及多表操作的业务，如取件、巡检、积分发放，使用事务注解保证事务的原子性。例如取件操作需要更新认领申请表、失物表、积分日志表和用户表，若任何一步失败则全部回滚，确保数据的一致性。事务隔离级别设置为读已提交，防止脏读但允许不可重复读，在性能和一致性之间取得平衡。')

    doc.add_heading('4.5.3 异常处理机制', 3)
    add_paragraph(doc, '所有业务异常统一抛出业务异常类，全局异常处理器捕获异常并返回统一格式的JSON响应，包含状态码、消息和数据字段。HTTP状态码包括：200成功、400业务错误、401未认证、403无权限、500系统错误。异常信息使用中文描述，便于前端展示给用户。')

    doc.add_heading('4.5.4 并发控制机制', 3)
    add_paragraph(doc, '系统通过多种机制防止并发冲突。认领申请使用数据库唯一索引防止重复提交。物品状态变更使用乐观锁，使用版本号字段防止并发修改。积分发放使用分布式锁，使用Redis防止重复发放。巡检操作验证状态为待巡检防止重复巡检。')

    # ================== 第5章 ==================
    doc.add_heading('第5章 数据库设计', 1)

    doc.add_heading('5.5 数据库表详细设计', 2)

    doc.add_heading('5.5.1 数据库E-R图', 3)
    add_paragraph(doc, '系统数据库包含10张核心业务表，它们之间通过外键关系相互关联，形成完整的数据模型。数据库采用MySQL 8.0，使用InnoDB存储引擎，支持事务和外键约束。字符集统一使用utf8mb4，支持Emoji等特殊字符。')

    add_figure(doc, er_file, 6.5)

    add_paragraph(doc, '各表之间的关系如下：用户表是核心表，与失物表、认领申请表、寻物启事表、积分日志表、站点表都有关联；失物表与认领申请表、交物日志表关联；站点表与失物表、交物日志表关联。所有外键都设置了级联策略，保证数据的参照完整性。')

    doc.add_heading('5.5.2 核心表设计', 3)

    # 用户表
    add_paragraph(doc, '(1) 用户表')
    add_paragraph(doc, '用户表存储用户基本信息、角色和诚信积分。表名为user，主要字段如下表所示：')

    user_table_data = [
        ['字段名', '数据类型', '约束', '说明'],
        ['id', 'BIGINT', 'PRIMARY KEY, AUTO_INCREMENT', '用户ID'],
        ['username', 'VARCHAR(50)', 'UNIQUE NOT NULL', '用户名'],
        ['password', 'VARCHAR(255)', 'NOT NULL', '密码（BCrypt加密）'],
        ['real_name', 'VARCHAR(50)', 'NULL', '真实姓名'],
        ['student_id', 'VARCHAR(20)', 'UNIQUE', '学号/工号'],
        ['phone', 'VARCHAR(20)', 'NULL', '手机号'],
        ['email', 'VARCHAR(100)', 'NULL', '邮箱'],
        ['role', 'ENUM', 'NOT NULL DEFAULT USER', '角色'],
        ['credit_score', 'INT', 'NOT NULL DEFAULT 0', '诚信积分'],
        ['status', 'TINYINT', 'NOT NULL DEFAULT 1', '状态'],
        ['created_at', 'DATETIME', 'NOT NULL', '创建时间'],
        ['updated_at', 'DATETIME', 'NOT NULL', '更新时间']
    ]
    create_table(doc, user_table_data)

    add_paragraph(doc, '索引设计：主键索引id；唯一索引username、student_id；普通索引role、credit_score降序。')
    add_paragraph(doc, '业务规则：用户名和学号必须唯一；密码使用BCrypt加密强度10；积分不能为负数；禁用用户status=0不能登录。')
    doc.add_paragraph()

    # 失物表
    add_paragraph(doc, '(2) 失物表')
    add_paragraph(doc, '失物表存储失物详细信息。表名为found_item，主要字段如下表所示：')

    item_table_data = [
        ['字段名', '数据类型', '约束', '说明'],
        ['id', 'BIGINT', 'PRIMARY KEY, AUTO_INCREMENT', '物品ID'],
        ['title', 'VARCHAR(200)', 'NOT NULL', '物品标题'],
        ['category', 'VARCHAR(50)', 'NOT NULL', '物品类别'],
        ['description', 'TEXT', 'NULL', '物品描述'],
        ['found_location', 'VARCHAR(255)', 'NOT NULL', '拾取地点'],
        ['found_time', 'DATETIME', 'NOT NULL', '拾取时间'],
        ['images', 'JSON', 'NULL', '物品图片（JSON数组）'],
        ['claim_question', 'VARCHAR(500)', 'NOT NULL', '防伪问题'],
        ['item_status', 'TINYINT', 'NOT NULL DEFAULT 1', '物品状态'],
        ['founder_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '发布者ID'],
        ['drop_point_id', 'BIGINT', 'FOREIGN KEY', '所在站点ID'],
        ['published_at', 'DATETIME', 'NOT NULL', '发布时间']
    ]
    create_table(doc, item_table_data)

    add_paragraph(doc, '索引设计：主键索引id；普通索引item_status、category、published_at降序、founder_id、drop_point_id。')
    add_paragraph(doc, '外键约束：founder_id引用user表id，级联删除；drop_point_id引用drop_point表id，置空删除。')
    add_paragraph(doc, '物品状态：0已作废、1公开待认领、2认领中、3已取件、4已归档、5已过期。')
    doc.add_paragraph()

    # 认领申请表
    add_paragraph(doc, '(3) 认领申请表')
    add_paragraph(doc, '认领申请表存储认领申请信息。表名为claim_apply，主要字段如下表所示：')

    claim_table_data = [
        ['字段名', '数据类型', '约束', '说明'],
        ['id', 'BIGINT', 'PRIMARY KEY, AUTO_INCREMENT', '申请ID'],
        ['item_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '物品ID'],
        ['claimer_id', 'BIGINT', 'FOREIGN KEY NOT NULL', '认领者ID'],
        ['answer', 'VARCHAR(500)', 'NOT NULL', '防伪问题答案'],
        ['apply_status', 'TINYINT', 'NOT NULL DEFAULT 0', '申请状态'],
        ['reject_reason', 'VARCHAR(500)', 'NULL', '拒绝理由'],
        ['reject_count', 'INT', 'NOT NULL DEFAULT 0', '被拒绝次数'],
        ['confidence_score', 'INT', 'NULL', '置信度评分（0-100）'],
        ['pickup_time', 'DATETIME', 'NULL', '取件时间'],
        ['pickup_photo', 'VARCHAR(255)', 'NULL', '取件照片URL'],
        ['credit_issued', 'TINYINT', 'NOT NULL DEFAULT 0', '是否已发放积分']
    ]
    create_table(doc, claim_table_data)

    add_paragraph(doc, '索引设计：主键索引id；联合唯一索引(item_id, claimer_id)；普通索引claimer_id、apply_status。')
    add_paragraph(doc, '业务规则：同一用户对同一物品只能申请一次；拒绝次数达到3次自动锁定；置信度评分低于40分需重点审核。')
    add_paragraph(doc, '申请状态：0待审核、1已完成、2已拒绝、3已锁定、4已通过待取件。')
    doc.add_paragraph()

    # 其他核心表
    add_paragraph(doc, '(4) 其他核心表')
    add_paragraph(doc, '招领站点表（drop_point）：存储站点名称、位置、管理员ID、是否有监控等信息。')
    add_paragraph(doc, '交物日志表（hand_in_log）：记录物品交付日志，包含item_id唯一索引、巡检状态、巡检人、积分发放标识等字段。')
    add_paragraph(doc, '积分日志表（credit_log）：记录所有积分变动，包含用户ID、变动金额、操作类型（巡检发放、取件发放、回滚、扣除）、关联物品ID等。')
    add_paragraph(doc, '寻物启事表（lost_notice）：记录寻物启事，包含标题、类别、丢失地点、发布者ID、匹配物品ID等。')
    add_paragraph(doc, '监控调阅日志表（camera_log）：记录监控调阅申请，包含站点ID、申请人ID、时间范围、审批状态等。')
    add_paragraph(doc, '争议处理表（dispute）：记录争议申诉，包含申诉人ID、申请ID、争议类型、证据图片、处理结果等。')
    add_paragraph(doc, '举报日志表（report_log）：记录举报信息，包含举报人ID、物品ID、举报类型、证据、处理状态等。')
    doc.add_paragraph()

    doc.add_heading('5.5.3 性能优化策略', 3)

    # 索引优化
    add_paragraph(doc, '(1) 索引优化')
    add_paragraph(doc, '针对常用查询场景设计复合索引，提高查询效率。主要索引策略如下表所示：')

    index_data = [
        ['查询场景', '索引字段', '查询条件', '排序字段'],
        ['公开物品列表', '(item_status, category, published_at)', 'status=1, category=?', 'published_at DESC'],
        ['用户积分排行', '(credit_score DESC)', 'status=1', 'credit_score DESC'],
        ['站点待巡检物品', '(drop_point_id, hand_in_status)', 'drop_point_id=?, status=1', 'created_at ASC'],
        ['用户认领列表', '(claimer_id, created_at DESC)', 'claimer_id=?', 'created_at DESC']
    ]
    create_table(doc, index_data)
    doc.add_paragraph()

    # 缓存策略
    add_paragraph(doc, '(2) 缓存策略')
    add_paragraph(doc, '使用Redis缓存热点数据，减少数据库访问压力。缓存策略如下表所示：')

    cache_data = [
        ['缓存Key', '数据类型', 'TTL', '更新策略'],
        ['user:{userId}', 'String (JSON)', '30分钟', '用户信息变更时删除'],
        ['item:{itemId}', 'String (JSON)', '5分钟', '物品状态变更时删除'],
        ['leaderboard:credit', 'Sorted Set', '10分钟', '积分变更时异步更新'],
        ['drop_points:active', 'String (JSON)', '1小时', '站点信息变更时删除']
    ]
    create_table(doc, cache_data)
    doc.add_paragraph()

    # 分区策略
    add_paragraph(doc, '(3) 分区策略')
    add_paragraph(doc, '对于日志类表采用按月分区，提高历史数据查询效率。分区策略使用RANGE分区，按created_at字段的月份分区。每月创建一个分区，查询时MySQL自动根据时间范围选择相应的分区，减少扫描的数据量。')
    add_paragraph(doc, '适用表：credit_log积分日志表、camera_log监控调阅日志表、report_log举报日志表。')
    add_paragraph(doc, '优势：提高历史数据查询效率、便于归档和清理过期数据、减少单表数据量、优化索引性能。')
    doc.add_paragraph()

    doc.add_heading('5.5.4 数据库安全设计', 3)

    # 访问控制
    add_paragraph(doc, '(1) 访问控制')
    add_paragraph(doc, '数据库采用最小权限原则，不同账号授予不同权限。账号权限设计如下表所示：')

    access_data = [
        ['账号类型', '授予权限', '使用场景'],
        ['应用程序账号', 'SELECT, INSERT, UPDATE', '应用程序数据访问'],
        ['只读账号', 'SELECT', '数据分析和报表查询'],
        ['管理员账号', '全部权限', '数据库运维操作']
    ]
    create_table(doc, access_data)
    add_paragraph(doc, '安全策略：应用程序账号不授予DELETE和DROP权限；使用强密码并定期更换；限制数据库访问IP白名单。')
    doc.add_paragraph()

    # 敏感数据保护
    add_paragraph(doc, '(2) 敏感数据保护')
    add_paragraph(doc, '对敏感数据进行加密和脱敏处理。保护策略如下表所示：')

    sensitive_data = [
        ['数据类型', '保护措施', '说明'],
        ['用户密码', 'BCrypt加密（强度10）', '不可逆加密，防止彩虹表攻击'],
        ['手机号', '应用层脱敏（135****8888）', '仅展示部分信息'],
        ['邮箱', '应用层脱敏（abc***@qq.com）', '仅展示部分信息'],
        ['学号', '应用层脱敏（202001****）', '仅展示部分信息'],
        ['取件照片', '随机UUID路径', '防止遍历下载']
    ]
    create_table(doc, sensitive_data)
    doc.add_paragraph()

    # 审计日志
    add_paragraph(doc, '(3) 审计日志')
    add_paragraph(doc, '记录所有敏感操作到审计日志，支持事后追溯和问题排查。审计范围如下表所示：')

    audit_data = [
        ['操作类型', '记录内容', '保留期限'],
        ['管理员操作', '积分调整、用户禁用、争议处理', '永久保留'],
        ['批量删除操作', '删除人、删除时间、删除范围', '永久保留'],
        ['权限变更操作', '变更人、变更时间、变更内容', '永久保留'],
        ['监控调阅申请', '申请人、申请时间、调阅范围', '永久保留']
    ]
    create_table(doc, audit_data)

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计报告_完善版.docx'
    doc.save(output_path)

    print('\n' + '=' * 60)
    print(f'✓ Word文档已生成: {output_path}')
    print('=' * 60)
    print('文档优化：')
    print('- 5.5章增加表格，内容更清晰')
    print('- 3.3章增加类关系图')
    print('- 格式统一，无粗体混杂')
    print('=' * 60)

if __name__ == '__main__':
    main()
