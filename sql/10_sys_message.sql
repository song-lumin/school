-- 站内消息：积分发放/回滚、认领作废、归还入库等业务通知
CREATE TABLE IF NOT EXISTS `sys_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id` BIGINT NOT NULL COMMENT '接收人',
  `title` VARCHAR(120) NOT NULL COMMENT '标题',
  `content` VARCHAR(2000) NOT NULL COMMENT '正文',
  `biz_type` VARCHAR(40) DEFAULT NULL COMMENT '业务类型：CREDIT / DISPUTE / RETURN / CLAIM',
  `related_item_id` BIGINT DEFAULT NULL COMMENT '关联物品',
  `related_apply_id` BIGINT DEFAULT NULL COMMENT '关联认领申请',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';
