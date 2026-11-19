CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  dispute_id BIGINT DEFAULT NULL COMMENT '关联纠纷工单，工单内三方沟通',
  item_id BIGINT DEFAULT NULL COMMENT '关联物品（无工单时的物品咨询，可空）',
  sender_id BIGINT NOT NULL COMMENT '发送人',
  sender_role VARCHAR(20) NOT NULL COMMENT 'USER/POINT_ADMIN/SYS_ADMIN',
  content VARCHAR(1000) NOT NULL COMMENT '留言内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (id),
  KEY idx_chat_dispute (dispute_id),
  KEY idx_chat_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纠纷沟通留言（申诉人-站点-管理员三方共享）';
