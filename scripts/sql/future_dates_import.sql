-- =============================================================
--  未来日期班次复制（模板 → 后续 13 天，含列车/库存/座位/车厢）
--  生成时间：2026-09-27T14:27:47.464Z
-- =============================================================
USE qiangpiao;

-- ---------- D+1 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 1 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 1 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 1 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 1 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 1 DAY;

-- ---------- D+2 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 2 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 2 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 2 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 2 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 2 DAY;

-- ---------- D+3 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 3 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 3 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 3 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 3 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 3 DAY;

-- ---------- D+4 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 4 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 4 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 4 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 4 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 4 DAY;

-- ---------- D+5 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 5 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 5 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 5 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 5 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 5 DAY;

-- ---------- D+6 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 6 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 6 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 6 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 6 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 6 DAY;

-- ---------- D+7 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 7 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 7 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 7 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 7 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 7 DAY;

-- ---------- D+8 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 8 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 8 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 8 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 8 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 8 DAY;

-- ---------- D+9 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 9 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 9 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 9 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 9 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 9 DAY;

-- ---------- D+10 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 10 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 10 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 10 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 10 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 10 DAY;

-- ---------- D+11 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 11 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 11 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 11 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 11 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 11 DAY;

-- ---------- D+12 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 12 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 12 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 12 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 12 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 12 DAY;

-- ---------- D+13 ----------
INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,
                           to_station_id, to_station_name, depart_date, depart_time,
                           arrive_time, duration_minutes, status)
SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,
       t.to_station_id, t.to_station_name, CURDATE() + INTERVAL 13 DAY, t.depart_time, t.arrive_time,
       t.duration_minutes, 1
FROM t_train t
WHERE t.depart_date = CURDATE() AND t.status = 1
  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = CURDATE() + INTERVAL 13 DAY);

INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)
SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_train_stock s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 13 DAY;

INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)
SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_seat s ON s.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 13 DAY;

INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)
SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count
FROM t_train nt
         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()
         JOIN t_carriage c ON c.train_id = tp.id
WHERE nt.depart_date = CURDATE() + INTERVAL 13 DAY;
