-- 校园失物招领及诚信积分系统 - 数据库建表脚本

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user
-- ----------------------------
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
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_id` (`student_id`),
  KEY `idx_role` (`role`),
  KEY `idx_credit_score` (`credit_score` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- Table structure for drop_point
-- ----------------------------
DROP TABLE IF EXISTS `drop_point`;
CREATE TABLE `drop_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '站点ID',
  `name` VARCHAR(100) NOT NULL COMMENT '站点名称',
  `location` VARCHAR(255) NOT NULL COMMENT '位置描述',
  `admin_id` BIGINT DEFAULT NULL COMMENT '管理员ID',
  `has_camera` TINYINT NOT NULL DEFAULT 0 COMMENT '是否有监控：0-无, 1-有',
  `camera_info` VARCHAR(500) DEFAULT NULL COMMENT '监控信息（JSON格式）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用, 1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_admin_id` (`admin_id`),
  CONSTRAINT `fk_drop_point_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招领站点表';

-- ----------------------------
-- Table structure for found_item
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
  `perishable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否易腐：0-否, 1-是',
  `item_status` TINYINT NOT NULL DEFAULT 6 COMMENT '物品状态：6-已发布待交物, 0-已作废, 1-公开待认领, 2-认领中, 3-已取件, 4-已归档, 5-已过期',
  `founder_id` BIGINT NOT NULL COMMENT '发布者ID',
  `actual_founder_id` BIGINT DEFAULT NULL COMMENT '实际拾取者ID（用于代发布）',
  `drop_point_id` BIGINT DEFAULT NULL COMMENT '所在站点ID',
  `published_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `claimed_at` DATETIME DEFAULT NULL COMMENT '取件完成时间',
  `expire_warning_sent` TINYINT NOT NULL DEFAULT 0 COMMENT '过期提醒是否已发送：0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_item_status` (`item_status`),
  KEY `idx_founder_id` (`founder_id`),
  KEY `idx_drop_point_id` (`drop_point_id`),
  KEY `idx_category` (`category`),
  KEY `idx_published_at` (`published_at` DESC),
  CONSTRAINT `fk_found_item_founder` FOREIGN KEY (`founder_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_found_item_actual_founder` FOREIGN KEY (`actual_founder_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_found_item_drop_point` FOREIGN KEY (`drop_point_id`) REFERENCES `drop_point` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拾取物品表';

-- ----------------------------
-- Table structure for hand_in_log
-- ----------------------------
DROP TABLE IF EXISTS `hand_in_log`;
CREATE TABLE `hand_in_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `item_id` BIGINT NOT NULL COMMENT '物品ID',
  `drop_point_id` BIGINT NOT NULL COMMENT '站点ID',
  `hand_in_status` TINYINT NOT NULL DEFAULT 0 COMMENT '交物状态：0-待交物, 1-已交物待巡检, 2-已巡检, 3-已作废',
  `handed_in_at` DATETIME DEFAULT NULL COMMENT '交物时间',
  `checked_at` DATETIME DEFAULT NULL COMMENT '巡检时间',
  `checked_by` BIGINT DEFAULT NULL COMMENT '巡检人ID',
  `check_note` VARCHAR(500) DEFAULT NULL COMMENT '巡检备注',
  `credit_issued` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发放积分：0-否, 1-是',
  `credit_issued_at` DATETIME DEFAULT NULL COMMENT '积分发放时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_id` (`item_id`),
  KEY `idx_drop_point_id` (`drop_point_id`),
  KEY `idx_hand_in_status` (`hand_in_status`),
  CONSTRAINT `fk_hand_in_log_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_hand_in_log_drop_point` FOREIGN KEY (`drop_point_id`) REFERENCES `drop_point` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_hand_in_log_checker` FOREIGN KEY (`checked_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交物日志表';

-- ----------------------------
-- Table structure for claim_apply
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
  `credit_rollback` TINYINT NOT NULL DEFAULT 0 COMMENT '积分是否已回滚：0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_claimer` (`item_id`, `claimer_id`),
  KEY `idx_claimer_id` (`claimer_id`),
  KEY `idx_apply_status` (`apply_status`),
  CONSTRAINT `fk_claim_apply_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_claim_apply_claimer` FOREIGN KEY (`claimer_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认领申请表';

-- ----------------------------
-- Table structure for credit_log
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
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at` DESC),
  KEY `idx_operation_type` (`operation_type`),
  CONSTRAINT `fk_credit_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_credit_log_item` FOREIGN KEY (`related_item_id`) REFERENCES `found_item` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_credit_log_apply` FOREIGN KEY (`related_apply_id`) REFERENCES `claim_apply` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_credit_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分变动日志表';

-- ----------------------------
-- Table structure for lost_notice
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
  CONSTRAINT `fk_lost_notice_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lost_notice_matched_item` FOREIGN KEY (`matched_item_id`) REFERENCES `found_item` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='寻物启事表';

-- ----------------------------
-- Table structure for camera_log (Sprint 2)
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
  CONSTRAINT `fk_camera_log_drop_point` FOREIGN KEY (`drop_point_id`) REFERENCES `drop_point` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_camera_log_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_camera_log_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_camera_log_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监控调阅日志表';

SET FOREIGN_KEY_CHECKS = 1;
