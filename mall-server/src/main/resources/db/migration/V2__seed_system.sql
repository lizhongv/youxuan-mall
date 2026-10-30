-- 系统与会员的种子数据：角色、菜单树、角色授权、数据字典、系统参数、会员等级。
-- 约定：
--   1. 全部使用固定 ID，便于后续迁移脚本按 ID 增量调整，也便于前端 / 测试引用；
--   2. 不包含管理员账号与密码哈希：admin 由应用首次启动时按 MALL_INIT_ADMIN_PASSWORD 创建（AdminInitializer），
--      避免仓库里出现等同于公开口令的哈希；
--   3. 菜单 ID 按目录分段：1xxx 首页、2xxx 商品、3xxx 订单、4xxx 营销、5xxx 会员、6xxx 系统，
--      目录为 x000，菜单为 x0y0 / x0y1 递增，按钮紧跟所属菜单。

-- ======================== 角色 ========================

INSERT INTO sys_role (id, name, code, sort, status, remark) VALUES
(1, '超级管理员', 'super_admin', 0, 1, '拥有全部权限，不受角色授权限制'),
(2, '运营专员', 'operator', 1, 1, '商品、订单、营销、会员的查看与操作，不含系统管理');

-- ======================== 菜单 ========================
-- 前端为静态路由，sys_menu 用于角色授权与按权限过滤菜单；path / permission 与 docs/mall/api.md §4 保持一致。

INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, permission, sort, visible, status) VALUES
-- 首页
(1000, 0,    'DIR',    '首页',       '/home',             '',                     'HomeFilled', '',                       1, 1, 1),
(1001, 1000, 'MENU',   '数据概览',   '/dashboard',        'dashboard/index',      'Odometer',   'stats:view',             1, 1, 1),

-- 商品
(2000, 0,    'DIR',    '商品',       '/goods',            '',                     'Goods',      '',                       2, 1, 1),
(2001, 2000, 'MENU',   '商品列表',   '/goods/list',       'goods/list/index',     'List',       'goods:goods:view',       1, 1, 1),
(2002, 2001, 'BUTTON', '商品编辑',   '',                  '',                     '',           'goods:goods:edit',       1, 1, 1),
(2003, 2000, 'MENU',   '发布商品',   '/goods/edit/:id?',  'goods/edit/index',     'Edit',       'goods:goods:view',       2, 0, 1),
(2010, 2000, 'MENU',   '商品类目',   '/goods/category',   'goods/category/index', 'Menu',       'goods:category:view',    3, 1, 1),
(2011, 2010, 'BUTTON', '类目编辑',   '',                  '',                     '',           'goods:category:edit',    1, 1, 1),
(2020, 2000, 'MENU',   '品牌管理',   '/goods/brand',      'goods/brand/index',    'PriceTag',   'goods:brand:view',       4, 1, 1),
(2021, 2020, 'BUTTON', '品牌编辑',   '',                  '',                     '',           'goods:brand:edit',       1, 1, 1),

-- 订单
(3000, 0,    'DIR',    '订单',       '/trade',            '',                     'Tickets',    '',                       3, 1, 1),
(3001, 3000, 'MENU',   '订单列表',   '/trade/order',      'trade/order/index',    'List',       'trade:order:view',       1, 1, 1),
(3002, 3001, 'BUTTON', '订单操作',   '',                  '',                     '',           'trade:order:edit',       1, 1, 1),
(3003, 3001, 'BUTTON', '订单发货',   '',                  '',                     '',           'trade:order:deliver',    2, 1, 1),
(3004, 3000, 'MENU',   '订单详情',   '/trade/order/:id',  'trade/order/detail',   'Document',   'trade:order:view',       2, 0, 1),
(3010, 3000, 'MENU',   '售后处理',   '/trade/aftersale',  'trade/aftersale/index','RefreshLeft','trade:aftersale:view',   3, 1, 1),
(3011, 3010, 'BUTTON', '售后审核',   '',                  '',                     '',           'trade:aftersale:audit',  1, 1, 1),
(3020, 3000, 'MENU',   '评价管理',   '/trade/review',     'trade/review/index',   'ChatDotRound','trade:review:view',     4, 1, 1),
(3021, 3020, 'BUTTON', '评价处理',   '',                  '',                     '',           'trade:review:edit',      1, 1, 1),

-- 营销
(4000, 0,    'DIR',    '营销',       '/marketing',        '',                     'Present',    '',                       4, 1, 1),
(4001, 4000, 'MENU',   '优惠券',     '/marketing/coupon', 'marketing/coupon/index','Ticket',    'marketing:coupon:view',  1, 1, 1),
(4002, 4001, 'BUTTON', '优惠券编辑', '',                  '',                     '',           'marketing:coupon:edit',  1, 1, 1),
(4010, 4000, 'MENU',   '秒杀活动',   '/marketing/seckill','marketing/seckill/index','Timer',    'marketing:seckill:view', 2, 1, 1),
(4011, 4010, 'BUTTON', '秒杀编辑',   '',                  '',                     '',           'marketing:seckill:edit', 1, 1, 1),
(4020, 4000, 'MENU',   '轮播广告',   '/marketing/banner', 'marketing/banner/index','Picture',   'marketing:banner:view',  3, 1, 1),
(4021, 4020, 'BUTTON', '轮播编辑',   '',                  '',                     '',           'marketing:banner:edit',  1, 1, 1),
(4030, 4000, 'MENU',   '商城公告',   '/marketing/notice', 'marketing/notice/index','Bell',      'marketing:notice:view',  4, 1, 1),
(4031, 4030, 'BUTTON', '公告编辑',   '',                  '',                     '',           'marketing:notice:edit',  1, 1, 1),

-- 会员
(5000, 0,    'DIR',    '会员',       '/member',           '',                     'User',       '',                       5, 1, 1),
(5001, 5000, 'MENU',   '会员列表',   '/member/list',      'member/list/index',    'UserFilled', 'member:member:view',     1, 1, 1),
(5002, 5001, 'BUTTON', '会员操作',   '',                  '',                     '',           'member:member:edit',     1, 1, 1),
(5010, 5000, 'MENU',   '会员等级',   '/member/level',     'member/level/index',   'Medal',      'member:level:view',      2, 1, 1),
(5011, 5010, 'BUTTON', '等级编辑',   '',                  '',                     '',           'member:level:edit',      1, 1, 1),

-- 系统
(6000, 0,    'DIR',    '系统',       '/system',           '',                     'Setting',    '',                       6, 1, 1),
(6001, 6000, 'MENU',   '管理员',     '/system/admin',     'system/admin/index',   'Avatar',     'system:admin:view',      1, 1, 1),
(6002, 6001, 'BUTTON', '管理员编辑', '',                  '',                     '',           'system:admin:edit',      1, 1, 1),
(6010, 6000, 'MENU',   '角色权限',   '/system/role',      'system/role/index',    'Lock',       'system:role:view',       2, 1, 1),
(6011, 6010, 'BUTTON', '角色编辑',   '',                  '',                     '',           'system:role:edit',       1, 1, 1),
(6020, 6000, 'MENU',   '菜单管理',   '/system/menu',      'system/menu/index',    'Menu',       'system:menu:view',       3, 1, 1),
(6021, 6020, 'BUTTON', '菜单编辑',   '',                  '',                     '',           'system:menu:edit',       1, 1, 1),
(6030, 6000, 'MENU',   '数据字典',   '/system/dict',      'system/dict/index',    'Collection', 'system:dict:view',       4, 1, 1),
(6031, 6030, 'BUTTON', '字典编辑',   '',                  '',                     '',           'system:dict:edit',       1, 1, 1),
(6040, 6000, 'MENU',   '系统参数',   '/system/config',    'system/config/index',  'Tools',      'system:config:view',     5, 1, 1),
(6041, 6040, 'BUTTON', '参数编辑',   '',                  '',                     '',           'system:config:edit',     1, 1, 1),
(6050, 6000, 'MENU',   '操作日志',   '/system/oper-log',  'system/oper-log/index','Document',   'system:log:view',        6, 1, 1),
(6060, 6000, 'MENU',   '登录日志',   '/system/login-log', 'system/login-log/index','Key',       'system:log:view',        7, 1, 1);

-- ======================== 角色授权 ========================
-- 关联 ID = 角色 ID * 100000 + 菜单 ID，固定且不会冲突。
-- 超管在代码里直接放行，这里仍写全量授权，让角色授权页能正确回显「全部勾选」。
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + id, 1, id FROM sys_menu WHERE deleted = 0;

-- 运营专员：首页、商品、订单、营销、会员（菜单 ID < 6000），不含系统管理
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 200000 + id, 2, id FROM sys_menu WHERE deleted = 0 AND id < 6000;

-- ======================== 数据字典 ========================
-- 快递公司 value 用快递鸟 / 快递 100 通用的公司编码，日后接物流轨迹查询可直接使用；
-- 原因类字典 value 与 label 相同，订单 / 售后里直接存原因文本，便于阅读。

INSERT INTO sys_dict (id, dict_type, label, value, sort, status, remark) VALUES
(10001, 'express_company', '顺丰速运', 'SF',   1, 1, ''),
(10002, 'express_company', '中通快递', 'ZTO',  2, 1, ''),
(10003, 'express_company', '圆通速递', 'YTO',  3, 1, ''),
(10004, 'express_company', '韵达快递', 'YD',   4, 1, ''),
(10005, 'express_company', '申通快递', 'STO',  5, 1, ''),
(10006, 'express_company', '京东物流', 'JD',   6, 1, ''),
(10007, 'express_company', 'EMS',      'EMS',  7, 1, ''),
(10008, 'express_company', '极兔速递', 'JTSD', 8, 1, ''),

(10101, 'aftersale_reason', '不想要了',       '不想要了',       1, 1, ''),
(10102, 'aftersale_reason', '商品与描述不符', '商品与描述不符', 2, 1, ''),
(10103, 'aftersale_reason', '质量问题',       '质量问题',       3, 1, ''),
(10104, 'aftersale_reason', '少件/漏发',      '少件/漏发',      4, 1, ''),
(10105, 'aftersale_reason', '发错货',         '发错货',         5, 1, ''),
(10106, 'aftersale_reason', '其他',           '其他',           6, 1, ''),

(10201, 'cancel_reason', '不想买了',     '不想买了',     1, 1, ''),
(10202, 'cancel_reason', '信息填写错误', '信息填写错误', 2, 1, ''),
(10203, 'cancel_reason', '重复下单',     '重复下单',     3, 1, ''),
(10204, 'cancel_reason', '其他',         '其他',         4, 1, '');

-- ======================== 系统参数 ========================
-- 键与 com.platform.cloud.mall.system.api.ConfigKeys 一一对应。

INSERT INTO sys_config (id, config_key, config_value, name, remark) VALUES
(20001, 'mall.name',                    '优选商城',     '商城名称',           '商城端标题、分享文案使用'),
(20002, 'mall.service_phone',           '400-800-8888', '客服电话',           '商城端「联系客服」展示'),
(20003, 'trade.freight_amount',         '10.00',        '基础运费（元）',     '未达到包邮门槛时收取'),
(20004, 'trade.freight_free_threshold', '99.00',        '包邮门槛（元）',     '会员折扣后商品金额达到该值包邮，0 表示一律包邮'),
(20005, 'trade.pay_timeout_minutes',    '30',           '支付超时（分钟）',   '下单后超过该时间未支付自动关单'),
(20006, 'trade.auto_receive_days',      '7',            '自动确认收货（天）', '发货后超过该天数自动确认收货'),
(20007, 'trade.aftersale_days',         '7',            '售后期限（天）',     '确认收货后多少天内可申请售后'),
(20008, 'member.points_per_yuan',       '1',            '每元赠送积分',       '按实付金额计算，确认收货时发放'),
(20009, 'member.growth_per_yuan',       '1',            '每元增加成长值',     '按实付金额计算，确认收货时发放'),
(20010, 'member.register_gift_balance', '200.00',       '注册赠送余额（元）', '新会员注册即赠送，用于演示余额支付；设为 0 关闭');

-- ======================== 会员等级 ========================

INSERT INTO mbr_level (id, name, min_growth, discount, icon, sort) VALUES
(1, '普通会员', 0,     1.00, '', 1),
(2, '白银会员', 500,   0.98, '', 2),
(3, '黄金会员', 2000,  0.95, '', 3),
(4, '钻石会员', 10000, 0.90, '', 4);
