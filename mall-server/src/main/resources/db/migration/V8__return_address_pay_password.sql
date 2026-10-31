-- 功能补缺：售后退货地址、余额支付密码。
-- 1. 退货地址三项配置：退货退款审核通过后展示给买家寄回商品；默认空串 = 未配置，后台同意退货前必须先填写。
--    ID 段 28001 起，避免与 V2（基础参数）、V6（支付）、V7（27001 起基础设施）冲突。
--    sensitive 是 MySQL 8 保留字，必须加反引号；地址、电话需要展示给买家，不属于敏感值。
-- 2. 会员支付密码：BCrypt 密文，空串表示未设置。连续错误计数放 Redis（mall:pay-pwd-fail:{memberId}，30 分钟过期），
--    不落库：计数需要原子自增与自动过期，放在行上既要额外的解锁时间列，又会和余额的条件更新争同一行锁。

INSERT INTO sys_config (id, config_key, config_value, name, remark, `sensitive`) VALUES
(28001, 'trade.return_receiver_name',   '', '退货收件人',   '退货退款审核通过后展示给买家；三项都为空时后台不能同意退货申请', 0),
(28002, 'trade.return_receiver_mobile', '', '退货联系电话', '手机号或座机', 0),
(28003, 'trade.return_address',         '', '退货地址',     '省市区 + 详细地址，如：广东省深圳市南山区科技园 1 号 商城售后部', 0);

ALTER TABLE mbr_member
    ADD COLUMN pay_password VARCHAR(100) NOT NULL DEFAULT '' COMMENT '余额支付密码 BCrypt 密文，空串表示未设置' AFTER password;
