"""
基于真实系统代码生成第3-6章实验报告
只包含：系统架构设计、数据库设计、接口设计、核心业务流程设计
"""

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from datetime import datetime

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

def add_paragraph_custom(doc, text, font_size=12, bold=False):
    """添加自定义段落"""
    para = doc.add_paragraph(text)
    for run in para.runs:
        run.font.name = '宋体'
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        run.font.size = Pt(font_size)
        run.font.bold = bold
    return para

def create_table_with_data(doc, data, has_header=True):
    """创建表格"""
    table = doc.add_table(rows=len(data), cols=len(data[0]))
    table.style = 'Light Grid Accent 1'

    for i, row_data in enumerate(data):
        row = table.rows[i]
        for j, cell_text in enumerate(row_data):
            cell = row.cells[j]
            cell.text = str(cell_text)
            for para in cell.paragraphs:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER if (i == 0 and has_header) else WD_ALIGN_PARAGRAPH.LEFT
                for run in para.runs:
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                    run.font.size = Pt(10 if len(data[0]) <= 3 else 9)
                    run.font.bold = True if (i == 0 and has_header) else False
    return table

def create_report():
    """创建实验报告（第3-6章）"""
    doc = Document()

    # 设置默认字体
    doc.styles['Normal'].font.name = '宋体'
    doc.styles['Normal']._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    doc.styles['Normal'].font.size = Pt(12)

    # ==================== 第三章 系统架构设计 ====================
    add_heading_custom(doc, '第三章 系统架构设计', level=1)

    add_heading_custom(doc, '3.1 总体架构', level=2)

    arch_text = """本系统采用前后端分离的三层架构设计，分为前端展示层、后端服务层和数据持久层。

前端展示层：
- 基于Vue 3 + Element Plus + Vite构建
- 采用组件化开发，划分为页面组件（views）和公共组件（components）
- 通过Axios与后端进行HTTP通信
- 使用Pinia进行状态管理
- 支持游客浏览、用户操作、管理后台三种访问模式

后端服务层：
- 基于SpringBoot 3框架搭建RESTful API服务
- Controller层：接收HTTP请求，参数校验，调用Service层
- Service层：实现核心业务逻辑，包括状态流转、积分发放、权限控制等
- Mapper层：基于MyBatis Plus进行数据访问
- 安全层：Spring Security + JWT实现认证授权
- AOP切面：实现日志记录、异常处理、幂等性控制
- 定时任务：超期预警、自动作废等

数据持久层：
- MySQL 8.0：主数据存储，存储用户、物品、申请等核心数据
- Redis：缓存层，缓存用户信息、积分统计等热点数据
- 文件存储：本地存储物品图片、签名照片等文件"""

    add_paragraph_custom(doc, arch_text)

    add_heading_custom(doc, '3.2 技术栈选型', level=2)

    tech_data = [
        ['技术分类', '具体技术', '版本/说明'],
        ['前端框架', 'Vue 3', '3.x 组合式API'],
        ['前端UI库', 'Element Plus', '最新版'],
        ['前端构建', 'Vite', '快速构建工具'],
        ['状态管理', 'Pinia', 'Vue 3推荐方案'],
        ['HTTP客户端', 'Axios', '异步请求库'],
        ['后端框架', 'SpringBoot', '3.x 版本'],
        ['安全框架', 'Spring Security', '认证授权'],
        ['ORM框架', 'MyBatis Plus', '简化数据访问'],
        ['数据库', 'MySQL', '8.0版本'],
        ['缓存', 'Redis', '7.x版本'],
        ['容器化', 'Docker + Docker Compose', '容器化部署']
    ]

    create_table_with_data(doc, tech_data)

    add_heading_custom(doc, '3.3 系统分层设计', level=2)

    layer_text = """【Controller层（控制器层）】
职责：接收HTTP请求，参数校验，调用Service层，返回统一响应
核心控制器：
- AuthController：用户认证（注册、登录、个人资料）
- FoundItemController：招领物品管理（发布、查询、投放、作废）
- ClaimController：认领申请管理（提交、审核、领取）
- LostNoticeController：寻物启事管理（发布、智能匹配、识图搜索）
- DropPointController：投放点管理（CRUD、巡检、库存）
- CreditController：积分管理（查询、台账、光荣榜、证明）
- AdminController：系统管理（用户管理、数据看板、积分调整）
- DisputeController：纠纷申诉管理
- CameraLogAdminController：监控调取管理

【Service层（业务逻辑层）】
职责：实现核心业务逻辑，包括状态流转、积分计算、权限控制等
核心服务：
- UserService：用户注册登录、信息维护
- FoundItemService：招领发布、状态流转、超期处理
- ClaimService：认领申请、审核逻辑、3次驳回锁定
- CreditService：积分发放、回滚、一致性保障
- DropPointService：投放点管理、巡检清点
- LostNoticeService：寻物启事、智能匹配
- ImageSearchService：识图搜索、差值哈希算法
- DisputeService：纠纷申诉处理
- RiskControlService：风控检测、频率限制

【Mapper层（数据访问层）】
职责：数据库CRUD操作，基于MyBatis Plus实现
核心Mapper：
- UserMapper：用户表操作
- FoundItemMapper：招领物品表操作
- ClaimApplyMapper：认领申请表操作
- HandInLogMapper：投放登记表操作
- CreditLogMapper：积分台账表操作
- DropPointMapper：投放点表操作
- LostNoticeMapper：寻物启事表操作
- DisputeMapper：纠纷申诉表操作
- CameraLogMapper：监控调取表操作
- ReportLogMapper：举报记录表操作"""

    add_paragraph_custom(doc, layer_text)

    add_heading_custom(doc, '3.4 安全架构设计', level=2)

    security_text = """【认证授权】
- 基于Spring Security + JWT实现无状态认证
- 用户登录后生成JWT Token，后续请求携带Token
- JwtAuthenticationFilter拦截请求验证Token有效性
- 支持角色权限控制：USER（普通用户）、POINT_ADMIN（投放点管理员）、SYS_ADMIN（系统管理员）

【权限控制】
- 基于RBAC（基于角色的访问控制）模型
- 使用@PreAuthorize注解进行方法级权限控制
- 例如：@PreAuthorize("hasRole('SYS_ADMIN')")限制仅系统管理员访问

【密码安全】
- 使用BCrypt算法加密存储密码
- 密码强度要求：至少6位

【接口安全】
- 统一异常处理，避免敏感信息泄露
- 参数校验，防止SQL注入、XSS攻击
- 幂等性控制，防止重复提交"""

    add_paragraph_custom(doc, security_text)

    doc.add_page_break()

    # ==================== 第四章 数据库设计 ====================
    add_heading_custom(doc, '第四章 数据库设计', level=1)

    add_heading_custom(doc, '4.1 数据库E-R图', level=2)

    er_text = """系统核心实体及其关系：

【核心实体】
1. User（用户）：系统所有用户，包括普通用户、投放点管理员、系统管理员
2. FoundItem（招领物品）：拾得者发布的招领信息
3. HandInLog（投放登记）：物品投放到投放点的记录，与FoundItem一对一关系
4. ClaimApply（认领申请）：失主提交的认领申请
5. DropPoint（投放点）：校园内失物投放点位
6. CreditLog（积分台账）：用户积分变动记录
7. LostNotice（寻物启事）：失主发布的寻物信息
8. Dispute（纠纷申诉）：认领后产生纠纷的申诉记录
9. CameraLog（监控调取）：物品丢失后调取监控的记录
10. ReportLog（举报记录）：用户举报记录
11. ItemImageFingerprint（图片指纹）：识图搜索用的图片哈希值

【实体关系】
- User 1:N FoundItem（一个用户可发布多个招领）
- User 1:N ClaimApply（一个用户可提交多个认领申请）
- FoundItem 1:1 HandInLog（一个招领对应一条投放记录）
- FoundItem 1:N ClaimApply（一个招领可有多个认领申请）
- DropPoint 1:N FoundItem（一个投放点可存放多个物品）
- DropPoint 1:N HandInLog（一个投放点有多条投放记录）
- User 1:N CreditLog（一个用户有多条积分记录）
- User 1:N LostNotice（一个用户可发布多个寻物启事）
- ClaimApply 1:N Dispute（一个认领可能产生纠纷）
- DropPoint 1:N CameraLog（一个投放点有多条监控调取记录）
- User 1:N ReportLog（一个用户可提交多个举报）
- FoundItem 1:N ItemImageFingerprint（一个招领可有多张图片指纹）"""

    add_paragraph_custom(doc, er_text)

    add_heading_custom(doc, '4.2 核心数据表设计', level=2)

    add_heading_custom(doc, '4.2.1 用户表（user）', level=3)

    user_fields = [
        ['字段名', '类型', '说明', '约束'],
        ['id', 'BIGINT', '用户ID', '主键，自增'],
        ['username', 'VARCHAR(50)', '用户名', '唯一，非空'],
        ['password', 'VARCHAR(255)', '密码（BCrypt加密）', '非空'],
        ['real_name', 'VARCHAR(50)', '真实姓名', ''],
        ['student_id', 'VARCHAR(20)', '学号/工号', '唯一'],
        ['phone', 'VARCHAR(20)', '手机号', ''],
        ['email', 'VARCHAR(100)', '邮箱', ''],
        ['role', 'ENUM', '角色：USER/POINT_ADMIN/SYS_ADMIN', '非空，默认USER'],
        ['credit_score', 'INT', '诚信积分', '默认0'],
        ['status', 'TINYINT', '状态：0禁用 1启用', '默认1'],
        ['allow_leaderboard', 'TINYINT', '是否展示在光荣榜：0否 1是', '默认1'],
        ['created_at', 'TIMESTAMP', '注册时间', '自动填充'],
        ['updated_at', 'TIMESTAMP', '更新时间', '自动更新']
    ]

    create_table_with_data(doc, user_fields)
    add_paragraph_custom(doc, '索引：idx_student_id, idx_role, idx_credit_score（降序）', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.2 招领物品表（found_item）', level=3)

    item_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '招领物品ID（主键，自增）'],
        ['title', 'VARCHAR(200)', '标题'],
        ['category', 'VARCHAR(50)', '分类：证件类/电子产品/生活用品/其他'],
        ['description', 'TEXT', '物品描述'],
        ['found_location', 'VARCHAR(255)', '拾取地点'],
        ['found_time', 'DATETIME', '拾取时间'],
        ['images', 'JSON', '物品图片（JSON数组）'],
        ['claim_question', 'TEXT', '认领问题（开放性问题）'],
        ['reference_answer', 'TEXT', '参考答案（供发布人参考）'],
        ['perishable', 'TINYINT', '是否易腐：0否 1是，默认0'],
        ['item_status', 'TINYINT', '物品状态：6待投放 0作废 1公开待认领 2申请中 3已发放 4归档 5超期'],
        ['founder_id', 'BIGINT', '发布人ID（外键→user.id）'],
        ['actual_founder_id', 'BIGINT', '实际捡到者ID（代发时记录）'],
        ['drop_point_id', 'BIGINT', '投放点ID（外键→drop_point.id）'],
        ['forwarded_notice_id', 'BIGINT', '转发自哪个寻物启事（外键→lost_notice.id）'],
        ['forwarder_id', 'BIGINT', '转发人ID'],
        ['published_at', 'TIMESTAMP', '公开时间（投放后）'],
        ['claimed_at', 'TIMESTAMP', '认领成功时间'],
        ['expire_warning_sent', 'TINYINT', '90天预警是否已发送，默认0'],
        ['takedown_reason', 'VARCHAR(500)', '下架原因（违规下架）'],
        ['takedown_at', 'TIMESTAMP', '下架时间'],
        ['takedown_by', 'BIGINT', '下架操作人ID'],
        ['appeal_status', 'TINYINT', '申诉状态：0无申诉 1申诉中 2申诉通过 3申诉驳回'],
        ['appeal_reason', 'TEXT', '申诉理由'],
        ['appeal_at', 'TIMESTAMP', '申诉时间'],
        ['created_at', 'TIMESTAMP', '发布时间（自动填充）'],
        ['updated_at', 'TIMESTAMP', '更新时间（自动更新）']
    ]

    create_table_with_data(doc, item_fields)
    add_paragraph_custom(doc, '索引：idx_item_status, idx_category, idx_drop_point_id, idx_published_at, idx_founder_id', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.3 投放登记表（hand_in_log）', level=3)

    handin_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '投放登记ID（主键，自增）'],
        ['item_id', 'BIGINT', '关联物品ID（外键→found_item.id，唯一）'],
        ['drop_point_id', 'BIGINT', '投放点ID（外键→drop_point.id）'],
        ['hand_in_status', 'TINYINT', '投放状态：0待投放 1已投放待巡检 2已清点 3作废'],
        ['handed_in_at', 'TIMESTAMP', '投放时间（点"已投放"时间）'],
        ['checked_at', 'TIMESTAMP', '管理员清点时间'],
        ['checked_by', 'BIGINT', '清点人ID（外键→user.id）'],
        ['check_note', 'TEXT', '清点备注'],
        ['credit_issued', 'TINYINT', '1分是否已发放：0否 1是'],
        ['credit_issued_at', 'TIMESTAMP', '1分发放时间'],
        ['created_at', 'TIMESTAMP', '记录创建时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    create_table_with_data(doc, handin_fields)
    add_paragraph_custom(doc, '索引：idx_hand_in_status, idx_drop_point_id, idx_credit_issued, unique_item_id（item_id唯一）', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.4 认领申请表（claim_apply）', level=3)

    claim_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '认领申请ID（主键，自增）'],
        ['item_id', 'BIGINT', '关联物品ID（外键→found_item.id）'],
        ['source_notice_id', 'BIGINT', '来源寻物启事ID（转发申请时记录）'],
        ['claimer_id', 'BIGINT', '认领人ID（外键→user.id）'],
        ['answer', 'TEXT', '认领问题回答'],
        ['apply_status', 'TINYINT', '申请状态：0待审核 4已同意待领取 1已发放 2驳回 3锁定'],
        ['reject_reason', 'TEXT', '驳回原因'],
        ['reject_count', 'INT', '该用户对该物品累计驳回次数'],
        ['confidence_score', 'INT', '置信度评分（0-100）'],
        ['low_confidence', 'TINYINT', '是否低置信度：0否 1是'],
        ['confidence_reason', 'TEXT', '置信度判断原因'],
        ['pickup_time', 'TIMESTAMP', '现场领取时间'],
        ['pickup_photo', 'VARCHAR(255)', '领取合影照片'],
        ['pickup_signature', 'VARCHAR(255)', '签字图片'],
        ['credit_issued', 'TINYINT', '3+1分是否已发放：0否 1是'],
        ['credit_rollback', 'TINYINT', '积分是否已回滚：0否 1是'],
        ['created_at', 'TIMESTAMP', '申请时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    create_table_with_data(doc, claim_fields)
    add_paragraph_custom(doc, '索引：idx_item_id, idx_claimer_id, idx_apply_status, unique_item_claimer（item_id+claimer_id唯一）', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.5 投放点表（drop_point）', level=3)

    point_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '投放点ID（主键，自增）'],
        ['name', 'VARCHAR(100)', '投放点名称（如：图书馆一楼大厅）'],
        ['location', 'VARCHAR(255)', '具体位置描述'],
        ['admin_id', 'BIGINT', '负责管理员ID（外键→user.id）'],
        ['has_camera', 'TINYINT', '是否有监控：0否 1是'],
        ['camera_info', 'TEXT', '监控信息（设备号、覆盖范围等）'],
        ['status', 'TINYINT', '状态：0停用 1启用'],
        ['created_at', 'TIMESTAMP', '创建时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    create_table_with_data(doc, point_fields)
    add_paragraph_custom(doc, '索引：idx_status, idx_admin_id', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.6 积分台账表（credit_log）', level=3)

    credit_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '积分日志ID（主键，自增）'],
        ['user_id', 'BIGINT', '用户ID（外键→user.id）'],
        ['change_amount', 'INT', '变动积分（正负）'],
        ['operation_type', 'VARCHAR(50)', '操作类型：CHECK_ISSUE/PICKUP_ISSUE/ROLLBACK/DEDUCT/MANUAL'],
        ['related_item_id', 'BIGINT', '关联物品ID（外键→found_item.id）'],
        ['related_apply_id', 'BIGINT', '关联认领申请ID（外键→claim_apply.id）'],
        ['reason', 'VARCHAR(500)', '原因说明'],
        ['operator_id', 'BIGINT', '操作人ID（管理员操作时记录）'],
        ['created_at', 'TIMESTAMP', '发生时间']
    ]

    create_table_with_data(doc, credit_fields)
    add_paragraph_custom(doc, '索引：idx_user_id, idx_created_at（降序）', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.7 寻物启事表（lost_notice）', level=3)

    notice_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '寻物启事ID（主键，自增）'],
        ['title', 'VARCHAR(200)', '标题'],
        ['category', 'VARCHAR(50)', '分类'],
        ['description', 'TEXT', '物品描述'],
        ['lost_location', 'VARCHAR(255)', '丢失地点'],
        ['lost_time', 'DATETIME', '丢失时间'],
        ['contact_info', 'VARCHAR(200)', '联系方式'],
        ['images', 'JSON', '物品图片（JSON数组）'],
        ['status', 'TINYINT', '状态：0已关闭 1进行中 2已找到'],
        ['publisher_id', 'BIGINT', '发布人ID（外键→user.id）'],
        ['matched_item_id', 'BIGINT', '匹配到的招领物品ID（智能匹配）'],
        ['takedown_reason', 'VARCHAR(500)', '下架原因'],
        ['takedown_at', 'TIMESTAMP', '下架时间'],
        ['takedown_by', 'BIGINT', '下架操作人ID'],
        ['appeal_status', 'TINYINT', '申诉状态'],
        ['appeal_reason', 'TEXT', '申诉理由'],
        ['appeal_at', 'TIMESTAMP', '申诉时间'],
        ['created_at', 'TIMESTAMP', '发布时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    create_table_with_data(doc, notice_fields)
    add_paragraph_custom(doc, '索引：idx_status, idx_publisher_id, idx_category', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.8 纠纷申诉表（dispute）', level=3)

    dispute_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '纠纷申诉ID（主键，自增）'],
        ['applicant_id', 'BIGINT', '申请人ID（外键→user.id）'],
        ['apply_id', 'BIGINT', '关联认领申请ID（外键→claim_apply.id）'],
        ['item_id', 'BIGINT', '关联物品ID（外键→found_item.id）'],
        ['dispute_type', 'VARCHAR(50)', '纠纷类型：物品不符/其他'],
        ['description', 'TEXT', '纠纷描述'],
        ['evidence_images', 'JSON', '证据图片（JSON数组）'],
        ['status', 'TINYINT', '状态：0待处理 1通过 2驳回'],
        ['handler_note', 'TEXT', '处理说明'],
        ['handler_id', 'BIGINT', '处理人ID（外键→user.id）'],
        ['handled_at', 'TIMESTAMP', '处理时间'],
        ['created_at', 'TIMESTAMP', '申请时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    create_table_with_data(doc, dispute_fields)
    add_paragraph_custom(doc, '索引：idx_apply_id, idx_status', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.9 监控调取表（camera_log）', level=3)

    camera_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '监控调取记录ID（主键，自增）'],
        ['drop_point_id', 'BIGINT', '投放点ID（外键→drop_point.id）'],
        ['item_id', 'BIGINT', '关联物品ID（外键→found_item.id）'],
        ['apply_reason', 'TEXT', '调取原因'],
        ['applicant_id', 'BIGINT', '申请人ID（外键→user.id）'],
        ['time_range_start', 'DATETIME', '调取时间范围开始'],
        ['time_range_end', 'DATETIME', '调取时间范围结束'],
        ['status', 'TINYINT', '状态：0待处理 1已调取 2无异常 3有异常'],
        ['result_note', 'TEXT', '调取结果说明'],
        ['handler_id', 'BIGINT', '处理人ID（外键→user.id）'],
        ['handled_at', 'TIMESTAMP', '处理时间'],
        ['created_at', 'TIMESTAMP', '申请时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    create_table_with_data(doc, camera_fields)
    add_paragraph_custom(doc, '索引：idx_drop_point_id, idx_status', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.10 举报记录表（report_log）', level=3)

    report_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '举报记录ID（主键，自增）'],
        ['reporter_id', 'BIGINT', '举报人ID（外键→user.id）'],
        ['target_type', 'VARCHAR(50)', '举报目标类型：found_item/lost_notice'],
        ['item_id', 'BIGINT', '被举报招领物品ID'],
        ['notice_id', 'BIGINT', '被举报寻物启事ID'],
        ['report_type', 'VARCHAR(50)', '举报类型：虚假信息/违规内容/其他'],
        ['description', 'TEXT', '举报描述'],
        ['evidence_images', 'JSON', '证据图片（JSON数组）'],
        ['status', 'TINYINT', '状态：0待处理 1已处理 2驳回'],
        ['handler_note', 'TEXT', '处理说明'],
        ['handler_id', 'BIGINT', '处理人ID（外键→user.id）'],
        ['handled_at', 'TIMESTAMP', '处理时间'],
        ['created_at', 'TIMESTAMP', '举报时间'],
        ['updated_at', 'TIMESTAMP', '更新时间']
    ]

    create_table_with_data(doc, report_fields)
    add_paragraph_custom(doc, '索引：idx_item_id, idx_notice_id, idx_status', font_size=10)

    doc.add_paragraph()
    add_heading_custom(doc, '4.2.11 图片指纹表（item_image_fingerprint）', level=3)

    fingerprint_fields = [
        ['字段名', '类型', '说明'],
        ['id', 'BIGINT', '图片指纹ID（主键，自增）'],
        ['item_id', 'BIGINT', '关联物品ID（外键→found_item.id）'],
        ['image_url', 'VARCHAR(500)', '图片URL'],
        ['fingerprint', 'VARCHAR(64)', '图片哈希值（差值哈希算法）'],
        ['created_at', 'TIMESTAMP', '创建时间']
    ]

    create_table_with_data(doc, fingerprint_fields)
    add_paragraph_custom(doc, '索引：idx_item_id, idx_fingerprint', font_size=10)

    doc.add_page_break()

    # ==================== 第五章 接口设计 ====================
    add_heading_custom(doc, '第五章 接口设计', level=1)

    add_heading_custom(doc, '5.1 RESTful API设计规范', level=2)

    api_spec_text = """本系统采用RESTful风格设计API接口，遵循以下规范：

【统一响应格式】
所有接口返回统一的JSON格式：
{
  "code": 200,           // 状态码
  "message": "success",  // 消息
  "data": { ... },       // 数据（可为null）
  "timestamp": 1672502400000  // 时间戳
}

【统一错误码】
- 200：成功
- 400：请求参数错误
- 401：未认证（未登录或Token失效）
- 403：无权限（已登录但权限不足）
- 404：资源不存在
- 409：业务冲突（例如状态不允许此操作）
- 429：请求过于频繁（触发限流）
- 500：服务器内部错误

【HTTP方法语义】
- GET：查询资源
- POST：创建资源
- PUT：更新资源
- DELETE：删除资源

【接口命名规范】
- 使用小写字母和连字符
- 资源名使用复数形式
- 例如：/api/found-items、/api/claims

【认证方式】
- 登录后返回JWT Token
- 后续请求在Header中携带：Authorization: Bearer <token>
- 部分公开接口（如浏览招领列表）无需Token"""

    add_paragraph_custom(doc, api_spec_text)

    add_heading_custom(doc, '5.2 用户认证模块接口', level=2)

    auth_apis = [
        ['方法', '路径', '说明', '权限'],
        ['POST', '/api/auth/register', '用户注册', '无需登录'],
        ['POST', '/api/auth/login', '用户登录', '无需登录'],
        ['POST', '/api/auth/logout', '用户登出', '需登录'],
        ['GET', '/api/auth/me', '获取当前用户信息', '需登录'],
        ['PUT', '/api/auth/profile', '更新个人资料', '需登录'],
        ['PUT', '/api/auth/password', '修改密码', '需登录']
    ]

    create_table_with_data(doc, auth_apis)

    add_heading_custom(doc, '5.3 招领物品模块接口', level=2)

    item_apis = [
        ['方法', '路径', '说明', '权限'],
        ['POST', '/api/found-items', '发布招领', '需登录'],
        ['GET', '/api/found-items', '查询招领列表（公开）', '无需登录'],
        ['GET', '/api/found-items/my', '查询我发布的招领', '需登录'],
        ['GET', '/api/found-items/{id}', '查询招领详情', '无需登录'],
        ['PUT', '/api/found-items/{id}/mark-placed', '点击"已投放"（状态6→1）', '需登录'],
        ['PUT', '/api/found-items/{id}/invalidate', '作废招领', '需登录'],
        ['PUT', '/api/found-items/{id}/forward', '转发到寻物启事', '需登录'],
        ['PUT', '/api/found-items/{id}/archive', '归档招领', '需登录']
    ]

    create_table_with_data(doc, item_apis)

    add_heading_custom(doc, '5.4 认领申请模块接口', level=2)

    claim_apis = [
        ['方法', '路径', '说明', '权限'],
        ['POST', '/api/claims', '提交认领申请（答题）', '需登录'],
        ['GET', '/api/claims', '查询认领申请列表（按物品ID）', '需登录'],
        ['GET', '/api/claims/my', '查询我的认领申请', '需登录'],
        ['GET', '/api/claims/review', '查询待我审核的申请', '需登录'],
        ['GET', '/api/claims/{id}', '查询认领申请详情', '需登录'],
        ['PUT', '/api/claims/{id}/approve', '发布人同意认领（状态0→4）', '需登录'],
        ['PUT', '/api/claims/{id}/reject', '发布人驳回认领（状态0→2）', '需登录'],
        ['PUT', '/api/claims/{id}/pickup', '现场领取（状态4→1，发放3+1分）', '需登录'],
        ['PUT', '/api/claims/{id}/cancel-pickup', '取消认领（解锁其他申请）', '需登录']
    ]

    create_table_with_data(doc, claim_apis)

    add_heading_custom(doc, '5.5 投放点管理模块接口', level=2)

    point_apis = [
        ['方法', '路径', '说明', '权限'],
        ['GET', '/api/drop-points', '查询投放点列表', '无需登录'],
        ['GET', '/api/drop-points/{id}', '查询投放点详情', '无需登录'],
        ['POST', '/api/drop-points', '创建投放点', '系统管理员'],
        ['PUT', '/api/drop-points/{id}', '更新投放点', '系统管理员'],
        ['DELETE', '/api/drop-points/{id}', '删除投放点', '系统管理员'],
        ['GET', '/api/drop-points/{id}/items', '查询待清点物品清单', '投放点管理员/系统管理员'],
        ['GET', '/api/drop-points/{id}/pending-pickups', '查询待领取清单', '投放点管理员/系统管理员'],
        ['PUT', '/api/drop-points/{id}/check', '巡检清点（发放1分）', '投放点管理员/系统管理员'],
        ['GET', '/api/drop-points/{id}/inventory', '查询库存（所有物品）', '投放点管理员/系统管理员']
    ]

    create_table_with_data(doc, point_apis)

    add_heading_custom(doc, '5.6 诚信积分模块接口', level=2)

    credit_apis = [
        ['方法', '路径', '说明', '权限'],
        ['GET', '/api/credits/me', '查询我的积分', '需登录'],
        ['GET', '/api/credits/logs', '查询我的积分台账', '需登录'],
        ['GET', '/api/credits/leaderboard', '查询光荣榜（TOP20）', '需登录'],
        ['GET', '/api/credits/certificate', '获取诚信证明数据', '需登录'],
        ['PUT', '/api/credits/leaderboard-setting', '设置是否展示在光荣榜', '需登录']
    ]

    create_table_with_data(doc, credit_apis)

    add_heading_custom(doc, '5.7 寻物启事模块接口', level=2)

    notice_apis = [
        ['方法', '路径', '说明', '权限'],
        ['POST', '/api/lost-notices', '发布寻物启事', '需登录'],
        ['GET', '/api/lost-notices', '查询寻物启事列表', '无需登录'],
        ['GET', '/api/lost-notices/{id}', '查询寻物启事详情', '无需登录'],
        ['PUT', '/api/lost-notices/{id}/close', '关闭寻物启事', '需登录'],
        ['GET', '/api/lost-notices/{id}/matches', '智能匹配招领（基础版）', '需登录'],
        ['GET', '/api/lost-notices/{id}/matches/v2', '智能匹配招领（增强版）', '需登录'],
        ['POST', '/api/lost-notices/{id}/forward', '转发认领邀请', '需登录'],
        ['GET', '/api/lost-notices/{id}/forwarded-items', '查询转发到本启事的招领', '无需登录'],
        ['POST', '/api/lost-notices/search-by-image', '识图搜索（上传图片）', '需登录']
    ]

    create_table_with_data(doc, notice_apis)

    add_heading_custom(doc, '5.8 系统管理模块接口', level=2)

    admin_apis = [
        ['方法', '路径', '说明', '权限'],
        ['GET', '/api/admin/users', '查询用户列表', '系统管理员'],
        ['PUT', '/api/admin/users/{id}/role', '分配角色', '系统管理员'],
        ['PUT', '/api/admin/users/{id}/status', '禁用/启用用户', '系统管理员'],
        ['GET', '/api/admin/dashboard', '获取数据看板', '系统管理员'],
        ['GET', '/api/admin/expire-warnings', '获取超期预警列表', '系统管理员'],
        ['PUT', '/api/admin/users/{id}/credit', '手动调整积分', '系统管理员'],
        ['GET', '/api/admin/users/{id}/credit-logs', '查询用户积分台账', '系统管理员'],
        ['POST', '/api/admin/credits/rollback', '积分回滚（纠纷申诉）', '系统管理员']
    ]

    create_table_with_data(doc, admin_apis)

    add_heading_custom(doc, '5.9 纠纷申诉模块接口', level=2)

    dispute_apis = [
        ['方法', '路径', '说明', '权限'],
        ['POST', '/api/disputes', '提交纠纷申诉', '需登录'],
        ['GET', '/api/disputes/my', '查询我的申诉', '需登录'],
        ['GET', '/api/admin/disputes', '查询所有申诉（管理员）', '系统管理员'],
        ['PUT', '/api/admin/disputes/{id}/handle', '处理申诉', '系统管理员']
    ]

    create_table_with_data(doc, dispute_apis)

    add_heading_custom(doc, '5.10 其他接口', level=2)

    other_apis = [
        ['方法', '路径', '说明', '权限'],
        ['POST', '/api/upload/image', '上传图片', '需登录'],
        ['GET', '/api/categories', '获取物品分类字典', '无需登录']
    ]

    create_table_with_data(doc, other_apis)

    doc.add_page_break()

    # ==================== 第六章 核心业务流程设计 ====================
    add_heading_custom(doc, '第六章 核心业务流程设计', level=1)

    add_heading_custom(doc, '6.1 核心业务主链路', level=2)

    flow_text = """系统核心业务流程为：发布不公开 → 投放后公开 → 巡检发1分 → 认领领取发3+1分

【Step 1: 发布招领】
- 拾得者填写物品信息：标题、分类、描述、拾取地点、拾取时间
- 上传物品照片（支持多张）
- 设置认领问题（开放性问题，如"请描述钱包里有什么特征"）
- 可选填参考答案（供发布人审核时参考）
- 选择投放点（从下拉列表选择顺路的投放点）
- 状态：item_status = 6（待投放），仅发布人可见
- 自动生成投放记录：hand_in_status = 0（待投放）
- 系统自动计算图片哈希值（差值哈希算法），用于后续识图搜索

【Step 2: 已投放公开】
- 拾得者线下将实物投放到选定的投放点
- 在系统点击"已投放"按钮确认
- 状态流转：
  * item_status: 6→1（公开待认领）
  * hand_in_status: 0→1（已投放待巡检）
  * published_at记录公开时间
- 招领信息对所有用户公开，进入待认领清单
- 失主可以浏览、搜索、识图查找

【Step 3: 巡检核对发1分】
- 投放点管理员定期巡检，查看待清点清单
- 核对实物真实存在，检查是否与描述相符
- 清点确认后：
  * hand_in_status: 1→2（已清点）
  * checked_at记录清点时间
  * checked_by记录清点人ID
  * 可填写清点备注（check_note）
- 系统自动发放积分：
  * 发布人 +1分
  * 更新user.credit_score
  * 插入credit_log记录（operation_type: CHECK_ISSUE）
  * hand_in_log.credit_issued = 1，防止重复发放
  * 使用数据库事务保证一致性

【Step 4: 失主答题申请】
- 失主浏览公开招领，找到疑似物品
- 点击"申请认领"，查看认领问题
- 填写文本回答（不是选择题）
- 提交申请后：
  * apply_status = 0（待审核）
  * item_status: 1→2（认领申请中）
  * 系统可选计算置信度评分（基于关键词匹配）
- 同一用户对同一物品只能提交一次申请（数据库唯一索引约束）
- 如果该用户对该物品已被驳回3次且在24h内，则拒绝提交（apply_status = 3锁定）

【Step 5: 发布人审核】
- 发布人查看待审核的认领申请列表
- 查看失主的回答，对比参考答案（如有）
- 可查看失主的诚信积分、历史认领成功率等信息
- 决策：
  * 同意：apply_status: 0→4（已同意待领取）
    - 同时将该物品的其他申请自动驳回（状态0→2）
    - 通知失主到指定投放点领取
  * 驳回：apply_status: 0→2（驳回）
    - 填写驳回原因（reject_reason）
    - reject_count +1
    - 如果同一用户对同一物品累计驳回3次，自动锁定24h（状态0→3）
    - 物品状态回退：item_status: 2→1（如果没有其他待审核申请）

【Step 6: 现场领取发3+1分】
- 失主到投放点现场领取
- 投放点管理员协助核对：
  * 核对失主身份（学生证/工号牌）
  * 核对物品是否相符
  * 失主签字确认（pickup_signature）
  * 失物合影存档（pickup_photo）
- 确认无误后，管理员在系统点击"确认领取"：
  * apply_status: 4→1（已发放）
  * item_status: 2→3（已实物发放）
  * pickup_time记录领取时间
  * claimed_at记录认领成功时间
- 系统自动发放积分：
  * 发布人 +3分
  * 失主 +1分
  * 更新user.credit_score
  * 插入credit_log记录（operation_type: PICKUP_ISSUE）
  * claim_apply.credit_issued = 1，防止重复发放
  * 使用数据库事务保证一致性

【特殊流程】
1. 代发人发布：actual_founder_id记录实际捡到者，积分发给founder_id（代发人）
2. 先领后检：如果失主先领取，投放点管理员事后清点时补发1分
3. 转发认领邀请：失主通过智能匹配找到物品后，可转发认领邀请给发布人
4. 取消认领：发布人同意后，如果失主不来领取，可取消认领，解锁其他申请"""

    add_paragraph_custom(doc, flow_text)

    add_heading_custom(doc, '6.2 业务状态机设计', level=2)

    state_text = """系统设计了三套独立的状态机，分别管理物品、投放、认领的生命周期：

【招领物品状态机（item_status）- 7个状态】
- 6 (已发布待投放)：初始状态，发布后仅发布人可见
- 0 (已作废)：7天未投放自动作废，或发布人手动作废
- 1 (公开待认领)：投放后公开，所有用户可见
- 2 (认领申请中)：有人提交认领申请
- 3 (已实物发放)：现场领取完成
- 4 (已归档)：自然归档或手动归档
- 5 (超期待线下处置)：365天（易腐7天）无人认领

合法状态流转：
- 6 → 1: 点击"已投放"
- 6 → 0: 7天未投放自动作废，或发布人手动作废
- 1 → 2: 失主提交认领申请
- 2 → 1: 所有申请都被驳回，回到待认领状态
- 2 → 3: 现场领取完成
- 1 → 5: 超期（365天或易腐7天）
- 3 → 4: 归档
- 5 → 4: 线下处置后归档

禁止的跨状态跳转（Service层校验）：
- 不能从6直接跳到3（必须经过1、2）
- 不能从0回到其他状态（作废后不可恢复）
- 不能从4回到其他状态（归档后不可恢复）

【投放登记状态机（hand_in_status）- 4个状态】
- 0 (已发布待投放)：初始状态，发布时自动生成
- 1 (已放置实物待巡检)：点"已投放"后，进入待清点清单
- 2 (管理员已清点核对)：清点后，作为发放1分的依据
- 3 (作废)：物品作废时同步

合法状态流转：
- 0 → 1: 点击"已投放"（同时item_status: 6→1）
- 1 → 2: 投放点管理员清点核对（触发发放1分）
- 0 → 3: 物品作废时同步
- 1 → 3: 物品作废时同步

【认领申请状态机（apply_status）- 5个状态】
- 0 (待发布人审核)：失主提交申请后的初始状态
- 4 (已同意待现场领取)：发布人同意，等待失主到场领取
- 1 (认领成功已发放)：现场领取完成（发3+1分）
- 2 (认领驳回)：发布人驳回
- 3 (已锁定)：3次驳回后24h内锁定

合法状态流转：
- 0 → 4: 发布人同意认领（同时将其他申请改为2）
- 0 → 2: 发布人驳回
- 0 → 3: 同一用户对同一物品3次驳回，锁定24h
- 4 → 1: 现场领取完成（同时item_status: 2→3，发放3+1分）
- 4 → 2: 取消认领（解锁其他申请）

状态流转实现机制：
1. Service层枚举定义合法流转路径（ItemStatus.VALID_TRANSITIONS）
2. 每次更新状态前，先查询当前状态，校验新状态是否在合法路径中
3. 不合法则抛出BusinessException，前端显示错误提示
4. 数据库CHECK约束作为最后防线（可选）"""

    add_paragraph_custom(doc, state_text)

    add_heading_custom(doc, '6.3 诚信积分规则', level=2)

    credit_text = """【积分发放规则】"""
    add_paragraph_custom(doc, credit_text, bold=True)

    credit_rules = [
        ['环节', '角色', '积分', '触发条件', '操作类型'],
        ['巡检核对', '发布人', '+1', '投放点管理员清点核对实物存在', 'CHECK_ISSUE'],
        ['现场领取', '发布人', '+3', '失主现场领取完成', 'PICKUP_ISSUE'],
        ['现场领取', '失主', '+1', '失主现场领取完成', 'PICKUP_ISSUE']
    ]

    create_table_with_data(doc, credit_rules)

    credit_detail = """
【积分发放流程】
1. 触发时机：巡检清点或现场领取
2. 事务开始
3. 更新user.credit_score（UPDATE user SET credit_score = credit_score + amount）
4. 插入credit_log记录（user_id, change_amount, operation_type, related_item_id等）
5. 更新业务表标记（hand_in_log.credit_issued 或 claim_apply.credit_issued）
6. 事务提交
7. 更新Redis缓存（用户积分）

【幂等性保护】
- 每次发放前检查credit_issued标记
- 如果已发放（credit_issued = 1），则跳过发放，返回成功
- 防止网络重试、定时任务重复执行等导致的重复发放

【积分回滚规则】
1. 纠纷申诉通过：回滚发布人+3、失主+1（领取环节积分）
2. 举报审查违规：回滚所有相关积分（巡检+领取）
3. 物品丢失申诉：回滚发布人+1（巡检环节积分）

回滚流程：
1. 事务开始
2. 更新user.credit_score（UPDATE user SET credit_score = credit_score - amount）
3. 插入credit_log记录（change_amount为负值，operation_type: ROLLBACK）
4. 更新claim_apply.credit_rollback = 1
5. 记录operator_id（管理员操作）
6. 事务提交
7. 更新Redis缓存

【特殊说明】
- 代发人发布时，积分发给代发人（founder_id），不发给物品实际失主（actual_founder_id）
- 先领后检：如果失主先领取（状态4→1），投放点管理员事后清点时补发1分给发布人
- 积分不能为负数（CHECK约束：credit_score >= 0）
- 所有积分变动都记录到credit_log表，包含操作类型、关联物品、操作人等信息"""

    add_paragraph_custom(doc, credit_detail)

    add_heading_custom(doc, '6.4 风控规则设计', level=2)

    risk_text = """【发布频率限制】
- 规则：自然周≤3条有效核验（发放积分的记录）
- 实现：AOP切面拦截发布请求，查询本周已发布且发放积分的数量
- 超出限制：标记"高风险"，前端显示警告，管理员重点核对
- 目的：防止恶意刷分

【新账号冷却期】
- 规则：注册7天内发布的招领，前端标记"新手发布"
- 实现：前端判断user.created_at，距今<7天则显示标记
- 目的：投放点管理员清点时重点核对，防止新账号恶意刷分

【3次驳回锁定】
- 规则：同一用户对同一物品认领被驳回≥3次，锁定24小时
- 实现：
  * claim_apply表unique索引（item_id + claimer_id）保证一用户一物品只有一条记录
  * reject_count字段记录驳回次数
  * Service层校验：reject_count >= 3 且 updated_at在24h内 → 拒绝提交
  * 24h后可重新申请，reject_count重置为0
- 目的：防止恶意多次尝试冒领

【幂等性控制】
- 规则：防止重复提交导致的重复操作
- 实现：关键操作（发布、申请、审核、领取、发放积分）检查标记位
- 例如：积分发放前检查credit_issued，已发放则跳过
- 目的：保证数据一致性，防止重复操作

【串通检测（增强功能）】
- 规则：统计同一发布人-失主对的交易次数，超过阈值（如5次）预警
- 实现：定期任务扫描claim_apply表，按(founder_id, claimer_id)分组统计
- 管理员审查：人工判断是否存在串通刷分行为
- 目的：防止两个账号互相发布、互相认领刷积分

【图片查重（增强功能）】
- 规则：检测同一物品重复发布（一物多发刷积分）
- 实现：
  * 发布时计算图片哈希值（差值哈希算法，8x8=64位哈希）
  * 存储到item_image_fingerprint表
  * 新发布时查询相似图片（汉明距离<10）
  * 相似度高则提示可能重复
- 目的：防止同一物品多次发布刷积分

【举报机制】
- 规则：用户可举报虚假投放、违规内容等
- 实现：
  * 用户提交举报（report_log表）
  * 管理员审查（查看证据、调查情况）
  * 处理：下架招领、回滚积分、封禁账号等
- 目的：群众监督，共同维护系统公平性"""

    add_paragraph_custom(doc, risk_text)

    add_heading_custom(doc, '6.5 超期处理流程', level=2)

    expire_text = """【90天预警】
- 定时任务：每天扫描published_at + 90天 且 item_status = 1 的物品
- 发送预警通知（可扩展：站内信、邮件、短信）
- 更新expire_warning_sent = 1，避免重复预警
- 目的：提醒发布人、失主关注即将超期的物品

【自动转超期状态】
- 定时任务：每天扫描
  * 普通物品：published_at + 365天 且 item_status = 1 → item_status = 5
  * 易腐物品：published_at + 7天 且 item_status = 1 → item_status = 5
- 同步更新hand_in_status = 3（作废）
- 目的：长期无人认领的物品标记为超期，等待线下处置

【7天未投放自动作废】
- 定时任务：每天扫描created_at + 7天 且 item_status = 6 的物品
- 自动作废：item_status = 6 → 0
- 同步更新hand_in_status = 3（作废）
- 目的：防止占用系统资源，督促拾得者及时投放

【线下处置流程】
- 管理员将超期物品线下处置（捐赠、销毁等）
- 在系统录入处置说明（可扩展：disposal_log表）
- 更新物品状态：item_status = 5 → 4（归档）
- 目的：完整记录物品生命周期，便于审计"""

    add_paragraph_custom(doc, expire_text)

    # 保存文档
    output_path = 'D:\\gdut\\school\\docs\\系统分析与设计实验报告（第3-6章）.docx'
    doc.save(output_path)
    print(f'实验报告（第3-6章）已生成：{output_path}')
    return output_path

if __name__ == '__main__':
    create_report()
