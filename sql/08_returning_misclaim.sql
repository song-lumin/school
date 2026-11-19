-- 误领/冒领闭环：物品「待归还」(item_status=7) 与用户误领计次
-- 说明：user.misclaim_count 记录误领/冒领次数；非恶意误领只记次不扣分，恶意冒领另扣 5 分（FALSE_CLAIM_PENALTY）

ALTER TABLE `user` ADD COLUMN `misclaim_count` INT NOT NULL DEFAULT 0 COMMENT '误领/冒领次数' AFTER `credit_score`;
