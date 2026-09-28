-- Sprint 2 claim confidence and local image fingerprint storage.
-- Run once after 01_schema.sql and 03_sprint2_tables.sql.
USE school_lost_found;

ALTER TABLE `claim_apply`
  ADD COLUMN `source_notice_id` BIGINT DEFAULT NULL COMMENT '来源寻物启事ID' AFTER `item_id`,
  ADD COLUMN `confidence_score` TINYINT NOT NULL DEFAULT 100 COMMENT '回答置信度建议分 0-100' AFTER `reject_count`,
  ADD COLUMN `low_confidence` TINYINT NOT NULL DEFAULT 0 COMMENT '是否低置信度提示' AFTER `confidence_score`,
  ADD COLUMN `confidence_reason` VARCHAR(255) DEFAULT NULL COMMENT '置信度提示原因' AFTER `low_confidence`,
  ADD KEY `idx_source_notice_id` (`source_notice_id`),
  ADD CONSTRAINT `fk_claim_apply_source_notice` FOREIGN KEY (`source_notice_id`) REFERENCES `lost_notice` (`id`) ON DELETE SET NULL;

CREATE TABLE `item_image_fingerprint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `item_id` BIGINT NOT NULL,
  `image_url` VARCHAR(500) NOT NULL,
  `hash_value` CHAR(16) NOT NULL COMMENT '64-bit dHash as lower-case hexadecimal',
  `algorithm_version` VARCHAR(32) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_image` (`item_id`, `image_url`),
  KEY `idx_fingerprint_hash` (`hash_value`),
  CONSTRAINT `fk_image_fingerprint_item` FOREIGN KEY (`item_id`) REFERENCES `found_item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招领物品图片感知哈希';
