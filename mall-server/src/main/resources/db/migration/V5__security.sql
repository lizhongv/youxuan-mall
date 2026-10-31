-- 安全加固：生产安全默认值、强制修改初始密码、令牌版本号
-- 注意：V4 预留给并行开发的其他迁移，版本号不连续是有意为之

-- 1. 注册赠送余额默认 0：演示数据的 200 元在真实营业时等于白送钱（可被批量注册薅取）。
--    只改仍为种子值的记录，运营已经调整过的值保留；演示环境可在后台「系统参数」改回
UPDATE sys_config
SET config_value = '0.00',
    remark       = '新会员注册即赠送；默认 0 关闭，演示环境可在后台改为大于 0 的金额'
WHERE config_key = 'member.register_gift_balance'
  AND config_value = '200.00';

-- 2. 管理员：强制改密标记、密码修改时间、令牌版本号
ALTER TABLE sys_admin
    ADD COLUMN must_change_password TINYINT  NOT NULL DEFAULT 0 COMMENT '1 下次登录必须先修改密码' AFTER remark,
    ADD COLUMN pwd_changed_at       DATETIME NULL COMMENT '最近一次修改密码时间' AFTER must_change_password,
    ADD COLUMN token_version        INT      NOT NULL DEFAULT 0 COMMENT '令牌版本号，改密/停用/调整角色时自增' AFTER pwd_changed_at;

-- 已存在的内置 admin 无法判断是否仍在用初始口令（库里只有哈希），统一要求改一次
UPDATE sys_admin SET must_change_password = 1 WHERE username = 'admin';

-- 3. 会员：令牌版本号、密码修改时间
ALTER TABLE mbr_member
    ADD COLUMN pwd_changed_at DATETIME NULL COMMENT '最近一次修改密码时间' AFTER last_login_at,
    ADD COLUMN token_version  INT      NOT NULL DEFAULT 0 COMMENT '令牌版本号，改密/重置密码/冻结时自增' AFTER pwd_changed_at;
