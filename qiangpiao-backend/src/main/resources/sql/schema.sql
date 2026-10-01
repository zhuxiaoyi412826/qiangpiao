-- =============================================================
--  火车票秒杀抢票系统 - 数据库表结构（MySQL 8.x）
-- =============================================================
CREATE DATABASE IF NOT EXISTS qiangpiao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE qiangpiao;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名/登录账号',
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密密码',
    real_name   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    -- 敏感字段存 AES 密文（"ENC:" 前缀 + Base64）：
    --   手机号明文 11 字节 → 密文 28 字符；身份证明文 18 字节 → 密文 48 字符，故统一 64
    phone       VARCHAR(64)  DEFAULT NULL COMMENT '手机号（AES 密文）',
    id_card     VARCHAR(64)  DEFAULT NULL COMMENT '身份证号（AES 密文）',
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 车站表
CREATE TABLE IF NOT EXISTS t_station (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    station_name VARCHAR(64) NOT NULL COMMENT '车站名称',
    city         VARCHAR(64) NOT NULL COMMENT '所属城市',
    py_code      VARCHAR(64) DEFAULT NULL COMMENT '拼音简码',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_station (station_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='车站表';

-- 车次表
CREATE TABLE IF NOT EXISTS t_train (
    id                BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    train_no          VARCHAR(32) NOT NULL COMMENT '车次号，如 G1001',
    train_type        VARCHAR(20) NOT NULL DEFAULT '高铁' COMMENT '车型：高铁/动车/直达',
    from_station_id   BIGINT      NOT NULL COMMENT '始发站ID',
    from_station_name VARCHAR(64) NOT NULL COMMENT '始发站名称（冗余）',
    to_station_id     BIGINT      NOT NULL COMMENT '终点站ID',
    to_station_name   VARCHAR(64) NOT NULL COMMENT '终点站名称（冗余）',
    depart_date       DATE        NOT NULL COMMENT '发车日期',
    depart_time       TIME        NOT NULL COMMENT '发车时间',
    arrive_time       TIME        NOT NULL COMMENT '到达时间',
    duration_minutes  INT         NOT NULL DEFAULT 0 COMMENT '历时（分钟）',
    status            TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1-可售 0-停运',
    create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_train_date (train_no, depart_date),
    KEY idx_route_date (from_station_name, to_station_name, depart_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='车次表';

-- 车次席别库存表（秒杀库存，乐观锁 version 防超卖）
CREATE TABLE IF NOT EXISTS t_train_stock (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    train_id        BIGINT       NOT NULL COMMENT '车次ID',
    seat_type       TINYINT      NOT NULL COMMENT '席别：1-商务座 2-一等座 3-二等座',
    total_count     INT          NOT NULL DEFAULT 0 COMMENT '总座位数',
    available_count INT          NOT NULL DEFAULT 0 COMMENT '剩余可售数',
    price           DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '票价',
    version         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_train_seat_type (train_id, seat_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='车次席别库存表';

-- 座位表
CREATE TABLE IF NOT EXISTS t_seat (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    train_id    BIGINT      NOT NULL COMMENT '车次ID',
    seat_type   TINYINT     NOT NULL COMMENT '席别：1-商务座 2-一等座 3-二等座',
    carriage_no INT         NOT NULL COMMENT '车厢号',
    seat_no     VARCHAR(16) NOT NULL COMMENT '座位号，如 05A',
    status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0-可售 1-已售 2-锁定',
    order_no    VARCHAR(64) DEFAULT NULL COMMENT '占用订单号',
    version     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_seat (train_id, carriage_no, seat_no),
    KEY idx_train_type_status (train_id, seat_type, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='座位表';

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id             BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no       VARCHAR(64)   NOT NULL COMMENT '订单号',
    user_id        BIGINT        NOT NULL COMMENT '用户ID',
    train_id       BIGINT        NOT NULL COMMENT '车次ID',
    -- 车次快照：车次日期会按天滚动、也可能被重建清理，历史订单必须自带车次信息
    train_no_snapshot     VARCHAR(32)   DEFAULT NULL COMMENT '车次号快照',
    train_type_snapshot   VARCHAR(16)   DEFAULT NULL COMMENT '车次类型快照',
    from_station_snapshot VARCHAR(64)   DEFAULT NULL COMMENT '出发站快照',
    to_station_snapshot   VARCHAR(64)   DEFAULT NULL COMMENT '到达站快照',
    depart_time_snapshot  TIME          DEFAULT NULL COMMENT '发车时刻快照',
    arrive_time_snapshot  TIME          DEFAULT NULL COMMENT '到达时刻快照',
    seat_id        BIGINT        DEFAULT NULL COMMENT '座位ID',
    seat_type      TINYINT       NOT NULL COMMENT '席别',
    carriage_no    INT           DEFAULT NULL COMMENT '车厢号',
    seat_no        VARCHAR(16)   DEFAULT NULL COMMENT '座位号',
    passenger_name VARCHAR(64)   NOT NULL COMMENT '乘客姓名',
    id_card        VARCHAR(64)   DEFAULT NULL COMMENT '身份证号（AES 密文，明文 18 位 → 密文 48 字符）',
    price          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '票价',
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消 3-已退票 4-已超时',
    depart_date    DATE          DEFAULT NULL COMMENT '乘车日期（购票时快照，车次日期滚动时订单日期保持不变）',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    pay_time       DATETIME      DEFAULT NULL COMMENT '支付时间',
    cancel_time    DATETIME      DEFAULT NULL COMMENT '取消时间',
    expire_time    DATETIME      DEFAULT NULL COMMENT '支付超时时间',
    -- 退票手续费：按距开车时间阶梯计费（8天以上免费 / 5% / 10% / 20%，尾数 5 角取整、最低 2 元）
    refund_fee     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '退票手续费',
    refund_amount  DECIMAL(10,2) DEFAULT NULL COMMENT '实退金额 = 票价 - 手续费',
    -- 最初购票车次的开车时间：改签后不更新；退票费率按此时间取档（原票不足 8 天改签后仍收 5%）
    origin_depart_time DATETIME  DEFAULT NULL COMMENT '最初购票车次开车时间（改签不更新）',
    changed        TINYINT       NOT NULL DEFAULT 0 COMMENT '是否改签过：0-否 1-是',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status (user_id, status),
    KEY idx_train (train_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

-- 支付流水表（两阶段支付：发起支付建单 -> 渠道异步回调后才扣款入账）
CREATE TABLE IF NOT EXISTS t_payment (
    id             BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    pay_no         VARCHAR(64)   NOT NULL COMMENT '支付流水号',
    order_no       VARCHAR(64)   NOT NULL COMMENT '订单号',
    user_id        BIGINT        NOT NULL COMMENT '用户ID',
    pay_type       VARCHAR(16)   NOT NULL DEFAULT 'ALIPAY' COMMENT '支付方式：ALIPAY / WECHAT / BALANCE',
    amount         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '支付金额',
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0-支付中 1-支付成功 2-支付失败 3-已关闭',
    idempotent_key VARCHAR(64)   NOT NULL COMMENT '发起支付幂等键：同一键只生成一笔有效支付单',
    trade_no       VARCHAR(64)   DEFAULT NULL COMMENT '渠道交易号（回调带回）',
    notify_count   INT           NOT NULL DEFAULT 0 COMMENT '回调次数（含重复回调）',
    pay_time       DATETIME      DEFAULT NULL COMMENT '支付成功时间',
    notify_time    DATETIME      DEFAULT NULL COMMENT '最近一次回调时间',
    expire_time    DATETIME      DEFAULT NULL COMMENT '支付单超时时间',
    fail_reason    VARCHAR(255)  DEFAULT NULL COMMENT '失败 / 关闭原因',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pay_no (pay_no),
    UNIQUE KEY uk_idempotent_key (idempotent_key),
    KEY idx_order_no (order_no),
    KEY idx_user_time (user_id, create_time),
    KEY idx_status_expire (status, expire_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付流水表';

-- 钱包表（余额 + 累计充值/消费，乐观锁防并发扣款）
CREATE TABLE IF NOT EXISTS t_wallet (
    id             BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT         NOT NULL COMMENT '用户ID',
    balance        DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '余额',
    total_recharge DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '累计充值金额',
    total_consume  DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
    version        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户钱包表';

-- 零钱流水表（每笔余额变动留痕：买了什么票、花了多少、余额多少）
CREATE TABLE IF NOT EXISTS t_wallet_flow (
    id         BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    flow_no    VARCHAR(64)    NOT NULL COMMENT '流水号',
    user_id    BIGINT         NOT NULL COMMENT '用户ID',
    biz_no     VARCHAR(64)    DEFAULT NULL COMMENT '业务单号：支付时为订单号，充值时为充值流水号',
    type       TINYINT        NOT NULL COMMENT '类型：1-充值 2-消费 3-退款',
    title      VARCHAR(128)   NOT NULL COMMENT '摘要，如 购买 G1001 二等座',
    detail     VARCHAR(255)   DEFAULT NULL COMMENT '明细，如 北京南->上海虹桥 05车12A',
    amount     DECIMAL(10,2)  NOT NULL COMMENT '变动金额：消费为负，充值/退款为正',
    balance    DECIMAL(12,2)  NOT NULL COMMENT '变动后余额',
    remark     VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    -- 幂等键：退款类操作防重复入账（如 REFUND:订单号）
    idempotent_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键（退款防重）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    UNIQUE KEY uk_flow_no (flow_no),
    UNIQUE KEY uk_idempotent_key (idempotent_key),
    KEY idx_user_time (user_id, create_time),
    KEY idx_biz_no (biz_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='零钱流水表';

-- 幂等/防重复购买记录
CREATE TABLE IF NOT EXISTS t_seckill_record (
    id          BIGINT    PRIMARY KEY AUTO_INCREMENT,
    train_id    BIGINT    NOT NULL,
    seat_type   TINYINT   NOT NULL,
    user_id     BIGINT    NOT NULL,
    order_no    VARCHAR(64) DEFAULT NULL,
    create_time DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_train_type (train_id, seat_type, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀成功记录（一人一单）';
