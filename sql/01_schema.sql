-- ============================================================
-- 校园失物招领及诚信积分系统 - 数据库建表脚本（01_schema.sql）
-- ------------------------------------------------------------
-- 作用：创建系统核心的 8 张表：
--   user            用户表（含角色、诚信积分）
--   drop_point      招领站点表（失物存放点，可配监控）
--   found_item      拾取物品表（核心业务表，含状态机）
--   hand_in_log     交物日志表（站点巡检记录，一物一日志）
--   claim_apply     认领申请表（防伪问答 + 审核状态机）
--   credit_log      积分变动日志表（积分台账，可审计）
--   lost_notice     寻物启事表（失主发布，可关联匹配物品）
--   camera_log      监控调阅日志表（Sprint 2 丢失申诉用）
-- 执行顺序：本脚本为第一个脚本，其余脚本均以其为前提。
-- 注意：开头关闭外键检查以便 DROP TABLE 顺序无关，末尾恢复。
-- ============================================================

-- 设置客户端字符集为 utf8mb4（支持 emoji 等完整 Unicode）
SET NAMES utf8mb4;
-- 暂时关闭外键检查，允许任意顺序 DROP/CREATE 表
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户表（user）：系统所有账号，RBAC 三种角色
-- ----------------------------
-- 先删后建，保证脚本可重复执行
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `student_id` VARCHAR(20) DEFAULT NULL COMMENT '学号/工号',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role` ENUM('USER', 'POINT_ADMIN', 'SYS_ADMIN') NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户, POINT_ADMIN-站点管理员, SYS_ADMIN-系统管理员',
  `credit_score` INT NOT NULL DEFAULT 0 COMMENT '诚信积分',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-正常',
  `allow_leaderboard` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许上榜：0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  -- 用户名与学号全局唯一（登录凭据 + 防止重复注册）
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_id` (`student_id`),
  KEY `idx_role` (`role`),
  -- 积分降序索引：光荣榜/排行榜 TOP N 查询专用
  KEY `idx_credit_score` (`credit_score` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 招领站点表（drop_point）：失物集中存放的物理站点
-- 每个站点可指定一名站点管理员，可配备监控设备
-- ----------------------------
DROP TABLE IF EXISTS `drop_point`;
CREATE TABLE `drop_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '站点ID',
  `name` VARCHAR(100) NOT NULL COMMENT '站点名称',
  `location` VARCHAR(255) NOT NULL COMMENT '位置描述',
  `admin_id` BIGINT DEFAULT NULL COMMENT '管理员ID',
  `has_camera` TINYINT NOT NULL DEFAULT 0 COMMENT '是否有监控：0-无, 1-有',
  `camera_info` VARCHAR(500) DEFAULT NULL COMMENT '监控信息（JSON格式：数量/覆盖范围/留存天数）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用, 1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_admin_id` (`admin_id`),
  -- 管理员被删时置空，避免站点数据连带丢失
  CONSTRAINT `fk_drop_point_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招领站点表';

-- ----------------------------
-- 拾取物品表（found_item）：核心业务表
-- item_status 状态机：1公开待认领 → 2认领中 → 3已取件；
--                     0已作废（举报成立/人工作废）、4已归档、5已过期
-- ----------------------------
DROP TABLE IF EXISTS `found_item`;
CREATE TABLE `found_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '物品ID',
  `title` VARCHAR(200) NOT NULL COMMENT '物品标题',
  `category` VARCHAR(50) NOT NULL COMMENT '物品类别',
  `description` TEXT COMMENT '物品描述',
  `found_location` VARCHAR(255) NOT NULL COMMENT '拾取地点',
  `found_time` DATETIME NOT NULL COMMENT '拾取时间',
  `images` JSON COMMENT '物品图片（JSON数组）',
  `claim_question` VARCHAR(500) NOT NULL COMMENT '防伪问题',
  `perishable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否易腐：0-否, 1-是（易腐物品保管期更短）',
  `item_status` TINYINT NOT NULL DEFAULT 1 COMMENT '物品状态：1-公开待认领, 0-已作废, 2-认领中, 3-已取件, 4-已归档, 5-已过期',
  `founder_id` BIGINT NOT NULL COMMENT '发布者ID',
  `actual_founder_id` BIGINT DEFAULT NULL COMMENT '实际拾取者ID（用于代发布）',
  `drop_point_id` BIGINT DEFAULT NULL COMMENT '所在站点ID',
  `published_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `claimed_at` DATETIME DEFAULT NULL COMMENT '取件完成时间',
  `expire_warning_sent` TINYINT NOT NULL DEFAULT 0 COMMENT '过期提醒是否已发送：0-否, 1-是（定时任务防重发）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  -- 常用筛选维度建索引：状态/发布者/站点/类别，发布时间降序用于列表排序
  KEY `idx_item_status` (`item_status`),
  KEY `idx_founder_id` (`founder_id`),
  KEY `idx_drop_point_id` (`drop_point_id`),
  KEY `idx_category` (`category`),
  KEY `idx_published_at` (`published_at` DESC),
  -- 发布者被删时物品级联删除；实际拾取者/站点被删时置空保留物品记录
  CONSTRAINT `fk_found_item_founder` FOREIGN KEY (`founder_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_found_item_actual_founder` FOREIGN KEY (`actual_founder_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_found_item_drop_point` FOREIGN KEY (`drop_point_id`) REFERENCES `drop_point` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拾取物品表';

-- ----------------------------
-- 交物日志表（hand_in_log）：站点管理员巡检的依据
-- 一件物品只有一条日志（uk_item_id 唯一约束）；
-- 巡检通过（状态2）后给拾取者发放积分，credit_issued 防止重复发放
-- ----------------------------
DROP TABLE IF EXISTS `hand_in_log`;
CREATE TABLE `hand_in_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `item_id` BIGINT NOT NULL COMMENT '物品ID',
  `drop_point_id` BIGINT NOT NULL COMMENT '站点ID',
  `hand_in_status` TINYINT NOT NULL DEFAULT 1 COMMENT '巡检状态：1-待巡检, 2-已巡检, 3-已作废',
  `handed_in_at` DATETIME DEFAULT NULL COMMENT '登记时间',
  `checked_at` DATETIME DEFAULT NULL COMMENT '巡检时间',
  `checked_by` BIGINT DEFAULT NULL COMMENT '巡检人ID',
  `check_note` VARCHAR(500) DEFAULT NULL COMMENT '巡检备注',
  `credit_issued` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发放积分：0-否, 1-是',
  `credit_issued_at` DATETIME DEFAULT NULL COMMENT '积分发放时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  -- 物品与日志 1:1：防止同一物品产生多条交物日志重复发积分
  UNIQUE KEY `uk_item_id` (`item_id`),
  KEY `idx_drop_point_id` (`drop_point_id`),
  KEY `idx_hand_in_status` (`hand_in_status`),
  -- 物品/站点删除时日志级联删除；巡检人删除时置空保留日志
  CONSTRAINT `fk_hand_in_log_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_hand_in_log_drop_point` FOREIGN KEY (`drop_point_id`) REFERENCES `drop_point` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_hand_in_log_checker` FOREIGN KEY (`checked_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交物日志表';

-- ----------------------------
-- 认领申请表（claim_apply）：失主凭防伪问题答案申请认领
-- apply_status 状态机：0待审核 → 4已通过待取件 → 1已完成；
--                      2已拒绝（满3次→3已锁定）
-- uk_item_claimer 联合唯一：同一用户对同一物品只能申请一次
-- ----------------------------
DROP TABLE IF EXISTS `claim_apply`;
CREATE TABLE `claim_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `item_id` BIGINT NOT NULL COMMENT '物品ID',
  `claimer_id` BIGINT NOT NULL COMMENT '认领者ID',
  `answer` VARCHAR(500) NOT NULL COMMENT '问题答案',
  `apply_status` TINYINT NOT NULL DEFAULT 0 COMMENT '申请状态：0-待审核, 4-已通过待取件, 1-已完成, 2-已拒绝, 3-已锁定',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝理由',
  `reject_count` INT NOT NULL DEFAULT 0 COMMENT '被拒绝次数',
  `pickup_time` DATETIME DEFAULT NULL COMMENT '取件时间',
  `pickup_photo` VARCHAR(255) DEFAULT NULL COMMENT '取件照片URL',
  `pickup_signature` VARCHAR(255) DEFAULT NULL COMMENT '取件签名URL',
  `credit_issued` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发放积分：0-否, 1-是',
  `credit_rollback` TINYINT NOT NULL DEFAULT 0 COMMENT '积分是否已回滚：0-否, 1-是（争议申诉用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  -- 联合唯一：防止同一用户对同一物品重复提交申请（幂等保护）
  UNIQUE KEY `uk_item_claimer` (`item_id`, `claimer_id`),
  KEY `idx_claimer_id` (`claimer_id`),
  KEY `idx_apply_status` (`apply_status`),
  -- 物品或认领者删除时，申请记录级联删除
  CONSTRAINT `fk_claim_apply_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_claim_apply_claimer` FOREIGN KEY (`claimer_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认领申请表';

-- ----------------------------
-- 积分变动日志表（credit_log）：积分系统的唯一事实来源
-- user.credit_score 必须与 SUM(change_amount) 一致（账实一致）；
-- 只增不改，纠错通过 ROLLBACK/ADJUST 补账
-- ----------------------------
DROP TABLE IF EXISTS `credit_log`;
CREATE TABLE `credit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `change_amount` INT NOT NULL COMMENT '变动积分（正数为增加，负数为减少）',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型：CHECK_ISSUE-巡检发放, PICKUP_ISSUE-取件发放, ROLLBACK-回滚, DEDUCT-扣除',
  `related_item_id` BIGINT DEFAULT NULL COMMENT '关联物品ID',
  `related_apply_id` BIGINT DEFAULT NULL COMMENT '关联认领申请ID',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '变动原因',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID（用于管理员操作）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  -- 按用户查台账、按时间倒序展示、按类型统计是三类常用查询
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at` DESC),
  KEY `idx_operation_type` (`operation_type`),
  -- 日志所属用户删除时级联删除；物品/申请/操作人删除时置空保留日志
  CONSTRAINT `fk_credit_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_credit_log_item` FOREIGN KEY (`related_item_id`) REFERENCES `found_item` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_credit_log_apply` FOREIGN KEY (`related_apply_id`) REFERENCES `claim_apply` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_credit_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分变动日志表';

-- ----------------------------
-- 寻物启事表（lost_notice）：失主主动发布的寻物信息
-- 可通过 matched_item_id 关联匹配到的招领物品；
-- Sprint 2 支持启事直接转化为认领申请（见 claim_apply.source_notice_id）
-- ----------------------------
DROP TABLE IF EXISTS `lost_notice`;
CREATE TABLE `lost_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '启事ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `category` VARCHAR(50) NOT NULL COMMENT '物品类别',
  `description` TEXT COMMENT '描述',
  `lost_location` VARCHAR(255) NOT NULL COMMENT '丢失地点',
  `lost_time` DATETIME NOT NULL COMMENT '丢失时间',
  `contact_info` VARCHAR(200) NOT NULL COMMENT '联系方式',
  `images` JSON COMMENT '图片（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待找回, 1-已找回, 2-已关闭',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
  `matched_item_id` BIGINT DEFAULT NULL COMMENT '匹配的物品ID（Sprint 2）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_category` (`category`),
  KEY `idx_created_at` (`created_at` DESC),
  -- 发布者删除时启事级联删除；匹配物品删除时仅置空关联
  CONSTRAINT `fk_lost_notice_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lost_notice_matched_item` FOREIGN KEY (`matched_item_id`) REFERENCES `found_item` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='寻物启事表';

-- ----------------------------
-- 监控调阅日志表（camera_log，Sprint 2 新增）
-- 物品丢失申诉：失主可申请调阅站点监控，需站点/系统管理员审核；
-- 只有 has_camera=1 的站点才支持调阅
-- ----------------------------
DROP TABLE IF EXISTS `camera_log`;
CREATE TABLE `camera_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `drop_point_id` BIGINT NOT NULL COMMENT '站点ID',
  `item_id` BIGINT DEFAULT NULL COMMENT '关联物品ID',
  `apply_reason` VARCHAR(500) NOT NULL COMMENT '调阅原因',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `time_range_start` DATETIME NOT NULL COMMENT '时间范围开始',
  `time_range_end` DATETIME NOT NULL COMMENT '时间范围结束',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核, 1-已同意, 2-已拒绝, 3-已完成',
  `result_note` VARCHAR(500) DEFAULT NULL COMMENT '处理结果备注',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_drop_point_id` (`drop_point_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`),
  -- 站点/申请人删除时级联删除；关联物品/处理人删除时置空保留记录
  CONSTRAINT `fk_camera_log_drop_point` FOREIGN KEY (`drop_point_id`) REFERENCES `drop_point` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_camera_log_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_camera_log_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_camera_log_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监控调阅日志表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;
