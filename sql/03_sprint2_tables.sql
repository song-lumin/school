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
  `dispute_type` VARCHAR(50) NOT NULL COMMENT '工单类型：ITEM_MISMATCH/OTHER=纠纷申诉, ITEM_LOST=丢失申诉',
  `description` VARCHAR(1000) NOT NULL COMMENT '申诉描述',
  `evidence_images` TEXT COMMENT '证据图片（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1通过 2驳回',
  `handler_note` VARCHAR(1000) DEFAULT NULL COMMENT '处理意见',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
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
