-- =============================================================
--  敏感字段加密存储 → 列长度扩容（存量库迁移脚本）
-- -------------------------------------------------------------
--  现象：注册 / 下单 500，日志报
--        MysqlDataTruncation: Data truncation: Data too long for column 'phone'
--
--  原因：phone 等列最初按「明文」设计（手机号 VARCHAR(11)、身份证 VARCHAR(18)），
--        改为 AES 密文存储后，实际写入的是 "ENC:" 前缀 + Base64：
--          手机号明文 11 字节  → PKCS5 填充到 16 → Base64 24 字符 → 总长 28
--          身份证明文 18 字节  → PKCS5 填充到 32 → Base64 44 字符 → 总长 48
--        远超原列长，插入即被 MySQL 截断报错。
--
--  说明：
--    1. 只放大列长度，不改写任何数据，存量数据零风险，可重复执行；
--    2. 存量「明文」数据无需迁移：SensitiveCrypto.decrypt 对无 "ENC:" 前缀的
--       值原样返回，读取端天然兼容，新写入才落密文；
--    3. 放大列长是安全操作（VARCHAR 11 → 64 不锁表重建、不丢数据），
--       若列上有唯一索引（如 t_passenger 的 uk_user_idcard），
--       64 * 4 = 256 字节 < InnoDB 3072 字节索引键上限，不受影响。
-- =============================================================
USE qiangpiao;

-- ---------- 0. 迁移前：确认当前列长（phone 应 < 28、id_card 应 < 48 才需要本脚本） ----------
-- SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_MAXIMUM_LENGTH
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = 'qiangpiao' AND COLUMN_NAME IN ('phone', 'id_card');

-- ---------- 1. 用户表 ----------
ALTER TABLE t_user MODIFY COLUMN phone   VARCHAR(64) DEFAULT NULL COMMENT '手机号（AES 密文，明文 11 位 → 密文 28 字符）';
ALTER TABLE t_user MODIFY COLUMN id_card VARCHAR(64) DEFAULT NULL COMMENT '身份证号（AES 密文，明文 18 位 → 密文 48 字符）';

-- ---------- 2. 订单表（乘客证件号快照） ----------
ALTER TABLE t_order MODIFY COLUMN id_card VARCHAR(64) DEFAULT NULL COMMENT '身份证号（AES 密文，明文 18 位 → 密文 48 字符）';

-- ---------- 3. 常用乘车人 ----------
ALTER TABLE t_passenger MODIFY COLUMN phone   VARCHAR(64) DEFAULT NULL COMMENT '手机号（AES 密文）';
ALTER TABLE t_passenger MODIFY COLUMN id_card VARCHAR(64) DEFAULT NULL COMMENT '身份证号（AES 密文）';

-- ---------- 4. 迁移后校验：phone >= 28、id_card >= 48 即正确 ----------
SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_MAXIMUM_LENGTH
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'qiangpiao'
  AND COLUMN_NAME IN ('phone', 'id_card')
ORDER BY TABLE_NAME, COLUMN_NAME;
