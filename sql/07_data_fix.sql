-- ============================================================
-- 数据修复脚本:修正种子数据与业务规则不一致之处
-- 执行前提:01~06 脚本已执行
-- 幂等性:使用固定 ID / 条件更新,可重复执行
-- ============================================================
USE school_lost_found;
SET NAMES utf8mb4;

-- ============================================================
-- 1. 补齐已完成认领申请的取件凭证(照片+签字)
-- 业务规则:现场领取必须存档合影与签字(ClaimServiceImpl.pickup)
-- ============================================================
UPDATE `claim_apply`
SET `pickup_photo` = 'https://example.com/pickup14.jpg',
    `pickup_signature` = 'https://example.com/sign14.jpg'
WHERE `id` = 12 AND (`pickup_photo` IS NULL OR `pickup_signature` IS NULL);

UPDATE `claim_apply`
SET `pickup_photo` = 'https://example.com/pickup15.jpg',
    `pickup_signature` = 'https://example.com/sign15.jpg'
WHERE `id` = 13 AND (`pickup_photo` IS NULL OR `pickup_signature` IS NULL);

-- ============================================================
-- 2. 修正物品状态与认领申请状态不自洽
-- 状态机:存在待审核申请→物品应为公开(1);
--        存在已通过待取件申请→物品应为认领中(2)
-- ============================================================
-- 物品 10:申请 11 已通过待取件,物品应为认领中
UPDATE `found_item` SET `item_status` = 2
WHERE `id` = 10 AND `item_status` = 1
  AND EXISTS (SELECT 1 FROM `claim_apply` WHERE `item_id` = 10 AND `apply_status` = 4);

-- 物品 13:申请 9 仅待审核,物品应为公开
UPDATE `found_item` SET `item_status` = 1
WHERE `id` = 13 AND `item_status` = 2
  AND NOT EXISTS (SELECT 1 FROM `claim_apply` WHERE `item_id` = 13 AND `apply_status` IN (0, 4) AND `id` <> 9 LIMIT 1)
  AND EXISTS (SELECT 1 FROM `claim_apply` WHERE `item_id` = 13 AND `id` = 9 AND `apply_status` = 0);

-- ============================================================
-- 3. 修正申诉工单数据
-- 申诉 1(ITEM_MISMATCH)要求申请已完成,原关联的申请 3 是联调残留
--   →改关联申请 12(物品 14,已完成,发布人 user5)
-- 申诉 5 状态为驳回但缺处理记录 →补齐
-- ============================================================
UPDATE `dispute`
SET `applicant_id` = 12, `apply_id` = 12, `item_id` = 14
WHERE `id` = 1 AND `dispute_type` = 'ITEM_MISMATCH'
  AND EXISTS (SELECT 1 FROM `claim_apply` WHERE `id` = 12 AND `apply_status` = 1);

UPDATE `dispute`
SET `handler_note` = '证据不足以证明发布者存在过错,驳回申诉',
    `handler_id` = 1,
    `handled_at` = '2026-09-27 16:30:00'
WHERE `id` = 5 AND `status` = 2 AND `handler_id` IS NULL;

-- ============================================================
-- 4. 补齐积分台账,使 credit_score 与 credit_log 汇总一致
-- 差额 = user.credit_score - SUM(credit_log.change_amount)
-- 用带固定标记的补账记录,重复执行不会重复插入
-- ============================================================
INSERT INTO `credit_log` (`user_id`, `change_amount`, `operation_type`, `related_item_id`, `related_apply_id`, `reason`, `operator_id`)
SELECT t.user_id, t.change_amount, 'ADJUST', NULL, NULL, t.reason, 1
FROM (
  SELECT 4  AS user_id,  3 AS change_amount, '初始积分校准:user1 账实差额补记' AS reason UNION ALL
  SELECT 6,  1, '初始积分校准:user3 账实差额补记' UNION ALL
  SELECT 9,  4, '初始积分校准:user5 账实差额补记' UNION ALL
  SELECT 10, -2, '初始积分校准:user6 账实差额补记' UNION ALL
  SELECT 11, 2, '初始积分校准:user7 账实差额补记' UNION ALL
  SELECT 13, 11, '初始积分校准:user9 账实差额补记' UNION ALL
  SELECT 14, 10, '初始积分校准:user10 账实差额补记' UNION ALL
  SELECT 15, 1, '初始积分校准:banned1 账实差额补记'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `credit_log` c
  WHERE c.operation_type = 'ADJUST' AND c.reason = t.reason
);

-- ============================================================
-- 5. 补图片指纹:物品 4 有图但无指纹
-- ============================================================
INSERT INTO `item_image_fingerprint` (`item_id`, `image_url`, `hash_value`, `algorithm_version`)
SELECT 4, 'https://example.com/image3.jpg', '1b8f4d6e2a9c5073', 'dhash-64-v1'
WHERE NOT EXISTS (
  SELECT 1 FROM `item_image_fingerprint`
  WHERE `item_id` = 4 AND `image_url` = 'https://example.com/image3.jpg'
);

-- ============================================================
-- 6. 新增重复图片演示数据(物品 20 与物品 9 同图)
-- 用于演示 /api/risk-control/duplicates 图片查重预警
-- 同一发布人 user5(9 号),与物品 9 仅 claim 问题不同
-- ============================================================
INSERT INTO `found_item` (`id`, `title`, `category`, `description`, `found_location`, `found_time`, `images`, `claim_question`, `perishable`, `item_status`, `founder_id`, `drop_point_id`, `published_at`)
SELECT 20, '白色蓝牙耳机(疑似重复)', '电子产品', '白色无线耳机,充电盒完好,左耳有贴纸', '图书馆四楼自习区', '2026-09-23 10:00:00', '["https://example.com/image9.jpg"]', '充电盒内侧有什么颜色标记?', 0, 1, 9, 1, '2026-09-23 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM `found_item` WHERE `id` = 20);

INSERT INTO `hand_in_log` (`item_id`, `drop_point_id`, `hand_in_status`, `handed_in_at`, `checked_at`, `checked_by`, `check_note`, `credit_issued`, `credit_issued_at`)
SELECT 20, 1, 2, '2026-09-23 11:30:00', '2026-09-23 15:00:00', 2, '物品完好,已入库', 1, '2026-09-23 15:00:00'
WHERE NOT EXISTS (SELECT 1 FROM `hand_in_log` WHERE `item_id` = 20);

INSERT INTO `credit_log` (`user_id`, `change_amount`, `operation_type`, `related_item_id`, `related_apply_id`, `reason`, `operator_id`)
SELECT 9, 1, 'CHECK_ISSUE', 20, NULL, '物品巡检通过', NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `credit_log`
  WHERE `user_id` = 9 AND `operation_type` = 'CHECK_ISSUE' AND `related_item_id` = 20
);

-- 图片指纹与物品 9 完全一致,触发查重预警
INSERT INTO `item_image_fingerprint` (`item_id`, `image_url`, `hash_value`, `algorithm_version`)
SELECT 20, 'https://example.com/image9.jpg', 'b7d2e9f04a1c8356', 'dhash-64-v1'
WHERE NOT EXISTS (
  SELECT 1 FROM `item_image_fingerprint`
  WHERE `item_id` = 20 AND `image_url` = 'https://example.com/image9.jpg'
);

-- ============================================================
-- 7. 管理员退出诚信积分体系
-- 管理员不参与积分:清空积分、删除积分日志、关闭光荣榜展示
-- ============================================================
DELETE cl FROM `credit_log` cl
JOIN `user` u ON u.`id` = cl.`user_id`
WHERE u.`role` IN ('POINT_ADMIN', 'SYS_ADMIN');

UPDATE `user`
SET `credit_score` = 0, `allow_leaderboard` = 0
WHERE `role` IN ('POINT_ADMIN', 'SYS_ADMIN');

-- ============================================================
-- 8. 最终账实对齐:credit_score 一律以 credit_log 汇总为准
-- 置于脚本末尾,重复执行时结果稳定
-- ============================================================
UPDATE `user` u
SET u.`credit_score` = (
  SELECT IFNULL(SUM(cl.`change_amount`), 0)
  FROM `credit_log` cl WHERE cl.`user_id` = u.`id`
)
WHERE u.`credit_score` <> (
  SELECT IFNULL(SUM(cl2.`change_amount`), 0)
  FROM `credit_log` cl2 WHERE cl2.`user_id` = u.`id`
);

-- ============================================================
-- 9. 迁移:取消「待交物」状态,发布即公开
-- 旧状态 6(已发布待交物)的物品改为公开(1),投放点缺失的置 NULL,
-- 并按其 drop_point 补建待巡检交物日志
-- ============================================================
UPDATE `found_item`
SET `item_status` = 1,
    `drop_point_id` = CASE WHEN `drop_point_id` IS NULL THEN (SELECT MIN(`id`) FROM `drop_point` WHERE `status` = 1) ELSE `drop_point_id` END
WHERE `item_status` = 6;

INSERT INTO `hand_in_log` (`item_id`, `drop_point_id`, `hand_in_status`, `handed_in_at`, `credit_issued`)
SELECT f.`id`, f.`drop_point_id`, 1, f.`published_at`, 0
FROM `found_item` f
WHERE NOT EXISTS (SELECT 1 FROM `hand_in_log` h WHERE h.`item_id` = f.`id`)
  AND f.`drop_point_id` IS NOT NULL
  AND f.`item_status` IN (1, 2);
