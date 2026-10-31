-- Phase 2.5: order 领域基础表
-- 简化设计：单表订单 + 订单项；真实电商会拆 SPU 快照、收货地址、营销快照、子单等

CREATE TABLE IF NOT EXISTS `orders`
(
    `id`           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '订单 ID',
    `order_no`     VARCHAR(64)     NOT NULL                COMMENT '业务单号（雪花 ID 字符串）',
    `user_id`      BIGINT          NOT NULL                COMMENT '下单用户',
    `total_amount` DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    `status`       TINYINT         NOT NULL DEFAULT 0      COMMENT '0待支付 1已支付 2已发货 3已完成 4已取消',
    `remark`       VARCHAR(255)             DEFAULT NULL,
    `created_by`   BIGINT                   DEFAULT NULL,
    `updated_by`   BIGINT                   DEFAULT NULL,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT         NOT NULL DEFAULT 0,
    `version`      INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`, `deleted`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';


CREATE TABLE IF NOT EXISTS `order_item`
(
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `order_id`     BIGINT          NOT NULL,
    `goods_id`     BIGINT          NOT NULL,
    `sku_id`       BIGINT          NOT NULL,
    `goods_name`   VARCHAR(120)    NOT NULL                COMMENT '快照',
    `sku_specs`    VARCHAR(512)             DEFAULT NULL   COMMENT 'SKU 规格快照',
    `unit_price`   DECIMAL(10, 2)  NOT NULL,
    `quantity`     INT             NOT NULL,
    `subtotal`     DECIMAL(10, 2)  NOT NULL                COMMENT 'unit_price × quantity',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';
