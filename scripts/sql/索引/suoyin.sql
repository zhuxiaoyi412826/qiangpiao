USE qiangpiao;
-- 建立索引
-- ① P0：票务监控「锁定座位」1.5s → 毫秒（过滤 + 排序 + LIMIT 全走索引）
ALTER TABLE t_seat ADD KEY idx_status_update (status, update_time);

-- ② 车次总数：全表扫 2970 行 → 覆盖索引
ALTER TABLE t_train ADD KEY idx_status (status);

-- ③ 经停站消除 filesort
ALTER TABLE t_train_stop ADD KEY idx_train_order (train_id, stop_order);

-- ④ 删冗余索引（都是唯一索引的前缀，白占写成本、还会误导优化器）
ALTER TABLE t_train_stop            DROP INDEX idx_train;  -- uk_train_station(train_id,...) 已覆盖
ALTER TABLE t_train_segment_stock   DROP INDEX idx_train;  -- uk_train_seat_seg(train_id,...) 已覆盖

-- ⑤ 刷新统计信息，让优化器立刻用上新索引
ANALYZE TABLE t_seat;
ANALYZE TABLE t_train;
ANALYZE TABLE t_train_stop;
ANALYZE TABLE t_train_segment_stock;
