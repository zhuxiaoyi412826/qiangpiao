-- =============================================================
--  车次导入：公开高铁线路（车次模板 + 库存 + 座位 + 车厢 + 线路/时刻表）
--  只插入「今天」的模板班次，TrainScheduleTask 会自动复制出未来 30 天班次
--  幂等：全部使用 INSERT IGNORE / NOT EXISTS，可重复执行
--  生成时间：2026-09-27T14:27:47.461Z
-- =============================================================
USE qiangpiao;

-- ---------- 1. 车次模板（depart_date = CURDATE()） ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
VALUES
('G1', '高铁', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '06:20:00', '10:48:00', 268, 1),
('G3', '高铁', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '07:00:00', '11:28:00', 268, 1),
('G5', '高铁', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '07:30:00', '12:05:00', 275, 1),
('G7', '高铁', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '08:00:00', '12:28:00', 268, 1),
('G9', '高铁', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '09:00:00', '13:32:00', 272, 1),
('G11', '高铁', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '10:00:00', '14:35:00', 275, 1),
('G2', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '06:30:00', '10:58:00', 268, 1),
('G4', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '07:10:00', '11:42:00', 272, 1),
('G6', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '08:00:00', '12:28:00', 268, 1),
('G8', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '09:00:00', '13:35:00', 275, 1),
('G10', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '10:00:00', '14:28:00', 268, 1),
('G12', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '11:00:00', '15:32:00', 272, 1),
('G101', '高铁', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '济南西'), '济南西', CURDATE(), '07:00:00', '08:22:00', 82, 1),
('G102', '高铁', (SELECT id FROM t_station WHERE station_name = '济南西'), '济南西', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '08:30:00', '09:52:00', 82, 1),
('G105', '高铁', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '08:00:00', '09:10:00', 70, 1),
('G106', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', CURDATE(), '09:30:00', '10:40:00', 70, 1),
('G107', '高铁', (SELECT id FROM t_station WHERE station_name = '济南西'), '济南西', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '12:00:00', '15:05:00', 185, 1),
('G108', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '济南西'), '济南西', CURDATE(), '13:00:00', '16:05:00', 185, 1),
('G71', '高铁', (SELECT id FROM t_station WHERE station_name = '北京西'), '北京西', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', CURDATE(), '07:00:00', '15:00:00', 480, 1),
('G79', '高铁', (SELECT id FROM t_station WHERE station_name = '北京西'), '北京西', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', CURDATE(), '10:00:00', '18:00:00', 480, 1),
('G72', '高铁', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', (SELECT id FROM t_station WHERE station_name = '北京西'), '北京西', CURDATE(), '08:00:00', '16:00:00', 480, 1),
('G80', '高铁', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', (SELECT id FROM t_station WHERE station_name = '北京西'), '北京西', CURDATE(), '10:30:00', '18:30:00', 480, 1),
('G501', '高铁', (SELECT id FROM t_station WHERE station_name = '北京西'), '北京西', (SELECT id FROM t_station WHERE station_name = '武汉'), '武汉', CURDATE(), '08:00:00', '12:30:00', 270, 1),
('G502', '高铁', (SELECT id FROM t_station WHERE station_name = '武汉'), '武汉', (SELECT id FROM t_station WHERE station_name = '北京西'), '北京西', CURDATE(), '09:00:00', '13:30:00', 270, 1),
('G1003', '高铁', (SELECT id FROM t_station WHERE station_name = '武汉'), '武汉', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', CURDATE(), '12:00:00', '16:00:00', 240, 1),
('G1004', '高铁', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', (SELECT id FROM t_station WHERE station_name = '武汉'), '武汉', CURDATE(), '13:00:00', '17:00:00', 240, 1),
('G1101', '高铁', (SELECT id FROM t_station WHERE station_name = '长沙南'), '长沙南', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', CURDATE(), '14:00:00', '16:20:00', 140, 1),
('G1102', '高铁', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', (SELECT id FROM t_station WHERE station_name = '长沙南'), '长沙南', CURDATE(), '15:00:00', '17:20:00', 140, 1),
('G1371', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '昆明南'), '昆明南', CURDATE(), '06:40:00', '17:16:00', 636, 1),
('G1372', '高铁', (SELECT id FROM t_station WHERE station_name = '昆明南'), '昆明南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '07:20:00', '17:56:00', 636, 1),
('G1301', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '长沙南'), '长沙南', CURDATE(), '08:15:00', '13:45:00', 330, 1),
('G1302', '高铁', (SELECT id FROM t_station WHERE station_name = '长沙南'), '长沙南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '09:00:00', '14:30:00', 330, 1),
('G1401', '高铁', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', (SELECT id FROM t_station WHERE station_name = '南昌西'), '南昌西', CURDATE(), '10:00:00', '12:20:00', 140, 1),
('G1402', '高铁', (SELECT id FROM t_station WHERE station_name = '南昌西'), '南昌西', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', CURDATE(), '11:00:00', '13:20:00', 140, 1),
('G901', '高铁', (SELECT id FROM t_station WHERE station_name = '北京朝阳'), '北京朝阳', (SELECT id FROM t_station WHERE station_name = '哈尔滨西'), '哈尔滨西', CURDATE(), '06:30:00', '11:00:00', 270, 1),
('G902', '高铁', (SELECT id FROM t_station WHERE station_name = '哈尔滨西'), '哈尔滨西', (SELECT id FROM t_station WHERE station_name = '北京朝阳'), '北京朝阳', CURDATE(), '07:00:00', '11:30:00', 270, 1),
('G3601', '高铁', (SELECT id FROM t_station WHERE station_name = '北京朝阳'), '北京朝阳', (SELECT id FROM t_station WHERE station_name = '沈阳北'), '沈阳北', CURDATE(), '08:00:00', '10:30:00', 150, 1),
('G3602', '高铁', (SELECT id FROM t_station WHERE station_name = '沈阳北'), '沈阳北', (SELECT id FROM t_station WHERE station_name = '北京朝阳'), '北京朝阳', CURDATE(), '09:00:00', '11:30:00', 150, 1),
('G2005', '高铁', (SELECT id FROM t_station WHERE station_name = '郑州东'), '郑州东', (SELECT id FROM t_station WHERE station_name = '西安北'), '西安北', CURDATE(), '07:00:00', '09:00:00', 120, 1),
('G2006', '高铁', (SELECT id FROM t_station WHERE station_name = '西安北'), '西安北', (SELECT id FROM t_station WHERE station_name = '郑州东'), '郑州东', CURDATE(), '08:00:00', '10:00:00', 120, 1),
('G2007', '高铁', (SELECT id FROM t_station WHERE station_name = '西安北'), '西安北', (SELECT id FROM t_station WHERE station_name = '兰州西'), '兰州西', CURDATE(), '09:00:00', '12:00:00', 180, 1),
('G2008', '高铁', (SELECT id FROM t_station WHERE station_name = '兰州西'), '兰州西', (SELECT id FROM t_station WHERE station_name = '西安北'), '西安北', CURDATE(), '10:00:00', '13:00:00', 180, 1),
('G8501', '高铁', (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东', (SELECT id FROM t_station WHERE station_name = '重庆北'), '重庆北', CURDATE(), '07:00:00', '08:30:00', 90, 1),
('G8502', '高铁', (SELECT id FROM t_station WHERE station_name = '重庆北'), '重庆北', (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东', CURDATE(), '08:00:00', '09:30:00', 90, 1),
('G8503', '高铁', (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东', (SELECT id FROM t_station WHERE station_name = '重庆北'), '重庆北', CURDATE(), '12:00:00', '13:30:00', 90, 1),
('G8504', '高铁', (SELECT id FROM t_station WHERE station_name = '重庆北'), '重庆北', (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东', CURDATE(), '13:00:00', '14:30:00', 90, 1),
('G6501', '高铁', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', CURDATE(), '07:00:00', '07:30:00', 30, 1),
('G6502', '高铁', (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', CURDATE(), '08:00:00', '08:30:00', 30, 1),
('G6503', '高铁', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', CURDATE(), '12:00:00', '12:30:00', 30, 1),
('G6504', '高铁', (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', CURDATE(), '13:00:00', '13:30:00', 30, 1),
('D3101', '动车', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', CURDATE(), '07:30:00', '15:30:00', 480, 1),
('D3102', '动车', (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', CURDATE(), '08:00:00', '16:00:00', 480, 1),
('D3201', '动车', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', (SELECT id FROM t_station WHERE station_name = '福州南'), '福州南', CURDATE(), '09:00:00', '12:30:00', 210, 1),
('D3202', '动车', (SELECT id FROM t_station WHERE station_name = '福州南'), '福州南', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', CURDATE(), '10:00:00', '13:30:00', 210, 1),
('C2001', '城际', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '天津'), '天津', CURDATE(), '07:00:00', '07:30:00', 30, 1),
('C2002', '城际', (SELECT id FROM t_station WHERE station_name = '天津'), '天津', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '07:30:00', '08:00:00', 30, 1),
('C2003', '城际', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', (SELECT id FROM t_station WHERE station_name = '天津'), '天津', CURDATE(), '12:00:00', '12:30:00', 30, 1),
('C2004', '城际', (SELECT id FROM t_station WHERE station_name = '天津'), '天津', (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南', CURDATE(), '12:30:00', '13:00:00', 30, 1),
('G7001', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', CURDATE(), '07:00:00', '08:15:00', 75, 1),
('G7002', '高铁', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '08:00:00', '09:15:00', 75, 1),
('G7003', '高铁', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', CURDATE(), '12:00:00', '13:15:00', 75, 1),
('G7004', '高铁', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', CURDATE(), '13:00:00', '14:15:00', 75, 1),
('G8801', '高铁', (SELECT id FROM t_station WHERE station_name = '北京北'), '北京北', (SELECT id FROM t_station WHERE station_name = '张家口'), '张家口', CURDATE(), '08:00:00', '09:00:00', 60, 1),
('G8802', '高铁', (SELECT id FROM t_station WHERE station_name = '张家口'), '张家口', (SELECT id FROM t_station WHERE station_name = '北京北'), '北京北', CURDATE(), '09:00:00', '10:00:00', 60, 1),
('G1601', '高铁', (SELECT id FROM t_station WHERE station_name = '合肥南'), '合肥南', (SELECT id FROM t_station WHERE station_name = '福州'), '福州', CURDATE(), '08:00:00', '11:00:00', 180, 1),
('G1602', '高铁', (SELECT id FROM t_station WHERE station_name = '福州'), '福州', (SELECT id FROM t_station WHERE station_name = '合肥南'), '合肥南', CURDATE(), '09:00:00', '12:00:00', 180, 1),
('G7601', '高铁', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', CURDATE(), '08:00:00', '09:10:00', 70, 1),
('G7602', '高铁', (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', CURDATE(), '09:00:00', '10:10:00', 70, 1),
('G6901', '高铁', (SELECT id FROM t_station WHERE station_name = '济南西'), '济南西', (SELECT id FROM t_station WHERE station_name = '青岛北'), '青岛北', CURDATE(), '08:00:00', '10:30:00', 150, 1),
('G6902', '高铁', (SELECT id FROM t_station WHERE station_name = '青岛北'), '青岛北', (SELECT id FROM t_station WHERE station_name = '济南西'), '济南西', CURDATE(), '09:00:00', '11:30:00', 150, 1),
('G2201', '高铁', (SELECT id FROM t_station WHERE station_name = '西安北'), '西安北', (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东', CURDATE(), '08:00:00', '11:30:00', 210, 1),
('G2202', '高铁', (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东', (SELECT id FROM t_station WHERE station_name = '西安北'), '西安北', CURDATE(), '09:00:00', '12:30:00', 210, 1)
;

-- ---------- 2. 席别库存（商务座 100 / 一等座 200 / 二等座 500） ----------
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G1' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G1' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G1' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G3' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G3' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G3' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G5' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G5' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G5' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G7' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G7' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G7' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G9' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G9' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G9' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G11' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G11' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G11' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G2' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G2' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G2' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G4' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G4' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G4' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G6' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G6' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G6' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G8' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G8' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G8' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G10' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G10' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G10' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1714.00, 0
FROM t_train t WHERE t.train_no = 'G12' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 885.00, 0
FROM t_train t WHERE t.train_no = 'G12' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 553.00, 0
FROM t_train t WHERE t.train_no = 'G12' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 570.00, 0
FROM t_train t WHERE t.train_no = 'G101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 294.00, 0
FROM t_train t WHERE t.train_no = 'G101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 184.00, 0
FROM t_train t WHERE t.train_no = 'G101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 570.00, 0
FROM t_train t WHERE t.train_no = 'G102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 294.00, 0
FROM t_train t WHERE t.train_no = 'G102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 184.00, 0
FROM t_train t WHERE t.train_no = 'G102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 432.00, 0
FROM t_train t WHERE t.train_no = 'G105' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 223.00, 0
FROM t_train t WHERE t.train_no = 'G105' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 140.00, 0
FROM t_train t WHERE t.train_no = 'G105' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 432.00, 0
FROM t_train t WHERE t.train_no = 'G106' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 223.00, 0
FROM t_train t WHERE t.train_no = 'G106' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 140.00, 0
FROM t_train t WHERE t.train_no = 'G106' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1231.00, 0
FROM t_train t WHERE t.train_no = 'G107' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 635.00, 0
FROM t_train t WHERE t.train_no = 'G107' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 397.00, 0
FROM t_train t WHERE t.train_no = 'G107' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1231.00, 0
FROM t_train t WHERE t.train_no = 'G108' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 635.00, 0
FROM t_train t WHERE t.train_no = 'G108' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 397.00, 0
FROM t_train t WHERE t.train_no = 'G108' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 2672.00, 0
FROM t_train t WHERE t.train_no = 'G71' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 1379.00, 0
FROM t_train t WHERE t.train_no = 'G71' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 862.00, 0
FROM t_train t WHERE t.train_no = 'G71' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 2672.00, 0
FROM t_train t WHERE t.train_no = 'G79' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 1379.00, 0
FROM t_train t WHERE t.train_no = 'G79' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 862.00, 0
FROM t_train t WHERE t.train_no = 'G79' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 2672.00, 0
FROM t_train t WHERE t.train_no = 'G72' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 1379.00, 0
FROM t_train t WHERE t.train_no = 'G72' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 862.00, 0
FROM t_train t WHERE t.train_no = 'G72' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 2672.00, 0
FROM t_train t WHERE t.train_no = 'G80' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 1379.00, 0
FROM t_train t WHERE t.train_no = 'G80' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 862.00, 0
FROM t_train t WHERE t.train_no = 'G80' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1614.00, 0
FROM t_train t WHERE t.train_no = 'G501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 833.00, 0
FROM t_train t WHERE t.train_no = 'G501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 521.00, 0
FROM t_train t WHERE t.train_no = 'G501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1614.00, 0
FROM t_train t WHERE t.train_no = 'G502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 833.00, 0
FROM t_train t WHERE t.train_no = 'G502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 521.00, 0
FROM t_train t WHERE t.train_no = 'G502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1437.00, 0
FROM t_train t WHERE t.train_no = 'G1003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 742.00, 0
FROM t_train t WHERE t.train_no = 'G1003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 464.00, 0
FROM t_train t WHERE t.train_no = 'G1003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1437.00, 0
FROM t_train t WHERE t.train_no = 'G1004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 742.00, 0
FROM t_train t WHERE t.train_no = 'G1004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 464.00, 0
FROM t_train t WHERE t.train_no = 'G1004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 973.00, 0
FROM t_train t WHERE t.train_no = 'G1101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 502.00, 0
FROM t_train t WHERE t.train_no = 'G1101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 314.00, 0
FROM t_train t WHERE t.train_no = 'G1101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 973.00, 0
FROM t_train t WHERE t.train_no = 'G1102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 502.00, 0
FROM t_train t WHERE t.train_no = 'G1102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 314.00, 0
FROM t_train t WHERE t.train_no = 'G1102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 2725.00, 0
FROM t_train t WHERE t.train_no = 'G1371' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 1406.00, 0
FROM t_train t WHERE t.train_no = 'G1371' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 879.00, 0
FROM t_train t WHERE t.train_no = 'G1371' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 2725.00, 0
FROM t_train t WHERE t.train_no = 'G1372' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 1406.00, 0
FROM t_train t WHERE t.train_no = 'G1372' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 879.00, 0
FROM t_train t WHERE t.train_no = 'G1372' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1851.00, 0
FROM t_train t WHERE t.train_no = 'G1301' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 955.00, 0
FROM t_train t WHERE t.train_no = 'G1301' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 597.00, 0
FROM t_train t WHERE t.train_no = 'G1301' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1851.00, 0
FROM t_train t WHERE t.train_no = 'G1302' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 955.00, 0
FROM t_train t WHERE t.train_no = 'G1302' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 597.00, 0
FROM t_train t WHERE t.train_no = 'G1302' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 817.00, 0
FROM t_train t WHERE t.train_no = 'G1401' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 422.00, 0
FROM t_train t WHERE t.train_no = 'G1401' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 264.00, 0
FROM t_train t WHERE t.train_no = 'G1401' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 817.00, 0
FROM t_train t WHERE t.train_no = 'G1402' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 422.00, 0
FROM t_train t WHERE t.train_no = 'G1402' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 264.00, 0
FROM t_train t WHERE t.train_no = 'G1402' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1677.00, 0
FROM t_train t WHERE t.train_no = 'G901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 866.00, 0
FROM t_train t WHERE t.train_no = 'G901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 541.00, 0
FROM t_train t WHERE t.train_no = 'G901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1677.00, 0
FROM t_train t WHERE t.train_no = 'G902' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 866.00, 0
FROM t_train t WHERE t.train_no = 'G902' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 541.00, 0
FROM t_train t WHERE t.train_no = 'G902' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1042.00, 0
FROM t_train t WHERE t.train_no = 'G3601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 538.00, 0
FROM t_train t WHERE t.train_no = 'G3601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 336.00, 0
FROM t_train t WHERE t.train_no = 'G3601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1042.00, 0
FROM t_train t WHERE t.train_no = 'G3602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 538.00, 0
FROM t_train t WHERE t.train_no = 'G3602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 336.00, 0
FROM t_train t WHERE t.train_no = 'G3602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 741.00, 0
FROM t_train t WHERE t.train_no = 'G2005' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 382.00, 0
FROM t_train t WHERE t.train_no = 'G2005' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 239.00, 0
FROM t_train t WHERE t.train_no = 'G2005' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 741.00, 0
FROM t_train t WHERE t.train_no = 'G2006' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 382.00, 0
FROM t_train t WHERE t.train_no = 'G2006' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 239.00, 0
FROM t_train t WHERE t.train_no = 'G2006' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 814.00, 0
FROM t_train t WHERE t.train_no = 'G2007' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 420.00, 0
FROM t_train t WHERE t.train_no = 'G2007' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 263.00, 0
FROM t_train t WHERE t.train_no = 'G2007' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 814.00, 0
FROM t_train t WHERE t.train_no = 'G2008' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 420.00, 0
FROM t_train t WHERE t.train_no = 'G2008' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 263.00, 0
FROM t_train t WHERE t.train_no = 'G2008' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 477.00, 0
FROM t_train t WHERE t.train_no = 'G8501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 246.00, 0
FROM t_train t WHERE t.train_no = 'G8501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 154.00, 0
FROM t_train t WHERE t.train_no = 'G8501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 477.00, 0
FROM t_train t WHERE t.train_no = 'G8502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 246.00, 0
FROM t_train t WHERE t.train_no = 'G8502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 154.00, 0
FROM t_train t WHERE t.train_no = 'G8502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 477.00, 0
FROM t_train t WHERE t.train_no = 'G8503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 246.00, 0
FROM t_train t WHERE t.train_no = 'G8503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 154.00, 0
FROM t_train t WHERE t.train_no = 'G8503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 477.00, 0
FROM t_train t WHERE t.train_no = 'G8504' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 246.00, 0
FROM t_train t WHERE t.train_no = 'G8504' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 154.00, 0
FROM t_train t WHERE t.train_no = 'G8504' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 231.00, 0
FROM t_train t WHERE t.train_no = 'G6501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 119.00, 0
FROM t_train t WHERE t.train_no = 'G6501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 75.00, 0
FROM t_train t WHERE t.train_no = 'G6501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 231.00, 0
FROM t_train t WHERE t.train_no = 'G6502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 119.00, 0
FROM t_train t WHERE t.train_no = 'G6502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 75.00, 0
FROM t_train t WHERE t.train_no = 'G6502' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 231.00, 0
FROM t_train t WHERE t.train_no = 'G6503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 119.00, 0
FROM t_train t WHERE t.train_no = 'G6503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 75.00, 0
FROM t_train t WHERE t.train_no = 'G6503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 231.00, 0
FROM t_train t WHERE t.train_no = 'G6504' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 119.00, 0
FROM t_train t WHERE t.train_no = 'G6504' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 75.00, 0
FROM t_train t WHERE t.train_no = 'G6504' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1736.00, 0
FROM t_train t WHERE t.train_no = 'D3101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 896.00, 0
FROM t_train t WHERE t.train_no = 'D3101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 560.00, 0
FROM t_train t WHERE t.train_no = 'D3101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1736.00, 0
FROM t_train t WHERE t.train_no = 'D3102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 896.00, 0
FROM t_train t WHERE t.train_no = 'D3102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 560.00, 0
FROM t_train t WHERE t.train_no = 'D3102' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 809.00, 0
FROM t_train t WHERE t.train_no = 'D3201' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 418.00, 0
FROM t_train t WHERE t.train_no = 'D3201' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 261.00, 0
FROM t_train t WHERE t.train_no = 'D3201' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 809.00, 0
FROM t_train t WHERE t.train_no = 'D3202' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 418.00, 0
FROM t_train t WHERE t.train_no = 'D3202' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 261.00, 0
FROM t_train t WHERE t.train_no = 'D3202' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 169.00, 0
FROM t_train t WHERE t.train_no = 'C2001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 87.00, 0
FROM t_train t WHERE t.train_no = 'C2001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 55.00, 0
FROM t_train t WHERE t.train_no = 'C2001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 169.00, 0
FROM t_train t WHERE t.train_no = 'C2002' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 87.00, 0
FROM t_train t WHERE t.train_no = 'C2002' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 55.00, 0
FROM t_train t WHERE t.train_no = 'C2002' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 169.00, 0
FROM t_train t WHERE t.train_no = 'C2003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 87.00, 0
FROM t_train t WHERE t.train_no = 'C2003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 55.00, 0
FROM t_train t WHERE t.train_no = 'C2003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 169.00, 0
FROM t_train t WHERE t.train_no = 'C2004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 87.00, 0
FROM t_train t WHERE t.train_no = 'C2004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 55.00, 0
FROM t_train t WHERE t.train_no = 'C2004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 432.00, 0
FROM t_train t WHERE t.train_no = 'G7001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 223.00, 0
FROM t_train t WHERE t.train_no = 'G7001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 140.00, 0
FROM t_train t WHERE t.train_no = 'G7001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 432.00, 0
FROM t_train t WHERE t.train_no = 'G7002' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 223.00, 0
FROM t_train t WHERE t.train_no = 'G7002' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 140.00, 0
FROM t_train t WHERE t.train_no = 'G7002' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 432.00, 0
FROM t_train t WHERE t.train_no = 'G7003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 223.00, 0
FROM t_train t WHERE t.train_no = 'G7003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 140.00, 0
FROM t_train t WHERE t.train_no = 'G7003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 432.00, 0
FROM t_train t WHERE t.train_no = 'G7004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 223.00, 0
FROM t_train t WHERE t.train_no = 'G7004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 140.00, 0
FROM t_train t WHERE t.train_no = 'G7004' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 270.00, 0
FROM t_train t WHERE t.train_no = 'G8801' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 139.00, 0
FROM t_train t WHERE t.train_no = 'G8801' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 87.00, 0
FROM t_train t WHERE t.train_no = 'G8801' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 270.00, 0
FROM t_train t WHERE t.train_no = 'G8802' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 139.00, 0
FROM t_train t WHERE t.train_no = 'G8802' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 87.00, 0
FROM t_train t WHERE t.train_no = 'G8802' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1107.00, 0
FROM t_train t WHERE t.train_no = 'G1601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 571.00, 0
FROM t_train t WHERE t.train_no = 'G1601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 357.00, 0
FROM t_train t WHERE t.train_no = 'G1601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 1107.00, 0
FROM t_train t WHERE t.train_no = 'G1602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 571.00, 0
FROM t_train t WHERE t.train_no = 'G1602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 357.00, 0
FROM t_train t WHERE t.train_no = 'G1602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 367.00, 0
FROM t_train t WHERE t.train_no = 'G7601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 190.00, 0
FROM t_train t WHERE t.train_no = 'G7601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 119.00, 0
FROM t_train t WHERE t.train_no = 'G7601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 367.00, 0
FROM t_train t WHERE t.train_no = 'G7602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 190.00, 0
FROM t_train t WHERE t.train_no = 'G7602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 119.00, 0
FROM t_train t WHERE t.train_no = 'G7602' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 552.00, 0
FROM t_train t WHERE t.train_no = 'G6901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 285.00, 0
FROM t_train t WHERE t.train_no = 'G6901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 178.00, 0
FROM t_train t WHERE t.train_no = 'G6901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 552.00, 0
FROM t_train t WHERE t.train_no = 'G6902' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 285.00, 0
FROM t_train t WHERE t.train_no = 'G6902' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 178.00, 0
FROM t_train t WHERE t.train_no = 'G6902' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 815.00, 0
FROM t_train t WHERE t.train_no = 'G2201' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 421.00, 0
FROM t_train t WHERE t.train_no = 'G2201' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 263.00, 0
FROM t_train t WHERE t.train_no = 'G2201' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 1, 100, 100, 815.00, 0
FROM t_train t WHERE t.train_no = 'G2202' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 2, 200, 200, 421.00, 0
FROM t_train t WHERE t.train_no = 'G2202' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, 3, 500, 500, 263.00, 0
FROM t_train t WHERE t.train_no = 'G2202' AND t.depart_date = CURDATE();

-- ---------- 3. 座位图（1 车厢商务 / 2-3 车厢一等 / 4-8 车厢二等，每车厢 20 排 × 5 座） ----------
CREATE TABLE IF NOT EXISTS t_seq (n INT PRIMARY KEY);
INSERT IGNORE INTO t_seq (n) VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13),(14),(15),(16),(17),(18),(19),(20);

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT t.id, ty.seat_type, c.n, CONCAT(LPAD(r.n, 2, '0'), sl.letter), 0, 0
FROM t_train t
         CROSS JOIN (SELECT 1 AS seat_type UNION ALL SELECT 2 UNION ALL SELECT 3) ty
         CROSS JOIN (SELECT n FROM t_seq WHERE n <= 8) c
         CROSS JOIN (SELECT n FROM t_seq WHERE n <= 20) r
         CROSS JOIN (SELECT 'A' letter UNION ALL SELECT 'B' UNION ALL SELECT 'C'
                     UNION ALL SELECT 'D' UNION ALL SELECT 'F') sl
WHERE t.train_no IN ('G1', 'G3', 'G5', 'G7', 'G9', 'G11', 'G2', 'G4', 'G6', 'G8', 'G10', 'G12', 'G101', 'G102', 'G105', 'G106', 'G107', 'G108', 'G71', 'G79', 'G72', 'G80', 'G501', 'G502', 'G1003', 'G1004', 'G1101', 'G1102', 'G1371', 'G1372', 'G1301', 'G1302', 'G1401', 'G1402', 'G901', 'G902', 'G3601', 'G3602', 'G2005', 'G2006', 'G2007', 'G2008', 'G8501', 'G8502', 'G8503', 'G8504', 'G6501', 'G6502', 'G6503', 'G6504', 'D3101', 'D3102', 'D3201', 'D3202', 'C2001', 'C2002', 'C2003', 'C2004', 'G7001', 'G7002', 'G7003', 'G7004', 'G8801', 'G8802', 'G1601', 'G1602', 'G7601', 'G7602', 'G6901', 'G6902', 'G2201', 'G2202') AND t.depart_date = CURDATE()
  AND (   (ty.seat_type = 1 AND c.n = 1)
       OR (ty.seat_type = 2 AND c.n IN (2, 3))
       OR (ty.seat_type = 3 AND c.n BETWEEN 4 AND 8));

-- ---------- 4. 车厢表 ----------
INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT t.id, c.n, CASE WHEN c.n = 1 THEN 1 WHEN c.n <= 3 THEN 2 ELSE 3 END, 100
FROM t_train t
         CROSS JOIN (SELECT n FROM t_seq WHERE n <= 8) c
WHERE t.train_no IN ('G1', 'G3', 'G5', 'G7', 'G9', 'G11', 'G2', 'G4', 'G6', 'G8', 'G10', 'G12', 'G101', 'G102', 'G105', 'G106', 'G107', 'G108', 'G71', 'G79', 'G72', 'G80', 'G501', 'G502', 'G1003', 'G1004', 'G1101', 'G1102', 'G1371', 'G1372', 'G1301', 'G1302', 'G1401', 'G1402', 'G901', 'G902', 'G3601', 'G3602', 'G2005', 'G2006', 'G2007', 'G2008', 'G8501', 'G8502', 'G8503', 'G8504', 'G6501', 'G6502', 'G6503', 'G6504', 'D3101', 'D3102', 'D3201', 'D3202', 'C2001', 'C2002', 'C2003', 'C2004', 'G7001', 'G7002', 'G7003', 'G7004', 'G8801', 'G8802', 'G1601', 'G1602', 'G7601', 'G7602', 'G6901', 'G6902', 'G2201', 'G2202') AND t.depart_date = CURDATE();

-- ---------- 5. 线路（t_line + t_line_station） ----------
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '京沪高铁',
       (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南',
       (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '京沪高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order UNION ALL              SELECT '廊坊' AS station_name, 2 AS stop_order UNION ALL              SELECT '天津南' AS station_name, 3 AS stop_order UNION ALL              SELECT '德州东' AS station_name, 4 AS stop_order UNION ALL              SELECT '济南西' AS station_name, 5 AS stop_order UNION ALL              SELECT '泰安' AS station_name, 6 AS stop_order UNION ALL              SELECT '曲阜东' AS station_name, 7 AS stop_order UNION ALL              SELECT '徐州东' AS station_name, 8 AS stop_order UNION ALL              SELECT '蚌埠南' AS station_name, 9 AS stop_order UNION ALL              SELECT '南京南' AS station_name, 10 AS stop_order UNION ALL              SELECT '镇江南' AS station_name, 11 AS stop_order UNION ALL              SELECT '无锡东' AS station_name, 12 AS stop_order UNION ALL              SELECT '苏州北' AS station_name, 13 AS stop_order UNION ALL              SELECT '上海虹桥' AS station_name, 14 AS stop_order
             ) tmp
WHERE l.line_name = '京沪高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '京广高铁',
       (SELECT id FROM t_station WHERE station_name = '北京西'), '北京西',
       (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '京广高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '北京西' AS station_name, 1 AS stop_order UNION ALL              SELECT '保定东' AS station_name, 2 AS stop_order UNION ALL              SELECT '石家庄' AS station_name, 3 AS stop_order UNION ALL              SELECT '郑州东' AS station_name, 4 AS stop_order UNION ALL              SELECT '武汉' AS station_name, 5 AS stop_order UNION ALL              SELECT '长沙南' AS station_name, 6 AS stop_order UNION ALL              SELECT '广州南' AS station_name, 7 AS stop_order
             ) tmp
WHERE l.line_name = '京广高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '沪昆高铁',
       (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥',
       (SELECT id FROM t_station WHERE station_name = '昆明南'), '昆明南', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '沪昆高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '上海虹桥' AS station_name, 1 AS stop_order UNION ALL              SELECT '杭州东' AS station_name, 2 AS stop_order UNION ALL              SELECT '南昌西' AS station_name, 3 AS stop_order UNION ALL              SELECT '长沙南' AS station_name, 4 AS stop_order UNION ALL              SELECT '贵阳北' AS station_name, 5 AS stop_order UNION ALL              SELECT '昆明南' AS station_name, 6 AS stop_order
             ) tmp
WHERE l.line_name = '沪昆高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '京哈高铁',
       (SELECT id FROM t_station WHERE station_name = '北京朝阳'), '北京朝阳',
       (SELECT id FROM t_station WHERE station_name = '哈尔滨西'), '哈尔滨西', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '京哈高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '北京朝阳' AS station_name, 1 AS stop_order UNION ALL              SELECT '承德南' AS station_name, 2 AS stop_order UNION ALL              SELECT '沈阳北' AS station_name, 3 AS stop_order UNION ALL              SELECT '长春西' AS station_name, 4 AS stop_order UNION ALL              SELECT '哈尔滨西' AS station_name, 5 AS stop_order
             ) tmp
WHERE l.line_name = '京哈高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '徐兰高铁',
       (SELECT id FROM t_station WHERE station_name = '郑州东'), '郑州东',
       (SELECT id FROM t_station WHERE station_name = '兰州西'), '兰州西', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '徐兰高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '郑州东' AS station_name, 1 AS stop_order UNION ALL              SELECT '洛阳龙门' AS station_name, 2 AS stop_order UNION ALL              SELECT '西安北' AS station_name, 3 AS stop_order UNION ALL              SELECT '宝鸡南' AS station_name, 4 AS stop_order UNION ALL              SELECT '兰州西' AS station_name, 5 AS stop_order
             ) tmp
WHERE l.line_name = '徐兰高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '成渝高铁',
       (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东',
       (SELECT id FROM t_station WHERE station_name = '重庆北'), '重庆北', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '成渝高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '成都东' AS station_name, 1 AS stop_order UNION ALL              SELECT '资阳北' AS station_name, 2 AS stop_order UNION ALL              SELECT '内江北' AS station_name, 3 AS stop_order UNION ALL              SELECT '重庆北' AS station_name, 4 AS stop_order
             ) tmp
WHERE l.line_name = '成渝高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '广深港高铁',
       (SELECT id FROM t_station WHERE station_name = '广州南'), '广州南',
       (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '广深港高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '广州南' AS station_name, 1 AS stop_order UNION ALL              SELECT '虎门' AS station_name, 2 AS stop_order UNION ALL              SELECT '光明城' AS station_name, 3 AS stop_order UNION ALL              SELECT '深圳北' AS station_name, 4 AS stop_order
             ) tmp
WHERE l.line_name = '广深港高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '杭深线',
       (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东',
       (SELECT id FROM t_station WHERE station_name = '深圳北'), '深圳北', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '杭深线');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '杭州东' AS station_name, 1 AS stop_order UNION ALL              SELECT '宁波' AS station_name, 2 AS stop_order UNION ALL              SELECT '台州西' AS station_name, 3 AS stop_order UNION ALL              SELECT '温州南' AS station_name, 4 AS stop_order UNION ALL              SELECT '福州南' AS station_name, 5 AS stop_order UNION ALL              SELECT '厦门北' AS station_name, 6 AS stop_order UNION ALL              SELECT '深圳北' AS station_name, 7 AS stop_order
             ) tmp
WHERE l.line_name = '杭深线';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '京津城际',
       (SELECT id FROM t_station WHERE station_name = '北京南'), '北京南',
       (SELECT id FROM t_station WHERE station_name = '天津'), '天津', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '京津城际');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order UNION ALL              SELECT '武清' AS station_name, 2 AS stop_order UNION ALL              SELECT '天津' AS station_name, 3 AS stop_order
             ) tmp
WHERE l.line_name = '京津城际';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '沪宁城际',
       (SELECT id FROM t_station WHERE station_name = '上海虹桥'), '上海虹桥',
       (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '沪宁城际');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '上海虹桥' AS station_name, 1 AS stop_order UNION ALL              SELECT '苏州' AS station_name, 2 AS stop_order UNION ALL              SELECT '无锡' AS station_name, 3 AS stop_order UNION ALL              SELECT '常州' AS station_name, 4 AS stop_order UNION ALL              SELECT '南京南' AS station_name, 5 AS stop_order
             ) tmp
WHERE l.line_name = '沪宁城际';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '京张高铁',
       (SELECT id FROM t_station WHERE station_name = '北京北'), '北京北',
       (SELECT id FROM t_station WHERE station_name = '张家口'), '张家口', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '京张高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '北京北' AS station_name, 1 AS stop_order UNION ALL              SELECT '清河' AS station_name, 2 AS stop_order UNION ALL              SELECT '张家口' AS station_name, 3 AS stop_order
             ) tmp
WHERE l.line_name = '京张高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '合福高铁',
       (SELECT id FROM t_station WHERE station_name = '合肥南'), '合肥南',
       (SELECT id FROM t_station WHERE station_name = '福州'), '福州', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '合福高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '合肥南' AS station_name, 1 AS stop_order UNION ALL              SELECT '黄山北' AS station_name, 2 AS stop_order UNION ALL              SELECT '南平市' AS station_name, 3 AS stop_order UNION ALL              SELECT '福州' AS station_name, 4 AS stop_order
             ) tmp
WHERE l.line_name = '合福高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '宁杭高铁',
       (SELECT id FROM t_station WHERE station_name = '南京南'), '南京南',
       (SELECT id FROM t_station WHERE station_name = '杭州东'), '杭州东', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '宁杭高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '南京南' AS station_name, 1 AS stop_order UNION ALL              SELECT '溧阳' AS station_name, 2 AS stop_order UNION ALL              SELECT '宜兴' AS station_name, 3 AS stop_order UNION ALL              SELECT '湖州' AS station_name, 4 AS stop_order UNION ALL              SELECT '杭州东' AS station_name, 5 AS stop_order
             ) tmp
WHERE l.line_name = '宁杭高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '济青高铁',
       (SELECT id FROM t_station WHERE station_name = '济南西'), '济南西',
       (SELECT id FROM t_station WHERE station_name = '青岛北'), '青岛北', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '济青高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '济南西' AS station_name, 1 AS stop_order UNION ALL              SELECT '淄博北' AS station_name, 2 AS stop_order UNION ALL              SELECT '潍坊北' AS station_name, 3 AS stop_order UNION ALL              SELECT '青岛北' AS station_name, 4 AS stop_order
             ) tmp
WHERE l.line_name = '济青高铁';
INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)
SELECT '西成高铁',
       (SELECT id FROM t_station WHERE station_name = '西安北'), '西安北',
       (SELECT id FROM t_station WHERE station_name = '成都东'), '成都东', 1
WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '西成高铁');
INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)
SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order
FROM t_line l
         JOIN (
             SELECT '西安北' AS station_name, 1 AS stop_order UNION ALL              SELECT '汉中' AS station_name, 2 AS stop_order UNION ALL              SELECT '广元' AS station_name, 3 AS stop_order UNION ALL              SELECT '成都东' AS station_name, 4 AS stop_order
             ) tmp
WHERE l.line_name = '西成高铁';

-- ---------- 6. 车次时刻表（t_train_stop） ----------
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '06:20:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '廊坊', 2, '06:41:00', '06:43:00', 2 UNION ALL
             SELECT '天津南', 3, '07:04:00', '07:06:00', 2 UNION ALL
             SELECT '德州东', 4, '07:27:00', '07:29:00', 2 UNION ALL
             SELECT '济南西', 5, '07:50:00', '07:52:00', 2 UNION ALL
             SELECT '泰安', 6, '08:13:00', '08:15:00', 2 UNION ALL
             SELECT '曲阜东', 7, '08:36:00', '08:38:00', 2 UNION ALL
             SELECT '徐州东', 8, '08:59:00', '09:01:00', 2 UNION ALL
             SELECT '蚌埠南', 9, '09:22:00', '09:24:00', 2 UNION ALL
             SELECT '南京南', 10, '09:45:00', '09:47:00', 2 UNION ALL
             SELECT '镇江南', 11, '10:08:00', '10:10:00', 2 UNION ALL
             SELECT '无锡东', 12, '10:31:00', '10:33:00', 2 UNION ALL
             SELECT '苏州北', 13, '10:54:00', '10:56:00', 2 UNION ALL
             SELECT '上海虹桥', 14, '10:48:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G1' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '廊坊', 2, '07:21:00', '07:23:00', 2 UNION ALL
             SELECT '天津南', 3, '07:44:00', '07:46:00', 2 UNION ALL
             SELECT '德州东', 4, '08:07:00', '08:09:00', 2 UNION ALL
             SELECT '济南西', 5, '08:30:00', '08:32:00', 2 UNION ALL
             SELECT '泰安', 6, '08:53:00', '08:55:00', 2 UNION ALL
             SELECT '曲阜东', 7, '09:16:00', '09:18:00', 2 UNION ALL
             SELECT '徐州东', 8, '09:39:00', '09:41:00', 2 UNION ALL
             SELECT '蚌埠南', 9, '10:02:00', '10:04:00', 2 UNION ALL
             SELECT '南京南', 10, '10:25:00', '10:27:00', 2 UNION ALL
             SELECT '镇江南', 11, '10:48:00', '10:50:00', 2 UNION ALL
             SELECT '无锡东', 12, '11:11:00', '11:13:00', 2 UNION ALL
             SELECT '苏州北', 13, '11:34:00', '11:36:00', 2 UNION ALL
             SELECT '上海虹桥', 14, '11:28:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G3' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:30:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '廊坊', 2, '07:51:00', '07:53:00', 2 UNION ALL
             SELECT '天津南', 3, '08:14:00', '08:16:00', 2 UNION ALL
             SELECT '德州东', 4, '08:37:00', '08:39:00', 2 UNION ALL
             SELECT '济南西', 5, '09:00:00', '09:02:00', 2 UNION ALL
             SELECT '泰安', 6, '09:23:00', '09:25:00', 2 UNION ALL
             SELECT '曲阜东', 7, '09:46:00', '09:48:00', 2 UNION ALL
             SELECT '徐州东', 8, '10:09:00', '10:11:00', 2 UNION ALL
             SELECT '蚌埠南', 9, '10:32:00', '10:34:00', 2 UNION ALL
             SELECT '南京南', 10, '10:55:00', '10:57:00', 2 UNION ALL
             SELECT '镇江南', 11, '11:18:00', '11:20:00', 2 UNION ALL
             SELECT '无锡东', 12, '11:41:00', '11:43:00', 2 UNION ALL
             SELECT '苏州北', 13, '12:04:00', '12:06:00', 2 UNION ALL
             SELECT '上海虹桥', 14, '12:05:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G5' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '廊坊', 2, '08:21:00', '08:23:00', 2 UNION ALL
             SELECT '天津南', 3, '08:44:00', '08:46:00', 2 UNION ALL
             SELECT '德州东', 4, '09:07:00', '09:09:00', 2 UNION ALL
             SELECT '济南西', 5, '09:30:00', '09:32:00', 2 UNION ALL
             SELECT '泰安', 6, '09:53:00', '09:55:00', 2 UNION ALL
             SELECT '曲阜东', 7, '10:16:00', '10:18:00', 2 UNION ALL
             SELECT '徐州东', 8, '10:39:00', '10:41:00', 2 UNION ALL
             SELECT '蚌埠南', 9, '11:02:00', '11:04:00', 2 UNION ALL
             SELECT '南京南', 10, '11:25:00', '11:27:00', 2 UNION ALL
             SELECT '镇江南', 11, '11:48:00', '11:50:00', 2 UNION ALL
             SELECT '无锡东', 12, '12:11:00', '12:13:00', 2 UNION ALL
             SELECT '苏州北', 13, '12:34:00', '12:36:00', 2 UNION ALL
             SELECT '上海虹桥', 14, '12:28:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G7' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '09:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '廊坊', 2, '09:21:00', '09:23:00', 2 UNION ALL
             SELECT '天津南', 3, '09:44:00', '09:46:00', 2 UNION ALL
             SELECT '德州东', 4, '10:07:00', '10:09:00', 2 UNION ALL
             SELECT '济南西', 5, '10:30:00', '10:32:00', 2 UNION ALL
             SELECT '泰安', 6, '10:53:00', '10:55:00', 2 UNION ALL
             SELECT '曲阜东', 7, '11:16:00', '11:18:00', 2 UNION ALL
             SELECT '徐州东', 8, '11:39:00', '11:41:00', 2 UNION ALL
             SELECT '蚌埠南', 9, '12:02:00', '12:04:00', 2 UNION ALL
             SELECT '南京南', 10, '12:25:00', '12:27:00', 2 UNION ALL
             SELECT '镇江南', 11, '12:48:00', '12:50:00', 2 UNION ALL
             SELECT '无锡东', 12, '13:11:00', '13:13:00', 2 UNION ALL
             SELECT '苏州北', 13, '13:34:00', '13:36:00', 2 UNION ALL
             SELECT '上海虹桥', 14, '13:32:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G9' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '10:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '廊坊', 2, '10:21:00', '10:23:00', 2 UNION ALL
             SELECT '天津南', 3, '10:44:00', '10:46:00', 2 UNION ALL
             SELECT '德州东', 4, '11:07:00', '11:09:00', 2 UNION ALL
             SELECT '济南西', 5, '11:30:00', '11:32:00', 2 UNION ALL
             SELECT '泰安', 6, '11:53:00', '11:55:00', 2 UNION ALL
             SELECT '曲阜东', 7, '12:16:00', '12:18:00', 2 UNION ALL
             SELECT '徐州东', 8, '12:39:00', '12:41:00', 2 UNION ALL
             SELECT '蚌埠南', 9, '13:02:00', '13:04:00', 2 UNION ALL
             SELECT '南京南', 10, '13:25:00', '13:27:00', 2 UNION ALL
             SELECT '镇江南', 11, '13:48:00', '13:50:00', 2 UNION ALL
             SELECT '无锡东', 12, '14:11:00', '14:13:00', 2 UNION ALL
             SELECT '苏州北', 13, '14:34:00', '14:36:00', 2 UNION ALL
             SELECT '上海虹桥', 14, '14:35:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G11' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '廊坊', 2, '07:21:00', '07:23:00', 2 UNION ALL
             SELECT '天津南', 3, '07:44:00', '07:46:00', 2 UNION ALL
             SELECT '德州东', 4, '08:07:00', '08:09:00', 2 UNION ALL
             SELECT '济南西', 5, '08:22:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '南京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '镇江南', 2, '08:18:00', '08:20:00', 2 UNION ALL
             SELECT '无锡东', 3, '08:38:00', '08:40:00', 2 UNION ALL
             SELECT '苏州北', 4, '08:58:00', '09:00:00', 2 UNION ALL
             SELECT '上海虹桥', 5, '09:10:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G105' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '济南西' AS station_name, 1 AS stop_order, NULL AS arrive_time, '12:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '泰安', 2, '12:21:00', '12:23:00', 2 UNION ALL
             SELECT '曲阜东', 3, '12:44:00', '12:46:00', 2 UNION ALL
             SELECT '徐州东', 4, '13:07:00', '13:09:00', 2 UNION ALL
             SELECT '蚌埠南', 5, '13:30:00', '13:32:00', 2 UNION ALL
             SELECT '南京南', 6, '13:53:00', '13:55:00', 2 UNION ALL
             SELECT '镇江南', 7, '14:16:00', '14:18:00', 2 UNION ALL
             SELECT '无锡东', 8, '14:39:00', '14:41:00', 2 UNION ALL
             SELECT '苏州北', 9, '15:02:00', '15:04:00', 2 UNION ALL
             SELECT '上海虹桥', 10, '15:05:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G107' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京西' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '保定东', 2, '08:20:00', '08:22:00', 2 UNION ALL
             SELECT '石家庄', 3, '09:42:00', '09:44:00', 2 UNION ALL
             SELECT '郑州东', 4, '11:04:00', '11:06:00', 2 UNION ALL
             SELECT '武汉', 5, '12:26:00', '12:28:00', 2 UNION ALL
             SELECT '长沙南', 6, '13:48:00', '13:50:00', 2 UNION ALL
             SELECT '广州南', 7, '15:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G71' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京西' AS station_name, 1 AS stop_order, NULL AS arrive_time, '10:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '保定东', 2, '11:20:00', '11:22:00', 2 UNION ALL
             SELECT '石家庄', 3, '12:42:00', '12:44:00', 2 UNION ALL
             SELECT '郑州东', 4, '14:04:00', '14:06:00', 2 UNION ALL
             SELECT '武汉', 5, '15:26:00', '15:28:00', 2 UNION ALL
             SELECT '长沙南', 6, '16:48:00', '16:50:00', 2 UNION ALL
             SELECT '广州南', 7, '18:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G79' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京西' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '保定东', 2, '09:08:00', '09:10:00', 2 UNION ALL
             SELECT '石家庄', 3, '10:18:00', '10:20:00', 2 UNION ALL
             SELECT '郑州东', 4, '11:28:00', '11:30:00', 2 UNION ALL
             SELECT '武汉', 5, '12:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '武汉' AS station_name, 1 AS stop_order, NULL AS arrive_time, '12:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '长沙南', 2, '14:00:00', '14:02:00', 2 UNION ALL
             SELECT '广州南', 3, '16:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G1003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '长沙南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '14:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '广州南', 2, '16:20:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G1101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '上海虹桥' AS station_name, 1 AS stop_order, NULL AS arrive_time, '06:40:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '杭州东', 2, '08:47:00', '08:49:00', 2 UNION ALL
             SELECT '南昌西', 3, '10:56:00', '10:58:00', 2 UNION ALL
             SELECT '长沙南', 4, '13:05:00', '13:07:00', 2 UNION ALL
             SELECT '贵阳北', 5, '15:14:00', '15:16:00', 2 UNION ALL
             SELECT '昆明南', 6, '17:16:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G1371' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '上海虹桥' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:15:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '杭州东', 2, '10:05:00', '10:07:00', 2 UNION ALL
             SELECT '南昌西', 3, '11:57:00', '11:59:00', 2 UNION ALL
             SELECT '长沙南', 4, '13:45:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G1301' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '杭州东' AS station_name, 1 AS stop_order, NULL AS arrive_time, '10:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '南昌西', 2, '12:20:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G1401' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京朝阳' AS station_name, 1 AS stop_order, NULL AS arrive_time, '06:30:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '承德南', 2, '07:38:00', '07:40:00', 2 UNION ALL
             SELECT '沈阳北', 3, '08:48:00', '08:50:00', 2 UNION ALL
             SELECT '长春西', 4, '09:58:00', '10:00:00', 2 UNION ALL
             SELECT '哈尔滨西', 5, '11:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京朝阳' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '承德南', 2, '09:15:00', '09:17:00', 2 UNION ALL
             SELECT '沈阳北', 3, '10:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G3601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '郑州东' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '洛阳龙门', 2, '08:00:00', '08:02:00', 2 UNION ALL
             SELECT '西安北', 3, '09:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G2005' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '西安北' AS station_name, 1 AS stop_order, NULL AS arrive_time, '09:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '宝鸡南', 2, '10:30:00', '10:32:00', 2 UNION ALL
             SELECT '兰州西', 3, '12:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G2007' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '成都东' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '资阳北', 2, '07:30:00', '07:32:00', 2 UNION ALL
             SELECT '内江北', 3, '08:02:00', '08:04:00', 2 UNION ALL
             SELECT '重庆北', 4, '08:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G8501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '成都东' AS station_name, 1 AS stop_order, NULL AS arrive_time, '12:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '资阳北', 2, '12:30:00', '12:32:00', 2 UNION ALL
             SELECT '内江北', 3, '13:02:00', '13:04:00', 2 UNION ALL
             SELECT '重庆北', 4, '13:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G8503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '广州南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '虎门', 2, '07:10:00', '07:12:00', 2 UNION ALL
             SELECT '光明城', 3, '07:22:00', '07:24:00', 2 UNION ALL
             SELECT '深圳北', 4, '07:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G6501' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '广州南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '12:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '虎门', 2, '12:10:00', '12:12:00', 2 UNION ALL
             SELECT '光明城', 3, '12:22:00', '12:24:00', 2 UNION ALL
             SELECT '深圳北', 4, '12:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G6503' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '杭州东' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:30:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '宁波', 2, '08:50:00', '08:52:00', 2 UNION ALL
             SELECT '台州西', 3, '10:12:00', '10:14:00', 2 UNION ALL
             SELECT '温州南', 4, '11:34:00', '11:36:00', 2 UNION ALL
             SELECT '福州南', 5, '12:56:00', '12:58:00', 2 UNION ALL
             SELECT '厦门北', 6, '14:18:00', '14:20:00', 2 UNION ALL
             SELECT '深圳北', 7, '15:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'D3101' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '杭州东' AS station_name, 1 AS stop_order, NULL AS arrive_time, '09:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '宁波', 2, '09:53:00', '09:55:00', 2 UNION ALL
             SELECT '台州西', 3, '10:48:00', '10:50:00', 2 UNION ALL
             SELECT '温州南', 4, '11:43:00', '11:45:00', 2 UNION ALL
             SELECT '福州南', 5, '12:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'D3201' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '武清', 2, '07:15:00', '07:17:00', 2 UNION ALL
             SELECT '天津', 3, '07:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'C2001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '12:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '武清', 2, '12:15:00', '12:17:00', 2 UNION ALL
             SELECT '天津', 3, '12:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'C2003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '上海虹桥' AS station_name, 1 AS stop_order, NULL AS arrive_time, '07:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '苏州', 2, '07:19:00', '07:21:00', 2 UNION ALL
             SELECT '无锡', 3, '07:40:00', '07:42:00', 2 UNION ALL
             SELECT '常州', 4, '08:01:00', '08:03:00', 2 UNION ALL
             SELECT '南京南', 5, '08:15:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G7001' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '上海虹桥' AS station_name, 1 AS stop_order, NULL AS arrive_time, '12:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '苏州', 2, '12:19:00', '12:21:00', 2 UNION ALL
             SELECT '无锡', 3, '12:40:00', '12:42:00', 2 UNION ALL
             SELECT '常州', 4, '13:01:00', '13:03:00', 2 UNION ALL
             SELECT '南京南', 5, '13:15:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G7003' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '北京北' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '清河', 2, '08:30:00', '08:32:00', 2 UNION ALL
             SELECT '张家口', 3, '09:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G8801' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '合肥南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '黄山北', 2, '09:00:00', '09:02:00', 2 UNION ALL
             SELECT '南平市', 3, '10:02:00', '10:04:00', 2 UNION ALL
             SELECT '福州', 4, '11:00:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G1601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '南京南' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '溧阳', 2, '08:18:00', '08:20:00', 2 UNION ALL
             SELECT '宜兴', 3, '08:38:00', '08:40:00', 2 UNION ALL
             SELECT '湖州', 4, '08:58:00', '09:00:00', 2 UNION ALL
             SELECT '杭州东', 5, '09:10:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G7601' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '济南西' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '淄博北', 2, '08:50:00', '08:52:00', 2 UNION ALL
             SELECT '潍坊北', 3, '09:42:00', '09:44:00', 2 UNION ALL
             SELECT '青岛北', 4, '10:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G6901' AND t.depart_date = CURDATE();
INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)
SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),
       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes
FROM t_train t
         JOIN (
             SELECT '西安北' AS station_name, 1 AS stop_order, NULL AS arrive_time, '08:00:00' AS depart_time, 0 AS stop_minutes UNION ALL
             SELECT '汉中', 2, '09:10:00', '09:12:00', 2 UNION ALL
             SELECT '广元', 3, '10:22:00', '10:24:00', 2 UNION ALL
             SELECT '成都东', 4, '11:30:00', NULL, 2
             ) tmp
WHERE t.train_no = 'G2201' AND t.depart_date = CURDATE();

-- ---------- 7. 校准库存与真实可售座位数一致 ----------
UPDATE t_train_stock st
    JOIN (SELECT train_id, seat_type, COUNT(*) AS cnt FROM t_seat WHERE status = 0
          GROUP BY train_id, seat_type) s
    ON s.train_id = st.train_id AND s.seat_type = st.seat_type
SET st.total_count = s.cnt, st.available_count = s.cnt;
