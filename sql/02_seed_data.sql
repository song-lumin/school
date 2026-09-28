-- 校园失物招领及诚信积分系统 - 种子数据

SET NAMES utf8mb4;

-- ----------------------------
-- 插入测试用户
-- 密码均为：123456（BCrypt加密后）
-- ----------------------------
INSERT INTO `user` (`username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `credit_score`, `status`, `allow_leaderboard`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN001', '13800000000', 'admin@school.edu', 'SYS_ADMIN', 0, 1, 0),
('pointadmin1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '站点管理员1', 'PA001', '13800000001', 'pointadmin1@school.edu', 'POINT_ADMIN', 5, 1, 1),
('pointadmin2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '站点管理员2', 'PA002', '13800000002', 'pointadmin2@school.edu', 'POINT_ADMIN', 3, 1, 1),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '2024001', '13900000001', 'user1@school.edu', 'USER', 10, 1, 1),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '2024002', '13900000002', 'user2@school.edu', 'USER', 8, 1, 1),
('user3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', '2024003', '13900000003', 'user3@school.edu', 'USER', 6, 1, 1),
('user4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', '2024004', '13900000004', 'user4@school.edu', 'USER', 0, 1, 1);

-- ----------------------------
-- 插入招领站点
-- ----------------------------
INSERT INTO `drop_point` (`name`, `location`, `admin_id`, `has_camera`, `camera_info`, `status`) VALUES
('图书馆失物招领处', '图书馆一楼大厅东侧', 2, 1, '{"camera_count": 2, "coverage": "大厅全景", "retention_days": 30}', 1),
('学生活动中心站点', '学生活动中心二楼服务台', 3, 1, '{"camera_count": 1, "coverage": "服务台区域", "retention_days": 15}', 1),
('宿舍区服务站', '1号宿舍楼楼管办公室', 2, 0, NULL, 1),
('教学楼管理处', '第一教学楼一楼管理室', 3, 1, '{"camera_count": 1, "coverage": "管理室门口", "retention_days": 7}', 1);

-- ----------------------------
-- 插入测试物品（模拟不同状态）
-- ----------------------------
-- 状态 1：公开待认领
INSERT INTO `found_item` (`title`, `category`, `description`, `found_location`, `found_time`, `images`, `claim_question`, `perishable`, `item_status`, `founder_id`, `drop_point_id`, `published_at`) VALUES
('黑色华为手机', '电子产品', '黑色华为Mate60，后盖有轻微划痕，屏幕完好', '图书馆三楼阅览室', '2024-03-20 14:30:00', '["https://example.com/image1.jpg"]', '手机壳是什么颜色？', 0, 1, 4, 1, '2024-03-20 15:00:00'),
('蓝色保温杯', '生活用品', '不锈钢保温杯，蓝色，杯身有卡通贴纸', '学生活动中心篮球场', '2024-03-21 10:00:00', '["https://example.com/image2.jpg"]', '杯身上的贴纸图案是什么？', 0, 1, 5, 2, '2024-03-21 11:00:00'),
('校园卡', '证件卡类', '学生校园卡，卡面有照片', '第一教学楼201教室', '2024-03-22 09:15:00', '[]', '卡片背面的学号后四位是？', 0, 1, 6, 4, '2024-03-22 10:00:00');

-- 状态 6：已发布待交物
INSERT INTO `found_item` (`title`, `category`, `description`, `found_location`, `found_time`, `images`, `claim_question`, `perishable`, `item_status`, `founder_id`, `published_at`) VALUES
('红色钱包', '钱包证件', '红色皮质钱包，内有少量现金和卡片', '宿舍区篮球场', '2024-03-23 16:00:00', '["https://example.com/image3.jpg"]', '钱包内有什么颜色的会员卡？', 0, 6, 4, '2024-03-23 16:30:00');

-- 状态 3：已取件
INSERT INTO `found_item` (`title`, `category`, `description`, `found_location`, `found_time`, `images`, `claim_question`, `perishable`, `item_status`, `founder_id`, `drop_point_id`, `published_at`, `claimed_at`) VALUES
('钥匙串', '钥匙配饰', '一串钥匙，带有小熊挂件', '图书馆门口', '2024-03-18 08:00:00', '[]', '挂件是什么动物？', 0, 3, 5, 1, '2024-03-18 09:00:00', '2024-03-19 14:00:00');

-- ----------------------------
-- 插入交物日志
-- ----------------------------
INSERT INTO `hand_in_log` (`item_id`, `drop_point_id`, `hand_in_status`, `handed_in_at`, `checked_at`, `checked_by`, `check_note`, `credit_issued`, `credit_issued_at`) VALUES
(1, 1, 2, '2024-03-20 15:30:00', '2024-03-20 16:00:00', 2, '物品状态良好，已入库', 1, '2024-03-20 16:00:00'),
(2, 2, 2, '2024-03-21 11:30:00', '2024-03-21 14:00:00', 3, '物品完好，已登记', 1, '2024-03-21 14:00:00'),
(3, 4, 2, '2024-03-22 10:30:00', '2024-03-22 15:00:00', 3, '证件类物品，妥善保管', 1, '2024-03-22 15:00:00'),
(5, 1, 2, '2024-03-18 10:00:00', '2024-03-18 11:00:00', 2, '钥匙串完好', 1, '2024-03-18 11:00:00');

-- ----------------------------
-- 插入认领申请
-- ----------------------------
INSERT INTO `claim_apply` (`item_id`, `claimer_id`, `answer`, `apply_status`, `pickup_time`, `pickup_photo`, `pickup_signature`, `credit_issued`) VALUES
(5, 6, '小熊', 1, '2024-03-19 14:00:00', 'https://example.com/pickup1.jpg', 'https://example.com/sign1.jpg', 1);

-- ----------------------------
-- 插入积分变动日志
-- ----------------------------
INSERT INTO `credit_log` (`user_id`, `change_amount`, `operation_type`, `related_item_id`, `reason`) VALUES
-- 巡检发放积分
(4, 1, 'CHECK_ISSUE', 1, '物品巡检通过'),
(5, 1, 'CHECK_ISSUE', 2, '物品巡检通过'),
(6, 1, 'CHECK_ISSUE', 3, '物品巡检通过'),
(5, 1, 'CHECK_ISSUE', 5, '物品巡检通过'),
-- 取件发放积分
(5, 3, 'PICKUP_ISSUE', 5, '失主取件完成（拾取者）'),
(6, 1, 'PICKUP_ISSUE', 5, '失主取件完成（认领者）'),
-- 其他积分（模拟历史数据）
(4, 3, 'PICKUP_ISSUE', NULL, '历史取件积分'),
(4, 3, 'PICKUP_ISSUE', NULL, '历史取件积分'),
(5, 3, 'PICKUP_ISSUE', NULL, '历史取件积分'),
(6, 3, 'PICKUP_ISSUE', NULL, '历史取件积分'),
(2, 1, 'CHECK_ISSUE', NULL, '历史巡检积分'),
(2, 1, 'CHECK_ISSUE', NULL, '历史巡检积分'),
(2, 1, 'CHECK_ISSUE', NULL, '历史巡检积分'),
(2, 1, 'CHECK_ISSUE', NULL, '历史巡检积分'),
(2, 1, 'CHECK_ISSUE', NULL, '历史巡检积分'),
(3, 1, 'CHECK_ISSUE', NULL, '历史巡检积分'),
(3, 1, 'CHECK_ISSUE', NULL, '历史巡检积分'),
(3, 1, 'CHECK_ISSUE', NULL, '历史巡检积分');

-- ----------------------------
-- 插入寻物启事
-- ----------------------------
INSERT INTO `lost_notice` (`title`, `category`, `description`, `lost_location`, `lost_time`, `contact_info`, `images`, `status`, `publisher_id`) VALUES
('寻找黑色背包', '箱包背包', '黑色双肩背包，里面有笔记本电脑和教材，背包上有英文字母标识', '图书馆或教学楼', '2024-03-19 13:00:00', '13900000005', '[]', 0, 7),
('丢失运动手表', '电子产品', '黑色运动手表，品牌为佳明，表带有磨损', '操场或健身房', '2024-03-21 17:00:00', '13900000006', '["https://example.com/lost1.jpg"]', 0, 4);
