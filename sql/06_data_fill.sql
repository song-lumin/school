-- ============================================================
-- 数据补充脚本:补全各状态演示数据
-- 执行前提:01~05 脚本已执行
-- 幂等性:使用 NOT EXISTS / 固定 ID 判断,可重复执行
-- ============================================================
USE school_lost_found;
SET NAMES utf8mb4;

-- ============================================================
-- 1. 补充用户(9 号起):普通用户若干 + 禁用用户 + 不上榜用户
-- 密码均为 123456
-- ============================================================
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `credit_score`, `status`, `allow_leaderboard`)
SELECT t.id, t.username, t.password, t.real_name, t.student_id, t.phone, t.email, t.role, t.credit_score, t.status, t.allow_leaderboard
FROM (
  SELECT 9  AS id, 'user5'  AS username, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' AS password, '陈七'   AS real_name, '2024005' AS student_id, '13900000005' AS phone, 'user5@school.edu'  AS email, 'USER' AS role, 12 AS credit_score, 1 AS status, 1 AS allow_leaderboard UNION ALL
  SELECT 10, 'user6',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周八',   '2024006', '13900000006', 'user6@school.edu',  'USER', 9,  1, 1 UNION ALL
  SELECT 11, 'user7',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴九',   '2024007', '13900000007', 'user7@school.edu',  'USER', 7,  1, 1 UNION ALL
  SELECT 12, 'user8',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '郑十',   '2024008', '13900000008', 'user8@school.edu',  'USER', 4,  1, 1 UNION ALL
  SELECT 13, 'user9',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙一',   '2024009', '13900000009', 'user9@school.edu',  'USER', 15, 1, 1 UNION ALL
  SELECT 14, 'user10', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '钱二',   '2024010', '13900000010', 'user10@school.edu', 'USER', 11, 1, 0 UNION ALL
  SELECT 15, 'banned1','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '违规用户','2024011','13900000011', 'banned1@school.edu','USER', -5, 0, 0
) t
WHERE NOT EXISTS (SELECT 1 FROM `user` u WHERE u.id = t.id);

-- ============================================================
-- 2. 补充站点(6 号起):体育馆站点,交由站点管理员1管理
-- ============================================================
INSERT INTO `drop_point` (`id`, `name`, `location`, `admin_id`, `has_camera`, `camera_info`, `status`)
SELECT 6, '体育馆失物招领处', '体育馆东侧一层入口值班室', 2, 1, '{"camera_count": 2, "coverage": "入口及看台", "retention_days": 30}', 1
WHERE NOT EXISTS (SELECT 1 FROM `drop_point` WHERE id = 6);

-- ============================================================
-- 3. 补充物品:覆盖全部状态(9 号起)
-- ============================================================
INSERT INTO `found_item` (`id`, `title`, `category`, `description`, `found_location`, `found_time`, `images`, `claim_question`, `perishable`, `item_status`, `founder_id`, `drop_point_id`, `published_at`, `claimed_at`)
SELECT t.id, t.title, t.category, t.description, t.found_location, t.found_time, t.images, t.claim_question, t.perishable, t.item_status, t.founder_id, t.drop_point_id, t.published_at, t.claimed_at
FROM (
  -- 状态 1 公开待认领(4 件,近两周,便于首页/统计展示)
  SELECT 9  AS id, '白色AirPods耳机' AS title, '电子产品' AS category, '左耳有贴纸,充电盒完好' AS description, '图书馆四楼自习区' AS found_location, '2026-09-20 10:00:00' AS found_time, '["https://example.com/image9.jpg"]' AS images, '耳机盒底部有什么标记?' AS claim_question, 0 AS perishable, 1 AS item_status, 9 AS founder_id, 1 AS drop_point_id, '2026-09-20 11:00:00' AS published_at, NULL AS claimed_at UNION ALL
  SELECT 10, '黑色雨伞', '生活用品', '长柄雨伞,伞柄弯曲,有轻微使用痕迹', '教学楼B栋一楼大厅', '2026-09-21 16:00:00', '[]', '伞柄是什么形状?', 0, 1, 10, 3, '2026-09-21 17:00:00', NULL UNION ALL
  SELECT 11, '高等数学教材', '书籍资料', '同济第七版上册,书内有铅笔笔记,扉页写有名字', '第一教学楼301教室', '2026-09-22 18:30:00', '["https://example.com/image11.jpg"]', '扉页上写的名字是?', 0, 1, 11, 4, '2026-09-22 19:00:00', NULL UNION ALL
  SELECT 12, '银色手链', '饰品', '细银手链,带有小星星吊坠', '学生活动中心门口', '2026-09-24 12:00:00', '["https://example.com/image12.jpg"]', '吊坠是什么形状?', 0, 1, 12, 2, '2026-09-24 13:00:00', NULL UNION ALL
  -- 状态 2 认领中(1 件,有人申请待审核)
  SELECT 13, '黑色显卡包', '电子产品', '装有显卡的防静电袋,外层帆布包', '实验楼405机房', '2026-09-25 15:00:00', '[]', '防静电袋是什么颜色?', 0, 2, 13, 1, '2026-09-25 16:00:00', NULL UNION ALL
  -- 状态 3 已取件(1 件,近期)
  SELECT 14, '粉色水杯', '生活用品', '塑料水杯,粉色,杯盖有吸管', '食堂二楼', '2026-09-18 12:30:00', '["https://example.com/image14.jpg"]', '杯盖是什么颜色?', 0, 3, 9, 2, '2026-09-18 13:00:00', '2026-09-19 10:00:00' UNION ALL
  -- 状态 4 已归档(1 件)
  SELECT 15, '黑色皮带', '衣物', '黑色皮质皮带,金属扣头', '体育馆更衣室', '2026-09-10 19:00:00', '[]', '扣头是什么材质?', 0, 4, 10, 6, '2026-09-10 20:00:00', '2026-09-12 14:00:00' UNION ALL
  -- 状态 5 已过期(2 件,发布超 30 天无人认领)
  SELECT 16, '蓝色笔记本', '学习用品', 'A5蓝色笔记本,内页写满英语笔记', '图书馆二楼', '2026-08-10 09:00:00', '["https://example.com/image16.jpg"]', '笔记本是第几页有姓名?', 0, 5, 11, 1, '2026-08-10 10:00:00', NULL UNION ALL
  SELECT 17, '黑色发圈', '饰品', '黑色布艺发圈,带有珍珠装饰', '舞蹈室', '2026-08-05 20:00:00', '[]', '发圈上有什么装饰?', 0, 5, 12, 6, '2026-08-05 21:00:00', NULL UNION ALL
  -- 状态 1 公开待认领(待巡检,演示巡检队列)
  SELECT 18, '绿色伞兵包', '箱包背包', '绿色帆布包,内有水壶', '操场看台', '2026-09-27 17:00:00', '["https://example.com/image18.jpg"]', '包内有什么物品?', 0, 1, 13, 6, '2026-09-27 18:00:00', NULL UNION ALL
  -- 状态 0 已作废(1 件,曾被举报成立)
  SELECT 19, 'iPhone数据线', '电子产品', '原装数据线,白色,一米长', '图书馆充电区', '2026-09-08 14:00:00', '[]', '数据线接口类型?', 0, 0, 14, 1, '2026-09-08 15:00:00', NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM `found_item` f WHERE f.id = t.id);

-- ============================================================
-- 4. 补充交物日志:新站点物品全部覆盖
-- ============================================================
INSERT INTO `hand_in_log` (`item_id`, `drop_point_id`, `hand_in_status`, `handed_in_at`, `checked_at`, `checked_by`, `check_note`, `credit_issued`, `credit_issued_at`)
SELECT t.item_id, t.drop_point_id, t.hand_in_status, t.handed_in_at, t.checked_at, t.checked_by, t.check_note, t.credit_issued, t.credit_issued_at
FROM (
  SELECT 9  AS item_id, 1 AS drop_point_id, 2 AS hand_in_status, '2026-09-20 11:30:00' AS handed_in_at, '2026-09-20 14:00:00' AS checked_at, 2 AS checked_by, '物品完好,已入库' AS check_note, 1 AS credit_issued, '2026-09-20 14:00:00' AS credit_issued_at UNION ALL
  SELECT 10, 3, 2, '2026-09-21 17:30:00', '2026-09-22 09:00:00', 3, '雨伞完好', 1, '2026-09-22 09:00:00' UNION ALL
  SELECT 11, 4, 2, '2026-09-22 19:30:00', '2026-09-23 10:00:00', 3, '书籍有笔记,已登记', 1, '2026-09-23 10:00:00' UNION ALL
  SELECT 12, 2, 2, '2026-09-24 13:30:00', '2026-09-24 16:00:00', 2, '饰品完好,已入保险柜', 1, '2026-09-24 16:00:00' UNION ALL
  SELECT 13, 1, 2, '2026-09-25 16:30:00', '2026-09-26 09:00:00', 2, '已入库,待认领审核', 1, '2026-09-26 09:00:00' UNION ALL
  SELECT 14, 2, 2, '2026-09-18 13:30:00', '2026-09-18 15:00:00', 3, '水杯完好', 1, '2026-09-18 15:00:00' UNION ALL
  SELECT 15, 6, 2, '2026-09-10 20:30:00', '2026-09-11 09:00:00', 2, '皮带完好', 1, '2026-09-11 09:00:00' UNION ALL
  SELECT 16, 1, 2, '2026-08-10 10:30:00', '2026-08-10 14:00:00', 2, '笔记本完好', 1, '2026-08-10 14:00:00' UNION ALL
  SELECT 17, 6, 2, '2026-08-05 21:30:00', '2026-08-06 09:00:00', 2, '发圈完好', 1, '2026-08-06 09:00:00' UNION ALL
  -- 状态 1:待巡检(演示巡检队列)
  SELECT 18, 6, 1, '2026-09-27 18:00:00', NULL, NULL, NULL, 0, NULL UNION ALL
  SELECT 19, 1, 1, '2026-09-08 15:30:00', NULL, NULL, NULL, 0, NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM `hand_in_log` h WHERE h.item_id = t.item_id);

-- ============================================================
-- 5. 补充认领申请:公开物品的待审核申请 + 历史流程
-- ============================================================
INSERT INTO `claim_apply` (`id`, `item_id`, `claimer_id`, `answer`, `apply_status`, `confidence_score`, `low_confidence`, `confidence_reason`, `reject_reason`, `pickup_time`, `credit_issued`)
SELECT t.id, t.item_id, t.claimer_id, t.answer, t.apply_status, t.confidence_score, t.low_confidence, t.confidence_reason, t.reject_reason, t.pickup_time, t.credit_issued
FROM (
  -- 待审核(0):对应认领中物品 13
  SELECT 9  AS id, 13 AS item_id, 9  AS claimer_id, '黑色,防静电袋是黑色的' AS answer, 0 AS apply_status, 85 AS confidence_score, 0 AS low_confidence, NULL AS confidence_reason, NULL AS reject_reason, NULL AS pickup_time, 0 AS credit_issued UNION ALL
  -- 待审核(0):公开物品 9 的申请
  SELECT 10, 9, 10, '标记是笑脸贴纸', 0, 90, 0, NULL, NULL, NULL, 0 UNION ALL
  -- 已通过待取件(4)
  SELECT 11, 10, 11, '弯钩形状', 4, 95, 0, NULL, NULL, NULL, 0 UNION ALL
  -- 已完成(1):物品 14
  SELECT 12, 14, 12, '粉色', 1, 100, 0, NULL, NULL, '2026-09-19 10:00:00', 1 UNION ALL
  -- 已完成(1):物品 15
  SELECT 13, 15, 13, '金属扣头', 1, 100, 0, NULL, NULL, '2026-09-12 14:00:00', 1 UNION ALL
  -- 已拒绝(2):答案错误
  SELECT 14, 11, 14, '扉页没有写名字', 2, 20, 1, '答案与防伪问题明显不符', '答案不正确,请核对后重新申请', NULL, 0 UNION ALL
  -- 已锁定(3):多次被拒
  SELECT 15, 12, 15, '圆形吊坠', 3, 30, 1, '多次答案错误,已锁定', '答案与吊坠形状不符', NULL, 0
) t
WHERE NOT EXISTS (SELECT 1 FROM `claim_apply` c WHERE c.id = t.id);

-- ============================================================
-- 6. 补充积分日志:覆盖新用户和新物品
-- ============================================================
INSERT INTO `credit_log` (`user_id`, `change_amount`, `operation_type`, `related_item_id`, `related_apply_id`, `reason`, `operator_id`)
SELECT t.user_id, t.change_amount, t.operation_type, t.related_item_id, t.related_apply_id, t.reason, t.operator_id
FROM (
  SELECT 9  AS user_id, 1  AS change_amount, 'CHECK_ISSUE'  AS operation_type, 9  AS related_item_id, NULL AS related_apply_id, '物品巡检通过' AS reason, NULL AS operator_id UNION ALL
  SELECT 10, 1, 'CHECK_ISSUE', 10, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 11, 1, 'CHECK_ISSUE', 11, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 12, 1, 'CHECK_ISSUE', 12, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 13, 1, 'CHECK_ISSUE', 13, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 9,  1, 'CHECK_ISSUE', 14, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 10, 1, 'CHECK_ISSUE', 15, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 11, 1, 'CHECK_ISSUE', 16, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 12, 1, 'CHECK_ISSUE', 17, NULL, '物品巡检通过', NULL UNION ALL
  SELECT 9,  3, 'PICKUP_ISSUE', 14, 12, '失主取件完成(拾取者)', NULL UNION ALL
  SELECT 12, 1, 'PICKUP_ISSUE', 14, 12, '失主取件完成(认领者)', NULL UNION ALL
  SELECT 10, 3, 'PICKUP_ISSUE', 15, 13, '失主取件完成(拾取者)', NULL UNION ALL
  SELECT 13, 1, 'PICKUP_ISSUE', 15, 13, '失主取件完成(认领者)', NULL UNION ALL
  SELECT 9,  3, 'PICKUP_ISSUE', NULL, NULL, '历史取件积分', NULL UNION ALL
  SELECT 10, 3, 'PICKUP_ISSUE', NULL, NULL, '历史取件积分', NULL UNION ALL
  SELECT 10, 3, 'PICKUP_ISSUE', NULL, NULL, '历史取件积分', NULL UNION ALL
  SELECT 11, 3, 'PICKUP_ISSUE', NULL, NULL, '历史取件积分', NULL UNION ALL
  SELECT 12, 1, 'CHECK_ISSUE', NULL, NULL, '历史巡检积分', NULL UNION ALL
  SELECT 13, 1, 'CHECK_ISSUE', NULL, NULL, '历史巡检积分', NULL UNION ALL
  SELECT 13, 1, 'CHECK_ISSUE', NULL, NULL, '历史巡检积分', NULL UNION ALL
  SELECT 14, 1, 'CHECK_ISSUE', 19, NULL, '物品巡检通过', 2 UNION ALL
  -- 违规用户被扣除
  SELECT 15, -5, 'DEDUCT', 19, NULL, '虚假投放数据线,举报成立,扣除积分', 1 UNION ALL
  -- 历史驳回后回滚(申请 15 被拒对应)
  SELECT 15, -1, 'ROLLBACK', 12, 15, '认领申请被拒,回滚申请积分', NULL
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `credit_log` c
  WHERE c.user_id = t.user_id AND c.operation_type = t.operation_type
    AND c.related_item_id <=> t.related_item_id AND c.related_apply_id <=> t.related_apply_id
    AND c.change_amount = t.change_amount
);

-- ============================================================
-- 7. 补充寻物启事:含已找回(关联匹配物品)和更多待找回
-- ============================================================
INSERT INTO `lost_notice` (`id`, `title`, `category`, `description`, `lost_location`, `lost_time`, `contact_info`, `images`, `status`, `publisher_id`, `matched_item_id`)
SELECT t.id, t.title, t.category, t.description, t.lost_location, t.lost_time, t.contact_info, t.images, t.status, t.publisher_id, t.matched_item_id
FROM (
  SELECT 5  AS id, '丢失白色蓝牙耳机' AS title, '电子产品' AS category, '白色AirPods,左耳贴了笑脸贴纸' AS description, '图书馆' AS lost_location, '2026-09-20 09:00:00' AS lost_time, '13900000010' AS contact_info, '[]' AS images, 1 AS status, 10 AS publisher_id, 9 AS matched_item_id UNION ALL
  SELECT 6, '丢失粉色水杯', '生活用品', '粉色塑料水杯,带吸管杯盖', '第一食堂', '2026-09-18 12:00:00', '13900000012', '[]', 1, 12, 14 UNION ALL
  SELECT 7, '丢失高等数学教材', '书籍资料', '同济第七版上册,扉页有我的名字', '第一教学楼', '2026-09-22 17:00:00', '13900000014', '[]', 0, 14, NULL UNION ALL
  SELECT 8, '丢失银色手链', '饰品', '细银手链,小星星吊坠,妈妈送的', '学生活动中心', '2026-09-24 11:00:00', '13900000015', '[]', 0, 15, NULL UNION ALL
  SELECT 9, '丢失黑色显卡包', '电子产品', '帆布包内有防静电袋装的显卡', '实验楼', '2026-09-25 14:00:00', '13900000009', '[]', 0, 9, NULL UNION ALL
  SELECT 10, '丢失校园卡', '证件卡类', '校园卡,卡号2024xxxx,已挂失', '食堂到教学楼路上', '2026-09-15 08:00:00', '13900000003', '[]', 0, 7, NULL UNION ALL
  SELECT 11, '丢失黑色皮带', '衣物', '黑色皮带金属扣,健身后遗失', '体育馆', '2026-09-10 18:00:00', '13900000010', '[]', 2, 10, NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM `lost_notice` n WHERE n.id = t.id);

-- ============================================================
-- 8. 补充监控调阅日志(camera_log 为空,全量补充)
-- ============================================================
INSERT INTO `camera_log` (`id`, `drop_point_id`, `item_id`, `apply_reason`, `applicant_id`, `time_range_start`, `time_range_end`, `status`, `result_note`, `handler_id`, `handled_at`)
SELECT t.id, t.drop_point_id, t.item_id, t.apply_reason, t.applicant_id, t.time_range_start, t.time_range_end, t.status, t.result_note, t.handler_id, t.handled_at
FROM (
  SELECT 1 AS id, 1 AS drop_point_id, 9  AS item_id, '耳机在自习区被拿走,申请调阅监控确认' AS apply_reason, 10 AS applicant_id, '2026-09-20 09:00:00' AS time_range_start, '2026-09-20 12:00:00' AS time_range_end, 1 AS status, '同意调阅,请注意保护他人隐私' AS result_note, 2 AS handler_id, '2026-09-20 15:00:00' AS handled_at UNION ALL
  SELECT 2, 2, 14, '水杯疑似被人拿错,调阅监控核实', 12, '2026-09-18 11:00:00', '2026-09-18 14:00:00', 3, '已调阅完毕,确认为他人误拿并已归还', 3, '2026-09-18 17:00:00' UNION ALL
  SELECT 3, 4, 11, '教材内有重要笔记,希望确认拾取时间', 14, '2026-09-22 17:00:00', '2026-09-22 19:00:00', 1, '同意,请于工作日到管理室查看', 3, '2026-09-23 09:00:00' UNION ALL
  SELECT 4, 1, 16, '笔记本遗忘在座位,调阅确认去向', 11, '2026-08-10 08:00:00', '2026-08-10 11:00:00', 2, '时间范围超出留存期限,无法调阅', 2, '2026-08-11 10:00:00' UNION ALL
  SELECT 5, 6, 15, '皮带在更衣室遗失,申请查看入口监控', 10, '2026-09-10 18:00:00', '2026-09-10 21:00:00', 0, NULL, NULL, NULL UNION ALL
  SELECT 6, 2, 12, '手链在活动中心遗失,查看门口监控', 15, '2026-09-24 10:00:00', '2026-09-24 13:00:00', 0, NULL, NULL, NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM `camera_log` c WHERE c.id = t.id);

-- ============================================================
-- 9. 补充举报工单:待处理 + 回滚积分状态
-- ============================================================
INSERT INTO `report_log` (`id`, `reporter_id`, `item_id`, `report_type`, `description`, `evidence_images`, `status`, `handler_note`, `handler_id`, `handled_at`)
SELECT t.id, t.reporter_id, t.item_id, t.report_type, t.description, t.evidence_images, t.status, t.handler_note, t.handler_id, t.handled_at
FROM (
  SELECT 3 AS id, 9  AS reporter_id, 19 AS item_id, 'FAKE_PUBLISH' AS report_type, '该数据线是坏的,根本不能用,疑似刷积分' AS description, '[]' AS evidence_images, 1 AS status, '举报成立,物品已作废,扣除发布者积分' AS handler_note, 1 AS handler_id, '2026-09-09 10:00:00' AS handled_at UNION ALL
  SELECT 4, 11, 10, 'DESC_MISMATCH', '雨伞实际上有一根伞骨断裂,与描述不符', '[]', 0, NULL, NULL, NULL UNION ALL
  SELECT 5, 12, 11, 'OTHER', '教材扉页名字被撕掉,怀疑不是拾取而是故意占有', '[]', 0, NULL, NULL, NULL UNION ALL
  SELECT 6, 13, 16, 'FAKE_PUBLISH', '过期物品一直挂着不处理,怀疑刷积分', '[]', 2, '过期属于正常流程,不构成虚假投放' AS handler_note, 1, '2026-09-26 14:00:00' UNION ALL
  SELECT 7, 10, 12, 'DESC_MISMATCH', '手链星星吊坠有脱落,但申请时未说明', '[]', 3, '举报成立,已回滚发布者本次积分' AS handler_note, 1, '2026-09-27 09:00:00'
) t
WHERE NOT EXISTS (SELECT 1 FROM `report_log` r WHERE r.id = t.id);

-- ============================================================
-- 10. 补充申诉工单:丢失申诉 + 待处理 + 驳回
-- ============================================================
INSERT INTO `dispute` (`id`, `applicant_id`, `apply_id`, `item_id`, `dispute_type`, `description`, `evidence_images`, `status`, `handler_note`, `handler_id`, `handled_at`)
SELECT t.id, t.applicant_id, t.apply_id, t.item_id, t.dispute_type, t.description, t.evidence_images, t.status, t.handler_note, t.handler_id, t.handled_at
FROM (
  SELECT 2 AS id, 14 AS applicant_id, NULL AS apply_id, 11 AS item_id, 'ITEM_LOST' AS dispute_type, '我的教材被别人发布招领,但防伪问题答案我只告诉过室友,怀疑被冒领' AS description, '[]' AS evidence_images, 0 AS status, NULL AS handler_note, NULL AS handler_id, NULL AS handled_at UNION ALL
  SELECT 3, 15, NULL, 12, 'ITEM_LOST', '我的手链被发布到平台,但发布者拒绝我的认领申请,请协助处理', '[]', 0, NULL, NULL, NULL UNION ALL
  SELECT 4, 12, 13, NULL, 'ITEM_MISMATCH', '取件时发现皮带扣头有划痕,与描述完好不符', '[]', 2, '查证物品发布时照片,划痕为取件前已有,维持原判' AS handler_note, 1, '2026-09-27 15:00:00' UNION ALL
  SELECT 5, 10, 14, 11, 'OTHER', '认为认领人答案明显不对,申请重新审核', '[]', 0, NULL, NULL, NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM `dispute` d WHERE d.id = t.id);

-- ============================================================
-- 11. 补充图片指纹(item_image_fingerprint 为空)
-- 注意:hash_value 为 16 位十六进制占位数据,真实指纹由后端上传图片时生成
-- ============================================================
INSERT INTO `item_image_fingerprint` (`item_id`, `image_url`, `hash_value`, `algorithm_version`)
SELECT t.item_id, t.image_url, t.hash_value, 'dhash-64-v1'
FROM (
  SELECT 1 AS item_id, 'https://example.com/image1.jpg' AS image_url, 'a3f5c2e1890b7d64' AS hash_value UNION ALL
  SELECT 9, 'https://example.com/image9.jpg',  'b7d2e9f04a1c8356' UNION ALL
  SELECT 11, 'https://example.com/image11.jpg','c9e1a4b7d2f50836' UNION ALL
  SELECT 12, 'https://example.com/image12.jpg','d4b8f2a6c3e19075' UNION ALL
  SELECT 14, 'https://example.com/image14.jpg','e2c7a9d4f1b63850' UNION ALL
  SELECT 16, 'https://example.com/image16.jpg','f6a3d8c2e5b19407' UNION ALL
  SELECT 18, 'https://example.com/image18.jpg','0a5e9c3f7b2d6184'
) t
WHERE NOT EXISTS (SELECT 1 FROM `item_image_fingerprint` f WHERE f.item_id = t.item_id AND f.image_url = t.image_url);
