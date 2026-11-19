#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
生成系统分析与设计报告 - 包含真实的UML图片
"""

from docx import Document
from docx.shared import Pt, Inches, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
import requests
import base64
import zlib
from io import BytesIO

def setup_document():
    """初始化文档"""
    doc = Document()
    sections = doc.sections
    for section in sections:
        section.top_margin = Cm(2.54)
        section.bottom_margin = Cm(2.54)
        section.left_margin = Cm(3.17)
        section.right_margin = Cm(3.17)

    style = doc.styles['Normal']
    style.font.name = '宋体'
    style.font.size = Pt(12)
    style.paragraph_format.line_spacing = 1.5

    return doc

def plantuml_encode(plantuml_text):
    """编码PlantUML文本为URL格式"""
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
    """通过在线PlantUML服务生成图片"""
    try:
        encoded = plantuml_encode(plantuml_code)
        url = f'http://www.plantuml.com/plantuml/png/{encoded}'

        print(f'正在生成图片: {filename}...')
        response = requests.get(url, timeout=30)

        if response.status_code == 200:
            with open(filename, 'wb') as f:
                f.write(response.content)
            print(f'✓ {filename} 生成成功')
            return True
        else:
            print(f'✗ {filename} 生成失败: HTTP {response.status_code}')
            return False
    except Exception as e:
        print(f'✗ {filename} 生成失败: {str(e)}')
        return False

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

def add_text(doc, text, bold=False):
    """添加文本"""
    p = doc.add_paragraph(text)
    if bold:
        for run in p.runs:
            run.font.bold = True
    return p

def main():
    doc = setup_document()

    # 封面
    add_title(doc, '校园失物招领及诚信积分系统', 0)
    add_title(doc, '系统分析与设计报告', 0)
    doc.add_paragraph()
    doc.add_paragraph()

    # ================== 生成所有UML图 ==================
    print('=' * 60)
    print('开始生成UML图...')
    print('=' * 60)

    # 1. 类图
    class_diagram = '''@startuml
skinparam classAttributeIconSize 0
skinparam backgroundColor white

class User {
  -id: Long
  -username: String
  -password: String
  -realName: String
  -studentId: String
  -phone: String
  -email: String
  -role: UserRole
  -creditScore: Integer
  -status: Integer
  -createdAt: LocalDateTime
  -updatedAt: LocalDateTime
}

class FoundItem {
  -id: Long
  -title: String
  -category: String
  -description: String
  -foundLocation: String
  -foundTime: LocalDateTime
  -images: List<String>
  -claimQuestion: String
  -itemStatus: Integer
  -founderId: Long
  -dropPointId: Long
  -publishedAt: LocalDateTime
  -claimedAt: LocalDateTime
}

class ClaimApply {
  -id: Long
  -itemId: Long
  -claimerId: Long
  -answer: String
  -applyStatus: Integer
  -rejectReason: String
  -rejectCount: Integer
  -confidenceScore: Integer
  -pickupTime: LocalDateTime
  -pickupPhoto: String
  -creditIssued: Integer
}

class DropPoint {
  -id: Long
  -name: String
  -location: String
  -adminId: Long
  -hasCamera: Integer
  -status: Integer
}

class CreditLog {
  -id: Long
  -userId: Long
  -changeAmount: Integer
  -operationType: String
  -relatedItemId: Long
  -reason: String
  -createdAt: LocalDateTime
}

class HandInLog {
  -id: Long
  -itemId: Long
  -dropPointId: Long
  -handInStatus: Integer
  -checkedAt: LocalDateTime
  -checkedBy: Long
  -creditIssued: Integer
}

class LostNotice {
  -id: Long
  -title: String
  -category: String
  -lostLocation: String
  -lostTime: LocalDateTime
  -status: Integer
  -publisherId: Long
  -matchedItemId: Long
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

    class_diagram_file = 'D:\\gdut\\school\\docs\\images\\class_diagram.png'
    generate_plantuml_image(class_diagram, class_diagram_file)

    # 2. 用户注册登录时序图
    auth_sequence = '''@startuml
skinparam backgroundColor white
actor 用户
participant "AuthController" as Controller
participant "UserService" as Service
participant "PasswordEncoder" as Encoder
participant "UserMapper" as Mapper
database MySQL

== 用户注册流程 ==
用户 -> Controller: POST /api/auth/register
activate Controller
Controller -> Service: register(RegisterRequest)
activate Service
Service -> Mapper: selectByUsername(username)
Mapper -> MySQL: SELECT * FROM user
MySQL --> Mapper: 查询结果
Mapper --> Service: 用户信息
alt 用户名已存在
    Service --> Controller: 抛出异常
    Controller --> 用户: 400 Bad Request
end
Service -> Encoder: encode(password)
Encoder --> Service: 加密密码
Service -> Mapper: insert(User)
Mapper -> MySQL: INSERT INTO user
MySQL --> Mapper: 插入成功
Service -> Service: generateJwtToken(userId)
Service --> Controller: LoginVO(token, userInfo)
deactivate Service
Controller --> 用户: 200 OK
deactivate Controller

@enduml'''

    auth_seq_file = 'D:\\gdut\\school\\docs\\images\\auth_sequence.png'
    generate_plantuml_image(auth_sequence, auth_seq_file)

    # 3. 失物发布时序图
    publish_sequence = '''@startuml
skinparam backgroundColor white
actor 拾取者
participant "FoundItemController" as Controller
participant "FoundItemService" as Service
participant "DropPointService" as PointService
participant "FoundItemMapper" as Mapper
participant "HandInLogMapper" as LogMapper
database MySQL

拾取者 -> Controller: POST /api/found-items
activate Controller
Controller -> Controller: 验证JWT Token
Controller -> Service: publish(FoundItemRequest, userId)
activate Service
Service -> PointService: getById(dropPointId)
activate PointService
PointService -> MySQL: SELECT * FROM drop_point
MySQL --> PointService: DropPoint对象
PointService --> Service: DropPoint对象
deactivate PointService

alt 站点不可用
    Service --> Controller: 抛出异常
    Controller --> 拾取者: 400 Bad Request
end

Service -> Mapper: insert(FoundItem)
activate Mapper
Mapper -> MySQL: INSERT INTO found_item
MySQL --> Mapper: 插入成功, 返回itemId
Mapper --> Service: itemId
deactivate Mapper

Service -> LogMapper: insert(HandInLog)
activate LogMapper
LogMapper -> MySQL: INSERT INTO hand_in_log
MySQL --> LogMapper: 插入成功
LogMapper --> Service: 成功
deactivate LogMapper

Service --> Controller: FoundItemVO(物品详情)
deactivate Service
Controller --> 拾取者: 201 Created
deactivate Controller

@enduml'''

    publish_seq_file = 'D:\\gdut\\school\\docs\\images\\publish_sequence.png'
    generate_plantuml_image(publish_sequence, publish_seq_file)

    # 4. 认领申请时序图
    claim_sequence = '''@startuml
skinparam backgroundColor white
actor 失主
participant "ClaimController" as Controller
participant "ClaimService" as Service
participant "ClaimApplyMapper" as Mapper
participant "FoundItemMapper" as ItemMapper
database MySQL

失主 -> Controller: POST /api/claims/apply
activate Controller
Controller -> Service: apply(ClaimApplyRequest, userId)
activate Service

Service -> ItemMapper: selectById(itemId)
ItemMapper -> MySQL: SELECT * FROM found_item
MySQL --> ItemMapper: FoundItem对象
ItemMapper --> Service: FoundItem对象

alt 物品不可认领
    Service --> Controller: 抛出异常
    Controller --> 失主: 400 Bad Request
end

Service -> Mapper: selectByItemAndClaimer(itemId, userId)
Mapper -> MySQL: SELECT * FROM claim_apply
MySQL --> Mapper: 已存在的申请
Mapper --> Service: ClaimApply对象

alt 已申请过
    Service --> Controller: 抛出异常
    Controller --> 失主: 400 Bad Request
end

Service -> Service: calculateConfidenceScore(answer)
note right: 计算置信度评分(0-100分)

Service -> Mapper: insert(ClaimApply)
Mapper -> MySQL: INSERT INTO claim_apply
MySQL --> Mapper: 插入成功

Service -> ItemMapper: updateStatus(itemId, CLAIMING)
ItemMapper -> MySQL: UPDATE found_item SET item_status=2
MySQL --> ItemMapper: 更新成功

Service --> Controller: ClaimApplyVO(申请详情)
deactivate Service
Controller --> 失主: 201 Created
deactivate Controller

@enduml'''

    claim_seq_file = 'D:\\gdut\\school\\docs\\images\\claim_sequence.png'
    generate_plantuml_image(claim_sequence, claim_seq_file)

    # 5. 取件时序图
    pickup_sequence = '''@startuml
skinparam backgroundColor white
actor 站点管理员
participant "ClaimController" as Controller
participant "ClaimService" as Service
participant "CreditService" as CreditService
participant "ClaimApplyMapper" as Mapper
participant "FoundItemMapper" as ItemMapper
participant "CreditLogMapper" as CreditMapper
database MySQL

站点管理员 -> Controller: POST /api/claims/{id}/pickup
activate Controller
Controller -> Service: pickup(claimId, photo, signature)
activate Service

Service -> Mapper: selectById(claimId)
Mapper -> MySQL: SELECT * FROM claim_apply
MySQL --> Mapper: ClaimApply对象
Mapper --> Service: ClaimApply对象

alt 状态错误
    Service --> Controller: 抛出异常
    Controller --> 站点管理员: 400 Bad Request
end

Service -> Mapper: updatePickup(claimId, photo, signature)
Mapper -> MySQL: UPDATE claim_apply SET pickup_time=NOW()
MySQL --> Mapper: 更新成功

Service -> ItemMapper: updateStatus(itemId, CLAIMED)
ItemMapper -> MySQL: UPDATE found_item SET item_status=3
MySQL --> ItemMapper: 更新成功

Service -> CreditService: issuePickupCredit(claimId)
activate CreditService
CreditService -> CreditMapper: insert(CreditLog)
note right: 失主获得+1积分
CreditMapper -> MySQL: INSERT INTO credit_log
MySQL --> CreditMapper: 插入成功
CreditService -> MySQL: UPDATE user SET credit_score=credit_score+1
MySQL --> CreditService: 更新成功
CreditService --> Service: 积分发放成功
deactivate CreditService

Service --> Controller: ClaimApplyVO(取件完成)
deactivate Service
Controller --> 站点管理员: 200 OK
deactivate Controller

@enduml'''

    pickup_seq_file = 'D:\\gdut\\school\\docs\\images\\pickup_sequence.png'
    generate_plantuml_image(pickup_sequence, pickup_seq_file)

    # 6. 巡检时序图
    check_sequence = '''@startuml
skinparam backgroundColor white
actor 站点管理员
participant "AdminController" as Controller
participant "AdminService" as Service
participant "CreditService" as CreditService
participant "HandInLogMapper" as LogMapper
participant "CreditLogMapper" as CreditMapper
database MySQL

站点管理员 -> Controller: POST /api/admin/check-item
activate Controller
Controller -> Service: checkItem(CheckRequest, adminId)
activate Service

Service -> LogMapper: selectByItemId(itemId)
LogMapper -> MySQL: SELECT * FROM hand_in_log
MySQL --> LogMapper: HandInLog对象
LogMapper --> Service: HandInLog对象

alt 无权限或已巡检
    Service --> Controller: 抛出异常
    Controller --> 站点管理员: 403 Forbidden
end

Service -> LogMapper: updateCheck(itemId, adminId, checkNote)
LogMapper -> MySQL: UPDATE hand_in_log SET hand_in_status=2
MySQL --> LogMapper: 更新成功

Service -> CreditService: issueCheckCredit(itemId, founderId)
activate CreditService
CreditService -> CreditMapper: insert(CreditLog)
note right: 拾取者获得+2积分
CreditMapper -> MySQL: INSERT INTO credit_log
MySQL --> CreditMapper: 插入成功
CreditService -> MySQL: UPDATE user SET credit_score=credit_score+2
MySQL --> CreditService: 更新成功
CreditService -> LogMapper: updateCreditIssued(itemId)
LogMapper -> MySQL: UPDATE hand_in_log SET credit_issued=1
MySQL --> LogMapper: 更新成功
CreditService --> Service: 积分发放成功
deactivate CreditService

Service --> Controller: 巡检完成
deactivate Service
Controller --> 站点管理员: 200 OK
deactivate Controller

@enduml'''

    check_seq_file = 'D:\\gdut\\school\\docs\\images\\check_sequence.png'
    generate_plantuml_image(check_sequence, check_seq_file)

    # 7. 数据库E-R图
    er_diagram = '''@startuml
skinparam backgroundColor white

entity "user" as user {
  * id : BIGINT <<PK>>
  --
  * username : VARCHAR(50)
  * password : VARCHAR(255)
  real_name : VARCHAR(50)
  student_id : VARCHAR(20)
  phone : VARCHAR(20)
  email : VARCHAR(100)
  * role : ENUM
  * credit_score : INT
  * status : TINYINT
}

entity "found_item" as item {
  * id : BIGINT <<PK>>
  --
  * title : VARCHAR(200)
  * category : VARCHAR(50)
  description : TEXT
  * found_location : VARCHAR(255)
  * found_time : DATETIME
  images : JSON
  * claim_question : VARCHAR(500)
  * item_status : TINYINT
  * founder_id : BIGINT <<FK>>
  drop_point_id : BIGINT <<FK>>
}

entity "claim_apply" as claim {
  * id : BIGINT <<PK>>
  --
  * item_id : BIGINT <<FK>>
  * claimer_id : BIGINT <<FK>>
  * answer : VARCHAR(500)
  * apply_status : TINYINT
  reject_reason : VARCHAR(500)
  * reject_count : INT
  confidence_score : INT
  pickup_time : DATETIME
}

entity "drop_point" as point {
  * id : BIGINT <<PK>>
  --
  * name : VARCHAR(100)
  * location : VARCHAR(255)
  admin_id : BIGINT <<FK>>
  * has_camera : TINYINT
  * status : TINYINT
}

entity "credit_log" as credit {
  * id : BIGINT <<PK>>
  --
  * user_id : BIGINT <<FK>>
  * change_amount : INT
  * operation_type : VARCHAR(50)
  related_item_id : BIGINT <<FK>>
  reason : VARCHAR(500)
}

entity "hand_in_log" as handin {
  * id : BIGINT <<PK>>
  --
  * item_id : BIGINT <<FK>>
  * drop_point_id : BIGINT <<FK>>
  * hand_in_status : TINYINT
  checked_at : DATETIME
  checked_by : BIGINT <<FK>>
  * credit_issued : TINYINT
}

entity "lost_notice" as notice {
  * id : BIGINT <<PK>>
  --
  * title : VARCHAR(200)
  * category : VARCHAR(50)
  * lost_location : VARCHAR(255)
  * lost_time : DATETIME
  * status : TINYINT
  * publisher_id : BIGINT <<FK>>
  matched_item_id : BIGINT <<FK>>
}

user ||--o{ item : "发布"
user ||--o{ claim : "认领"
user ||--o{ notice : "发布"
user ||--o{ credit : "积分记录"
user ||--|| point : "管理"

point ||--o{ item : "存放"
point ||--o{ handin : "交物记录"

item ||--o{ claim : "认领申请"
item ||--|| handin : "交物日志"

notice ||--o{ claim : "转认领申请"

@enduml'''

    er_file = 'D:\\gdut\\school\\docs\\images\\er_diagram.png'
    generate_plantuml_image(er_diagram, er_file)

    print('=' * 60)
    print('UML图生成完成！')
    print('=' * 60)

    # ================== 第3章 ==================
    add_title(doc, '第3章 系统设计 - 领域模型（类图）', 1)

    add_title(doc, '3.1 核心领域模型', 2)
    add_text(doc, '本系统的领域模型包含以下核心实体类，它们之间通过关联、聚合等关系相互协作，共同实现校园失物招领及诚信积分管理功能。')
    doc.add_paragraph()

    add_text(doc, '图3-1 系统核心类图', bold=True)
    try:
        doc.add_picture(class_diagram_file, width=Inches(6))
        last_paragraph = doc.paragraphs[-1]
        last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    except:
        add_text(doc, '[类图加载失败]')
    doc.add_paragraph()

    add_title(doc, '3.2 核心类说明', 2)

    add_text(doc, '（1）User（用户类）', bold=True)
    add_text(doc, '管理用户基本信息、角色权限和诚信积分。包含username、password、role、creditScore等属性。支持三种角色：普通用户（USER）、站点管理员（POINT_ADMIN）、系统管理员（SYS_ADMIN）。')
    doc.add_paragraph()

    add_text(doc, '（2）FoundItem（失物类）', bold=True)
    add_text(doc, '记录拾取物品的详细信息。核心属性包括title（标题）、category（类别）、claimQuestion（防伪问题）、itemStatus（物品状态）。物品状态包括：公开待认领、认领中、已取件、已归档等。')
    doc.add_paragraph()

    add_text(doc, '（3）ClaimApply（认领申请类）', bold=True)
    add_text(doc, '管理物品认领申请流程。包含answer（防伪问题答案）、confidenceScore（置信度评分0-100分）、rejectCount（拒绝次数）等属性。申请状态包括：待审核、已通过待取件、已完成、已拒绝、已锁定。')
    doc.add_paragraph()

    add_text(doc, '（4）DropPoint（招领站点类）', bold=True)
    add_text(doc, '管理物理站点信息。包含站点名称、位置、管理员、监控设备等信息。每个站点由一名站点管理员负责日常运营。')
    doc.add_paragraph()

    add_text(doc, '（5）CreditLog（积分日志类）', bold=True)
    add_text(doc, '记录所有积分变动，支持审计。操作类型包括：CHECK_ISSUE（巡检发放+2分）、PICKUP_ISSUE（取件发放+1分）、ROLLBACK（积分回滚）、DEDUCT（积分扣除）。')
    doc.add_paragraph()

    add_text(doc, '（6）HandInLog（交物日志类）', bold=True)
    add_text(doc, '记录物品交付到站点的日志。一个物品对应一条交物日志。巡检状态包括：待巡检、已巡检、已作废。')
    doc.add_paragraph()

    add_text(doc, '（7）LostNotice（寻物启事类）', bold=True)
    add_text(doc, '记录用户发布的寻物启事。支持与失物的智能匹配，启事可以直接转化为认领申请。')
    doc.add_paragraph()

    add_title(doc, '3.3 类关系说明', 2)
    add_text(doc, '• 一个用户可以发布多个失物（User 1对多 FoundItem）')
    add_text(doc, '• 一个用户可以提交多个认领申请（User 1对多 ClaimApply）')
    add_text(doc, '• 一个失物可以有多个认领申请（FoundItem 1对多 ClaimApply）')
    add_text(doc, '• 一个失物对应一条交物日志（FoundItem 1对1 HandInLog）')
    add_text(doc, '• 多个失物存放在一个站点（FoundItem 多对1 DropPoint）')
    add_text(doc, '• 一个站点管理员管理一个站点（User 1对1 DropPoint）')
    add_text(doc, '• 寻物启事可以转化为认领申请（LostNotice 1对多 ClaimApply）')
    doc.add_paragraph()
    doc.add_paragraph()

    # ================== 第4章 ==================
    add_title(doc, '第4章 系统设计 - 对象交互模型（时序图）', 1)

    add_title(doc, '4.1 用户注册登录时序图', 2)
    add_text(doc, '用户注册和登录是系统的基础功能，采用JWT Token进行无状态认证。密码使用BCrypt算法加密，Token有效期为24小时。')
    doc.add_paragraph()

    add_text(doc, '图4-1 用户注册时序图', bold=True)
    try:
        doc.add_picture(auth_seq_file, width=Inches(6))
        last_paragraph = doc.paragraphs[-1]
        last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    except:
        add_text(doc, '[时序图加载失败]')
    doc.add_paragraph()

    add_text(doc, '流程说明：用户提交注册信息后，系统验证用户名和学号的唯一性，使用BCrypt加密密码，将用户信息插入数据库，生成JWT Token并返回给前端。')
    doc.add_paragraph()

    add_title(doc, '4.2 失物发布时序图', 2)
    add_text(doc, '拾取者发现物品后，通过系统发布失物信息，物品将被送到指定站点等待认领。发布时需要填写防伪问题，用于后续验证失主身份。')
    doc.add_paragraph()

    add_text(doc, '图4-2 失物发布时序图', bold=True)
    try:
        doc.add_picture(publish_seq_file, width=Inches(6))
        last_paragraph = doc.paragraphs[-1]
        last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    except:
        add_text(doc, '[时序图加载失败]')
    doc.add_paragraph()

    add_text(doc, '流程说明：拾取者填写物品信息并提交，系统验证站点有效性，插入物品记录，同时创建交物日志（状态为"待巡检"）。站点管理员巡检通过后发放积分。')
    doc.add_paragraph()

    add_title(doc, '4.3 失物认领时序图', 2)
    add_text(doc, '认领流程分为申请认领、审核认领和到站取件三个阶段。')
    doc.add_paragraph()

    add_text(doc, '【第一阶段：申请认领】', bold=True)
    doc.add_paragraph()

    add_text(doc, '图4-3 认领申请时序图', bold=True)
    try:
        doc.add_picture(claim_seq_file, width=Inches(6))
        last_paragraph = doc.paragraphs[-1]
        last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    except:
        add_text(doc, '[时序图加载失败]')
    doc.add_paragraph()

    add_text(doc, '流程说明：失主找到疑似物品后，填写防伪问题答案并提交申请。系统验证物品状态和用户是否已申请过，计算置信度评分（基于答案相似度和用户信用分），插入认领申请记录，更新物品状态为"认领中"。')
    doc.add_paragraph()

    add_text(doc, '【第二阶段：审核认领】', bold=True)
    add_text(doc, '站点管理员查看待审核的认领申请列表，对比物品和答案，决定通过或拒绝。若通过，系统拒绝该物品的其他所有申请；若拒绝，系统检查拒绝次数是否≥3次，若是则自动锁定该申请。')
    doc.add_paragraph()

    add_text(doc, '【第三阶段：到站取件】', bold=True)
    doc.add_paragraph()

    add_text(doc, '图4-4 到站取件时序图', bold=True)
    try:
        doc.add_picture(pickup_seq_file, width=Inches(6))
        last_paragraph = doc.paragraphs[-1]
        last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    except:
        add_text(doc, '[时序图加载失败]')
    doc.add_paragraph()

    add_text(doc, '流程说明：失主携带证件到站点取件，管理员核实身份后拍摄取件照片和电子签名。系统更新认领申请状态为"已完成"，更新物品状态为"已取件"，发放积分给失主（+1分），记录积分变动日志。该操作使用事务保证原子性。')
    doc.add_paragraph()

    add_title(doc, '4.4 站点巡检与积分发放时序图', 2)
    add_text(doc, '物品送到站点后，站点管理员需要进行巡检确认，巡检通过后系统自动发放积分给拾取者。')
    doc.add_paragraph()

    add_text(doc, '图4-5 站点巡检时序图', bold=True)
    try:
        doc.add_picture(check_seq_file, width=Inches(6))
        last_paragraph = doc.paragraphs[-1]
        last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    except:
        add_text(doc, '[时序图加载失败]')
    doc.add_paragraph()

    add_text(doc, '流程说明：站点管理员核实物品信息并填写巡检备注，系统验证管理员权限和巡检状态，更新巡检状态为"已巡检"，发放积分给拾取者（+2分），记录积分变动日志，更新交物日志的credit_issued字段。')
    doc.add_paragraph()

    add_title(doc, '4.5 时序图设计说明', 2)

    add_text(doc, '【认证与授权】', bold=True)
    add_text(doc, '所有API请求都需要携带JWT Token（登录和注册除外）。Spring Security拦截器自动验证Token有效性，Controller层通过Authentication对象获取当前用户ID和角色。')
    doc.add_paragraph()

    add_text(doc, '【事务管理】', bold=True)
    add_text(doc, '涉及多表操作的业务（如取件、巡检）使用@Transactional注解保证原子性。积分发放与状态更新必须在同一事务中完成，要么全部成功，要么全部回滚。')
    doc.add_paragraph()

    add_text(doc, '【异常处理】', bold=True)
    add_text(doc, '所有业务异常统一抛出BusinessException，全局异常处理器@ControllerAdvice捕获异常并返回统一格式的JSON响应。')
    doc.add_paragraph()

    add_text(doc, '【并发控制】', bold=True)
    add_text(doc, '认领申请使用数据库唯一索引(item_id, claimer_id)防止重复提交。物品状态变更使用乐观锁（version字段）防止并发冲突。积分发放使用分布式锁（Redis）防止重复发放。')
    doc.add_paragraph()
    doc.add_paragraph()

    # ================== 第5.5章 ==================
    add_title(doc, '第5章 数据库设计（节选5.5）', 1)

    add_title(doc, '5.5 数据库表详细设计', 2)

    add_title(doc, '5.5.1 数据库E-R图', 3)
    add_text(doc, '系统数据库包含10张核心业务表，它们之间通过外键关系相互关联。')
    doc.add_paragraph()

    add_text(doc, '图5-1 数据库E-R图', bold=True)
    try:
        doc.add_picture(er_file, width=Inches(6))
        last_paragraph = doc.paragraphs[-1]
        last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    except:
        add_text(doc, '[E-R图加载失败]')
    doc.add_paragraph()

    add_title(doc, '5.5.2 核心表设计', 3)

    add_text(doc, '【用户表（user）】', bold=True)
    add_text(doc, '存储用户基本信息、角色和诚信积分。主要字段：id（主键）、username（用户名，唯一）、password（BCrypt加密）、role（角色：USER/POINT_ADMIN/SYS_ADMIN）、credit_score（诚信积分）、status（状态：0-禁用，1-正常）。')
    add_text(doc, '索引：PRIMARY KEY (id)、UNIQUE KEY (username)、UNIQUE KEY (student_id)、INDEX (role)、INDEX (credit_score DESC)。')
    doc.add_paragraph()

    add_text(doc, '【失物表（found_item）】', bold=True)
    add_text(doc, '存储失物详细信息。主要字段：id（主键）、title（标题）、category（类别）、claim_question（防伪问题）、item_status（物品状态：0-已作废，1-公开待认领，2-认领中，3-已取件，4-已归档，5-已过期）、founder_id（发布者ID，外键）、drop_point_id（所在站点ID，外键）。')
    add_text(doc, '索引：PRIMARY KEY (id)、INDEX (item_status)、INDEX (founder_id)、INDEX (drop_point_id)、INDEX (category)、INDEX (published_at DESC)。')
    doc.add_paragraph()

    add_text(doc, '【认领申请表（claim_apply）】', bold=True)
    add_text(doc, '存储认领申请信息。主要字段：id（主键）、item_id（物品ID，外键）、claimer_id（认领者ID，外键）、answer（防伪问题答案）、apply_status（申请状态：0-待审核，1-已完成，2-已拒绝，3-已锁定，4-已通过待取件）、confidence_score（置信度评分0-100）、reject_count（拒绝次数）。')
    add_text(doc, '约束：UNIQUE KEY (item_id, claimer_id)防止重复申请。业务规则：拒绝次数≥3次自动锁定。')
    doc.add_paragraph()

    add_text(doc, '【招领站点表（drop_point）】', bold=True)
    add_text(doc, '存储站点信息。主要字段：id（主键）、name（站点名称）、location（位置）、admin_id（管理员ID，外键）、has_camera（是否有监控）、status（状态）。')
    doc.add_paragraph()

    add_text(doc, '【交物日志表（hand_in_log）】', bold=True)
    add_text(doc, '记录物品交付日志。主要字段：id（主键）、item_id（物品ID，外键，唯一）、drop_point_id（站点ID，外键）、hand_in_status（巡检状态：1-待巡检，2-已巡检，3-已作废）、checked_by（巡检人ID）、credit_issued（是否已发放积分）。')
    add_text(doc, '约束：UNIQUE KEY (item_id)保证一个物品只有一条交物日志。')
    doc.add_paragraph()

    add_text(doc, '【积分日志表（credit_log）】', bold=True)
    add_text(doc, '记录所有积分变动。主要字段：id（主键）、user_id（用户ID，外键）、change_amount（变动积分，正数增加，负数减少）、operation_type（操作类型：CHECK_ISSUE/PICKUP_ISSUE/ROLLBACK/DEDUCT）、related_item_id（关联物品ID）、reason（变动原因）。')
    add_text(doc, '业务规则：所有积分变动必须记录到此表，支持审计和追溯。')
    doc.add_paragraph()

    add_text(doc, '【寻物启事表（lost_notice）】', bold=True)
    add_text(doc, '记录用户发布的寻物启事。主要字段：id（主键）、title（标题）、category（类别）、lost_location（丢失地点）、status（状态：0-待找回，1-已找回，2-已关闭）、publisher_id（发布者ID，外键）、matched_item_id（匹配的物品ID，外键）。')
    doc.add_paragraph()

    add_title(doc, '5.5.3 性能优化与安全', 3)

    add_text(doc, '【索引优化】', bold=True)
    add_text(doc, '• 公开物品列表查询：使用复合索引(item_status, category, published_at)支持按状态、类别筛选和时间排序。')
    add_text(doc, '• 用户积分排行榜：使用降序索引(credit_score DESC)支持快速查询TOP100用户。')
    add_text(doc, '• 站点待巡检物品：使用复合索引(drop_point_id, hand_in_status)支持站点管理员快速查看待处理任务。')
    doc.add_paragraph()

    add_text(doc, '【缓存策略】', bold=True)
    add_text(doc, '使用Redis缓存热点数据：用户信息（TTL 30分钟）、物品详情（TTL 5分钟）、积分排行榜（Sorted Set，TTL 10分钟）、活跃站点列表（TTL 1小时）。缓存更新策略：用户信息变更时删除缓存，物品状态变更时删除物品缓存和列表缓存，积分变更时异步更新排行榜。')
    doc.add_paragraph()

    add_text(doc, '【安全设计】', bold=True)
    add_text(doc, '• 访问控制：应用程序账号只授予SELECT、INSERT、UPDATE权限，不授予DELETE和DROP权限。')
    add_text(doc, '• 敏感数据保护：密码使用BCrypt加密（强度10），手机号/邮箱应用层脱敏展示（135****8888），学号部分脱敏（202001****）。')
    add_text(doc, '• 审计日志：记录所有敏感操作，包括管理员操作、批量删除、权限变更、监控调阅等。')
    doc.add_paragraph()

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计报告_含UML图.docx'
    doc.save(output_path)

    print('\n' + '=' * 60)
    print(f'✓ Word文档已生成: {output_path}')
    print('=' * 60)
    print('文档包含：')
    print('- 第3章：系统设计 - 领域模型（含类图）')
    print('- 第4章：系统设计 - 对象交互模型（含5个时序图）')
    print('- 第5章：数据库设计（含E-R图）')
    print('=' * 60)

if __name__ == '__main__':
    import os
    os.makedirs('D:\\gdut\\school\\docs\\images', exist_ok=True)
    main()
