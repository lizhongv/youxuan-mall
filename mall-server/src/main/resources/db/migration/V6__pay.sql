-- 真实支付渠道（微信支付 APIv3、支付宝手机网站支付）与微信网页授权登录。
-- 说明见 docs/mall/payment.md。
--   1. sys_config 增加 `sensitive` 列（MySQL 8 保留字，SQL 中一律加反引号）：私钥、密钥类参数在后台列表只返回掩码，提交掩码表示不修改；
--      config_value 加宽到 4096，容纳 RSA PEM 私钥（2048 位约 1.7K 字符，4096 位约 3.3K）；
--   2. 支付参数 ID 段 201xx、微信登录 202xx，与 V2 的 200xx 及其他迁移错开；
--   3. trd_payment 记录每次发起支付的渠道交易类型与调起参数（同一订单短时间内重复发起时复用）；
--   4. 售后新增 REFUNDING（渠道退款处理中）；
--   5. mbr_social 会员第三方账号绑定（公众号 openid）；
--   6. 菜单「订单 / 支付流水」3050，授权超级管理员与运营专员（关联 ID 规则同 V2：角色 ID * 100000 + 菜单 ID）。

-- ======================== 系统参数 ========================

ALTER TABLE sys_config
    MODIFY config_value VARCHAR(4096) NOT NULL DEFAULT '',
    ADD COLUMN `sensitive` TINYINT NOT NULL DEFAULT 0 COMMENT '1 敏感值：列表返回掩码，提交掩码不覆盖' AFTER remark;

INSERT INTO sys_config (id, config_key, config_value, name, remark, `sensitive`) VALUES
(20101, 'pay.notify_base_url',       '',      '支付回调根地址',            '必须 HTTPS 且域名已备案，如 https://shop.example.com/api；回调 = 根地址 + /app/public/pay/notify/{wechat|alipay}', 0),
(20102, 'pay.return_url',            '',      '支付完成跳转页',            '商城 H5 支付结果页，{orderId} 替换为订单 ID，如 https://shop.example.com/pay/result?orderId={orderId}', 0),
(20111, 'pay.wechat.enabled',        'false', '微信支付-公众号支付开关',   'true / false；JSAPI 支付，仅在微信内打开商城且会员已微信授权时可用', 0),
(20112, 'pay.wechat.h5_enabled',     'false', '微信支付-H5支付开关',       'true / false；手机浏览器（非微信）内支付，需在商户平台单独开通 H5 支付并配置域名', 0),
(20113, 'pay.wechat.app_id',         '',      '微信支付-公众号AppID',      '与商户号绑定的公众号 AppID；留空使用「微信登录-公众号AppID」', 0),
(20114, 'pay.wechat.mch_id',         '',      '微信支付-商户号',           '商户平台「账户中心 / 商户信息」中的商户号', 0),
(20115, 'pay.wechat.mch_serial_no',  '',      '微信支付-商户证书序列号',   '商户平台「API 安全 / API 证书」的证书序列号', 0),
(20116, 'pay.wechat.api_v3_key',     '',      '微信支付-APIv3密钥',        '32 位，商户平台「API 安全」设置', 1),
(20117, 'pay.wechat.private_key',    '',      '微信支付-商户私钥',         'apiclient_key.pem 全文（含 BEGIN/END 行）', 1),
(20118, 'pay.wechat.public_key_id',  '',      '微信支付-微信支付公钥ID',   '商户平台「API 安全 / 微信支付公钥」中的公钥 ID（PUB_KEY_ID_ 开头）；留空则使用平台证书模式自动下载', 0),
(20119, 'pay.wechat.public_key',     '',      '微信支付-微信支付公钥',     'pub_key.pem 全文；与公钥 ID 同时填写才生效', 0),
(20121, 'pay.alipay.enabled',        'false', '支付宝-手机网站支付开关',   'true / false；微信内无法使用支付宝', 0),
(20122, 'pay.alipay.app_id',         '',      '支付宝-应用AppID',          '开放平台应用的 AppID，需签约「手机网站支付」', 0),
(20123, 'pay.alipay.private_key',    '',      '支付宝-应用私钥',           '密钥工具生成的应用私钥（PKCS8，可不含 BEGIN/END 行）', 1),
(20124, 'pay.alipay.public_key',     '',      '支付宝-支付宝公钥',         '上传应用公钥后开放平台给出的「支付宝公钥」，不是应用公钥', 0),
(20125, 'pay.alipay.gateway',        'https://openapi.alipay.com/gateway.do', '支付宝-网关地址', '沙箱：https://openapi-sandbox.dl.alipaydev.com/gateway.do', 0),
(20201, 'wechat.mp.app_id',          '',      '微信登录-公众号AppID',      '已认证服务号的 AppID，用于公众号内网页授权登录', 0),
(20202, 'wechat.mp.app_secret',      '',      '微信登录-公众号AppSecret',  '公众号后台「设置与开发 / 基本配置」', 1);

-- ======================== 支付流水 ========================

ALTER TABLE trd_payment
    MODIFY status VARCHAR(24) NOT NULL DEFAULT 'WAITING'
        COMMENT 'WAITING / SUCCESS / CLOSED / REFUNDED / REFUNDED_AFTER_CLOSE（关单后到账已自动退款）',
    ADD COLUMN trade_type VARCHAR(16) NOT NULL DEFAULT '' COMMENT '发起方式 WECHAT_JSAPI / WECHAT_H5 / ALIPAY_WAP，同步渠道为空' AFTER channel,
    ADD COLUMN pay_params TEXT NULL COMMENT '返回给前端的调起参数 JSON，短时间内重复发起时复用' AFTER channel_no,
    ADD COLUMN remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '关闭原因、异常说明' AFTER paid_at,
    ADD KEY idx_status_created (status, created_at);

-- ======================== 售后 ========================

ALTER TABLE trd_aftersale
    MODIFY status VARCHAR(20) NOT NULL
        COMMENT 'APPLYING / WAIT_RETURN / RETURNING / REFUNDING / REFUNDED / REJECTED / CANCELLED',
    ADD KEY idx_status_updated (status, updated_at);

-- ======================== 会员第三方账号 ========================

CREATE TABLE mbr_social (
    id         BIGINT       NOT NULL PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    platform   VARCHAR(16)  NOT NULL COMMENT 'WECHAT_MP 微信公众号',
    openid     VARCHAR(64)  NOT NULL COMMENT '该平台应用下的用户标识',
    unionid    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '开放平台 unionid，公众号绑定开放平台后才有',
    created_by BIGINT NULL, updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_platform_openid (platform, openid, deleted),
    UNIQUE KEY uk_member_platform (member_id, platform, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员第三方账号绑定';

-- ======================== 菜单与授权 ========================

INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, permission, sort, visible, status) VALUES
(3050, 3000, 'MENU',   '支付流水', '/trade/payment', 'trade/payment/index', 'Wallet', 'trade:payment:view', 5, 1, 1),
(3051, 3050, 'BUTTON', '支付查单', '',               '',                    '',       'trade:payment:edit', 1, 1, 1);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(103050, 1, 3050), (103051, 1, 3051),
(203050, 2, 3050), (203051, 2, 3051);
