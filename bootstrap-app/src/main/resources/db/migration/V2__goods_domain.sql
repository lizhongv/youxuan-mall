-- 商品域基础表（Phase 1.3）
-- 比老项目精简：去掉 wap_banner_url / extra_price / app_exclusive 等无用字段
-- ID 全部 BIGINT 为后续分库分表预留 sharding key 空间
-- 全部表统一审计列：created_at/updated_at/created_by/updated_by + deleted（逻辑删除）+ version（乐观锁）

CREATE TABLE IF NOT EXISTS `goods_category`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父分类，0 为根',
    `name`        VARCHAR(64)  NOT NULL                COMMENT '分类名',
    `slug`        VARCHAR(64)            DEFAULT NULL  COMMENT 'URL 友好别名',
    `icon_url`    VARCHAR(255)           DEFAULT NULL  COMMENT '图标 URL',
    `banner_url`  VARCHAR(255)           DEFAULT NULL  COMMENT '横幅 URL',
    `sort_order`  INT          NOT NULL DEFAULT 100    COMMENT '排序，升序',
    `visible`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否可见 0否 1是',
    `created_by`  BIGINT                 DEFAULT NULL  COMMENT '创建人',
    `updated_by`  BIGINT                 DEFAULT NULL  COMMENT '修改人',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
    `version`     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_visible` (`visible`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类';


CREATE TABLE IF NOT EXISTS `goods_brand`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL                COMMENT '品牌名',
    `logo_url`    VARCHAR(255)           DEFAULT NULL  COMMENT 'Logo URL',
    `description` VARCHAR(255)           DEFAULT NULL  COMMENT '简介',
    `sort_order`  INT          NOT NULL DEFAULT 100    COMMENT '排序',
    `visible`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否可见',
    `created_by`  BIGINT                 DEFAULT NULL,
    `updated_by`  BIGINT                 DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    `version`     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`, `deleted`),
    KEY `idx_visible` (`visible`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品品牌';


CREATE TABLE IF NOT EXISTS `goods`
(
    `id`           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键 (SPU)',
    `goods_no`     VARCHAR(64)     NOT NULL                COMMENT '商品编号',
    `name`         VARCHAR(120)    NOT NULL                COMMENT '商品名',
    `category_id`  BIGINT          NOT NULL                COMMENT '分类',
    `brand_id`     BIGINT                   DEFAULT NULL   COMMENT '品牌',
    `cover_image`  VARCHAR(255)             DEFAULT NULL   COMMENT '封面图',
    `subtitle`     VARCHAR(255)             DEFAULT NULL   COMMENT '副标题/卖点',
    `description`  TEXT                                    COMMENT '富文本详情',
    `min_price`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00   COMMENT '最低价（取所有 SKU）',
    `max_price`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00   COMMENT '最高价（取所有 SKU）',
    `unit`         VARCHAR(16)              DEFAULT NULL   COMMENT '单位 件/盒/箱',
    `status`       TINYINT         NOT NULL DEFAULT 0      COMMENT '0草稿 1上架 2下架',
    `sort_order`   INT             NOT NULL DEFAULT 100,
    `created_by`   BIGINT                   DEFAULT NULL,
    `updated_by`   BIGINT                   DEFAULT NULL,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT         NOT NULL DEFAULT 0,
    `version`      INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_goods_no` (`goods_no`, `deleted`),
    KEY `idx_category` (`category_id`),
    KEY `idx_brand` (`brand_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品 SPU';


CREATE TABLE IF NOT EXISTS `goods_sku`
(
    `id`         BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `goods_id`   BIGINT          NOT NULL                COMMENT '所属 SPU',
    `sku_no`     VARCHAR(64)     NOT NULL                COMMENT 'SKU 编号',
    `specs`      VARCHAR(512)             DEFAULT NULL   COMMENT '规格 JSON {"color":"red","size":"M"}',
    `price`      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    `stock`      INT             NOT NULL DEFAULT 0      COMMENT '库存（Phase 2 拆 stock-service）',
    `image`      VARCHAR(255)             DEFAULT NULL,
    `enabled`    TINYINT         NOT NULL DEFAULT 1,
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT         NOT NULL DEFAULT 0,
    `version`    INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_no` (`sku_no`, `deleted`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品 SKU';


-- 种子数据：方便手测
INSERT INTO `goods_category` (`name`, `parent_id`, `sort_order`) VALUES
    ('数码', 0, 10),
    ('服饰', 0, 20),
    ('食品', 0, 30);

INSERT INTO `goods_brand` (`name`, `description`, `sort_order`) VALUES
    ('Apple', 'Think different', 10),
    ('Nike', 'Just Do It', 20);
