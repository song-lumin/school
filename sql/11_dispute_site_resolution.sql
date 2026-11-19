ALTER TABLE dispute
  ADD COLUMN site_verdict VARCHAR(30) NULL COMMENT '站点结论码',
  ADD COLUMN site_dispose VARCHAR(20) NULL COMMENT '站点建议物品处置',
  ADD COLUMN site_suggest_items VARCHAR(200) NULL COMMENT '按结论推导的应回滚项',
  ADD COLUMN need_review TINYINT NOT NULL DEFAULT 0 COMMENT '站点责或不可抗力，需系统复核',
  ADD COLUMN site_resolved_at DATETIME NULL COMMENT '站点提交结论时间',
  ADD COLUMN credit_actual_items VARCHAR(200) NULL COMMENT '系统实际执行回滚项',
  ADD COLUMN credit_handler_id BIGINT NULL COMMENT '积分处置人',
  ADD COLUMN credit_handled_at DATETIME NULL COMMENT '积分处置时间';
