#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
生成系统分析与设计报告 - 格式完全统一版
确保所有段落字体格式一致
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
    style.font.bold = False  # 确保不加粗
    style.paragraph_format.line_spacing = 1.5
    style.paragraph_format.space_after = Pt(6)

    # 设置标题1样式
    style = doc.styles['Heading 1']
    style.font.name = '黑体'
    style.font.size = Pt(18)
    style.font.bold = True
    style.font.color.rgb = RGBColor(0, 0, 0)
    style.paragraph_format.space_before = Pt(24)
    style.paragraph_format.space_after = Pt(12)

    # 设置标题2样式
    style = doc.styles['Heading 2']
    style.font.name = '黑体'
    style.font.size = Pt(16)
    style.font.bold = True
    style.font.color.rgb = RGBColor(0, 0, 0)
    style.paragraph_format.space_before = Pt(18)
    style.paragraph_format.space_after = Pt(10)

    # 设置标题3样式
    style = doc.styles['Heading 3']
    style.font.name = '黑体'
    style.font.size = Pt(14)
    style.font.bold = True
    style.font.color.rgb = RGBColor(0, 0, 0)
    style.paragraph_format.space_before = Pt(12)
    style.paragraph_format.space_after = Pt(8)

    return doc

def add_paragraph(doc, text):
    """添加段落并确保格式统一"""
    p = doc.add_paragraph()
    run = p.add_run(text)
    run.font.name = '宋体'
    run.font.size = Pt(12)
    run.font.bold = False  # 确保不加粗
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    return p

def add_figure(doc, image_path, width=6.0):
    """添加图片（不含图注）"""
    try:
        paragraph = doc.add_paragraph()
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = paragraph.add_run()
        run.add_picture(image_path, width=Inches(width))
    except Exception as e:
        add_paragraph(doc, '[图片加载失败]')

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
    # 检查图片是否已存在
    import os
    image_dir = 'D:\\gdut\\school\\docs\\images'
    images_exist = all([
        os.path.exists(f'{image_dir}\\class_diagram.png'),
        os.path.exists(f'{image_dir}\\auth_sequence.png'),
        os.path.exists(f'{image_dir}\\publish_sequence.png'),
        os.path.exists(f'{image_dir}\\claim_sequence.png'),
        os.path.exists(f'{image_dir}\\pickup_sequence.png'),
        os.path.exists(f'{image_dir}\\check_sequence.png'),
        os.path.exists(f'{image_dir}\\er_diagram.png')
    ])

    if images_exist:
        print('=' * 60)
        print('UML图已存在，跳过生成步骤...')
        print('=' * 60)

    # 设置图片路径
    class_diagram_file = f'{image_dir}\\class_diagram.png'
    auth_seq_file = f'{image_dir}\\auth_sequence.png'
    publish_seq_file = f'{image_dir}\\publish_sequence.png'
    claim_seq_file = f'{image_dir}\\claim_sequence.png'
    pickup_seq_file = f'{image_dir}\\pickup_sequence.png'
    check_seq_file = f'{image_dir}\\check_sequence.png'
    er_file = f'{image_dir}\\er_diagram.png'

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

    doc.add_heading('3.3 类关系说明', 2)
    add_paragraph(doc, '系统各实体类之间通过关联关系形成完整的业务模型：')
    add_paragraph(doc, '一个用户可以发布多个失物，一个失物由一个用户发布。')
    add_paragraph(doc, '一个用户可以提交多个认领申请，一个认领申请对应一个用户。')
    add_paragraph(doc, '一个失物可以有多个认领申请，但最终只有一个通过。')
    add_paragraph(doc, '一个失物对应一条交物日志，记录物品的巡检状态。')
    add_paragraph(doc, '多个失物存放在一个站点，一个站点可以存放多个失物。')
    add_paragraph(doc, '一个站点管理员管理一个站点。')
    add_paragraph(doc, '寻物启事可以转化为认领申请，一个启事可能产生多个申请。')

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

    # ================== 第5.5章 ==================
    doc.add_heading('第5章 数据库设计', 1)

    doc.add_heading('5.5 数据库表详细设计', 2)

    doc.add_heading('5.5.1 数据库E-R图', 3)
    add_paragraph(doc, '系统数据库包含10张核心业务表，它们之间通过外键关系相互关联，形成完整的数据模型。数据库采用MySQL 8.0，使用InnoDB存储引擎，支持事务和外键约束。字符集统一使用utf8mb4，支持Emoji等特殊字符。')

    add_figure(doc, er_file, 6.5)

    add_paragraph(doc, '各表之间的关系如下：用户表是核心表，与失物表、认领申请表、寻物启事表、积分日志表、站点表都有关联；失物表与认领申请表、交物日志表关联；站点表与失物表、交物日志表关联。所有外键都设置了级联策略，保证数据的参照完整性。')

    doc.add_heading('5.5.2 核心表设计说明', 3)

    add_paragraph(doc, '用户表存储用户基本信息、角色和诚信积分。主键为id，类型为BIGINT自增，用户名和学号设置了唯一索引保证不重复。密码字段使用VARCHAR类型，长度255，存储BCrypt加密后的密码。角色字段使用ENUM类型存储角色，包括普通用户、站点管理员、系统管理员。诚信积分字段存储诚信积分，默认为0，通过应用层逻辑保证不为负数。状态字段表示账号状态，0表示禁用，1表示正常，禁用用户不能登录和操作。')

    add_paragraph(doc, '索引设计包括：主键索引使用id字段；用户名唯一索引保证用户名不重复；学号唯一索引保证学号不重复；角色查询索引用于按角色查询；积分排行榜索引使用降序索引，用于快速查询积分排名。')

    add_paragraph(doc, '失物表存储失物详细信息。图片字段使用JSON类型存储图片URL数组。防伪问题字段存储防伪问题，最大长度500字符。物品状态字段存储物品状态，包括0已作废、1公开待认领、2认领中、3已取件、4已归档、5已过期。发布者ID和实际拾取者ID都是外键关联到用户表，前者是发布者，后者是实际拾取者，支持代发布场景。站点ID外键关联到站点表，表示物品存放的站点。')

    add_paragraph(doc, '索引设计包括：主键索引使用id字段；状态查询索引用于按状态查询；类别查询索引用于按类别查询；发布时间排序索引使用降序索引，用于按时间倒序查询；发布者查询索引用于查询用户发布的物品；站点查询索引用于查询站点的物品。')

    add_paragraph(doc, '外键约束包括：发布者ID外键关联用户表，用户删除时级联删除失物；站点ID外键关联站点表，站点删除时将站点ID置为NULL。')

    add_paragraph(doc, '认领申请表存储认领申请信息。联合唯一索引使用物品ID和认领者ID，保证同一用户对同一物品只能申请一次。申请状态字段存储申请状态，包括0待审核、1已完成、2已拒绝、3已锁定、4已通过待取件。置信度评分字段存储置信度评分，范围为0到100，可为NULL。拒绝次数字段记录拒绝次数，默认为0，达到3次自动锁定。取件照片和取件签名字段存储取件照片和签名的URL。')

    add_paragraph(doc, '交物日志表记录物品交付到站点的日志。物品ID唯一索引保证一个物品只有一条交物日志。巡检状态字段存储巡检状态，包括1待巡检、2已巡检、3已作废。积分发放标识字段标识是否已发放积分，0表示否，1表示是，防止重复发放。巡检人外键关联到用户表，记录巡检人。')

    add_paragraph(doc, '积分日志表记录所有积分变动，支持审计和追溯。变动积分字段存储变动积分，正数表示增加，负数表示减少。操作类型字段存储操作类型，包括巡检发放、取件发放、积分回滚、积分扣除。关联物品ID和关联申请ID外键关联到相关的失物和认领申请，支持追溯积分来源。操作人ID外键关联到操作人，用于管理员手动调整积分。')

    add_paragraph(doc, '其他核心表包括：招领站点表存储站点基本信息，管理员ID外键关联到管理员。寻物启事表存储寻物启事，匹配物品ID外键关联到匹配的失物。监控调阅日志表记录监控调阅申请和审批。争议处理表记录争议申诉和处理结果。举报日志表记录用户举报和处理情况。')

    doc.add_heading('5.5.3 性能优化策略', 3)

    add_paragraph(doc, '索引优化针对常用查询场景设计复合索引。公开物品列表查询使用复合索引，包括物品状态、类别、发布时间字段，支持按状态和类别筛选，按发布时间倒序排序。用户积分排行榜使用降序索引，使用积分字段，支持快速查询TOP100用户。站点待巡检物品查询使用复合索引，包括站点ID和巡检状态字段，支持站点管理员快速查看待处理任务。')

    add_paragraph(doc, '缓存策略使用Redis缓存热点数据，减少数据库访问压力。用户信息缓存30分钟，Key格式为user加冒号加用户ID。物品详情缓存5分钟，Key格式为item加冒号加物品ID。积分排行榜使用有序集合数据结构缓存10分钟，Key为leaderboard加冒号加credit。活跃站点列表缓存1小时，Key为drop_points加冒号加active。缓存更新策略：用户信息变更时删除缓存，物品状态变更时删除物品缓存和列表缓存，积分变更时异步更新排行榜缓存。')

    add_paragraph(doc, '分区策略对于日志类表，包括积分日志表、监控调阅日志表、举报日志表，采用按月分区，提高历史数据查询效率，便于归档和清理过期数据。分区策略使用RANGE分区，按创建时间字段的月份分区，每月创建一个分区。查询时MySQL自动根据时间范围选择相应的分区，减少扫描的数据量，提高查询性能。')

    doc.add_heading('5.5.4 数据库安全设计', 3)

    add_paragraph(doc, '访问控制数据库采用最小权限原则，应用程序账号只授予SELECT、INSERT、UPDATE权限，不授予DELETE和DROP权限，防止误操作导致数据丢失。只读账号只授予SELECT权限，用于数据分析和报表查询。管理员账号拥有完整权限，仅用于运维操作，使用强密码并定期更换。')

    add_paragraph(doc, '敏感数据保护用户密码使用BCrypt算法加密，强度为10，每次加密结果不同但都能验证通过，防止彩虹表攻击。手机号和邮箱在应用层脱敏展示，例如135星号星号星号星号8888。学号部分脱敏，例如202001星号星号星号星号。取件照片的存储路径包含随机UUID，防止遍历下载。所有敏感操作记录到审计日志，包括管理员操作、批量删除、权限变更、监控调阅等。')

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计报告_格式统一版.docx'
    doc.save(output_path)

    print('\n' + '=' * 60)
    print(f'✓ Word文档已生成: {output_path}')
    print('=' * 60)
    print('文档特点：')
    print('- 所有段落字体格式完全统一')
    print('- 无任何粗体混杂')
    print('- 图片无图注符号')
    print('- UML图完全中文化')
    print('=' * 60)

if __name__ == '__main__':
    import os
    os.makedirs('D:\\gdut\\school\\docs\\images', exist_ok=True)
    main()
