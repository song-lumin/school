-- ============================================================
-- Sprint 2 新增表
-- 执行前提：01_schema.sql 已执行
-- ============================================================
USE school_lost_found;

-- ----------------------------
-- Table structure for dispute (纠纷申诉表, Sprint 2)
-- ----------------------------
DROP TABLE IF EXISTS `dispute`;
CREATE TABLE `dispute` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `applicant_id` BIGINT NOT NULL COMMENT '申诉人ID',
  `apply_id` BIGINT DEFAULT NULL COMMENT '关联认领申请ID（纠纷申诉时必填）',
  `item_id` BIGINT DEFAULT NULL COMMENT '关联物品ID（丢失申诉时必填）',
  `dispute_type` VARCHAR(50) NOT NULL COMMENT '工单类型：ITEM_MISMATCH=物品不符, ITEM_DAMAGED=物品损坏, FALSE_CLAIM=物品被冒领, ITEM_NOT_FOUND=找不到物品, OTHER=其他纠纷',
  `description` VARCHAR(1000) NOT NULL COMMENT '申诉描述',
  `evidence_images` TEXT COMMENT '证据图片（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1通过 2驳回',
  `handler_note` VARCHAR(1000) DEFAULT NULL COMMENT '处理意见',
  `handle_result` VARCHAR(30) DEFAULT NULL COMMENT '找不到物品核查结论：FOUND/NOT_DELIVERED/STOLEN',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `point_check_note` VARCHAR(1000) DEFAULT NULL COMMENT '站点现场核验陈述（只写事实，不写结论）',
  `point_checker_id` BIGINT DEFAULT NULL COMMENT '回填核验的投放点管理员ID',
  `point_checked_at` DATETIME DEFAULT NULL COMMENT '站点回填时间',
  `point_escalated_at` DATETIME DEFAULT NULL COMMENT '最近一次催办时间（站点超24h未回填时由定时任务写入）',
  `point_remind_count` TINYINT NOT NULL DEFAULT 0 COMMENT '催办次数（最多3次）',
  `site_unresponsive` TINYINT NOT NULL DEFAULT 0 COMMENT '定责时站点仍未回填则强制标记1=站点未响应',
  `site_unresponsive_at` DATETIME DEFAULT NULL COMMENT '站点未响应标记时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_apply_id` (`apply_id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_handler_id` (`handler_id`),
  CONSTRAINT `fk_dispute_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_dispute_apply` FOREIGN KEY (`apply_id`) REFERENCES `claim_apply` (`id`),
  CONSTRAINT `fk_dispute_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`),
  CONSTRAINT `fk_dispute_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉工单表（纠纷+丢失）';
