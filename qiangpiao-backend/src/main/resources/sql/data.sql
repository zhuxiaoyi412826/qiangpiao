-- =============================================================
--  初始化数据：车站 / 车次 / 库存 / 座位 / 测试用户
--  密码统一为 123456（BCrypt 密文）
-- =============================================================
USE qiangpiao;

-- ---------- 车站 ----------
INSERT INTO t_station (station_name, city, py_code) VALUES
('北京南', '北京', 'BJN'),
('上海虹桥', '上海', 'SHHQ'),
('广州南', '广州', 'GZN'),
('深圳北', '深圳', 'SZB'),
('杭州东', '杭州', 'HZD'),
('南京南', '南京', 'NJN'),
('武汉', '武汉', 'WH'),
('成都东', '成都', 'CDD'),
('西安北', '西安', 'XAB'),
('长沙南', '长沙', 'CSN') ON DUPLICATE KEY UPDATE city = VALUES(city);

-- ---------- 用户（密码 123456） ----------
INSERT INTO t_user (username, password, real_name, phone, id_card, role, status) VALUES
('admin', '$2a$10$So/jYvXr9wSTuKZzOicO4.ums8Dn35ZLUJYXnJa3dnm2K1Dcsg9l.', '系统管理员', '13800000000', '110101199001011010', 'ROLE_ADMIN', 1),
('zhangsan', '$2a$10$So/jYvXr9wSTuKZzOicO4.ums8Dn35ZLUJYXnJa3dnm2K1Dcsg9l.', '张三', '13800000001', '110101199001011011', 'ROLE_USER', 1),
('lisi', '$2a$10$So/jYvXr9wSTuKZzOicO4.ums8Dn35ZLUJYXnJa3dnm2K1Dcsg9l.', '李四', '13800000002', '110101199001011012', 'ROLE_USER', 1)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), password = VALUES(password);

-- ---------- 车次（日期使用相对当天，便于测试） ----------
DELETE FROM t_seat;
DELETE FROM t_train_stock;
DELETE FROM t_train WHERE train_no LIKE 'G%' OR train_no LIKE 'D%';

INSERT INTO t_train (train_no, train_type, from_station_id, from_station_name, to_station_id, to_station_name,
                     depart_date, depart_time, arrive_time, duration_minutes, status) VALUES
('G1001', '高铁', 1, '北京南', 2, '上海虹桥', CURDATE(), '08:00:00', '12:30:00', 270, 1),
('G1002', '高铁', 1, '北京南', 2, '上海虹桥', CURDATE(), '13:00:00', '17:45:00', 285, 1),
('G2001', '高铁', 1, '北京南', 5, '杭州东', CURDATE(), '09:15:00', '14:05:00', 290, 1),
('D3001', '动车', 2, '上海虹桥', 6, '南京南', CURDATE(), '07:30:00', '09:05:00', 95, 1),
('G3001', '高铁', 3, '广州南', 4, '深圳北', CURDATE(), '10:00:00', '10:35:00', 35, 1),
('G4001', '高铁', 7, '武汉', 10, '长沙南', CURDATE(), '15:20:00', '17:10:00', 110, 1);

-- ---------- 席别库存（秒杀库存） ----------
INSERT INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT t.id, s.seat_type, s.total_count, s.total_count, s.price, 0
FROM t_train t
CROSS JOIN (
    SELECT 1 AS seat_type, 100 AS total_count, 1200.00 AS price
    UNION ALL SELECT 2, 200, 800.00
    UNION ALL SELECT 3, 500, 450.00
) s;

-- ---------- 座位：商务座(1车厢) / 一等座(2-3车厢) / 二等座(4-8车厢) ----------
DROP TABLE IF EXISTS t_seq;
CREATE TABLE t_seq (n INT PRIMARY KEY);
INSERT INTO t_seq (n) VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
                             (11),(12),(13),(14),(15),(16),(17),(18),(19),(20);

INSERT INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT t.id,
       ty.seat_type,
       c.n,
       CONCAT(LPAD(r.n, 2, '0'), sl.letter),
       0,
       0
FROM t_train t
CROSS JOIN (SELECT 1 AS seat_type UNION ALL SELECT 2 UNION ALL SELECT 3) ty
CROSS JOIN (SELECT n FROM t_seq WHERE n <= 8) c
CROSS JOIN (SELECT n FROM t_seq WHERE n <= 20) r
CROSS JOIN (SELECT 'A' letter UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D' UNION ALL SELECT 'F') sl
WHERE (ty.seat_type = 1 AND c.n = 1)
   OR (ty.seat_type = 2 AND c.n IN (2, 3))
   OR (ty.seat_type = 3 AND c.n BETWEEN 4 AND 8);

-- ---------- 钱包：为已有用户开户并赠送 1000 元初始余额（幂等） ----------
INSERT INTO t_wallet (user_id, balance, total_recharge, total_consume, version)
SELECT u.id, 1000.00, 1000.00, 0, 0
FROM t_user u
ON DUPLICATE KEY UPDATE balance = balance;

INSERT IGNORE INTO t_wallet_flow (flow_no, user_id, biz_no, type, title, detail, amount, balance, remark)
SELECT CONCAT('INIT', u.id), u.id, CONCAT('INIT', u.id), 1,
       '开户赠送', '系统赠送初始余额 1000.00 元', 1000.00, 1000.00, '初始化数据'
FROM t_user u;

-- ---------- 预热 Redis 库存的兜底：确保库存与真实可售座位数一致 ----------
UPDATE t_train_stock st
    JOIN (SELECT train_id, seat_type, COUNT(*) AS cnt
          FROM t_seat WHERE status = 0 GROUP BY train_id, seat_type) s
    ON s.train_id = st.train_id AND s.seat_type = st.seat_type
SET st.total_count = s.cnt, st.available_count = s.cnt;
