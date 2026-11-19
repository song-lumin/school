SET NAMES utf8mb4;

-- ============================================================
-- 演示数据重建脚本（清空业务表，只保留账号与投放点）
-- 备份见 backup-before-reseed.sql
-- ============================================================

DELETE FROM dispute;
DELETE FROM claim_apply;
DELETE FROM hand_in_log;
DELETE FROM camera_log;
DELETE FROM item_image_fingerprint;
DELETE FROM report_log;
DELETE FROM sys_message;
DELETE FROM credit_log;
DELETE FROM lost_notice;
DELETE FROM found_item;

ALTER TABLE found_item AUTO_INCREMENT = 100;
ALTER TABLE claim_apply AUTO_INCREMENT = 100;
ALTER TABLE dispute AUTO_INCREMENT = 100;
ALTER TABLE hand_in_log AUTO_INCREMENT = 100;
ALTER TABLE camera_log AUTO_INCREMENT = 100;
ALTER TABLE report_log AUTO_INCREMENT = 100;
ALTER TABLE sys_message AUTO_INCREMENT = 100;
ALTER TABLE credit_log AUTO_INCREMENT = 100;
ALTER TABLE lost_notice AUTO_INCREMENT = 100;

-- ------------------------------------------------------------
-- 1. 账号显示名（用户名与密码保持不变，便于登录演示）
-- ------------------------------------------------------------
UPDATE `user` SET real_name = '陈志远', phone = '13800000000' WHERE id = 1;
UPDATE `user` SET real_name = '周敏',   phone = '13800000001' WHERE id = 2;
UPDATE `user` SET real_name = '吴涛',   phone = '13800000002' WHERE id = 3;
UPDATE `user` SET real_name = '张明'   WHERE id = 4;
UPDATE `user` SET real_name = '李思雨' WHERE id = 5;
UPDATE `user` SET real_name = '王浩'   WHERE id = 6;
UPDATE `user` SET real_name = '赵晓萌' WHERE id = 7;
UPDATE `user` SET real_name = '陈曦'   WHERE id = 9;
UPDATE `user` SET real_name = '周宁'   WHERE id = 10;
UPDATE `user` SET real_name = '郑一鸣' WHERE id = 11;
UPDATE `user` SET real_name = '孙悦'   WHERE id = 12;
UPDATE `user` SET real_name = '林可'   WHERE id = 13;
UPDATE `user` SET real_name = '何俊'   WHERE id = 14;
UPDATE `user` SET real_name = '刷分小号' WHERE id = 15;
UPDATE `user` SET username = 'user11', real_name = '陆遥' WHERE id = 16;

-- ------------------------------------------------------------
-- 2. 招领物品 23 条（覆盖全部 9 种状态）
-- ------------------------------------------------------------
INSERT INTO found_item
(id, title, category, description, found_location, found_time, found_time_text, found_time_precision,
 images, claim_question, reference_answer, perishable, item_status, founder_id, drop_point_id,
 published_at, claimed_at, expire_warning_sent, takedown_reason, takedown_at, takedown_by,
 appeal_status, created_at, updated_at)
VALUES
(1, '黑色长柄折叠雨伞', '生活用品',
 '纯黑伞面，伞骨完好，弯钩手柄，伞套内侧用记号笔写着 3-2-14。',
 '图书馆三楼自习区靠窗座位',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 3 DAY)), '18:00:00'), '傍晚闭馆前在座位旁捡到', 4,
 '["/uploads/c2638b75-ad61-45c0-bb09-5a3463007b0a.jpg"]',
 '伞套内侧写着什么编号？', '3-2-14', 0, 1, 4, 1,
 DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

(2, '白色无线耳机（含充电盒）', '电子产品',
 '白色半入耳式耳机，附白色充电盒，盒盖内侧有一小块透明保护套。',
 '学生活动中心二楼自习角',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 5 DAY)), '14:35:00'), NULL, 0,
 '["/uploads/72df7493-5afd-4446-8309-31d86844cc50.jpg"]',
 '耳机盒背面刻着什么字母？', 'LY', 0, 1, 9, 2,
 DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

(3, '银色编织手链', '饰品',
 '银色细链编织款，搭扣为龙虾扣，链身有轻微氧化。',
 '体育馆羽毛球场边看台',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 2 DAY)), '16:00:00'), NULL, 1,
 '["/uploads/ae0b669c-c7cd-446b-acf6-a265dcef1264.jpg"]',
 '手链搭扣是什么样式？', '龙虾扣', 0, 1, 13, 6,
 DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(4, '深绿色帆布双肩包', '其他',
 '深墨绿色帆布双肩包，正面有一条米色织带，侧袋里有一张校园超市小票。',
 '第一教学楼 A 座 302 教室后排',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 1 DAY)), '09:00:00'), '上午第二节课前', 2,
 '["/uploads/backpack.jpg"]',
 '侧袋里放着什么？', '一张校园超市小票', 0, 1, 5, 4,
 DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(5, '深蓝色不锈钢保温杯', '生活用品',
 '深蓝磨砂杯身，银色旋盖，杯盖内侧为浅灰色密封圈。',
 '图书馆一楼借阅台旁',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 6 DAY)), '15:00:00'), NULL, 1,
 '["/uploads/bottle.jpg"]',
 '杯盖内侧密封圈是什么颜色？', '浅灰', 0, 1, 14, 1,
 DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

(6, '校园卡（姓名已遮挡）', '证件卡类',
 '白色校园卡，右上角浅绿色色块，背面签名栏姓「王」，其余信息已做遮挡处理。',
 '1 号宿舍楼一层快递柜旁',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 2 HOUR)), '15:00:00'), NULL, 3,
 '["/uploads/card.jpg"]',
 '卡背面签名栏写的姓氏是什么？', '王', 0, 1, 6, 3,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),

(7, '黑色智能手机（屏幕有裂痕）', '电子产品',
 '黑色直板手机，正面左上角圆形摄像头开孔，屏幕右下角有一道细裂痕，已关机封存在防静电袋中。',
 '第一教学楼 B 座 401 机房',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 4 DAY)), '21:00:00'), '晚上机房闭馆前', 5,
 '["/uploads/phone.jpg"]',
 '锁屏壁纸是什么？', '一只橘猫', 0, 1, 4, 4,
 DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

(8, '高等数学（第七版）教材', '书籍资料',
 '深蓝色布纹封面教材，书页边缘略卷，内页夹着一张写满公式的便利贴。',
 '图书馆二楼阅览区 2 号桌',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 8 DAY)), '16:00:00'), NULL, 1,
 '["/uploads/book.jpg"]',
 '书里夹着什么？', '一张写满公式的便利贴', 0, 1, 11, 1,
 DATE_SUB(NOW(), INTERVAL 8 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),

(9, '酒红色短款钱包', '生活用品',
 '酒红色哑光皮革短款钱包，银色按扣，内层有一张图书馆借书卡与少量现金（已现场清点登记）。',
 '图书馆一楼大厅休息区',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 10 DAY)), '11:00:00'), NULL, 1,
 '["/uploads/wallet.jpg"]',
 '钱包里除了现金还有哪张卡？', '图书馆借书卡', 0, 2, 13, 1,
 DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

(10, '白色 Type-C 数据线', '电子产品',
 '白色充电线，两端接口处缠有一圈白色胶带做标记。',
 '学生活动中心二楼服务台',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 3 DAY)), '10:00:00'), NULL, 1,
 '["/uploads/cable.jpg"]',
 '接口处有什么标记？', '一圈白色胶带', 0, 2, 6, 2,
 DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(11, '钥匙串（三把钥匙+门禁扣）', '钥匙',
 '金属钥匙圈上串三把钥匙，另有房子形状的银色门禁扣。',
 '1 号宿舍楼 2 楼走廊',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 20 DAY)), '12:00:00'), NULL, 1,
 '["/uploads/05753ad8-7dd4-4bb5-abb2-55582c05891b.jpg"]',
 '门禁扣是什么形状？', '房子形状', 0, 3, 9, 3,
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY), 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),

(12, '粉色陶瓷马克杯', '生活用品',
 '低饱和粉色陶瓷马克杯，白色内釉，杯底印有一颗星星。取件时杯把已断裂，已转待归还。',
 '第一教学楼 A 座饮水间',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 15 DAY)), '14:00:00'), NULL, 1,
 '["/uploads/mug.jpg"]',
 '杯底印着什么图案？', '一颗星星', 0, 7, 12, 4,
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),

(13, '显卡包装盒（RTX 4060）', '电子产品',
 '黑色显卡外包装盒，盒内为泡沫内衬与说明书，显卡本体情况存在争议（见工单）。',
 '第一教学楼 B 座 312 实验室',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 30 DAY)), '17:00:00'), NULL, 1,
 '["/uploads/1eb85866-e2c0-4e8f-8c22-78d9e66dd4c1.jpg"]',
 '盒内除显卡外还有什么？', '保修卡', 0, 3, 13, 4,
 DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY), 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY)),

(14, '白色无线耳机（第二代）', '电子产品',
 '白色入耳式耳机与充电盒，盒身套有透明保护壳。已被非失主冒领，工单已处理完毕。',
 '学生活动中心一楼大厅',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 18 DAY)), '13:20:00'), NULL, 0,
 '["/uploads/d0f2aeca-9683-43e5-82e6-fa65e2442fa9.jpg"]',
 '耳机盒保护套是什么颜色？', '透明', 0, 7, 14, 2,
 DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY), 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),

(15, '黑色长柄雨伞', '生活用品',
 '与 #1 同款黑色长柄伞，伞柄上系有一根红色挂绳。',
 '体育馆东侧入口雨伞架',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 12 DAY)), '19:00:00'), NULL, 1,
 '["/uploads/c2638b75-ad61-45c0-bb09-5a3463007b0a.jpg"]',
 '伞柄上挂着什么？', '一根红色挂绳', 0, 3, 13, 6,
 DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),

(16, '校园卡（尾号 3821）', '证件卡类',
 '校园卡一张，卡号尾号 3821，超过保管期限无人认领，已自动过期。',
 '图书馆一楼自助借还机',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 400 DAY)), '10:00:00'), NULL, 1,
 '["/uploads/card.jpg"]',
 '卡号后四位是多少？', '3821', 0, 5, 12, 1,
 DATE_SUB(NOW(), INTERVAL 400 DAY), NULL, 1, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 400 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),

(17, '蓝色保温杯（旧款）', '生活用品',
 '旧款蓝色保温杯，杯身贴有多张社团活动贴纸，超过保管期限无人认领。',
 '1 号宿舍楼洗衣房',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 380 DAY)), '20:00:00'), NULL, 1,
 '["/uploads/bottle.jpg"]',
 '杯身上贴着哪类贴纸？', '社团活动贴纸', 0, 5, 11, 3,
 DATE_SUB(NOW(), INTERVAL 380 DAY), NULL, 1, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 380 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),

(18, '校园卡（重复发布）', '证件卡类',
 '同一张校园卡在 24 小时内被重复发布，账号已进入黑名单，招领自动下架。',
 '1 号宿舍楼一层快递柜旁',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 10 DAY)), '18:00:00'), NULL, 1,
 '["/uploads/card.jpg"]',
 '卡面颜色是什么？', '白色配浅绿角标', 0, 0, 15, 3,
 DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 0,
 '发布人账号进入黑名单，招领自动下架', DATE_SUB(NOW(), INTERVAL 9 DAY), 1, 0,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

(19, '高等数学（第七版）教材（第二本）', '书籍资料',
 '同版教材另一本，扉页写有姓名与班级，超期后已移交学校统一归档。',
 '图书馆三楼书库归还车',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 120 DAY)), '11:00:00'), NULL, 1,
 '["/uploads/book.jpg"]',
 '扉页写了什么？', '姓名与班级', 0, 4, 7, 1,
 DATE_SUB(NOW(), INTERVAL 120 DAY), NULL, 1, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 120 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),

(20, '黑色智能手机（疑似被冒领）', '电子产品',
 '黑色手机，背面贴有一张卡通贴纸，已通过认领审核，但认领人到站后未找到实物。',
 '图书馆一楼大厅借阅台旁',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 6 DAY)), '12:00:00'), NULL, 1,
 '["/uploads/phone.jpg"]',
 '手机背面贴了什么？', '一张卡通贴纸', 0, 2, 5, 1,
 DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(21, '黑色折叠伞（第三把）', '生活用品',
 '黑色折叠伞，第二根伞骨有折痕。经核查确认被他人从货架取走，站点标记丢失待查。',
 '体育馆东侧入口雨伞架',
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 9 DAY)), '17:00:00'), NULL, 1,
 '["/uploads/c2638b75-ad61-45c0-bb09-5a3463007b0a.jpg"]',
 '伞面有没有破损？', '第二根伞骨有折痕', 0, 8, 13, 6,
 DATE_SUB(NOW(), INTERVAL 9 DAY), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

(22, '深蓝色保温杯（待投放）', '生活用品',
 '与 #5 同款保温杯，杯身刻有字母 C。发布人尚未把实物送到投放点。',
 '图书馆一楼大厅',
 TIMESTAMP(DATE_SUB(NOW(), INTERVAL 2 HOUR), '14:00:00'), NULL, 1,
 '["/uploads/bottle.jpg"]',
 '杯身有没有刻字？', '字母 C', 0, 6, 6, 1,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),

(23, '深绿色帆布双肩包（待投放）', '其他',
 '深绿色帆布包，包内有一件灰色外套。发布人尚未把实物送到投放点。',
 '第一教学楼 A 座 302 教室',
 TIMESTAMP(DATE_SUB(NOW(), INTERVAL 40 MINUTE), '09:00:00'), '上午课间', 2,
 '["/uploads/backpack.jpg"]',
 '包内有什么？', '一件灰色外套', 0, 6, 6, 4,
 DATE_SUB(NOW(), INTERVAL 40 MINUTE), NULL, 0, NULL, NULL, NULL, 0,
 DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE));

-- ------------------------------------------------------------
-- 3. 站点交接记录（hand_in_status：0 未投放 / 1 已投放待接收 / 2 已核验在站 / 3 已离站）
-- ------------------------------------------------------------
INSERT INTO hand_in_log
(item_id, drop_point_id, hand_in_status, handed_in_at, checked_at, checked_by, check_note, credit_issued, credit_issued_at, created_at, updated_at)
VALUES
(1,  1, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 2, '实物已入架，伞套编号与描述一致', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2,  2, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 3, '耳机成色与照片一致，已入柜', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3,  6, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 2, '已入玻璃柜', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4,  4, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3, '包内小票已拍照留存', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5,  1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6,  3, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 2, '卡片已做脱敏处理后再公开', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(7,  4, 2, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 3, '高价值物品已封存保险柜', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(8,  1, 2, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), 2, '书籍已入架，便利贴原位保留', 1, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(9,  1, 2, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 2, '现金已现场清点登记并由另一位工作人员复核', 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10, 2, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 3, '已入架', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(11, 3, 3, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), 2, '已由认领人现场取走并签收', 1, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),
(12, 4, 3, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), 3, '已入架；取件后发现损坏，转待归还，等待站点收回', 1, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
(13, 4, 3, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), 3, '已由认领人取走，取件照片留存', 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY)),
(14, 2, 3, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), 3, '已取走（后续发现为冒领）', 1, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
(15, 6, 3, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), 2, '已取走并签收', 1, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
(16, 1, 2, DATE_SUB(NOW(), INTERVAL 400 DAY), DATE_SUB(NOW(), INTERVAL 399 DAY), 2, '长期无人认领，已过期处理', 1, DATE_SUB(NOW(), INTERVAL 399 DAY), DATE_SUB(NOW(), INTERVAL 400 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(17, 3, 2, DATE_SUB(NOW(), INTERVAL 380 DAY), DATE_SUB(NOW(), INTERVAL 379 DAY), 2, '长期无人认领，已过期处理', 1, DATE_SUB(NOW(), INTERVAL 379 DAY), DATE_SUB(NOW(), INTERVAL 380 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(18, 3, 2, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), 2, '复核时发现为重复发布，配合下架', 1, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
(19, 1, 2, DATE_SUB(NOW(), INTERVAL 120 DAY), DATE_SUB(NOW(), INTERVAL 119 DAY), 2, '超期移交学校归档', 1, DATE_SUB(NOW(), INTERVAL 119 DAY), DATE_SUB(NOW(), INTERVAL 120 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(20, 1, 2, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 2, '已入柜；认领人到站后未找到，核查中', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(21, 6, 2, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), 2, '稽核发现实物已不在架，标记丢失待查', 1, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(22, 1, 0, NULL, NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(23, 4, 0, NULL, NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE));

-- ------------------------------------------------------------
-- 4. 认领申请 17 条（覆盖待审核/已完成/已拒绝/已锁定/待取件）
-- ------------------------------------------------------------
INSERT INTO claim_apply
(id, item_id, source_notice_id, claimer_id, answer, apply_status, reject_reason, reject_count,
 confidence_score, low_confidence, confidence_reason, pickup_time, pickup_photo, pickup_signature,
 credit_issued, credit_rollback, created_at, updated_at)
VALUES
(1,  9,  NULL, 5,  '图书馆借书卡', 4, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2,  10, NULL, 9,  '一圈白色胶带', 4, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3,  11, NULL, 7,  '房子形状', 1, NULL, 0, 100, 0, NULL, DATE_SUB(NOW(), INTERVAL 19 DAY), NULL, '赵晓萌', 1, 0, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),
(4,  12, NULL, 6,  '一颗星星', 1, NULL, 0, 100, 0, NULL, DATE_SUB(NOW(), INTERVAL 14 DAY), NULL, '王浩', 1, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
(5,  13, NULL, 14, '保修卡',   1, NULL, 0, 100, 0, NULL, DATE_SUB(NOW(), INTERVAL 29 DAY), NULL, '何俊', 1, 0, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY)),
(6,  14, NULL, 7,  '透明',     1, NULL, 0, 100, 0, NULL, DATE_SUB(NOW(), INTERVAL 17 DAY), NULL, '赵晓萌', 1, 1, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),
(7,  15, NULL, 10, '一根红色挂绳', 1, NULL, 0, 100, 0, NULL, DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, '周宁', 1, 0, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
(8,  20, NULL, 13, '一张卡通贴纸', 4, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9,  1,  NULL, 12, '3-2-15',   0, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10, 2,  NULL, 4,  'LY',       0, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(11, 7,  NULL, 12, '一只橘猫的照片', 2, '锁屏是橘猫，但未提及壁纸上的文字', 1, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(12, 7,  NULL, 11, '不知道',   2, '回答与防伪问题无关', 2, 72, 1, '历史误领次数较多', NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13, 7,  NULL, 14, '动漫头像', 3, '连续三次回答错误，已锁定 24 小时', 3, 60, 1, '近 30 天有 2 次误领记录', NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(14, 8,  NULL, 6,  '一张写满公式的便利贴', 0, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(15, 4,  NULL, 11, '一包纸巾', 2, '侧袋内实际是超市小票', 1, 88, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(16, 5,  NULL, 7,  '浅灰',     0, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(17, 21, NULL, 10, '第二根伞骨有折痕', 4, NULL, 0, 100, 0, NULL, NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY));

-- ------------------------------------------------------------
-- 5. 纠纷工单 6 条（覆盖待处理 / 通过 / 驳回；含三种定责口径）
-- ------------------------------------------------------------
INSERT INTO dispute
(id, applicant_id, apply_id, item_id, dispute_type, description, evidence_images, status,
 handler_note, handle_result, handler_id, handled_at, created_at, updated_at,
 point_check_note, point_checker_id, point_checked_at, point_escalated_at, point_remind_count,
 site_unresponsive, site_unresponsive_at)
VALUES
(1, 6, 4, 12, 'ITEM_DAMAGED',
 '取件时发现杯把已断裂，杯身有一处明显磕碰，站点交接时没有说明，怀疑是货架搬运时摔坏。',
 NULL, 1,
 '调阅监控：交接前杯子在货架边缘滑落，属站点保管不当。损坏件转「待归还」，站点收回后作废处理，不重新公开。',
 'SITE_FAULT', 2, DATE_SUB(NOW(), INTERVAL 13 DAY),
 DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY),
 '已调阅 11-05 18:00-19:00 监控并留存片段，确认跌落发生在站点交接环节', 2, DATE_SUB(NOW(), INTERVAL 13 DAY),
 NULL, 0, 0, NULL),

(2, 14, 6, 14, 'FALSE_CLAIM',
 '取件的同学并不是我本人，我的耳机被冒领了，请核查取件签字。',
 NULL, 1,
 '核对取件签字与监控，确认取件人为他人。物品转「待归还」由站点收回；冒领人扣 5 分并记误领 1 次，发布人奖励保留。',
 NULL, 2, DATE_SUB(NOW(), INTERVAL 16 DAY),
 DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY),
 '取件签字笔迹与登记人不一致，监控佐证', 2, DATE_SUB(NOW(), INTERVAL 16 DAY),
 NULL, 0, 0, NULL),

(3, 14, 5, 13, 'ITEM_MISMATCH',
 '取到的包装盒里只有泡沫内衬和说明书，显卡本体不在盒中，请核查入站时是否已开箱清点。',
 NULL, 0, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY),
 NULL, NULL, NULL, NULL, 0, 0, NULL),

(4, 10, NULL, 5, 'FALSE_CLAIM',
 '我的保温杯被人拿走了，要求核查这件招领的取件记录。',
 NULL, 2,
 '监控显示取件人即发布人本人，杯子已归还本人，不构成冒领，驳回。',
 NULL, 2, DATE_SUB(NOW(), INTERVAL 4 DAY),
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
 '监控片段已留存 7 天', 2, DATE_SUB(NOW(), INTERVAL 4 DAY),
 NULL, 0, 0, NULL),

(5, 13, 8, 20, 'ITEM_NOT_FOUND',
 '认领通过后到站取件，工作人员说货架上找不到这台手机，希望站点协助核查。',
 NULL, 0, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
 NULL, NULL, NULL, NULL, 0, 0, NULL),

(6, 10, 17, 21, 'ITEM_NOT_FOUND',
 '到站取件时站点说伞已经不在货架，要求调监控确认是否被他人取走。',
 NULL, 1,
 '调取 11-10 17:00-19:00 监控：18:40 一名男生取走该伞后离开。站点超 24 小时未回填，已催办 2 次并标记站点未响应。',
 'STOLEN', 2, DATE_SUB(NOW(), INTERVAL 6 DAY),
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
 '监控已调取，物品转「丢失待查」，站点承担保管责任并纳入月度考核', 2, DATE_SUB(NOW(), INTERVAL 6 DAY),
 DATE_SUB(NOW(), INTERVAL 7 DAY), 2, 1, DATE_SUB(NOW(), INTERVAL 7 DAY));

-- ------------------------------------------------------------
-- 6. 寻物启事 6 条（进行中 / 已完成 / 已下架；1 条命中匹配）
-- ------------------------------------------------------------
INSERT INTO lost_notice
(id, title, category, description, lost_location, lost_time, contact_info, images, status,
 publisher_id, matched_item_id, created_at, updated_at, takedown_reason, takedown_at, takedown_by, appeal_status)
VALUES
(1, '寻找一把黑色长柄雨伞', '生活用品',
 '伞套内侧用记号笔写着 3-2-14，上周三傍晚在图书馆三楼自习后忘记带走，如有人拾到请联系我。',
 '图书馆三楼自习区', TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 4 DAY)), '19:00:00'),
 '站内信联系，或到图书馆失物招领处登记', NULL, 0, 10, NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL, NULL, 0),

(2, '寻找校园卡（学号尾号 0621）', '证件卡类',
 '白色卡面、右上角有浅绿色色块，姓名孙悦。丢失后已挂失补办，如有人拾到请交回投放点。',
 '1 号宿舍楼食堂一层', TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 1 DAY)), '12:00:00'),
 '站内信联系', NULL, 0, 12, NULL,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, 0),

(3, '寻找白色无线耳机', '电子产品',
 '白色半入耳耳机，充电盒背面刻着 LY 两个字母，落在活动中心二楼自习角。',
 '学生活动中心二楼自习角', TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 6 DAY)), '15:00:00'),
 '站内信联系', NULL, 0, 12, 2,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL, NULL, 0),

(4, '寻找粉色陶瓷马克杯', '生活用品',
 '杯底有一颗星星图案。已通过招领领回，感谢拾到的同学。',
 '第一教学楼 A 座饮水间', TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 16 DAY)), '13:00:00'),
 '已找回，无需联系', NULL, 2, 11, 12,
 DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), NULL, NULL, NULL, 0),

(5, '寻找银色编织手链', '饰品',
 '编织银链，龙虾扣，链身有一点氧化。比赛时在看台摘下，可能落在座位下方。',
 '体育馆羽毛球场看台', TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 3 DAY)), '17:00:00'),
 '站内信联系', NULL, 0, 6, NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, NULL, 0),

(6, '寻找深绿色帆布双肩包', '其他',
 '包内有一件灰色外套，与已发布的招领信息重复，管理员已下架本条。',
 '第一教学楼 A 座 302', TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 2 DAY)), '10:00:00'),
 '站内信联系', NULL, 3, 10, 4,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
 '重复发布：与招领 #4 为同一物品', DATE_SUB(NOW(), INTERVAL 1 DAY), 1, 0);

UPDATE lost_notice SET matched_item_id = 2  WHERE id = 3;
UPDATE found_item  SET forwarded_notice_id = 3, forwarder_id = 5 WHERE id = 2;

-- ------------------------------------------------------------
-- 7. 诚信积分流水（最终分数由流水求和回写）
-- ------------------------------------------------------------
INSERT INTO credit_log
(user_id, change_amount, operation_type, related_item_id, related_apply_id, reason, operator_id, created_at)
VALUES
(4, 20, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(4, 1, 'CHECK_ISSUE', 1, NULL, '图书馆巡检核对：招领已入站且信息属实', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 1, 'CHECK_ISSUE', 7, NULL, '教学楼巡检核对：高价值物品已封存', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),

(5, 12, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(5, 1, 'CHECK_ISSUE', 4, NULL, '教学楼巡检核对：招领已入站', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 1, 'FORWARD_REWARD', 2, NULL, '转发寻物启事并成功匹配招领', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 1, 'CHECK_ISSUE', 20, NULL, '图书馆巡检核对：招领已入柜', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),

(6, 10, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(6, 1, 'CHECK_ISSUE', 10, NULL, '活动中心巡检核对：招领已入站', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 1, 'CHECK_ISSUE', 6, NULL, '宿舍区巡检核对：卡片脱敏后公开', NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(6, 1, 'PICKUP_ISSUE', 12, 4, '认领完成：马克杯已现场取走', NULL, DATE_SUB(NOW(), INTERVAL 14 DAY)),

(7, 6, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(7, 1, 'PICKUP_ISSUE', 11, 3, '认领完成：钥匙串已现场取走', NULL, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(7, 1, 'CHECK_ISSUE', 19, NULL, '图书馆巡检核对：教材超期归档', NULL, DATE_SUB(NOW(), INTERVAL 119 DAY)),
(7, 1, 'PICKUP_ISSUE', 14, 6, '认领完成：耳机已现场取走（后经核查为冒领）', NULL, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(7, -1, 'PICKUP_ISSUE', 14, 6, '冒领工单通过：回滚本次取件奖励 1 分', 2, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(7, -5, 'FALSE_CLAIM_PENALTY', 14, 6, '冒领工单通过：恶意冒领扣 5 分', 2, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(7, 0, 'MISCLAIM_COUNT', 14, 6, '冒领工单通过：计误领 1 次，不计分', 2, DATE_SUB(NOW(), INTERVAL 16 DAY)),

(9, 20, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(9, 1, 'CHECK_ISSUE', 2, NULL, '活动中心巡检核对：耳机已入柜', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(9, 1, 'CHECK_ISSUE', 11, NULL, '宿舍区巡检核对：招领已入站', NULL, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(9, 3, 'PICKUP_ISSUE', 11, 3, '名下招领被成功认领领取', NULL, DATE_SUB(NOW(), INTERVAL 19 DAY)),

(10, 10, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(10, 1, 'PICKUP_ISSUE', 15, 7, '认领完成：雨伞已现场取走', NULL, DATE_SUB(NOW(), INTERVAL 11 DAY)),

(11, 5, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(11, 1, 'CHECK_ISSUE', 8, NULL, '图书馆巡检核对：招领已入站', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(11, 1, 'CHECK_ISSUE', 17, NULL, '宿舍区巡检核对：保温杯超期处理', NULL, DATE_SUB(NOW(), INTERVAL 379 DAY)),

(12, 8, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(12, 1, 'CHECK_ISSUE', 12, NULL, '教学楼巡检核对：招领已入站', NULL, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(12, 1, 'CHECK_ISSUE', 16, NULL, '图书馆巡检核对：校园卡超期处理', NULL, DATE_SUB(NOW(), INTERVAL 399 DAY)),
(12, 3, 'PICKUP_ISSUE', 12, 4, '名下招领被成功认领领取（损坏定责站点，奖励保留）', NULL, DATE_SUB(NOW(), INTERVAL 14 DAY)),

(13, 15, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(13, 1, 'CHECK_ISSUE', 3, NULL, '体育馆巡检核对：手链已入玻璃柜', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13, 1, 'CHECK_ISSUE', 9, NULL, '图书馆巡检核对：钱包现金已双人清点', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(13, 1, 'CHECK_ISSUE', 13, NULL, '教学楼巡检核对：招领已入站', NULL, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(13, 3, 'PICKUP_ISSUE', 13, 5, '名下招领被成功认领领取', NULL, DATE_SUB(NOW(), INTERVAL 29 DAY)),
(13, 1, 'CHECK_ISSUE', 15, NULL, '体育馆巡检核对：雨伞已入架', NULL, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(13, 3, 'PICKUP_ISSUE', 15, 7, '名下招领被成功认领领取', NULL, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(13, 1, 'CHECK_ISSUE', 21, NULL, '体育馆巡检核对：入站登记已完成', NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),

(14, 12, 'ADMIN_ADJUST', NULL, NULL, '历史诚信积分结转（2024-2026 学年）', NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(14, 1, 'CHECK_ISSUE', 14, NULL, '活动中心巡检核对：耳机已入柜', NULL, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(14, 3, 'PICKUP_ISSUE', 14, 6, '名下招领被认领领取（后经核查为冒领，奖励保留）', NULL, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(14, 1, 'PICKUP_ISSUE', 13, 5, '认领完成：显卡包装盒已现场取走', NULL, DATE_SUB(NOW(), INTERVAL 29 DAY)),

(15, -6, 'ADMIN_ADJUST', NULL, NULL, '账号违规：跨账号互刷招领奖励，扣至下限并列入黑名单', 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(15, 1, 'CHECK_ISSUE', 18, NULL, '宿舍区复核入站信息（账号已黑名单）', NULL, DATE_SUB(NOW(), INTERVAL 9 DAY));

-- ------------------------------------------------------------
-- 8. 站内消息 10 条
-- ------------------------------------------------------------
INSERT INTO sys_message
(id, user_id, title, content, biz_type, related_item_id, related_apply_id, is_read, created_at)
VALUES
(1, 5, '认领已通过，请携带学生证取件',
 '你认领的「酒红色短款钱包」已通过发布人审核。请于 3 日内携带学生证到「图书馆一楼大厅东侧」失物招领处现场核对并签收，逾期将自动退回待审核。',
 'CLAIM', 9, 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),

(2, 13, '你的招领已有认领人待取件',
 '「酒红色短款钱包」已通过认领审核，认领人将在 3 日内到站取件。取件完成后你将获得 3 分诚信积分。',
 'CLAIM', 9, 1, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),

(3, 4, '巡检核对通过，诚信积分 +1',
 '你发布的「黑色智能手机（屏幕有裂痕）」已由站点管理员完成巡检核对，物品信息与实物一致，获得 1 分诚信积分。',
 'CREDIT', 7, NULL, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),

(4, 6, '损坏件已转「待归还」，站点将收回',
 '你的损坏申诉已通过，定责方为站点（保管/流转环节导致）。物品已转「待归还」，站点收回后作废处理，不会再重新进入公开池供他人认领。',
 'DISPUTE', 12, 4, 0, DATE_SUB(NOW(), INTERVAL 13 DAY)),

(5, 14, '物品被冒领，已对冒领人扣分',
 '你的「白色无线耳机（第二代）」被非本人取走。经站点调取监控与签字核对，工单判定成立：冒领人被扣 5 分并记误领 1 次，相关积分奖励已回滚。',
 'DISPUTE', 14, 6, 1, DATE_SUB(NOW(), INTERVAL 16 DAY)),

(6, 2, '站点超 24 小时未回填，已催办并标记',
 '工单 #6（体育馆·找不到物品）已超过 24 小时未回填现场核查结论，系统已自动催办并标记「站点未响应」，同时升级给系统管理员。',
 'SITE', 21, NULL, 0, DATE_SUB(NOW(), INTERVAL 7 DAY)),

(7, 1, '工单升级：站点未响应待定责',
 '工单 #6 已标记「站点未响应（11-12 09:40）」，请在定责时参考该标记，并按站点保管责任处理。',
 'SITE', 21, NULL, 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),

(8, 15, '账号已列入黑名单，名下招领自动下架',
 '你因跨账号互刷招领奖励，诚信积分扣至 -5 并进入黑名单。名下 1 件待认领招领已自动下架，账号暂时无法登录与发布。',
 'MODERATION', 18, NULL, 0, DATE_SUB(NOW(), INTERVAL 9 DAY)),

(9, 14, '防伪答题错误 3 次，已锁定 24 小时',
 '你对「黑色智能手机（屏幕有裂痕）」的认领申请连续 3 次回答错误，申请已锁定，24 小时内不能重新提交。本次锁定将在到期后由系统自动解锁。',
 'CLAIM', 7, 13, 0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),

(10, 13, '「找不到物品」已通知站点核查',
 '你在站点申报「找不到物品」已受理（工单 #5）。站点管理员将在 24 小时内核查货架与取件记录，必要时调取监控，请留意站内信通知。',
 'DISPUTE', 20, 8, 0, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ------------------------------------------------------------
-- 9. 举报工单 4 条
-- ------------------------------------------------------------
INSERT INTO report_log
(id, reporter_id, item_id, report_type, description, evidence_images, status, handler_note,
 handler_id, handled_at, created_at, updated_at, target_type, notice_id)
VALUES
(1, 12, 18, 'FAKE_PUBLISH', '同一张校园卡在一天内被不同账号发布三次，怀疑是刷分。', NULL, 1,
 '成立：已下架重复招领并对该账号扣至下限、列入黑名单。', 1, DATE_SUB(NOW(), INTERVAL 9 DAY),
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), 'ITEM', NULL),

(2, 9, 7, 'DESC_MISMATCH', '描述里写屏幕有裂痕，但照片上看不出来，担心是虚假描述。', NULL, 0, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'ITEM', NULL),

(3, 11, 6, 'OTHER', '这条寻物启事和另一条内容基本一样，像是重复发布。', NULL, 1,
 '成立：与招领信息重复，已下架该启事并提示发布人。', 1, DATE_SUB(NOW(), INTERVAL 1 DAY),
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'NOTICE', 6),

(4, 6, 11, 'OTHER', '这个钥匙串看起来不像他本人丢的，可能是拿别人的东西来发。', NULL, 2,
 '不成立：发布人已出示购买记录与宿舍门禁登记，物品来源清晰。', 1, DATE_SUB(NOW(), INTERVAL 18 DAY),
 DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), 'ITEM', NULL);

-- ------------------------------------------------------------
-- 10. 调监控申请 2 条（已处理 / 待处理）
-- ------------------------------------------------------------
INSERT INTO camera_log
(drop_point_id, item_id, apply_reason, applicant_id, time_range_start, time_range_end, status,
 result_note, handler_id, handled_at, created_at, updated_at)
VALUES
(6, 21, '认领人到站后未找到物品，需调取当时段监控确认是否被他人取走。', 13,
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 10 DAY)), '17:00:00'),
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 10 DAY)), '19:00:00'), 1,
 '确认 11-10 18:40 一名男生从雨伞架取走该伞后离开，已截图存档。', 2,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

(2, 20, '高价值手机在站失踪，需核对 11-15 至 11-18 的取件与进出记录。', 13,
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 4 DAY)), '08:00:00'),
 TIMESTAMP(DATE(DATE_SUB(NOW(), INTERVAL 1 DAY)), '20:00:00'), 0,
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ------------------------------------------------------------
-- 11. 误领次数与诚信分回写（分数必须等于流水求和）
-- -------------------------------------------------------------
UPDATE `user` SET misclaim_count = 0;
UPDATE `user` SET misclaim_count = 1 WHERE id = 7;

UPDATE `user` u
SET credit_score = (SELECT IFNULL(SUM(c.change_amount), 0) FROM credit_log c WHERE c.user_id = u.id);

-- ------------------------------------------------------------
-- 12. 时间线归一（创建 15:00 < 交接 15:30 < 核验 16:00 < 公开 16:30，拾取时刻早于公开）
-- ------------------------------------------------------------
UPDATE found_item SET found_time = TIMESTAMP(DATE(found_time),
  CASE id WHEN 1 THEN '15:00:00' WHEN 2 THEN '14:35:00' WHEN 3 THEN '16:00:00'
   WHEN 4 THEN '09:00:00' WHEN 5 THEN '15:00:00' WHEN 7 THEN '15:00:00'
   WHEN 8 THEN '16:00:00' WHEN 9 THEN '11:00:00' WHEN 10 THEN '10:00:00'
   WHEN 11 THEN '12:00:00' WHEN 12 THEN '14:00:00' WHEN 13 THEN '15:00:00'
   WHEN 14 THEN '13:20:00' WHEN 15 THEN '15:00:00' WHEN 16 THEN '10:00:00'
   WHEN 17 THEN '15:00:00' WHEN 18 THEN '15:00:00' WHEN 19 THEN '11:00:00'
   WHEN 20 THEN '12:00:00' WHEN 21 THEN '15:00:00' ELSE '15:00:00' END)
WHERE id IN (1,2,3,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21);

UPDATE found_item SET found_time = DATE_SUB(NOW(), INTERVAL 3 HOUR) WHERE id = 6;
UPDATE found_item SET found_time = DATE_SUB(NOW(), INTERVAL 4 HOUR) WHERE id = 22;
UPDATE found_item SET found_time = DATE_SUB(NOW(), INTERVAL 3 HOUR) WHERE id = 23;

UPDATE found_item
SET created_at   = TIMESTAMP(DATE(created_at), '15:00:00'),
    published_at = IF(published_at IS NULL, NULL, TIMESTAMP(DATE(published_at), '16:30:00'))
WHERE id IN (1,2,3,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21);

UPDATE found_item
SET updated_at = created_at
WHERE id IN (1,2,3,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21)
  AND updated_at < created_at;

UPDATE hand_in_log
SET handed_in_at    = TIMESTAMP(DATE(handed_in_at), '15:30:00'),
    checked_at      = IF(checked_at IS NULL, NULL, TIMESTAMP(DATE(checked_at), '16:00:00')),
    credit_issued_at= IF(credit_issued_at IS NULL, NULL, TIMESTAMP(DATE(credit_issued_at), '16:00:00')),
    created_at      = TIMESTAMP(DATE(created_at), '15:30:00')
WHERE item_id IN (1,2,3,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21);

-- 6 号是「今天刚入站」的样本，把四段时刻错开：拾取 3 小时前 → 创建 130 分钟前 → 交接 115 分钟前 → 核验 95 分钟前 → 公开 80 分钟前
UPDATE found_item SET created_at = DATE_SUB(NOW(), INTERVAL 130 MINUTE),
                     published_at = DATE_SUB(NOW(), INTERVAL 80 MINUTE) WHERE id = 6;
UPDATE hand_in_log SET handed_in_at = DATE_SUB(NOW(), INTERVAL 115 MINUTE),
                      checked_at = DATE_SUB(NOW(), INTERVAL 95 MINUTE),
                      credit_issued_at = DATE_SUB(NOW(), INTERVAL 95 MINUTE),
                      created_at = DATE_SUB(NOW(), INTERVAL 130 MINUTE) WHERE item_id = 6;


