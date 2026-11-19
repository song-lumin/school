#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
生成系统分析与设计报告 - 优化版
包含精美的中文UML图和规范的文档格式
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

def add_paragraph(doc, text, style=None):
    """添加段落"""
    p = doc.add_paragraph(text, style=style)
    return p

def add_figure(doc, image_path, width=6.0):
    """添加图片（不含图注）"""
    try:
        paragraph = doc.add_paragraph()
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = paragraph.add_run()
        run.add_picture(image_path, width=Inches(width))
    except Exception as e:
        add_paragraph(doc, f'[图片加载失败]')

def main():
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

    doc.add_paragraph('\n\n\n')

    print('=' * 60)
    print('开始生成UML图...')
    print('=' * 60)

    # ================== 生成UML图 ==================

    # 1. 优化的类图（中文标注）
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
    -phone : String
    -email : String
    -role : UserRole
    -creditScore : Integer
    -status : Integer
    -createdAt : LocalDateTime
    -updatedAt : LocalDateTime
}

class "失物类" as FoundItem {
    -id : Long
    -title : String
    -category : String
    -description : String
    -foundLocation : String
    -foundTime : LocalDateTime
    -images : List<String>
    -claimQuestion : String
    -itemStatus : Integer
    -founderId : Long
    -dropPointId : Long
    -publishedAt : LocalDateTime
    -claimedAt : LocalDateTime
}

class "认领申请类" as ClaimApply {
    -id : Long
    -itemId : Long
    -claimerId : Long
    -answer : String
    -applyStatus : Integer
    -rejectReason : String
    -rejectCount : Integer
    -confidenceScore : Integer
    -pickupTime : LocalDateTime
    -pickupPhoto : String
    -creditIssued : Integer
}

class "招领站点类" as DropPoint {
    -id : Long
    -name : String
    -location : String
    -adminId : Long
    -hasCamera : Integer
    -status : Integer
    -createdAt : LocalDateTime
}

class "积分日志类" as CreditLog {
    -id : Long
    -userId : Long
    -changeAmount : Integer
    -operationType : String
    -relatedItemId : Long
    -reason : String
    -createdAt : LocalDateTime
}

class "交物日志类" as HandInLog {
    -id : Long
    -itemId : Long
    -dropPointId : Long
    -handInStatus : Integer
    -checkedAt : LocalDateTime
    -checkedBy : Long
    -creditIssued : Integer
}

class "寻物启事类" as LostNotice {
    -id : Long
    -title : String
    -category : String
    -lostLocation : String
    -lostTime : LocalDateTime
    -status : Integer
    -publisherId : Long
    -matchedItemId : Long
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

note right of User : 用户角色包括：\\n普通用户、站点管理员、系统管理员
note right of FoundItem : 物品状态：公开待认领、\\n认领中、已取件等
note right of ClaimApply : 置信度评分：0-100分\\n拒绝3次自动锁定

@enduml'''

    class_diagram_file = 'D:\\gdut\\school\\docs\\images\\class_diagram.png'
    generate_plantuml_image(class_diagram, class_diagram_file)

    # 2. 用户注册登录时序图（中文）
    auth_sequence = '''@startuml
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 5
skinparam defaultFontSize 11
skinparam defaultFontName SimHei

skinparam sequence {
    ArrowColor #2C7BB6
    ActorBorderColor #2C7BB6
    LifeLineBorderColor #2C7BB6
    ParticipantBorderColor #2C7BB6
    ParticipantBackgroundColor #E8F4F8
    ActorBackgroundColor #D4E6F1
}

actor 用户 as user
participant "认证控制器" as controller
participant "用户服务" as service
participant "密码加密器" as encoder
participant "用户映射器" as mapper
database "数据库" as db

== 用户注册流程 ==
user -> controller : 提交注册信息
activate controller
controller -> service : 调用注册方法
activate service

service -> mapper : 查询用户名是否存在
mapper -> db : SELECT * FROM user
db --> mapper : 返回查询结果
mapper --> service : 返回用户信息

alt 用户名已存在
    service --> controller : 抛出异常
    controller --> user : 返回错误信息
end

service -> encoder : 加密密码
encoder --> service : 返回加密后的密码

service -> mapper : 插入用户记录
mapper -> db : INSERT INTO user
db --> mapper : 插入成功

service -> service : 生成JWT令牌
service --> controller : 返回登录信息
deactivate service

controller --> user : 注册成功
deactivate controller

@enduml'''

    auth_seq_file = 'D:\\gdut\\school\\docs\\images\\auth_sequence.png'
    generate_plantuml_image(auth_sequence, auth_seq_file)

    # 3. 失物发布时序图（中文）
    publish_sequence = '''@startuml
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 5
skinparam defaultFontSize 11
skinparam defaultFontName SimHei

skinparam sequence {
    ArrowColor #2C7BB6
    ActorBorderColor #2C7BB6
    LifeLineBorderColor #2C7BB6
    ParticipantBorderColor #2C7BB6
    ParticipantBackgroundColor #E8F4F8
    ActorBackgroundColor #D4E6F1
}

actor 拾取者 as user
participant "失物控制器" as controller
participant "失物服务" as service
participant "站点服务" as pointService
participant "失物映射器" as mapper
participant "交物日志映射器" as logMapper
database "数据库" as db

user -> controller : 提交失物信息
activate controller
controller -> controller : 验证令牌
controller -> service : 调用发布方法
activate service

service -> pointService : 验证站点有效性
activate pointService
pointService -> db : 查询站点信息
db --> pointService : 返回站点对象
pointService --> service : 站点有效
deactivate pointService

alt 站点不可用
    service --> controller : 抛出异常
    controller --> user : 返回错误
end

service -> mapper : 插入失物记录
activate mapper
mapper -> db : INSERT INTO found_item
db --> mapper : 返回物品ID
mapper --> service : 返回物品ID
deactivate mapper

service -> logMapper : 创建交物日志
activate logMapper
logMapper -> db : INSERT INTO hand_in_log
db --> logMapper : 插入成功
logMapper --> service : 创建成功
deactivate logMapper

service --> controller : 返回失物详情
deactivate service
controller --> user : 发布成功
deactivate controller

@enduml'''

    publish_seq_file = 'D:\\gdut\\school\\docs\\images\\publish_sequence.png'
    generate_plantuml_image(publish_sequence, publish_seq_file)

    # 4. 认领申请时序图（中文）
    claim_sequence = '''@startuml
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 5
skinparam defaultFontSize 11
skinparam defaultFontName SimHei

skinparam sequence {
    ArrowColor #2C7BB6
    ActorBorderColor #2C7BB6
    LifeLineBorderColor #2C7BB6
    ParticipantBorderColor #2C7BB6
    ParticipantBackgroundColor #E8F4F8
    ActorBackgroundColor #D4E6F1
}

actor 失主 as user
participant "认领控制器" as controller
participant "认领服务" as service
participant "认领申请映射器" as mapper
participant "失物映射器" as itemMapper
database "数据库" as db

user -> controller : 提交认领申请
activate controller
controller -> service : 调用申请方法
activate service

service -> itemMapper : 查询物品信息
itemMapper -> db : SELECT * FROM found_item
db --> itemMapper : 返回物品对象
itemMapper --> service : 返回物品信息

alt 物品不可认领
    service --> controller : 抛出异常
    controller --> user : 返回错误
end

service -> mapper : 检查是否已申请
mapper -> db : SELECT * FROM claim_apply
db --> mapper : 返回申请记录
mapper --> service : 返回结果

alt 已申请过
    service --> controller : 抛出异常
    controller --> user : 不能重复申请
end

service -> service : 计算置信度评分
note right : 基于答案相似度\\n和用户信用分

service -> mapper : 插入认领申请
mapper -> db : INSERT INTO claim_apply
db --> mapper : 插入成功

service -> itemMapper : 更新物品状态
itemMapper -> db : UPDATE found_item
db --> itemMapper : 更新成功

service --> controller : 返回申请详情
deactivate service
controller --> user : 申请提交成功
deactivate controller

@enduml'''

    claim_seq_file = 'D:\\gdut\\school\\docs\\images\\claim_sequence.png'
    generate_plantuml_image(claim_sequence, claim_seq_file)

    # 5. 取件时序图（中文）
    pickup_sequence = '''@startuml
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 5
skinparam defaultFontSize 11
skinparam defaultFontName SimHei

skinparam sequence {
    ArrowColor #2C7BB6
    ActorBorderColor #2C7BB6
    LifeLineBorderColor #2C7BB6
    ParticipantBorderColor #2C7BB6
    ParticipantBackgroundColor #E8F4F8
    ActorBackgroundColor #D4E6F1
}

actor 站点管理员 as admin
participant "认领控制器" as controller
participant "认领服务" as service
participant "积分服务" as creditService
participant "认领申请映射器" as mapper
participant "失物映射器" as itemMapper
participant "积分日志映射器" as creditMapper
database "数据库" as db

admin -> controller : 提交取件信息
activate controller
controller -> service : 调用取件方法
activate service

service -> mapper : 查询认领申请
mapper -> db : SELECT * FROM claim_apply
db --> mapper : 返回申请对象
mapper --> service : 返回申请信息

alt 状态错误
    service --> controller : 抛出异常
    controller --> admin : 返回错误
end

service -> mapper : 更新取件信息
mapper -> db : UPDATE claim_apply
db --> mapper : 更新成功

service -> itemMapper : 更新物品状态
itemMapper -> db : UPDATE found_item
db --> itemMapper : 更新成功

service -> creditService : 发放积分
activate creditService
creditService -> creditMapper : 插入积分日志
note right : 失主获得+1积分
creditMapper -> db : INSERT INTO credit_log
db --> creditMapper : 插入成功

creditService -> db : 更新用户积分
db --> creditService : 更新成功
creditService --> service : 积分发放成功
deactivate creditService

service --> controller : 返回取件结果
deactivate service
controller --> admin : 取件完成
deactivate controller

@enduml'''

    pickup_seq_file = 'D:\\gdut\\school\\docs\\images\\pickup_sequence.png'
    generate_plantuml_image(pickup_sequence, pickup_seq_file)

    # 6. 巡检时序图（中文）
    check_sequence = '''@startuml
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 5
skinparam defaultFontSize 11
skinparam defaultFontName SimHei

skinparam sequence {
    ArrowColor #2C7BB6
    ActorBorderColor #2C7BB6
    LifeLineBorderColor #2C7BB6
    ParticipantBorderColor #2C7BB6
    ParticipantBackgroundColor #E8F4F8
    ActorBackgroundColor #D4E6F1
}

actor 站点管理员 as admin
participant "管理控制器" as controller
participant "管理服务" as service
participant "积分服务" as creditService
participant "交物日志映射器" as logMapper
participant "积分日志映射器" as creditMapper
database "数据库" as db

admin -> controller : 提交巡检结果
activate controller
controller -> service : 调用巡检方法
activate service

service -> logMapper : 查询交物日志
logMapper -> db : SELECT * FROM hand_in_log
db --> logMapper : 返回日志对象
logMapper --> service : 返回日志信息

alt 无权限或已巡检
    service --> controller : 抛出异常
    controller --> admin : 返回错误
end

service -> logMapper : 更新巡检状态
logMapper -> db : UPDATE hand_in_log
db --> logMapper : 更新成功

service -> creditService : 发放积分
activate creditService
creditService -> creditMapper : 插入积分日志
note right : 拾取者获得+2积分
creditMapper -> db : INSERT INTO credit_log
db --> creditMapper : 插入成功

creditService -> db : 更新用户积分
db --> creditService : 更新成功

creditService -> logMapper : 更新积分发放标识
logMapper -> db : UPDATE hand_in_log
db --> logMapper : 更新成功
creditService --> service : 积分发放成功
deactivate creditService

service --> controller : 返回巡检结果
deactivate service
controller --> admin : 巡检完成
deactivate controller

@enduml'''

    check_seq_file = 'D:\\gdut\\school\\docs\\images\\check_sequence.png'
    generate_plantuml_image(check_sequence, check_seq_file)

    # 7. 数据库E-R图（中文优化）
    er_diagram = '''@startuml
skinparam backgroundColor #FEFEFE
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontSize 11
skinparam defaultFontName SimHei

skinparam entity {
    BackgroundColor #E8F4F8
    BorderColor #2C7BB6
    BorderThickness 2
    FontSize 11
}

entity "用户表\\n(user)" as user {
  * id : BIGINT <<主键>>
  --
  * username : VARCHAR(50) <<唯一>>
  * password : VARCHAR(255)
  * real_name : VARCHAR(50)
  * student_id : VARCHAR(20) <<唯一>>
  * phone : VARCHAR(20)
  * email : VARCHAR(100)
  * role : ENUM
  * credit_score : INT
  * status : TINYINT
}

entity "失物表\\n(found_item)" as item {
  * id : BIGINT <<主键>>
  --
  * title : VARCHAR(200)
  * category : VARCHAR(50)
  * description : TEXT
  * found_location : VARCHAR(255)
  * found_time : DATETIME
  * images : JSON
  * claim_question : VARCHAR(500)
  * item_status : TINYINT
  * founder_id : BIGINT <<外键>>
  * drop_point_id : BIGINT <<外键>>
}

entity "认领申请表\\n(claim_apply)" as claim {
  * id : BIGINT <<主键>>
  --
  * item_id : BIGINT <<外键>>
  * claimer_id : BIGINT <<外键>>
  * answer : VARCHAR(500)
  * apply_status : TINYINT
  * reject_count : INT
  * confidence_score : INT
  * pickup_time : DATETIME
}

entity "招领站点表\\n(drop_point)" as point {
  * id : BIGINT <<主键>>
  --
  * name : VARCHAR(100)
  * location : VARCHAR(255)
  * admin_id : BIGINT <<外键>>
  * has_camera : TINYINT
  * status : TINYINT
}

entity "积分日志表\\n(credit_log)" as credit {
  * id : BIGINT <<主键>>
  --
  * user_id : BIGINT <<外键>>
  * change_amount : INT
  * operation_type : VARCHAR(50)
  * related_item_id : BIGINT <<外键>>
  * reason : VARCHAR(500)
}

entity "交物日志表\\n(hand_in_log)" as handin {
  * id : BIGINT <<主键>>
  --
  * item_id : BIGINT <<外键>>
  * drop_point_id : BIGINT <<外键>>
  * hand_in_status : TINYINT
  * checked_at : DATETIME
  * checked_by : BIGINT <<外键>>
  * credit_issued : TINYINT
}

entity "寻物启事表\\n(lost_notice)" as notice {
  * id : BIGINT <<主键>>
  --
  * title : VARCHAR(200)
  * category : VARCHAR(50)
  * lost_location : VARCHAR(255)
  * lost_time : DATETIME
  * status : TINYINT
  * publisher_id : BIGINT <<外键>>
  * matched_item_id : BIGINT <<外键>>
}

user ||--o{ item : 发布
user ||--o{ claim : 认领
user ||--o{ notice : 发布
user ||--o{ credit : 积分记录
user ||--|| point : 管理

point ||--o{ item : 存放
point ||--o{ handin : 交物记录

item ||--o{ claim : 认领申请
item ||--|| handin : 交物日志

notice ||--o{ claim : 转认领申请

@enduml'''

    er_file = 'D:\\gdut\\school\\docs\\images\\er_diagram.png'
    generate_plantuml_image(er_diagram, er_file)

    print('=' * 60)
    print('UML图生成完成！')
    print('=' * 60)

    # ================== 第3章 ==================
    doc.add_heading('第3章 系统设计 - 领域模型（类图）', 1)

    doc.add_heading('3.1 核心领域模型', 2)
    add_paragraph(doc, '本系统的领域模型包含7个核心实体类，它们之间通过关联、聚合等关系相互协作，共同实现校园失物招领及诚信积分管理功能。系统采用面向对象的设计思想，将业务逻辑封装在各个实体类中，确保系统的高内聚、低耦合。')

    add_figure(doc, class_diagram_file, 6.5)

    doc.add_heading('3.2 核心类详细说明', 2)

    doc.add_heading('3.2.1 用户类（User）', 3)
    add_paragraph(doc, '用户类是系统的核心实体之一，负责管理用户的基本信息、角色权限和诚信积分。系统支持三种角色：普通用户（USER）负责发布失物和认领物品，站点管理员（POINT_ADMIN）负责管理站点和审核认领申请，系统管理员（SYS_ADMIN）负责系统全局管理和争议处理。')
    add_paragraph(doc, '核心属性包括：用户ID、用户名（唯一）、密码（BCrypt加密）、真实姓名、学号/工号（唯一）、角色、诚信积分、账号状态等。用户通过拾取和认领物品可以累积诚信积分，积分达到一定数量可以上榜展示，激励拾金不昧的行为。')

    doc.add_heading('3.2.2 失物类（FoundItem）', 3)
    add_paragraph(doc, '失物类记录拾取物品的详细信息，是系统业务流程的核心。每个失物包含标题、类别、描述、拾取地点、拾取时间、物品图片、防伪问题等属性。防伪问题是系统的特色功能，用于在认领环节验证失主身份，防止恶意认领。')
    add_paragraph(doc, '物品状态包括：公开待认领（1）、认领中（2）、已取件（3）、已归档（4）、已过期（5）、已作废（0）。物品从发布到认领完成的整个生命周期由状态字段管理。系统支持易腐物品标记，易腐物品的过期时间为3天，普通物品为30天。')

    doc.add_heading('3.2.3 认领申请类（ClaimApply）', 3)
    add_paragraph(doc, '认领申请类管理物品认领的完整流程。失主申请认领时需要回答防伪问题，系统自动计算置信度评分（0-100分），评分基于答案相似度、用户信用分和历史记录综合计算。低置信度申请会被标记，提醒管理员重点审核。')
    add_paragraph(doc, '申请状态包括：待审核（0）、已完成（1）、已拒绝（2）、已锁定（3）、已通过待取件（4）。为防止恶意认领，系统记录拒绝次数，当同一申请被拒绝3次后自动锁定，不能再次申请。取件时需要拍摄照片和电子签名作为凭证。')

    doc.add_heading('3.2.4 其他核心类', 3)
    add_paragraph(doc, '（1）招领站点类（DropPoint）：管理物理站点信息，包括站点名称、位置、管理员、监控设备等。每个站点由一名站点管理员负责日常运营，包括物品巡检和认领审核。')
    add_paragraph(doc, '（2）积分日志类（CreditLog）：记录所有积分变动，支持审计和追溯。操作类型包括巡检发放（+2分）、取件发放（+1分）、积分回滚、积分扣除等。')
    add_paragraph(doc, '（3）交物日志类（HandInLog）：记录物品交付到站点的日志，一个物品对应一条记录。巡检状态包括待巡检、已巡检、已作废。')
    add_paragraph(doc, '（4）寻物启事类（LostNotice）：记录用户发布的寻物启事，支持与失物的智能匹配，启事可以直接转化为认领申请。')

    doc.add_heading('3.3 类关系说明', 2)
    add_paragraph(doc, '系统各实体类之间通过关联关系形成完整的业务模型：')
    add_paragraph(doc, '• 一个用户可以发布多个失物，一个失物由一个用户发布（1对多关联）')
    add_paragraph(doc, '• 一个用户可以提交多个认领申请，一个认领申请对应一个用户（1对多关联）')
    add_paragraph(doc, '• 一个失物可以有多个认领申请，但最终只有一个通过（1对多关联）')
    add_paragraph(doc, '• 一个失物对应一条交物日志，记录物品的巡检状态（1对1关联）')
    add_paragraph(doc, '• 多个失物存放在一个站点，一个站点可以存放多个失物（多对1关联）')
    add_paragraph(doc, '• 一个站点管理员管理一个站点（1对1关联）')
    add_paragraph(doc, '• 寻物启事可以转化为认领申请，一个启事可能产生多个申请（1对多关联）')

    # ================== 第4章 ==================
    doc.add_heading('第4章 系统设计 - 对象交互模型（时序图）', 1)

    doc.add_heading('4.1 用户注册登录时序图', 2)
    add_paragraph(doc, '用户注册和登录是系统的基础功能，采用JWT Token进行无状态认证。注册时系统验证用户名和学号的唯一性，使用BCrypt算法加密密码（强度为10），将用户信息插入数据库后生成JWT Token返回给前端。Token有效期为24小时，前端将Token存储在localStorage中，后续所有API请求都需要在请求头中携带Token。')

    add_figure(doc, auth_seq_file, 6.5)

    add_paragraph(doc, '登录流程验证用户名和密码的正确性，若验证通过则生成JWT Token。Spring Security拦截器会自动验证Token的有效性，并提取用户ID和角色信息，Controller层通过Authentication对象获取当前用户信息，实现了认证与业务逻辑的分离。')

    doc.add_heading('4.2 失物发布时序图', 2)
    add_paragraph(doc, '拾取者发现物品后，通过系统发布失物信息。发布时需要填写物品标题、类别、描述、拾取地点和时间，上传物品图片，设置防伪问题，并选择将物品送到哪个站点。系统验证站点的有效性（站点状态为"正常"），插入失物记录到found_item表，同时自动创建交物日志到hand_in_log表，初始状态为"待巡检"。')

    add_figure(doc, publish_seq_file, 6.5)

    add_paragraph(doc, '防伪问题是系统的核心功能，用于验证失主身份。问题应该只有真正的失主才知道答案，例如"书包里有什么颜色的笔记本？"、"钱包里有多少张银行卡？"等。站点管理员收到物品后进行巡检，巡检通过后系统自动发放积分给拾取者。')

    doc.add_heading('4.3 失物认领时序图', 2)
    add_paragraph(doc, '认领流程分为三个阶段：申请认领、审核认领和到站取件。整个流程涉及失主、站点管理员和系统的协同交互。')

    doc.add_heading('4.3.1 申请认领阶段', 3)
    add_paragraph(doc, '失主浏览失物列表，找到疑似物品后点击"申请认领"，填写防伪问题答案并提交。系统首先验证物品状态是否为"公开待认领"，然后检查用户是否已经申请过该物品（通过数据库唯一索引保证同一用户对同一物品只能申请一次）。')

    add_figure(doc, claim_seq_file, 6.5)

    add_paragraph(doc, '系统计算置信度评分（0-100分），评分算法综合考虑答案相似度、用户信用分和历史认领成功率。低置信度申请（<40分）会被标记，提醒管理员重点审核。插入认领申请记录后，系统将物品状态更新为"认领中"，防止其他用户申请。')

    doc.add_heading('4.3.2 审核认领阶段', 3)
    add_paragraph(doc, '站点管理员查看待审核的认领申请列表，对比物品和失主的答案，决定通过或拒绝。系统验证管理员是否有权限审核该物品（只能审核自己站点的物品）。若审核通过，系统将申请状态更新为"已通过待取件"，同时拒绝该物品的其他所有申请；若拒绝，系统记录拒绝理由和拒绝次数，若拒绝次数达到3次则自动锁定该申请。')

    doc.add_heading('4.3.3 到站取件阶段', 3)
    add_paragraph(doc, '失主携带证件到站点取件，站点管理员核实身份后拍摄取件照片和电子签名，上传到系统。系统验证申请状态为"已通过待取件"，更新认领申请状态为"已完成"，更新物品状态为"已取件"，发放积分给失主（+1分），记录积分变动日志。')

    add_figure(doc, pickup_seq_file, 6.5)

    add_paragraph(doc, '取件操作涉及多张表的更新（认领申请表、失物表、积分日志表、用户表），系统使用@Transactional注解保证事务的原子性，要么全部成功，要么全部回滚，确保数据一致性。')

    doc.add_heading('4.4 站点巡检与积分发放时序图', 2)
    add_paragraph(doc, '物品送到站点后，站点管理员需要进行巡检确认，核实物品信息是否真实有效。巡检通过后，系统自动发放积分给拾取者（+2分），激励拾金不昧的行为。巡检是积分发放的前置条件，未巡检的物品不会发放积分，防止虚假发布骗取积分。')

    add_figure(doc, check_seq_file, 6.5)

    add_paragraph(doc, '系统验证管理员是否为该站点的管理员，验证巡检状态是否为"待巡检"（防止重复巡检），更新巡检状态为"已巡检"并记录巡检人和时间。积分发放包括插入积分日志、更新用户积分、更新交物日志的credit_issued字段，确保积分只发放一次。')

    doc.add_heading('4.5 时序图设计说明', 2)

    doc.add_heading('4.5.1 认证与授权机制', 3)
    add_paragraph(doc, '系统采用JWT Token进行无状态认证，所有API请求（登录和注册除外）都需要在请求头中携带Token。Spring Security拦截器自动验证Token的有效性，若Token无效或已过期则返回401 Unauthorized错误。Controller层通过Authentication对象获取当前用户ID和角色，Service层根据角色进行权限校验，例如站点管理员只能审核自己站点的认领申请，系统管理员才能处理争议和举报。')

    doc.add_heading('4.5.2 事务管理策略', 3)
    add_paragraph(doc, '涉及多表操作的业务（如取件、巡检、积分发放）使用@Transactional注解保证事务的原子性。例如取件操作需要更新认领申请表、失物表、积分日志表和用户表，若任何一步失败则全部回滚，确保数据的一致性。事务隔离级别设置为READ_COMMITTED，防止脏读但允许不可重复读，在性能和一致性之间取得平衡。')

    doc.add_heading('4.5.3 异常处理机制', 3)
    add_paragraph(doc, '所有业务异常统一抛出BusinessException，全局异常处理器@ControllerAdvice捕获异常并返回统一格式的JSON响应：{code, message, data}。HTTP状态码包括：200成功、400业务错误、401未认证、403无权限、500系统错误。异常信息使用中文描述，便于前端展示给用户。')

    doc.add_heading('4.5.4 并发控制机制', 3)
    add_paragraph(doc, '系统通过多种机制防止并发冲突：（1）认领申请使用数据库唯一索引(item_id, claimer_id)防止重复提交；（2）物品状态变更使用乐观锁（version字段）防止并发修改；（3）积分发放使用分布式锁（Redis）防止重复发放；（4）巡检操作验证状态为"待巡检"防止重复巡检。')

    # ================== 第5.5章 ==================
    doc.add_heading('第5章 数据库设计（节选5.5）', 1)

    doc.add_heading('5.5 数据库表详细设计', 2)

    doc.add_heading('5.5.1 数据库E-R图', 3)
    add_paragraph(doc, '系统数据库包含10张核心业务表，它们之间通过外键关系相互关联，形成完整的数据模型。数据库采用MySQL 8.0，使用InnoDB存储引擎，支持事务和外键约束。字符集统一使用utf8mb4，支持Emoji等特殊字符。')

    add_figure(doc, er_file, 6.5)

    add_paragraph(doc, '各表之间的关系如下：用户表是核心表，与失物表、认领申请表、寻物启事表、积分日志表、站点表都有关联；失物表与认领申请表、交物日志表关联；站点表与失物表、交物日志表关联。所有外键都设置了级联策略，保证数据的参照完整性。')

    doc.add_heading('5.5.2 核心表设计说明', 3)

    add_paragraph(doc, '（1）用户表（user）')
    add_paragraph(doc, '存储用户基本信息、角色和诚信积分。主键为id（BIGINT自增），username和student_id设置了唯一索引保证不重复。密码字段使用VARCHAR(255)存储BCrypt加密后的密码。role字段使用ENUM类型存储角色（USER/POINT_ADMIN/SYS_ADMIN）。credit_score字段存储诚信积分，默认为0，通过应用层逻辑保证不为负数。status字段表示账号状态（0-禁用，1-正常），禁用用户不能登录和操作。')

    add_paragraph(doc, '索引设计：PRIMARY KEY (id)主键索引；UNIQUE KEY uk_username (username)用户名唯一索引；UNIQUE KEY uk_student_id (student_id)学号唯一索引；INDEX idx_role (role)角色查询索引；INDEX idx_credit_score (credit_score DESC)积分排行榜索引。')

    add_paragraph(doc, '（2）失物表（found_item）')
    add_paragraph(doc, '存储失物详细信息。images字段使用JSON类型存储图片URL数组。claim_question字段存储防伪问题，最大长度500字符。item_status字段存储物品状态（0-已作废，1-公开待认领，2-认领中，3-已取件，4-已归档，5-已过期）。founder_id和actual_founder_id都是外键关联到user表，前者是发布者，后者是实际拾取者，支持代发布场景。drop_point_id外键关联到drop_point表，表示物品存放的站点。')

    add_paragraph(doc, '索引设计：PRIMARY KEY (id)主键索引；INDEX idx_item_status (item_status)状态查询索引；INDEX idx_category (category)类别查询索引；INDEX idx_published_at (published_at DESC)发布时间排序索引；INDEX idx_founder_id (founder_id)发布者查询索引；INDEX idx_drop_point_id (drop_point_id)站点查询索引。')

    add_paragraph(doc, '外键约束：FOREIGN KEY (founder_id) REFERENCES user(id) ON DELETE CASCADE用户删除时级联删除失物；FOREIGN KEY (drop_point_id) REFERENCES drop_point(id) ON DELETE SET NULL站点删除时将drop_point_id置为NULL。')

    add_paragraph(doc, '（3）认领申请表（claim_apply）')
    add_paragraph(doc, '存储认领申请信息。UNIQUE KEY uk_item_claimer (item_id, claimer_id)联合唯一索引保证同一用户对同一物品只能申请一次。apply_status字段存储申请状态（0-待审核，1-已完成，2-已拒绝，3-已锁定，4-已通过待取件）。confidence_score字段存储置信度评分（0-100），可为NULL。reject_count字段记录拒绝次数，默认为0，达到3次自动锁定。pickup_photo和pickup_signature字段存储取件照片和签名的URL。')

    add_paragraph(doc, '（4）交物日志表（hand_in_log）')
    add_paragraph(doc, '记录物品交付到站点的日志。UNIQUE KEY uk_item_id (item_id)唯一索引保证一个物品只有一条交物日志。hand_in_status字段存储巡检状态（1-待巡检，2-已巡检，3-已作废）。credit_issued字段标识是否已发放积分（0-否，1-是），防止重复发放。checked_by外键关联到user表，记录巡检人。')

    add_paragraph(doc, '（5）积分日志表（credit_log）')
    add_paragraph(doc, '记录所有积分变动，支持审计和追溯。change_amount字段存储变动积分，正数表示增加，负数表示减少。operation_type字段存储操作类型（CHECK_ISSUE巡检发放、PICKUP_ISSUE取件发放、ROLLBACK积分回滚、DEDUCT积分扣除）。related_item_id和related_apply_id外键关联到相关的失物和认领申请，支持追溯积分来源。operator_id外键关联到操作人，用于管理员手动调整积分。')

    add_paragraph(doc, '（6）其他核心表')
    add_paragraph(doc, '招领站点表（drop_point）存储站点基本信息，admin_id外键关联到管理员。寻物启事表（lost_notice）存储寻物启事，matched_item_id外键关联到匹配的失物。监控调阅日志表（camera_log）记录监控调阅申请和审批。争议处理表（dispute）记录争议申诉和处理结果。举报日志表（report_log）记录用户举报和处理情况。')

    doc.add_heading('5.5.3 性能优化策略', 3)

    add_paragraph(doc, '（1）索引优化')
    add_paragraph(doc, '针对常用查询场景设计复合索引。公开物品列表查询使用复合索引(item_status, category, published_at)，支持按状态和类别筛选，按发布时间倒序排序。用户积分排行榜使用降序索引(credit_score DESC)，支持快速查询TOP100用户。站点待巡检物品查询使用复合索引(drop_point_id, hand_in_status)，支持站点管理员快速查看待处理任务。')

    add_paragraph(doc, '（2）缓存策略')
    add_paragraph(doc, '使用Redis缓存热点数据，减少数据库访问压力。用户信息缓存30分钟，Key格式为user:{userId}。物品详情缓存5分钟，Key格式为item:{itemId}。积分排行榜使用Sorted Set缓存10分钟，Key为leaderboard:credit。活跃站点列表缓存1小时，Key为drop_points:active。缓存更新策略：用户信息变更时删除缓存，物品状态变更时删除物品缓存和列表缓存，积分变更时异步更新排行榜缓存。')

    add_paragraph(doc, '（3）分区策略')
    add_paragraph(doc, '对于日志类表（credit_log、camera_log、report_log）采用按月分区，提高历史数据查询效率，便于归档和清理过期数据。分区策略使用RANGE分区，按created_at字段的月份分区，每月创建一个分区。查询时MySQL自动根据时间范围选择相应的分区，减少扫描的数据量，提高查询性能。')

    doc.add_heading('5.5.4 数据库安全设计', 3)

    add_paragraph(doc, '（1）访问控制')
    add_paragraph(doc, '数据库采用最小权限原则，应用程序账号只授予SELECT、INSERT、UPDATE权限，不授予DELETE和DROP权限，防止误操作导致数据丢失。只读账号只授予SELECT权限，用于数据分析和报表查询。管理员账号拥有完整权限，仅用于运维操作，使用强密码并定期更换。')

    add_paragraph(doc, '（2）敏感数据保护')
    add_paragraph(doc, '用户密码使用BCrypt算法加密，强度为10，每次加密结果不同但都能验证通过，防止彩虹表攻击。手机号和邮箱在应用层脱敏展示，例如135****8888。学号部分脱敏，例如202001****。取件照片的存储路径包含随机UUID，防止遍历下载。所有敏感操作记录到审计日志，包括管理员操作、批量删除、权限变更、监控调阅等。')

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计报告_最终版.docx'
    doc.save(output_path)

    print('\n' + '=' * 60)
    print(f'✓ Word文档已生成: {output_path}')
    print('=' * 60)
    print('文档特点：')
    print('- 格式规范统一，字体层次清晰')
    print('- UML图精美，使用中文标注')
    print('- 类图和时序图完全中文化')
    print('- 内容详实，说明充分')
    print('=' * 60)

if __name__ == '__main__':
    import os
    os.makedirs('D:\\gdut\\school\\docs\\images', exist_ok=True)
    main()
