-- V3.0 iteration: moderation takedown and appeal fields.
-- Run once after sql/01_schema.sql through sql/07_data_fix.sql.
USE school_lost_found;

ALTER TABLE `found_item`
  ADD COLUMN `takedown_reason` VARCHAR(500) DEFAULT NULL COMMENT '下架原因',
  ADD COLUMN `takedown_at` DATETIME DEFAULT NULL COMMENT '下架时间',
  ADD COLUMN `takedown_by` BIGINT DEFAULT NULL COMMENT '下架操作人ID',
  ADD COLUMN `appeal_status` TINYINT DEFAULT NULL COMMENT '申诉状态：1待审核、2通过、3驳回',
  ADD COLUMN `appeal_reason` VARCHAR(500) DEFAULT NULL COMMENT '申诉理由',
  ADD COLUMN `appeal_at` DATETIME DEFAULT NULL COMMENT '申诉时间';

ALTER TABLE `lost_notice`
  ADD COLUMN `takedown_reason` VARCHAR(500) DEFAULT NULL COMMENT '下架原因',
  ADD COLUMN `takedown_at` DATETIME DEFAULT NULL COMMENT '下架时间',
  ADD COLUMN `takedown_by` BIGINT DEFAULT NULL COMMENT '下架操作人ID',
  ADD COLUMN `appeal_status` TINYINT DEFAULT NULL COMMENT '申诉状态：1待审核、2通过、3驳回',
  ADD COLUMN `appeal_reason` VARCHAR(500) DEFAULT NULL COMMENT '申诉理由',
  ADD COLUMN `appeal_at` DATETIME DEFAULT NULL COMMENT '申诉时间';

-- lost_notice.status: 3 = removed by moderation.
