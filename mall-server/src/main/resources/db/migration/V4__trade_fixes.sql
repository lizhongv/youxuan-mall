-- 交易 / 营销 / 商品领域的正确性修复与运费模板。
-- 1. 索引：按定时任务、售后、统计的实际查询条件补齐；
-- 2. trd_order.seckill_id：取代「把秒杀活动 ID 写进下单日志 remark」的变通做法，并用于以订单为准统计个人限购；
-- 3. trd_order.reward_*：记录确认收货时实际发放的积分 / 成长值及其计算基数，售后退款时按比例回收；
-- 4. 运费模板三张表 + 商品 / SKU 的模板与重量字段；
-- 5. 后台菜单「运费模板」与角色授权。

-- ======================== 索引 ========================

-- 过期券任务：WHERE status = 'UNUSED' AND end_at <= now
ALTER TABLE mkt_member_coupon ADD KEY idx_status_end (status, end_at);
-- 按订单汇总已退款（确认收货发奖励、统计扣退款、整单退完判断）
ALTER TABLE trd_aftersale ADD KEY idx_order (order_id);
-- 统计按支付时间区间查询
ALTER TABLE trd_order ADD KEY idx_pay_time (pay_time);
-- 统计按注册日期分组
ALTER TABLE mbr_member ADD KEY idx_created (created_at);

-- ======================== 订单新列 ========================

ALTER TABLE trd_order
    ADD COLUMN seckill_id    BIGINT         NULL COMMENT '秒杀活动 ID，普通订单为空' AFTER order_type,
    ADD COLUMN reward_points INT            NULL COMMENT '确认收货时发放的积分，退款按比例回收' AFTER member_deleted,
    ADD COLUMN reward_growth INT            NULL COMMENT '确认收货时发放的成长值' AFTER reward_points,
    ADD COLUMN reward_base   DECIMAL(10, 2) NULL COMMENT '发放奖励时的计算基数（实付 − 已退款）' AFTER reward_growth,
    ADD KEY idx_seckill_member (seckill_id, member_id);

-- 历史秒杀订单：从下单日志 remark 中的 seckillId=<id> 回填，保证升级后取消 / 限购统计仍然正确
UPDATE trd_order o
    JOIN trd_order_log l ON l.order_id = o.id AND l.from_status = '' AND l.remark LIKE '%seckillId=%'
SET o.seckill_id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(l.remark, 'seckillId=', -1), ' ', 1) AS UNSIGNED)
WHERE o.order_type = 'SECKILL' AND o.seckill_id IS NULL;

-- ======================== 运费模板 ========================

CREATE TABLE trd_freight_template (
    id          BIGINT      NOT NULL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    charge_type VARCHAR(16) NOT NULL COMMENT 'BY_COUNT 按件 / BY_WEIGHT 按重量（kg）',
    is_default  TINYINT     NOT NULL DEFAULT 0 COMMENT '默认模板：商品未指定模板时使用，全局最多一个',
    status      TINYINT     NOT NULL DEFAULT 1,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_default (is_default, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '运费模板';

CREATE TABLE trd_freight_rule (
    id          BIGINT         NOT NULL PRIMARY KEY,
    template_id BIGINT         NOT NULL,
    provinces   JSON           NULL COMMENT '适用省份名数组；空数组表示默认（全国其他地区）',
    first_unit  DECIMAL(10, 3) NOT NULL COMMENT '首件（件）/ 首重（kg）',
    first_fee   DECIMAL(10, 2) NOT NULL COMMENT '首费',
    next_unit   DECIMAL(10, 3) NOT NULL DEFAULT 0 COMMENT '续件 / 续重，0 表示不再续费',
    next_fee    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '续费',
    sort        INT            NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_template (template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '运费模板计费规则';

CREATE TABLE trd_freight_free (
    id          BIGINT         NOT NULL PRIMARY KEY,
    template_id BIGINT         NOT NULL,
    provinces   JSON           NULL COMMENT '适用省份名数组；空数组表示全部地区',
    min_count   INT            NOT NULL DEFAULT 0 COMMENT '满 N 件包邮，0 表示不按件',
    min_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '满 X 元包邮（会员折扣后），0 表示不按金额',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_template (template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '运费模板指定条件包邮';

ALTER TABLE gds_goods
    ADD COLUMN freight_template_id BIGINT NULL COMMENT '运费模板，空表示使用默认模板' AFTER brand_id,
    ADD KEY idx_freight_template (freight_template_id);

ALTER TABLE gds_sku
    ADD COLUMN weight DECIMAL(10, 3) NOT NULL DEFAULT 0.000 COMMENT '单件重量（kg），按重量计费时使用' AFTER stock;

-- ======================== 菜单与授权 ========================
-- ID 规则沿用 V2：订单目录 3000，菜单 30y0，按钮紧跟；角色授权 ID = 角色 ID * 100000 + 菜单 ID。

INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, permission, sort, visible, status) VALUES
(3030, 3000, 'MENU',   '运费模板', '/trade/freight', 'trade/freight/index', 'Van', 'trade:freight:view', 5, 1, 1),
(3031, 3030, 'BUTTON', '模板编辑', '',               '',                    '',    'trade:freight:edit', 1, 1, 1);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(103030, 1, 3030), (103031, 1, 3031),
(203030, 2, 3030), (203031, 2, 3031);
