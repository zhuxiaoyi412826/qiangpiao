-- ============================================================
-- 区间票（同一座位分段售卖）+ 车次售卖时间窗口
-- 全部使用 IF NOT EXISTS / 幂等写法，可重复执行
-- ============================================================

-- 1. 车次售卖时间窗口（秒杀 / 下单前校验：未开始 / 已结束）
ALTER TABLE t_train
    ADD COLUMN sale_start_time DATETIME DEFAULT NULL COMMENT '售票开始时间；NULL 表示不限制';
ALTER TABLE t_train
    ADD COLUMN sale_end_time DATETIME DEFAULT NULL COMMENT '售票结束时间；NULL 表示不限制';

-- 2. 座位区间占用表：一个座位可被拆成多段卖给不同乘客（A-B 与 B-C 可共存）
--    区间语义为 [from_order, to_order)，冲突判定：已有区间 [a,b) 与新区间 [fo,to) 冲突 <=> a < to AND fo < b
CREATE TABLE IF NOT EXISTS t_seat_segment
(
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    train_id    BIGINT      NOT NULL COMMENT '车次ID',
    seat_id     BIGINT      NOT NULL COMMENT '座位ID',
    seat_type   TINYINT     NOT NULL COMMENT '席别：1-商务座 2-一等座 3-二等座',
    from_order  INT         NOT NULL COMMENT '上车站在经停序列中的序号（从 1 开始）',
    to_order    INT         NOT NULL COMMENT '下车站序号（不含，区间为 [from_order, to_order)）',
    order_no    VARCHAR(64) NOT NULL COMMENT '占用订单号',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1-占用中 0-已释放',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_seat (seat_id, status),
    KEY idx_train_range (train_id, from_order, to_order, status),
    KEY idx_order (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='座位区间占用表（区间票核心）';

-- 3. 区间库存表：按「相邻两站之间的单段」计数
--    seg_index = i 表示「第 i 站 → 第 i+1 站」这一段；
--    任意 OD 区间的余票 = 该区间覆盖的所有单段余票的最小值
CREATE TABLE IF NOT EXISTS t_train_segment_stock
(
    id              BIGINT  PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    train_id        BIGINT  NOT NULL COMMENT '车次ID',
    seat_type       TINYINT NOT NULL COMMENT '席别',
    seg_index       INT     NOT NULL COMMENT '段序号 i：第 i 站 → 第 i+1 站',
    total_count     INT     NOT NULL DEFAULT 0 COMMENT '该段总座位数',
    available_count INT     NOT NULL DEFAULT 0 COMMENT '该段剩余可售数',
    version         INT     NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_train_seat_seg (train_id, seat_type, seg_index),
    KEY idx_train (train_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='区间（相邻站单段）库存表';

-- 4. 订单记录实际乘车区间（原来只有车次的始发 / 终到快照）
ALTER TABLE t_order
    ADD COLUMN from_stop_order INT DEFAULT NULL COMMENT '上车站序号（区间票）';
ALTER TABLE t_order
    ADD COLUMN to_stop_order INT DEFAULT NULL COMMENT '下车站序号（区间票）';

-- 5. 数据补齐：为还没有时刻表的车次补上首尾两站（区间票的站位基础）
--    没有这两行，车次会被当成「全程票」处理（功能仍可用，只是不能分段卖）
INSERT INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes,
                          distance_km)
SELECT t.id, t.from_station_id, t.from_station_name, 1, t.depart_time, t.depart_time, 0, 0
FROM t_train t
WHERE NOT EXISTS (SELECT 1 FROM t_train_stop s WHERE s.train_id = t.id);

INSERT INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes,
                          distance_km)
SELECT t.id, t.to_station_id, t.to_station_name, 2, t.arrive_time, t.arrive_time, 0, 0
FROM t_train t
WHERE (SELECT COUNT(1) FROM t_train_stop s WHERE s.train_id = t.id) = 1;

-- 回滚（如需撤销本脚本）：
-- ALTER TABLE t_order DROP COLUMN from_stop_order, DROP COLUMN to_stop_order;
-- ALTER TABLE t_train DROP COLUMN sale_start_time, DROP COLUMN sale_end_time;
-- DROP TABLE IF EXISTS t_train_segment_stock;
-- DROP TABLE IF EXISTS t_seat_segment;
-- （第 5 步补的时刻表不回滚，它们本身也是正确的业务数据）
