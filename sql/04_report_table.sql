-- ============================================================
-- 举报审查表（Sprint 1 管理后台 - 举报机制）
-- 执行前提：01_schema.sql 已执行
-- ============================================================
USE school_lost_found;

-- ----------------------------
-- Table structure for report_log (举报工单表)
-- ----------------------------
DROP TABLE IF EXISTS `report_log`;
CREATE TABLE `report_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
  `item_id` BIGINT NOT NULL COMMENT '被举报物品ID',
  `report_type` VARCHAR(50) NOT NULL COMMENT '举报类型：FAKE_FAKE=虚假投放, DESC_MISMATCH=描述不符, OTHER=其他',
  `description` VARCHAR(1000) NOT NULL COMMENT '举报描述',
  `evidence_images` TEXT COMMENT '证据图片（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1成立(已处理) 2不成立 3已回滚积分',
  `handler_note` VARCHAR(1000) DEFAULT NULL COMMENT '处理意见',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_status` (`status`),
  KEY `idx_handler_id` (`handler_id`),
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_report_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`),
  CONSTRAINT `fk_report_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报工单表';
