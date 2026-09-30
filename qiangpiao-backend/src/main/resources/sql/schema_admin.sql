-- =============================================================
--  管理后台 / 退改签 / 日志追踪 相关表（MySQL 8.x）
-- =============================================================
USE qiangpiao;

-- 线路表（A 站 -> B 站，途经站点单独一张表维护顺序）
CREATE TABLE IF NOT EXISTS t_line (
    id                BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    line_name         VARCHAR(64) NOT NULL COMMENT '线路名称',
    from_station_id   BIGINT      NOT NULL COMMENT '起点站ID',
    from_station_name VARCHAR(64) NOT NULL COMMENT '起点站名称',
    to_station_id     BIGINT      NOT NULL COMMENT '终点站ID',
    to_station_name   VARCHAR(64) NOT NULL COMMENT '终点站名称',
    status            TINYINT     NOT NULL DEFAULT 1 COMMENT '1-启用 0-停用',
    create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='线路表';

-- 线路途经站（含顺序）
CREATE TABLE IF NOT EXISTS t_line_station (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    line_id      BIGINT      NOT NULL COMMENT '线路ID',
    station_id   BIGINT      NOT NULL COMMENT '车站ID',
    station_name VARCHAR(64) NOT NULL COMMENT '车站名称',
    stop_order   INT         NOT NULL DEFAULT 0 COMMENT '途经顺序，从 1 开始',
    UNIQUE KEY uk_line_station (line_id, station_id),
    KEY idx_line (line_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='线路途经站';

-- 车次时刻表（车次模板：每个途经站的到站 / 发车时刻）
CREATE TABLE IF NOT EXISTS t_train_stop (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    train_id     BIGINT      NOT NULL COMMENT '车次ID',
    station_id   BIGINT      NOT NULL COMMENT '车站ID',
    station_name VARCHAR(64) NOT NULL COMMENT '车站名称',
    stop_order   INT         NOT NULL DEFAULT 0 COMMENT '停靠顺序，从 1 开始',
    arrive_time  TIME        DEFAULT NULL COMMENT '到站时刻',
    depart_time  TIME        DEFAULT NULL COMMENT '发车时刻',
    stop_minutes INT         NOT NULL DEFAULT 0 COMMENT '停靠分钟数',
    distance_km  INT         DEFAULT NULL COMMENT '距始发站里程（可选）',
    UNIQUE KEY uk_train_station (train_id, station_id),
    KEY idx_train (train_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='车次时刻表';

-- 车厢表（车厢编号 / 类型 / 座位总数）
CREATE TABLE IF NOT EXISTS t_carriage (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    train_id    BIGINT      NOT NULL COMMENT '车次ID',
    carriage_no INT         NOT NULL COMMENT '车厢号',
    seat_type   TINYINT     NOT NULL COMMENT '席别：1-商务座 2-一等座 3-二等座 4-软卧 5-硬卧',
    seat_count  INT         NOT NULL DEFAULT 0 COMMENT '座位总数',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_train_carriage (train_id, carriage_no),
    KEY idx_train (train_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='车次车厢表';

-- 公告表（停运通知 / 节假日售票通知）
CREATE TABLE IF NOT EXISTS t_announcement (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title       VARCHAR(128) NOT NULL COMMENT '标题',
    content     VARCHAR(2000) NOT NULL COMMENT '正文',
    type        TINYINT      NOT NULL DEFAULT 3 COMMENT '1-停运通知 2-节假日通知 3-其他',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1-已发布 0-已下架',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status_time (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='公告表';

-- 订单流转日志（物流式时间轴 / 退改签历史 / MDC 串联）
CREATE TABLE IF NOT EXISTS t_order_log (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no    VARCHAR(64) NOT NULL COMMENT '订单号',
    action      VARCHAR(32) NOT NULL COMMENT '动作：CREATE/PAY/CANCEL/REFUND/CHANGE/EXPIRE',
    action_text VARCHAR(64) NOT NULL COMMENT '动作文案',
    detail      VARCHAR(255) DEFAULT NULL COMMENT '详情',
    operator    VARCHAR(64) DEFAULT NULL COMMENT '操作人：用户ID 或 admin',
    trace_id    VARCHAR(32) DEFAULT NULL COMMENT '日志追踪ID（MDC）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    KEY idx_order (order_no),
    KEY idx_trace (trace_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单流转日志';

-- 改签记录（后台可查改签历史）
CREATE TABLE IF NOT EXISTS t_order_change (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no      VARCHAR(64)   NOT NULL COMMENT '原订单号',
    new_order_no  VARCHAR(64)   NOT NULL COMMENT '新订单号',
    user_id       BIGINT        NOT NULL COMMENT '用户ID',
    old_train_id  BIGINT        DEFAULT NULL COMMENT '原车次ID',
    new_train_id  BIGINT        DEFAULT NULL COMMENT '新车次ID',
    old_seat_no   VARCHAR(16)   DEFAULT NULL COMMENT '原座位',
    new_seat_no   VARCHAR(16)   DEFAULT NULL COMMENT '新座位',
    diff_amount   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '差额（补款为正，退款为负）',
    change_fee    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '改签手续费（退还差额按阶梯退票费计收）',
    fee_rule      VARCHAR(64)   DEFAULT NULL COMMENT '本次改签计费档位说明',
    reason        VARCHAR(255)  DEFAULT NULL COMMENT '改签原因',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order (order_no),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='改签记录表';
