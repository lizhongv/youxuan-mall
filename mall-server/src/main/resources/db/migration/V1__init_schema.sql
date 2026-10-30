-- 商城初始表结构。约定：
--   1. 每张表都有审计列、逻辑删除 deleted、乐观锁 version（对应 BaseEntity）；
--   2. 主键用雪花 ID（应用侧生成），不依赖自增，便于日后分库分表；
--   3. 金额 DECIMAL(10,2)，状态用可读的 VARCHAR 枚举名，方便排查数据。
-- 表前缀：sys_ 系统、mbr_ 会员、gds_ 商品、mkt_ 营销、trd_ 交易。

-- ======================== 系统 ========================

CREATE TABLE sys_admin (
    id            BIGINT       NOT NULL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL COMMENT '登录名',
    password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    nickname      VARCHAR(64)  NOT NULL DEFAULT '',
    avatar        VARCHAR(255) NOT NULL DEFAULT '',
    mobile        VARCHAR(20)  NOT NULL DEFAULT '',
    email         VARCHAR(100) NOT NULL DEFAULT '',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    last_login_at DATETIME     NULL,
    last_login_ip VARCHAR(64)  NOT NULL DEFAULT '',
    remark        VARCHAR(255) NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台管理员';

CREATE TABLE sys_role (
    id      BIGINT       NOT NULL PRIMARY KEY,
    name    VARCHAR(64)  NOT NULL,
    code    VARCHAR(64)  NOT NULL COMMENT '角色编码，super_admin 拥有全部权限',
    sort    INT          NOT NULL DEFAULT 0,
    status  TINYINT      NOT NULL DEFAULT 1,
    remark  VARCHAR(255) NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (code, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

CREATE TABLE sys_menu (
    id         BIGINT       NOT NULL PRIMARY KEY,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    type       VARCHAR(16)  NOT NULL COMMENT 'DIR 目录 / MENU 菜单 / BUTTON 按钮',
    name       VARCHAR(64)  NOT NULL,
    path       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '前端路由',
    component  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '前端组件路径',
    icon       VARCHAR(64)  NOT NULL DEFAULT '',
    permission VARCHAR(128) NOT NULL DEFAULT '' COMMENT '权限码，如 goods:edit',
    sort       INT          NOT NULL DEFAULT 0,
    visible    TINYINT      NOT NULL DEFAULT 1,
    status     TINYINT      NOT NULL DEFAULT 1,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单与权限';

CREATE TABLE sys_admin_role (
    id       BIGINT NOT NULL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    UNIQUE KEY uk_admin_role (admin_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员-角色';

CREATE TABLE sys_role_menu (
    id      BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色-菜单';

CREATE TABLE sys_dict (
    id        BIGINT       NOT NULL PRIMARY KEY,
    dict_type VARCHAR(64)  NOT NULL COMMENT '字典类型，如 express_company',
    label     VARCHAR(64)  NOT NULL,
    value     VARCHAR(64)  NOT NULL,
    sort      INT          NOT NULL DEFAULT 0,
    status    TINYINT      NOT NULL DEFAULT 1,
    remark    VARCHAR(255) NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据字典';

CREATE TABLE sys_config (
    id           BIGINT        NOT NULL PRIMARY KEY,
    config_key   VARCHAR(64)   NOT NULL,
    config_value VARCHAR(2000) NOT NULL DEFAULT '',
    name         VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '展示名',
    remark       VARCHAR(255)  NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_key (config_key, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数（运费、超时时间、商城信息等）';

CREATE TABLE sys_oper_log (
    id          BIGINT        NOT NULL PRIMARY KEY,
    admin_id    BIGINT        NULL,
    username    VARCHAR(64)   NOT NULL DEFAULT '',
    module      VARCHAR(64)   NOT NULL DEFAULT '',
    action      VARCHAR(128)  NOT NULL DEFAULT '',
    method      VARCHAR(16)   NOT NULL DEFAULT '',
    uri         VARCHAR(255)  NOT NULL DEFAULT '',
    params      VARCHAR(2000) NOT NULL DEFAULT '',
    ip          VARCHAR(64)   NOT NULL DEFAULT '',
    success     TINYINT       NOT NULL DEFAULT 1,
    error_msg   VARCHAR(500)  NOT NULL DEFAULT '',
    cost_ms     INT           NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

CREATE TABLE sys_login_log (
    id         BIGINT       NOT NULL PRIMARY KEY,
    user_type  VARCHAR(16)  NOT NULL COMMENT 'ADMIN / MEMBER',
    user_id    BIGINT       NULL,
    username   VARCHAR(64)  NOT NULL DEFAULT '',
    ip         VARCHAR(64)  NOT NULL DEFAULT '',
    user_agent VARCHAR(255) NOT NULL DEFAULT '',
    success    TINYINT      NOT NULL DEFAULT 1,
    message    VARCHAR(255) NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志';

CREATE TABLE sys_file (
    id            BIGINT       NOT NULL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL DEFAULT '',
    path          VARCHAR(255) NOT NULL COMMENT '相对存储根目录的路径',
    url           VARCHAR(500) NOT NULL,
    size          BIGINT       NOT NULL DEFAULT 0,
    content_type  VARCHAR(100) NOT NULL DEFAULT '',
    biz_type      VARCHAR(32)  NOT NULL DEFAULT '' COMMENT 'goods / banner / avatar / review ...',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '上传文件';

-- ======================== 会员 ========================

CREATE TABLE mbr_level (
    id         BIGINT        NOT NULL PRIMARY KEY,
    name       VARCHAR(32)   NOT NULL,
    min_growth INT           NOT NULL DEFAULT 0 COMMENT '达到该成长值即升级',
    discount   DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT '会员折扣，0.95 表示 95 折',
    icon       VARCHAR(255)  NOT NULL DEFAULT '',
    sort       INT           NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员等级';

CREATE TABLE mbr_member (
    id            BIGINT        NOT NULL PRIMARY KEY,
    mobile        VARCHAR(20)   NOT NULL,
    password      VARCHAR(100)  NOT NULL DEFAULT '',
    nickname      VARCHAR(64)   NOT NULL DEFAULT '',
    avatar        VARCHAR(255)  NOT NULL DEFAULT '',
    gender        TINYINT       NOT NULL DEFAULT 0 COMMENT '0 未知 1 男 2 女',
    birthday      DATE          NULL,
    level_id      BIGINT        NULL,
    points        INT           NOT NULL DEFAULT 0 COMMENT '积分余额',
    growth        INT           NOT NULL DEFAULT 0 COMMENT '成长值，只增不减',
    balance       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1 正常 0 冻结',
    register_ip   VARCHAR(64)   NOT NULL DEFAULT '',
    last_login_at DATETIME      NULL,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_mobile (mobile, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员';

CREATE TABLE mbr_address (
    id         BIGINT       NOT NULL PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    name       VARCHAR(32)  NOT NULL,
    mobile     VARCHAR(20)  NOT NULL,
    province   VARCHAR(32)  NOT NULL,
    city       VARCHAR(32)  NOT NULL,
    district   VARCHAR(32)  NOT NULL,
    detail     VARCHAR(255) NOT NULL,
    is_default TINYINT      NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收货地址';

CREATE TABLE mbr_account_log (
    id           BIGINT         NOT NULL PRIMARY KEY,
    member_id    BIGINT         NOT NULL,
    account_type VARCHAR(16)    NOT NULL COMMENT 'BALANCE 余额 / POINTS 积分 / GROWTH 成长值',
    change_value DECIMAL(10, 2) NOT NULL COMMENT '正数增加，负数减少',
    after_value  DECIMAL(10, 2) NOT NULL,
    biz_type     VARCHAR(32)    NOT NULL COMMENT 'ORDER_PAY / ORDER_REFUND / ORDER_REWARD / ADMIN_ADJUST / RECHARGE / SIGN_IN',
    biz_no       VARCHAR(64)    NOT NULL DEFAULT '',
    remark       VARCHAR(255)   NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_member_type (member_id, account_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员账户流水（余额/积分/成长值）';

CREATE TABLE mbr_favorite (
    id        BIGINT NOT NULL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    goods_id  BIGINT NOT NULL,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品收藏';

CREATE TABLE mbr_browse_history (
    id        BIGINT NOT NULL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    goods_id  BIGINT NOT NULL,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_member (member_id, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '浏览足迹（同一商品只保留一条，更新时间即最近浏览）';

-- ======================== 商品 ========================

CREATE TABLE gds_category (
    id        BIGINT       NOT NULL PRIMARY KEY,
    parent_id BIGINT       NOT NULL DEFAULT 0 COMMENT '0 为一级类目；最多两级',
    name      VARCHAR(64)  NOT NULL,
    icon      VARCHAR(255) NOT NULL DEFAULT '',
    sort      INT          NOT NULL DEFAULT 0,
    status    TINYINT      NOT NULL DEFAULT 1,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品类目';

CREATE TABLE gds_brand (
    id           BIGINT       NOT NULL PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL,
    logo         VARCHAR(255) NOT NULL DEFAULT '',
    first_letter CHAR(1)      NOT NULL DEFAULT '',
    description  VARCHAR(500) NOT NULL DEFAULT '',
    sort         INT          NOT NULL DEFAULT 0,
    status       TINYINT      NOT NULL DEFAULT 1,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '品牌';

CREATE TABLE gds_goods (
    id             BIGINT         NOT NULL PRIMARY KEY,
    category_id    BIGINT         NOT NULL COMMENT '二级类目',
    brand_id       BIGINT         NULL,
    name           VARCHAR(128)   NOT NULL,
    subtitle       VARCHAR(255)   NOT NULL DEFAULT '',
    pic            VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '主图',
    album          JSON           NULL COMMENT '轮播图 URL 数组',
    detail         MEDIUMTEXT     NULL COMMENT '详情富文本',
    spec_items     JSON           NULL COMMENT '规格定义 [{"name":"颜色","values":["红","蓝"]}]',
    price          DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '最低 SKU 价，列表展示用',
    original_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    stock          INT            NOT NULL DEFAULT 0 COMMENT 'SKU 库存合计（冗余）',
    sales          INT            NOT NULL DEFAULT 0 COMMENT '真实销量',
    virtual_sales  INT            NOT NULL DEFAULT 0 COMMENT '展示用基础销量',
    unit           VARCHAR(16)    NOT NULL DEFAULT '件',
    keywords       VARCHAR(255)   NOT NULL DEFAULT '',
    status         VARCHAR(16)    NOT NULL DEFAULT 'OFF_SALE' COMMENT 'ON_SALE 上架 / OFF_SALE 下架',
    is_new         TINYINT        NOT NULL DEFAULT 0,
    is_hot         TINYINT        NOT NULL DEFAULT 0,
    is_recommend   TINYINT        NOT NULL DEFAULT 0,
    sort           INT            NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_category (category_id),
    KEY idx_status_sort (status, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品 SPU';

CREATE TABLE gds_sku (
    id             BIGINT         NOT NULL PRIMARY KEY,
    goods_id       BIGINT         NOT NULL,
    spec_values    JSON           NULL COMMENT '与 spec_items 顺序一致的取值 ["红","XL"]',
    spec_text      VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '展示用：颜色:红;尺码:XL',
    code           VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '商家编码',
    pic            VARCHAR(255)   NOT NULL DEFAULT '',
    price          DECIMAL(10, 2) NOT NULL,
    original_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    stock          INT            NOT NULL DEFAULT 0 COMMENT '可售库存；下单即扣，取消/关单回补',
    sales          INT            NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_goods (goods_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品 SKU';

-- ======================== 营销 ========================

CREATE TABLE mkt_banner (
    id         BIGINT       NOT NULL PRIMARY KEY,
    title      VARCHAR(64)  NOT NULL,
    image      VARCHAR(255) NOT NULL,
    link_type  VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE / GOODS / CATEGORY / URL',
    link_value VARCHAR(255) NOT NULL DEFAULT '',
    position   VARCHAR(16)  NOT NULL DEFAULT 'HOME',
    sort       INT          NOT NULL DEFAULT 0,
    status     TINYINT      NOT NULL DEFAULT 1,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '轮播广告';

CREATE TABLE mkt_notice (
    id      BIGINT       NOT NULL PRIMARY KEY,
    title   VARCHAR(128) NOT NULL,
    content TEXT         NULL,
    status  TINYINT      NOT NULL DEFAULT 1,
    sort    INT          NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商城公告';

CREATE TABLE mkt_coupon (
    id          BIGINT         NOT NULL PRIMARY KEY,
    name        VARCHAR(64)    NOT NULL,
    type        VARCHAR(16)    NOT NULL COMMENT 'FULL_REDUCTION 满减 / DISCOUNT 折扣 / NO_THRESHOLD 无门槛',
    threshold   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛（订单商品金额 ≥ 该值）',
    amount      DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '减免金额（满减/无门槛）',
    discount    DECIMAL(4, 2)  NOT NULL DEFAULT 1.00 COMMENT '折扣率（折扣券），0.80 即 8 折',
    max_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '折扣券最高减免，0 不限',
    scope       VARCHAR(16)    NOT NULL DEFAULT 'ALL' COMMENT 'ALL / CATEGORY / GOODS',
    scope_ids   JSON           NULL COMMENT '适用的类目或商品 ID 数组',
    total       INT            NOT NULL DEFAULT 0 COMMENT '发行量，0 不限',
    issued      INT            NOT NULL DEFAULT 0,
    used        INT            NOT NULL DEFAULT 0,
    per_limit   INT            NOT NULL DEFAULT 1 COMMENT '每人限领',
    valid_type  VARCHAR(16)    NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED 固定时间段 / DAYS 领取后 N 天',
    start_at    DATETIME       NULL,
    end_at      DATETIME       NULL,
    valid_days  INT            NOT NULL DEFAULT 0,
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1 可领取 0 停发',
    description VARCHAR(255)   NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '优惠券模板';

CREATE TABLE mkt_member_coupon (
    id        BIGINT      NOT NULL PRIMARY KEY,
    member_id BIGINT      NOT NULL,
    coupon_id BIGINT      NOT NULL,
    status    VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED / USED / EXPIRED',
    start_at  DATETIME    NOT NULL,
    end_at    DATETIME    NOT NULL,
    used_at   DATETIME    NULL,
    order_id  BIGINT      NULL,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_member_status (member_id, status),
    KEY idx_coupon (coupon_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员领取的优惠券';

CREATE TABLE mkt_seckill (
    id             BIGINT         NOT NULL PRIMARY KEY,
    title          VARCHAR(64)    NOT NULL,
    goods_id       BIGINT         NOT NULL,
    sku_id         BIGINT         NOT NULL,
    seckill_price  DECIMAL(10, 2) NOT NULL,
    stock          INT            NOT NULL COMMENT '活动库存（从 SKU 库存中划拨）',
    sold           INT            NOT NULL DEFAULT 0,
    limit_per_member INT          NOT NULL DEFAULT 1,
    start_at       DATETIME       NOT NULL,
    end_at         DATETIME       NOT NULL,
    status         TINYINT        NOT NULL DEFAULT 1,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_time (start_at, end_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀活动（单 SKU）';

-- ======================== 交易 ========================

CREATE TABLE trd_cart (
    id        BIGINT  NOT NULL PRIMARY KEY,
    member_id BIGINT  NOT NULL,
    goods_id  BIGINT  NOT NULL,
    sku_id    BIGINT  NOT NULL,
    quantity  INT     NOT NULL DEFAULT 1,
    checked   TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车';

CREATE TABLE trd_order (
    id               BIGINT         NOT NULL PRIMARY KEY,
    order_no         VARCHAR(32)    NOT NULL,
    member_id        BIGINT         NOT NULL,
    order_type       VARCHAR(16)    NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / SECKILL',
    status           VARCHAR(20)    NOT NULL COMMENT 'PENDING_PAY / PENDING_DELIVERY / DELIVERED / COMPLETED / CANCELLED',
    goods_amount     DECIMAL(10, 2) NOT NULL COMMENT '商品原价合计',
    freight_amount   DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    coupon_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '会员折扣、秒杀等其他优惠',
    adjust_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '后台改价，负数为减价',
    pay_amount       DECIMAL(10, 2) NOT NULL COMMENT '应付金额',
    member_coupon_id BIGINT         NULL,
    pay_type         VARCHAR(16)    NOT NULL DEFAULT '' COMMENT 'BALANCE / WECHAT / ALIPAY / MOCK',
    pay_time         DATETIME       NULL,
    pay_no           VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '支付流水号',
    pay_expire_at    DATETIME       NOT NULL COMMENT '超过该时间未支付自动关闭',
    receiver_name    VARCHAR(32)    NOT NULL,
    receiver_mobile  VARCHAR(20)    NOT NULL,
    receiver_province VARCHAR(32)   NOT NULL,
    receiver_city    VARCHAR(32)    NOT NULL,
    receiver_district VARCHAR(32)   NOT NULL,
    receiver_detail  VARCHAR(255)   NOT NULL,
    remark           VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '买家留言',
    admin_remark     VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '商家备注',
    delivery_company VARCHAR(64)    NOT NULL DEFAULT '',
    delivery_no      VARCHAR(64)    NOT NULL DEFAULT '',
    delivery_time    DATETIME       NULL,
    receive_time     DATETIME       NULL,
    auto_receive_at  DATETIME       NULL COMMENT '发货后超过该时间自动确认收货',
    cancel_time      DATETIME       NULL,
    cancel_reason    VARCHAR(255)   NOT NULL DEFAULT '',
    reviewed         TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已全部评价',
    member_deleted   TINYINT        NOT NULL DEFAULT 0 COMMENT '买家删除（只对买家隐藏）',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_member_status (member_id, status),
    KEY idx_status_expire (status, pay_expire_at),
    KEY idx_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单';

CREATE TABLE trd_order_item (
    id               BIGINT         NOT NULL PRIMARY KEY,
    order_id         BIGINT         NOT NULL,
    member_id        BIGINT         NOT NULL,
    goods_id         BIGINT         NOT NULL,
    sku_id           BIGINT         NOT NULL,
    goods_name       VARCHAR(128)   NOT NULL,
    pic              VARCHAR(255)   NOT NULL DEFAULT '',
    spec_text        VARCHAR(255)   NOT NULL DEFAULT '',
    price            DECIMAL(10, 2) NOT NULL COMMENT '下单时单价快照',
    quantity         INT            NOT NULL,
    total_amount     DECIMAL(10, 2) NOT NULL COMMENT 'price * quantity',
    pay_amount       DECIMAL(10, 2) NOT NULL COMMENT '分摊优惠后的实付，用于售后退款上限',
    aftersale_status VARCHAR(16)    NOT NULL DEFAULT 'NONE' COMMENT 'NONE / APPLYING / REFUNDED',
    reviewed         TINYINT        NOT NULL DEFAULT 0,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_order (order_id),
    KEY idx_goods (goods_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';

CREATE TABLE trd_order_log (
    id          BIGINT       NOT NULL PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    from_status VARCHAR(20)  NOT NULL DEFAULT '',
    to_status   VARCHAR(20)  NOT NULL,
    operator    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'member:手机号 / admin:用户名 / system',
    remark      VARCHAR(255) NOT NULL DEFAULT '',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单状态流转日志';

CREATE TABLE trd_payment (
    id         BIGINT         NOT NULL PRIMARY KEY,
    pay_no     VARCHAR(64)    NOT NULL COMMENT '本系统支付单号，发给支付渠道',
    order_id   BIGINT         NOT NULL,
    member_id  BIGINT         NOT NULL,
    channel    VARCHAR(16)    NOT NULL,
    amount     DECIMAL(10, 2) NOT NULL,
    status     VARCHAR(16)    NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING / SUCCESS / CLOSED / REFUNDED',
    channel_no VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '渠道交易号',
    paid_at    DATETIME       NULL,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付流水';

CREATE TABLE trd_aftersale (
    id              BIGINT         NOT NULL PRIMARY KEY,
    aftersale_no    VARCHAR(32)    NOT NULL,
    order_id        BIGINT         NOT NULL,
    order_item_id   BIGINT         NOT NULL,
    member_id       BIGINT         NOT NULL,
    type            VARCHAR(16)    NOT NULL COMMENT 'REFUND 仅退款 / RETURN_REFUND 退货退款',
    status          VARCHAR(20)    NOT NULL COMMENT 'APPLYING / WAIT_RETURN / RETURNING / REFUNDED / REJECTED / CANCELLED',
    reason          VARCHAR(64)    NOT NULL,
    description     VARCHAR(500)   NOT NULL DEFAULT '',
    images          JSON           NULL,
    amount          DECIMAL(10, 2) NOT NULL COMMENT '申请退款金额，≤ 明细实付',
    quantity        INT            NOT NULL DEFAULT 1,
    audit_remark    VARCHAR(255)   NOT NULL DEFAULT '',
    audit_time      DATETIME       NULL,
    return_company  VARCHAR(64)    NOT NULL DEFAULT '',
    return_no       VARCHAR(64)    NOT NULL DEFAULT '',
    refund_time     DATETIME       NULL,
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_no (aftersale_no),
    KEY idx_member (member_id),
    KEY idx_order_item (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '售后单';

CREATE TABLE trd_review (
    id            BIGINT        NOT NULL PRIMARY KEY,
    order_id      BIGINT        NOT NULL,
    order_item_id BIGINT        NOT NULL,
    goods_id      BIGINT        NOT NULL,
    member_id     BIGINT        NOT NULL,
    nickname      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '评价时快照；匿名时为 匿名用户',
    avatar        VARCHAR(255)  NOT NULL DEFAULT '',
    spec_text     VARCHAR(255)  NOT NULL DEFAULT '',
    rating        TINYINT       NOT NULL COMMENT '1-5 星',
    content       VARCHAR(1000) NOT NULL DEFAULT '',
    images        JSON          NULL,
    anonymous     TINYINT       NOT NULL DEFAULT 0,
    reply         VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '商家回复',
    reply_time    DATETIME      NULL,
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1 显示 0 隐藏',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_item (order_item_id, deleted),
    KEY idx_goods (goods_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品评价';
