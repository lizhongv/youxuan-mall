-- V3：商品与营销演示数据（类目 / 品牌 / 商品 / SKU / 轮播 / 公告 / 优惠券 / 秒杀）
-- 图片位于 deploy/mall/seed/uploads/demo/，由 scripts/gen_demo_images.py 生成，URL 前缀 /uploads/demo/

-- ======================== 类目 ========================
INSERT INTO gds_category (id, parent_id, name, icon, sort, status) VALUES
(10010001, 0, '手机数码', '/uploads/demo/cat_10010001.png', 1, 1),
(10010002, 0, '电脑办公', '/uploads/demo/cat_10010002.png', 2, 1),
(10010003, 0, '家用电器', '/uploads/demo/cat_10010003.png', 3, 1),
(10010004, 0, '服饰鞋包', '/uploads/demo/cat_10010004.png', 4, 1),
(10010005, 0, '美妆个护', '/uploads/demo/cat_10010005.png', 5, 1),
(10010006, 0, '食品生鲜', '/uploads/demo/cat_10010006.png', 6, 1),
(10010007, 0, '家居家装', '/uploads/demo/cat_10010007.png', 7, 1),
(10010008, 0, '运动户外', '/uploads/demo/cat_10010008.png', 8, 1),
(10010101, 10010001, '手机', '/uploads/demo/cat_10010101.png', 1, 1),
(10010102, 10010001, '耳机音箱', '/uploads/demo/cat_10010102.png', 2, 1),
(10010103, 10010001, '智能穿戴', '/uploads/demo/cat_10010103.png', 3, 1),
(10010201, 10010002, '笔记本', '/uploads/demo/cat_10010201.png', 1, 1),
(10010202, 10010002, '键鼠外设', '/uploads/demo/cat_10010202.png', 2, 1),
(10010203, 10010002, '办公文具', '/uploads/demo/cat_10010203.png', 3, 1),
(10010301, 10010003, '厨房电器', '/uploads/demo/cat_10010301.png', 1, 1),
(10010302, 10010003, '生活电器', '/uploads/demo/cat_10010302.png', 2, 1),
(10010303, 10010003, '个护电器', '/uploads/demo/cat_10010303.png', 3, 1),
(10010401, 10010004, '男装', '/uploads/demo/cat_10010401.png', 1, 1),
(10010402, 10010004, '女装', '/uploads/demo/cat_10010402.png', 2, 1),
(10010403, 10010004, '运动鞋', '/uploads/demo/cat_10010403.png', 3, 1),
(10010404, 10010004, '箱包', '/uploads/demo/cat_10010404.png', 4, 1),
(10010501, 10010005, '护肤', '/uploads/demo/cat_10010501.png', 1, 1),
(10010502, 10010005, '彩妆', '/uploads/demo/cat_10010502.png', 2, 1),
(10010503, 10010005, '洗护', '/uploads/demo/cat_10010503.png', 3, 1),
(10010601, 10010006, '休闲零食', '/uploads/demo/cat_10010601.png', 1, 1),
(10010602, 10010006, '茶饮冲调', '/uploads/demo/cat_10010602.png', 2, 1),
(10010603, 10010006, '生鲜水果', '/uploads/demo/cat_10010603.png', 3, 1),
(10010701, 10010007, '家纺', '/uploads/demo/cat_10010701.png', 1, 1),
(10010702, 10010007, '灯具', '/uploads/demo/cat_10010702.png', 2, 1),
(10010703, 10010007, '收纳', '/uploads/demo/cat_10010703.png', 3, 1),
(10010801, 10010008, '健身器材', '/uploads/demo/cat_10010801.png', 1, 1),
(10010802, 10010008, '户外装备', '/uploads/demo/cat_10010802.png', 2, 1),
(10010803, 10010008, '球类', '/uploads/demo/cat_10010803.png', 3, 1);

-- ======================== 品牌 ========================
INSERT INTO gds_brand (id, name, logo, first_letter, description, sort, status) VALUES
(10020001, '青禾', '/uploads/demo/brand_10020001.png', 'Q', '青禾专注天然好食材与日常文具，坚持产地直采、简单配方，让每一天的吃喝用度都更安心。', 1, 1),
(10020002, '北极星 Polaris', '/uploads/demo/brand_10020002.png', 'B', '北极星 Polaris 是专注效率办公与智能家电的科技品牌，以稳定可靠的品质陪伴每一位认真生活的人。', 2, 1),
(10020003, '星澜', '/uploads/demo/brand_10020003.png', 'X', '星澜科技致力于打造兼具性能与设计的智能手机与可穿戴设备，让科技触手可及。', 3, 1),
(10020004, '拾光', '/uploads/demo/brand_10020004.png', 'S', '拾光以温和有效的成分科学为核心，提供护肤、彩妆与个护电器，记录每一段美好时光。', 4, 1),
(10020005, '木野', '/uploads/demo/brand_10020005.png', 'M', '木野倡导自然、简约的北欧生活方式，产品覆盖家纺、灯具、收纳与出行箱包。', 5, 1),
(10020006, '远山', '/uploads/demo/brand_10020006.png', 'Y', '远山户外为城市里的探索者设计专业而轻量的运动与露营装备，向山而行。', 6, 1),
(10020007, '简屿', '/uploads/demo/brand_10020007.png', 'J', '简屿服饰以舒适面料与利落剪裁为本，打造四季百搭的基础款衣橱。', 7, 1),
(10020008, '栗子厨房', '/uploads/demo/brand_10020008.png', 'L', '栗子厨房专注小家电研发，用更聪明的厨房电器把下厨变成一件轻松愉快的事。', 8, 1),
(10020009, '赫兹', '/uploads/demo/brand_10020009.png', 'H', '赫兹声学深耕音频技术十余年，以纯净还原的音质与舒适佩戴赢得口碑。', 9, 1),
(10020010, '迪越', '/uploads/demo/brand_10020010.png', 'D', '迪越运动为跑者与球类爱好者打造专业运动装备，科技缓震，步步轻盈。', 10, 1);

-- ======================== 商品 SPU ========================
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030001, 10010101, 10020003, '星澜 X9 Pro 5G 旗舰手机', '2K 曲面屏 · 5000 万像素三摄 · 100W 闪充', '/uploads/demo/g10030001_main.jpg', '["/uploads/demo/g10030001_main.jpg","/uploads/demo/g10030001_1.jpg","/uploads/demo/g10030001_2.jpg"]',
 '<p>星澜 X9 Pro 搭载新一代旗舰芯片与 6.78 英寸 2K 曲面屏，影像、性能与续航全面进阶，是今年最值得入手的 5G 旗舰。</p><p><strong>产品亮点：</strong>6.78 英寸 2K 曲面屏；5000 万像素影像系统；100W 有线闪充。</p><img src="/uploads/demo/g10030001_1.jpg" style="width:100%"/><p>2K 曲面屏、5000 万像素三摄、100W 闪充，多色可选、质感随行。</p><img src="/uploads/demo/g10030001_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["曜石黑","月光白","远峰蓝"]},{"name":"存储","values":["8+256G","12+512G"]}]', 3299.00, 3599.00, 1481, 0, 3680, '台', '手机 5G 旗舰 曲面屏 快充 星澜', 'ON_SALE', 0, 1, 1, 10);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030002, 10010101, 10020003, '星澜 Note 12 5G 手机', '大电池长续航 · 120Hz 高刷护眼屏', '/uploads/demo/g10030002_main.jpg', '["/uploads/demo/g10030002_main.jpg","/uploads/demo/g10030002_1.jpg","/uploads/demo/g10030002_2.jpg"]',
 '<p>6000mAh 大电池配合 120Hz 高刷护眼屏，追剧、游戏一整天电量依旧从容，千元价位的越级体验。</p><p><strong>产品亮点：</strong>6000mAh 大电池；120Hz 高刷护眼屏；轻薄 7.9mm 机身。</p><img src="/uploads/demo/g10030002_1.jpg" style="width:100%"/><p>大电池长续航、120Hz 高刷护眼屏，多色可选、质感随行。</p><img src="/uploads/demo/g10030002_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["星空灰","薄荷绿"]},{"name":"存储","values":["6+128G","8+256G"]}]', 1299.00, 1499.00, 915, 0, 2150, '台', '手机 5G 长续航 高刷 学生 星澜', 'ON_SALE', 1, 0, 0, 20);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030003, 10010102, 10020009, '赫兹 AirBuds 3 主动降噪耳机', '-45dB 深度降噪 · 36 小时总续航', '/uploads/demo/g10030003_main.jpg', '["/uploads/demo/g10030003_main.jpg","/uploads/demo/g10030003_1.jpg","/uploads/demo/g10030003_2.jpg"]',
 '<p>赫兹 AirBuds 3 采用自研混合主动降噪方案，地铁、办公室一键安静，通透模式下也能自然听清环境声。</p><p><strong>产品亮点：</strong>-45dB 智能降噪；36 小时超长续航；蓝牙 5.3 低延迟。</p><img src="/uploads/demo/g10030003_1.jpg" style="width:100%"/><p>-45dB 深度降噪、36 小时总续航，多色可选、质感随行。</p><img src="/uploads/demo/g10030003_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["象牙白","石墨黑"]}]', 399.00, 499.00, 331, 0, 4820, '副', '蓝牙耳机 降噪 无线 真无线 赫兹', 'ON_SALE', 0, 1, 0, 30);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030004, 10010102, 10020009, '赫兹 Cube 便携蓝牙音箱', 'IP67 防水防尘 · 360° 环绕立体声', '/uploads/demo/g10030004_main.jpg', '["/uploads/demo/g10030004_main.jpg","/uploads/demo/g10030004_1.jpg","/uploads/demo/g10030004_2.jpg"]',
 '<p>巴掌大小的立方体机身，内置双被动辐射器，低音饱满有力；IP67 防水防尘，露营、泳池边都能放心使用。</p><p><strong>产品亮点：</strong>360° 环绕声场；IP67 防水防尘；20 小时续航。</p><img src="/uploads/demo/g10030004_1.jpg" style="width:100%"/><p>IP67 防水防尘、360° 环绕立体声，多色可选、质感随行。</p><img src="/uploads/demo/g10030004_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["深海蓝","珊瑚红","岩石灰"]}]', 259.00, 319.00, 682, 0, 960, '台', '蓝牙音箱 便携 防水 户外 赫兹', 'ON_SALE', 0, 0, 0, 40);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030005, 10010103, 10020003, '星澜 Watch S2 智能手表', '血氧心率监测 · 14 天超长续航', '/uploads/demo/g10030005_main.jpg', '["/uploads/demo/g10030005_main.jpg","/uploads/demo/g10030005_1.jpg","/uploads/demo/g10030005_2.jpg"]',
 '<p>星澜 Watch S2 支持全天候心率、血氧与睡眠监测，内置 100+ 运动模式和独立 GPS，日常使用续航长达 14 天。</p><p><strong>产品亮点：</strong>全天候健康监测；100+ 运动模式；14 天超长续航。</p><img src="/uploads/demo/g10030005_1.jpg" style="width:100%"/><p>血氧心率监测、14 天超长续航，多色可选、质感随行。</p><img src="/uploads/demo/g10030005_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["午夜黑","晨雾粉"]},{"name":"表带","values":["氟橡胶","米兰尼斯"]}]', 899.00, 1099.00, 1247, 0, 1340, '块', '智能手表 运动 心率 血氧 星澜', 'ON_SALE', 1, 0, 0, 50);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030006, 10010201, 10020002, '北极星 Polaris Book 14 轻薄本', '1.2kg 全金属机身 · 2.8K 高色域屏', '/uploads/demo/g10030006_main.jpg', '["/uploads/demo/g10030006_main.jpg","/uploads/demo/g10030006_1.jpg","/uploads/demo/g10030006_2.jpg"]',
 '<p>Polaris Book 14 采用一体成型铝合金机身，重量仅 1.2kg，搭配 2.8K 120Hz 高色域屏，出差通勤轻松随行。</p><p><strong>产品亮点：</strong>2.8K 120Hz 高色域屏；1.2kg 全金属机身；65W 快充 · 续航 14h。</p><img src="/uploads/demo/g10030006_1.jpg" style="width:100%"/><p>1.2kg 全金属机身、2.8K 高色域屏，高效办公、灵感随行。</p><img src="/uploads/demo/g10030006_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"配置","values":["i5/16G/512G","i7/16G/1TB"]},{"name":"颜色","values":["皓月银","深空灰"]}]', 4999.00, 5499.00, 967, 0, 860, '台', '笔记本电脑 轻薄本 办公 学生 北极星', 'ON_SALE', 0, 0, 1, 60);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030007, 10010202, 10020002, '北极星 K87 三模机械键盘', '热插拔轴座 · 三模连接 · RGB 背光', '/uploads/demo/g10030007_main.jpg', '["/uploads/demo/g10030007_main.jpg","/uploads/demo/g10030007_1.jpg","/uploads/demo/g10030007_2.jpg"]',
 '<p>87 键紧凑布局，支持有线、蓝牙与 2.4G 三模连接，全键热插拔可自由更换轴体，办公游戏两相宜。</p><p><strong>产品亮点：</strong>有线/蓝牙/2.4G 三模；全键热插拔；RGB 炫彩背光。</p><img src="/uploads/demo/g10030007_1.jpg" style="width:100%"/><p>热插拔轴座、三模连接、RGB 背光，高效办公、灵感随行。</p><img src="/uploads/demo/g10030007_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"轴体","values":["红轴","茶轴","青轴"]}]', 299.00, 379.00, 826, 0, 1720, '把', '机械键盘 三模 热插拔 RGB 北极星', 'ON_SALE', 0, 0, 0, 70);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030008, 10010202, 10020002, '北极星 M3 无线静音鼠标', '静音微动 · 人体工学 · 一年续航', '/uploads/demo/g10030008_main.jpg', '["/uploads/demo/g10030008_main.jpg","/uploads/demo/g10030008_1.jpg","/uploads/demo/g10030008_2.jpg"]',
 '<p>静音微动设计，点击声降低 90%，深夜加班不打扰他人；人体工学弧度贴合手掌，久握不累。</p><p><strong>产品亮点：</strong>静音微动 降噪 90%；人体工学握感；一节电池用一年。</p><img src="/uploads/demo/g10030008_1.jpg" style="width:100%"/><p>静音微动、人体工学、一年续航，高效办公、灵感随行。</p><img src="/uploads/demo/g10030008_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["曜石黑","珍珠白","樱花粉"]}]', 79.00, 99.00, 909, 0, 5000, '个', '无线鼠标 静音 办公 人体工学 北极星', 'ON_SALE', 0, 1, 0, 80);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030009, 10010203, 10020001, '青禾 A5 活页笔记本套装', '100g 道林纸 · 不洇墨 · 自由增减内页', '/uploads/demo/g10030009_main.jpg', '["/uploads/demo/g10030009_main.jpg","/uploads/demo/g10030009_1.jpg","/uploads/demo/g10030009_2.jpg"]',
 '<p>套装包含 A5 活页夹 1 个与 100g 道林纸内页 3 本，纸张厚实顺滑，钢笔书写不洇墨，可按需增减内页。</p><p><strong>产品亮点：</strong>100g 加厚道林纸；书写顺滑不洇墨；活页自由增减。</p><img src="/uploads/demo/g10030009_1.jpg" style="width:100%"/><p>100g 道林纸、不洇墨、自由增减内页，高效办公、灵感随行。</p><img src="/uploads/demo/g10030009_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[]', 29.90, 39.90, 137, 0, 3200, '套', '笔记本 活页本 文具 办公 学生 青禾', 'ON_SALE', 0, 0, 0, 90);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030010, 10010301, 10020008, '栗子厨房 可视空气炸锅 5L', '可视窗口 · 免翻面 · 少油更健康', '/uploads/demo/g10030010_main.jpg', '["/uploads/demo/g10030010_main.jpg","/uploads/demo/g10030010_1.jpg","/uploads/demo/g10030010_2.jpg"]',
 '<p>5L 大容量炸篮一次可做一只整鸡，可视窗口随时观察烹饪状态，360° 热风循环免翻面，少油也能外酥里嫩。</p><p><strong>产品亮点：</strong>可视烹饪窗口；360° 热风免翻面；5L 大容量。</p><img src="/uploads/demo/g10030010_1.jpg" style="width:100%"/><p>可视窗口、免翻面、少油更健康，智能家电、品质生活。</p><img src="/uploads/demo/g10030010_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["奶油白","薄荷绿"]}]', 299.00, 399.00, 593, 0, 4300, '台', '空气炸锅 家用 可视 大容量 栗子厨房', 'ON_SALE', 0, 1, 1, 100);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030011, 10010301, 10020008, '栗子厨房 静音破壁料理机', '低噪破壁 · 冷热双打 · 一键清洗', '/uploads/demo/g10030011_main.jpg', '["/uploads/demo/g10030011_main.jpg","/uploads/demo/g10030011_1.jpg","/uploads/demo/g10030011_2.jpg"]',
 '<p>8 叶精钢刀头配合降噪罩，破壁同时把噪音降到更低；冷热双打，豆浆、米糊、果汁一机搞定，用完一键自清洁。</p><p><strong>产品亮点：</strong>低噪 8 叶刀头；冷热双打 豆浆米糊；一键自清洁。</p><img src="/uploads/demo/g10030011_1.jpg" style="width:100%"/><p>低噪破壁、冷热双打、一键清洗，智能家电、品质生活。</p><img src="/uploads/demo/g10030011_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[]', 499.00, 699.00, 325, 0, 780, '台', '破壁机 料理机 豆浆机 静音 栗子厨房', 'ON_SALE', 1, 0, 0, 110);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030012, 10010302, 10020002, '北极星 S7 扫拖一体机器人', '激光导航 · 4000Pa 大吸力 · 自动集尘', '/uploads/demo/g10030012_main.jpg', '["/uploads/demo/g10030012_main.jpg","/uploads/demo/g10030012_1.jpg","/uploads/demo/g10030012_2.jpg"]',
 '<p>LDS 激光导航精准建图，4000Pa 大吸力轻松吸走地毯深处灰尘，扫拖一体，自动集尘版可 60 天免倒尘袋。</p><p><strong>产品亮点：</strong>LDS 激光导航；4000Pa 强劲吸力；扫拖一体 自动集尘。</p><img src="/uploads/demo/g10030012_1.jpg" style="width:100%"/><p>激光导航、4000Pa 大吸力、自动集尘，智能家电、品质生活。</p><img src="/uploads/demo/g10030012_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"版本","values":["标准版","自动集尘版"]}]', 1599.00, 1999.00, 488, 0, 1260, '台', '扫地机器人 扫拖一体 自动集尘 智能 北极星', 'ON_SALE', 0, 0, 1, 120);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030013, 10010302, 10020002, '北极星 桌面暖风机', '2 秒速热 · 倾倒断电 · 低噪运行', '/uploads/demo/g10030013_main.jpg', '["/uploads/demo/g10030013_main.jpg","/uploads/demo/g10030013_1.jpg","/uploads/demo/g10030013_2.jpg"]',
 '<p>PTC 陶瓷发热 2 秒出热风，桌下、浴室都能用；倾倒自动断电、过热保护，35dB 低噪运行不打扰睡眠。</p><p><strong>产品亮点：</strong>PTC 陶瓷 2 秒速热；倾倒自动断电；35dB 低噪运行。</p><img src="/uploads/demo/g10030013_1.jpg" style="width:100%"/><p>2 秒速热、倾倒断电、低噪运行，智能家电、品质生活。</p><img src="/uploads/demo/g10030013_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["云朵白","浅灰"]}]', 159.00, 199.00, 418, 0, 650, '台', '暖风机 取暖器 桌面 速热 北极星', 'ON_SALE', 0, 0, 0, 130);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030014, 10010303, 10020004, '拾光 高速负离子吹风机', '11 万转高速马达 · 负离子护发', '/uploads/demo/g10030014_main.jpg', '["/uploads/demo/g10030014_main.jpg","/uploads/demo/g10030014_1.jpg","/uploads/demo/g10030014_2.jpg"]',
 '<p>11 万转高速无刷马达，及肩长发 3 分钟速干；2 亿负离子与智能恒温协同，吹完发丝顺滑有光泽。</p><p><strong>产品亮点：</strong>11 万转高速马达；2 亿负离子护发；智能恒温不伤发。</p><img src="/uploads/demo/g10030014_1.jpg" style="width:100%"/><p>11 万转高速马达、负离子护发，智能家电、品质生活。</p><img src="/uploads/demo/g10030014_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["雾霾紫","冰川白"]}]', 269.00, 349.00, 829, 0, 2980, '台', '吹风机 负离子 高速 护发 拾光', 'ON_SALE', 0, 1, 0, 140);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030015, 10010401, 10020007, '简屿 男士精梳棉圆领 T 恤', '新疆长绒棉 · 亲肤透气 · 不易变形', '/uploads/demo/g10030015_main.jpg', '["/uploads/demo/g10030015_main.jpg","/uploads/demo/g10030015_1.jpg","/uploads/demo/g10030015_2.jpg"]',
 '<p>精选新疆长绒棉，230g 重磅面料挺括不透，亲肤透气；领口罗纹加固，反复洗涤也不易变形。</p><p><strong>产品亮点：</strong>新疆精梳长绒棉；亲肤透气 不闷汗；领口加固 不变形。</p><img src="/uploads/demo/g10030015_1.jpg" style="width:100%"/><p>新疆长绒棉、亲肤透气、不易变形，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030015_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["白色","黑色","藏青"]},{"name":"尺码","values":["M","L","XL"]}]', 69.00, 99.00, 2301, 0, 4600, '件', 'T恤 男装 纯棉 短袖 简屿', 'ON_SALE', 0, 1, 0, 150);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030016, 10010401, 10020007, '简屿 男士轻薄白鸭绒羽绒服', '90% 白鸭绒 · 轻至 280g · 可收纳', '/uploads/demo/g10030016_main.jpg', '["/uploads/demo/g10030016_main.jpg","/uploads/demo/g10030016_1.jpg","/uploads/demo/g10030016_2.jpg"]',
 '<p>90% 白鸭绒填充，整件仅重约 280g，可收纳进随附口袋；面料经防泼水处理，秋冬通勤一件搞定。</p><p><strong>产品亮点：</strong>90% 白鸭绒填充；轻至 280g；防泼水面料。</p><img src="/uploads/demo/g10030016_1.jpg" style="width:100%"/><p>90% 白鸭绒、轻至 280g、可收纳，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030016_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["经典黑","橄榄绿"]},{"name":"尺码","values":["L","XL","XXL"]}]', 399.00, 599.00, 1732, 0, 870, '件', '羽绒服 男装 轻薄 白鸭绒 简屿', 'ON_SALE', 1, 0, 0, 160);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030017, 10010402, 10020007, '简屿 法式碎花收腰连衣裙', '法式方领 · 收腰显瘦 · 垂感雪纺', '/uploads/demo/g10030017_main.jpg', '["/uploads/demo/g10030017_main.jpg","/uploads/demo/g10030017_1.jpg","/uploads/demo/g10030017_2.jpg"]',
 '<p>法式复古方领搭配收腰版型，垂感雪纺面料轻盈飘逸，约会、度假、日常通勤都很出片。</p><p><strong>产品亮点：</strong>法式复古方领；收腰显瘦版型；垂感雪纺面料。</p><img src="/uploads/demo/g10030017_1.jpg" style="width:100%"/><p>法式方领、收腰显瘦、垂感雪纺，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030017_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["碎花蓝","碎花粉"]},{"name":"尺码","values":["S","M","L"]}]', 189.00, 289.00, 1583, 0, 1530, '件', '连衣裙 女装 碎花 法式 收腰 简屿', 'ON_SALE', 1, 0, 1, 170);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030018, 10010402, 10020007, '简屿 女士羊毛针织开衫', '30% 羊毛混纺 · 软糯亲肤 · 百搭外搭', '/uploads/demo/g10030018_main.jpg', '["/uploads/demo/g10030018_main.jpg","/uploads/demo/g10030018_1.jpg","/uploads/demo/g10030018_2.jpg"]',
 '<p>30% 羊毛混纺纱线，软糯亲肤不扎人；宽松落肩版型，春秋可单穿，冬季内搭也合适。</p><p><strong>产品亮点：</strong>30% 羊毛混纺；软糯亲肤不扎；春秋百搭外搭。</p><img src="/uploads/demo/g10030018_1.jpg" style="width:100%"/><p>30% 羊毛混纺、软糯亲肤、百搭外搭，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030018_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["燕麦色","雾霾蓝"]},{"name":"尺码","values":["S","M","L"]}]', 259.00, 379.00, 1434, 0, 690, '件', '针织开衫 女装 羊毛 外套 简屿', 'ON_SALE', 0, 0, 0, 180);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030019, 10010403, 10020010, '迪越 云感缓震跑步鞋', '超临界发泡中底 · 轻弹缓震 · 透气网面', '/uploads/demo/g10030019_main.jpg', '["/uploads/demo/g10030019_main.jpg","/uploads/demo/g10030019_1.jpg","/uploads/demo/g10030019_2.jpg"]',
 '<p>超临界发泡中底带来柔软又回弹的脚感，单只仅重约 230g；飞织鞋面透气包裹，日常慢跑与长距离训练皆宜。</p><p><strong>产品亮点：</strong>超临界发泡中底；轻至 230g；飞织透气鞋面。</p><img src="/uploads/demo/g10030019_1.jpg" style="width:100%"/><p>超临界发泡中底、轻弹缓震、透气网面，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030019_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["黑白","荧光绿"]},{"name":"尺码","values":["40","41","42","43"]}]', 459.00, 599.00, 1980, 0, 3900, '双', '跑步鞋 运动鞋 缓震 透气 迪越', 'ON_SALE', 0, 1, 1, 190);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030020, 10010403, 10020010, '迪越 经典复古小白鞋', '头层牛皮鞋面 · 软弹鞋垫 · 百搭易打理', '/uploads/demo/g10030020_main.jpg', '["/uploads/demo/g10030020_main.jpg","/uploads/demo/g10030020_1.jpg","/uploads/demo/g10030020_2.jpg"]',
 '<p>头层牛皮鞋面细腻耐穿，配合软弹记忆鞋垫，久走不累；经典复古造型，牛仔裤、裙装都好搭。</p><p><strong>产品亮点：</strong>头层牛皮鞋面；软弹记忆鞋垫；百搭易打理。</p><img src="/uploads/demo/g10030020_1.jpg" style="width:100%"/><p>头层牛皮鞋面、软弹鞋垫、百搭易打理，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030020_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"尺码","values":["36","37","38","39","40"]}]', 299.00, 399.00, 1009, 0, 2100, '双', '小白鞋 板鞋 休闲鞋 百搭 迪越', 'ON_SALE', 1, 0, 0, 200);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030021, 10010404, 10020005, '木野 铝框万向轮拉杆箱', 'PC 镜面箱体 · 铝合金框 · 静音万向轮', '/uploads/demo/g10030021_main.jpg', '["/uploads/demo/g10030021_main.jpg","/uploads/demo/g10030021_1.jpg","/uploads/demo/g10030021_2.jpg"]',
 '<p>PC 镜面箱体搭配铝合金框架，抗压耐摔；静音万向飞机轮推行顺滑，TSA 海关锁出境旅行更安心。</p><p><strong>产品亮点：</strong>铝合金加固框架；静音万向飞机轮；TSA 海关密码锁。</p><img src="/uploads/demo/g10030021_1.jpg" style="width:100%"/><p>PC 镜面箱体、铝合金框、静音万向轮，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030021_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["雅士黑","奶茶色"]},{"name":"尺寸","values":["20 寸","24 寸"]}]', 499.00, 699.00, 1096, 0, 940, '个', '行李箱 拉杆箱 登机箱 铝框 木野', 'ON_SALE', 0, 0, 0, 210);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030022, 10010404, 10020005, '木野 通勤防泼水双肩包', '15.6 寸电脑仓 · 防泼水 · 减压背带', '/uploads/demo/g10030022_main.jpg', '["/uploads/demo/g10030022_main.jpg","/uploads/demo/g10030022_1.jpg","/uploads/demo/g10030022_2.jpg"]',
 '<p>独立加厚电脑仓可放 15.6 英寸笔记本，多隔层收纳有序；防泼水面料与透气减压背带，通勤出差都轻松。</p><p><strong>产品亮点：</strong>15.6 寸独立电脑仓；防泼水面料；透气减压背带。</p><img src="/uploads/demo/g10030022_1.jpg" style="width:100%"/><p>15.6 寸电脑仓、防泼水、减压背带，多色多码、百搭穿搭。</p><img src="/uploads/demo/g10030022_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["石墨黑","烟灰色"]}]', 199.00, 279.00, 181, 0, 1650, '个', '双肩包 电脑包 通勤 防泼水 木野', 'ON_SALE', 0, 0, 1, 220);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030023, 10010501, 10020004, '拾光 玻尿酸水润保湿精华', '5 重玻尿酸 · 深层补水 · 清爽不黏', '/uploads/demo/g10030023_main.jpg', '["/uploads/demo/g10030023_main.jpg","/uploads/demo/g10030023_1.jpg","/uploads/demo/g10030023_2.jpg"]',
 '<p>5 重分子量玻尿酸复配，从表层到深层逐层补水；质地清爽水润、吸收快，干皮油皮都适用。</p><p><strong>产品亮点：</strong>5 重玻尿酸复配；24 小时长效保湿；清爽质地不黏腻。</p><img src="/uploads/demo/g10030023_1.jpg" style="width:100%"/><p>5 重玻尿酸、深层补水、清爽不黏，温和配方、精致日常。</p><img src="/uploads/demo/g10030023_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"规格","values":["30ml","50ml"]}]', 199.00, 279.00, 592, 0, 4100, '瓶', '精华 玻尿酸 保湿 补水 护肤 拾光', 'ON_SALE', 0, 1, 1, 230);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030024, 10010501, 10020004, '拾光 氨基酸温和洁面乳', '氨基酸表活 · 弱酸性 · 洗后不紧绷', '/uploads/demo/g10030024_main.jpg', '["/uploads/demo/g10030024_main.jpg","/uploads/demo/g10030024_1.jpg","/uploads/demo/g10030024_2.jpg"]',
 '<p>氨基酸表活搭配弱酸性配方，泡沫绵密细腻，温和带走多余油脂与污垢，洗后清爽不紧绷。</p><p><strong>产品亮点：</strong>氨基酸温和表活；弱酸性亲肤配方；绵密泡沫 不紧绷。</p><img src="/uploads/demo/g10030024_1.jpg" style="width:100%"/><p>氨基酸表活、弱酸性、洗后不紧绷，温和配方、精致日常。</p><img src="/uploads/demo/g10030024_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[]', 69.00, 89.00, 84, 0, 2600, '支', '洗面奶 氨基酸 洁面 温和 拾光', 'ON_SALE', 0, 0, 0, 240);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030025, 10010502, 10020004, '拾光 丝绒雾面哑光口红', '丝绒雾面 · 显色持久 · 不拔干', '/uploads/demo/g10030025_main.jpg', '["/uploads/demo/g10030025_main.jpg","/uploads/demo/g10030025_1.jpg","/uploads/demo/g10030025_2.jpg"]',
 '<p>丝绒雾面质地一抹显色，添加植物油成分，持久不拔干；三款日常色号，通勤约会都能驾驭。</p><p><strong>产品亮点：</strong>丝绒雾面质地；一抹显色 持久不脱；添加植物油 不拔干。</p><img src="/uploads/demo/g10030025_1.jpg" style="width:100%"/><p>丝绒雾面、显色持久、不拔干，温和配方、精致日常。</p><img src="/uploads/demo/g10030025_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"色号","values":["#01 正红","#02 豆沙","#03 枫叶棕"]}]', 129.00, 169.00, 540, 0, 1890, '支', '口红 哑光 丝绒 显色 彩妆 拾光', 'ON_SALE', 1, 0, 0, 250);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030026, 10010503, 10020001, '青禾 山茶花控油洗发水', '山茶花精萃 · 控油蓬松 · 无硅油', '/uploads/demo/g10030026_main.jpg', '["/uploads/demo/g10030026_main.jpg","/uploads/demo/g10030026_1.jpg","/uploads/demo/g10030026_2.jpg"]',
 '<p>山茶花植萃配合氨基酸清洁成分，温和控油，洗后发根蓬松清爽；无硅油配方，减轻头皮负担。</p><p><strong>产品亮点：</strong>山茶花植萃；控油蓬松 72h；无硅油配方。</p><img src="/uploads/demo/g10030026_1.jpg" style="width:100%"/><p>山茶花精萃、控油蓬松、无硅油，温和配方、精致日常。</p><img src="/uploads/demo/g10030026_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"规格","values":["500ml","750ml"]}]', 59.00, 79.00, 863, 0, 1200, '瓶', '洗发水 控油 蓬松 无硅油 青禾', 'ON_SALE', 0, 0, 0, 260);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030027, 10010601, 10020001, '青禾 每日坚果混合装', '6 种坚果果干 · 科学配比 · 独立小袋', '/uploads/demo/g10030027_main.jpg', '["/uploads/demo/g10030027_main.jpg","/uploads/demo/g10030027_1.jpg","/uploads/demo/g10030027_2.jpg"]',
 '<p>核桃仁、巴旦木、腰果、榛子、蔓越莓干、蓝莓干 6 种搭配，每袋 25g 科学配比，独立充氮包装锁住新鲜。</p><p><strong>产品亮点：</strong>6 种坚果果干；每日 25g 科学配比；独立充氮小包装。</p><img src="/uploads/demo/g10030027_1.jpg" style="width:100%"/><p>6 种坚果果干、科学配比、独立小袋，新鲜好味、安心入口。</p><img src="/uploads/demo/g10030027_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"规格","values":["15 袋装","30 袋装"]}]', 69.00, 99.00, 793, 0, 4900, '盒', '每日坚果 坚果 零食 健康 青禾', 'ON_SALE', 0, 1, 1, 270);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030028, 10010601, 10020001, '青禾 风干手撕牛肉干', '精选牛后腿肉 · 高蛋白 · 越嚼越香', '/uploads/demo/g10030028_main.jpg', '["/uploads/demo/g10030028_main.jpg","/uploads/demo/g10030028_1.jpg","/uploads/demo/g10030028_2.jpg"]',
 '<p>精选牛后腿肉，传统风干工艺慢慢烘制，肉质紧实有嚼劲，越嚼越香，追剧加班的高蛋白小零食。</p><p><strong>产品亮点：</strong>精选牛后腿肉；高蛋白 低脂肪；传统风干工艺。</p><img src="/uploads/demo/g10030028_1.jpg" style="width:100%"/><p>精选牛后腿肉、高蛋白、越嚼越香，新鲜好味、安心入口。</p><img src="/uploads/demo/g10030028_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"口味","values":["原味","香辣","五香"]}]', 39.90, 54.90, 894, 0, 3300, '袋', '牛肉干 零食 高蛋白 手撕 青禾', 'ON_SALE', 0, 0, 0, 280);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030029, 10010602, 10020001, '青禾 明前西湖龙井 250g', '明前头采 · 豆香馥郁 · 罐装密封', '/uploads/demo/g10030029_main.jpg', '["/uploads/demo/g10030029_main.jpg","/uploads/demo/g10030029_1.jpg","/uploads/demo/g10030029_2.jpg"]',
 '<p>明前头采嫩芽，手工炒制，豆香馥郁、滋味鲜爽回甘；铁罐密封包装，自饮送礼都体面。</p><p><strong>产品亮点：</strong>明前头采嫩芽；豆香馥郁 回甘；铁罐密封保鲜。</p><img src="/uploads/demo/g10030029_1.jpg" style="width:100%"/><p>明前头采、豆香馥郁、罐装密封，新鲜好味、安心入口。</p><img src="/uploads/demo/g10030029_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[]', 168.00, 238.00, 132, 0, 560, '罐', '龙井 绿茶 明前 茶叶 青禾', 'ON_SALE', 0, 0, 1, 290);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030030, 10010602, 10020001, '青禾 冷萃冻干挂耳咖啡', '-40°C 冻干 · 3 秒速溶 · 0 蔗糖', '/uploads/demo/g10030030_main.jpg', '["/uploads/demo/g10030030_main.jpg","/uploads/demo/g10030030_1.jpg","/uploads/demo/g10030030_2.jpg"]',
 '<p>-40°C 低温冷萃冻干，最大程度保留咖啡风味，冷水、牛奶 3 秒速溶，0 蔗糖 0 植脂末更轻负担。</p><p><strong>产品亮点：</strong>-40°C 冷萃冻干；3 秒冷热速溶；0 蔗糖 0 植脂末。</p><img src="/uploads/demo/g10030030_1.jpg" style="width:100%"/><p>-40°C 冻干、3 秒速溶、0 蔗糖，新鲜好味、安心入口。</p><img src="/uploads/demo/g10030030_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"规格","values":["10 颗装","20 颗装"]}]', 49.00, 69.00, 583, 0, 1780, '盒', '咖啡 冻干 速溶 挂耳 青禾', 'ON_SALE', 1, 0, 0, 300);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030031, 10010603, 10020001, '青禾 赣南脐橙 新鲜当季', '产地直发 · 果大汁多 · 坏果包赔', '/uploads/demo/g10030031_main.jpg', '["/uploads/demo/g10030031_main.jpg","/uploads/demo/g10030031_1.jpg","/uploads/demo/g10030031_2.jpg"]',
 '<p>赣南原产地果园现摘现发，果大皮薄、汁水充足，酸甜比恰到好处；运输途中如有坏果，按个包赔。</p><p><strong>产品亮点：</strong>赣南原产地直发；果大皮薄 汁水多；坏果包赔。</p><img src="/uploads/demo/g10030031_1.jpg" style="width:100%"/><p>产地直发、果大汁多、坏果包赔，新鲜好味、安心入口。</p><img src="/uploads/demo/g10030031_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"规格","values":["5 斤装","10 斤装"]}]', 39.90, 59.90, 513, 0, 3600, '箱', '脐橙 橙子 水果 新鲜 赣南 青禾', 'ON_SALE', 0, 1, 0, 310);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030032, 10010701, 10020005, '木野 60 支长绒棉四件套', '60 支长绒棉 · 贡缎工艺 · 丝滑亲肤', '/uploads/demo/g10030032_main.jpg', '["/uploads/demo/g10030032_main.jpg","/uploads/demo/g10030032_1.jpg","/uploads/demo/g10030032_2.jpg"]',
 '<p>60 支新疆长绒棉，贡缎织法丝滑亲肤、光泽柔和；达到 A 类婴幼儿标准，家人睡得更安心。</p><p><strong>产品亮点：</strong>60 支新疆长绒棉；贡缎工艺 丝滑；A 类婴幼儿标准。</p><img src="/uploads/demo/g10030032_1.jpg" style="width:100%"/><p>60 支长绒棉、贡缎工艺、丝滑亲肤，温暖居所、简约美学。</p><img src="/uploads/demo/g10030032_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["月白","雾霾蓝"]},{"name":"尺寸","values":["1.5m 床","1.8m 床"]}]', 399.00, 599.00, 859, 0, 1100, '套', '四件套 床品 长绒棉 家纺 木野', 'ON_SALE', 0, 0, 1, 320);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030033, 10010702, 10020005, '木野 北欧实木落地灯', '实木灯杆 · 布艺灯罩 · 三档调光', '/uploads/demo/g10030033_main.jpg', '["/uploads/demo/g10030033_main.jpg","/uploads/demo/g10030033_1.jpg","/uploads/demo/g10030033_2.jpg"]',
 '<p>榉木实木灯杆搭配亚麻布艺灯罩，光线柔和不刺眼；三档无极调光，阅读、观影、氛围照明随心切换。</p><p><strong>产品亮点：</strong>榉木实木灯杆；亚麻布艺灯罩；三档无极调光。</p><img src="/uploads/demo/g10030033_1.jpg" style="width:100%"/><p>实木灯杆、布艺灯罩、三档调光，温暖居所、简约美学。</p><img src="/uploads/demo/g10030033_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"颜色","values":["原木色","胡桃色"]}]', 259.00, 349.00, 303, 0, 420, '台', '落地灯 北欧 实木 客厅 卧室 木野', 'ON_SALE', 1, 0, 0, 330);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030034, 10010703, 10020005, '木野 可折叠收纳箱 3 只装', '一秒折叠 · 可叠放 · 承重 30kg', '/uploads/demo/g10030034_main.jpg', '["/uploads/demo/g10030034_main.jpg","/uploads/demo/g10030034_1.jpg","/uploads/demo/g10030034_2.jpg"]',
 '<p>一组 3 只，一秒折叠不占空间，展开后可多层叠放；加厚 PP 材质单只承重 30kg，衣物、玩具、杂物统统装下。</p><p><strong>产品亮点：</strong>一秒折叠省空间；多层叠放；加厚 PP 承重 30kg。</p><img src="/uploads/demo/g10030034_1.jpg" style="width:100%"/><p>一秒折叠、可叠放、承重 30kg，温暖居所、简约美学。</p><img src="/uploads/demo/g10030034_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[]', 89.00, 129.00, 180, 0, 2400, '组', '收纳箱 折叠 整理箱 衣物收纳 木野', 'ON_SALE', 0, 0, 0, 340);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030035, 10010801, 10020006, '远山 可调节家用哑铃', '一秒调重 · 防滑握把 · 安全卡扣', '/uploads/demo/g10030035_main.jpg', '["/uploads/demo/g10030035_main.jpg","/uploads/demo/g10030035_1.jpg","/uploads/demo/g10030035_2.jpg"]',
 '<p>旋钮式一秒调重，一副顶多副；防滑橡胶握把手感扎实，安全锁扣设计防止配重片松脱，在家也能练出好身材。</p><p><strong>产品亮点：</strong>旋钮一秒调重；防滑橡胶握把；安全锁扣设计。</p><img src="/uploads/demo/g10030035_1.jpg" style="width:100%"/><p>一秒调重、防滑握把、安全卡扣，向山而行、自在运动。</p><img src="/uploads/demo/g10030035_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"重量","values":["10kg","20kg"]}]', 299.00, 399.00, 679, 0, 760, '副', '哑铃 健身 可调节 家用 远山', 'ON_SALE', 0, 0, 1, 350);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030036, 10010802, 10020006, '远山 双层防暴雨露营帐篷', '3000mm 防水 · 双层通风 · 快速搭建', '/uploads/demo/g10030036_main.jpg', '["/uploads/demo/g10030036_main.jpg","/uploads/demo/g10030036_1.jpg","/uploads/demo/g10030036_2.jpg"]',
 '<p>3000mm 防水指数外帐，暴雨天也能安心入睡；双层结构通风防结露，3 分钟快速搭建，新手也能轻松上手。</p><p><strong>产品亮点：</strong>3000mm 防水指数；双层通风防结露；3 分钟快速搭建。</p><img src="/uploads/demo/g10030036_1.jpg" style="width:100%"/><p>3000mm 防水、双层通风、快速搭建，向山而行、自在运动。</p><img src="/uploads/demo/g10030036_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[{"name":"人数","values":["2 人","3-4 人"]}]', 459.00, 619.00, 609, 0, 980, '顶', '帐篷 露营 防雨 户外 远山', 'ON_SALE', 1, 0, 1, 360);
INSERT INTO gds_goods (id, category_id, brand_id, name, subtitle, pic, album, detail, spec_items, price, original_price, stock, sales, virtual_sales, unit, keywords, status, is_new, is_hot, is_recommend, sort) VALUES
(10030037, 10010803, 10020010, '迪越 7 号室内外比赛篮球', 'PU 吸湿面料 · 手感细腻 · 耐磨防滑', '/uploads/demo/g10030037_main.jpg', '["/uploads/demo/g10030037_main.jpg","/uploads/demo/g10030037_1.jpg","/uploads/demo/g10030037_2.jpg"]',
 '<p>PU 吸湿超纤面料手感细腻，室内木地板、室外水泥场都耐磨防滑；深沟设计更好控球，标准 7 号比赛用球。</p><p><strong>产品亮点：</strong>PU 吸湿超纤面料；标准 7 号比赛用球；深沟设计 好控球。</p><img src="/uploads/demo/g10030037_1.jpg" style="width:100%"/><p>PU 吸湿面料、手感细腻、耐磨防滑，向山而行、自在运动。</p><img src="/uploads/demo/g10030037_2.jpg" style="width:100%"/><p>本店商品均为正品行货，支持 7 天无理由退换（不影响二次销售），下单后 48 小时内发货。</p>',
 '[]', 149.00, 199.00, 333, 0, 1450, '个', '篮球 7号球 比赛 室内外 迪越', 'ON_SALE', 0, 0, 0, 370);

-- ======================== 商品 SKU ========================
INSERT INTO gds_sku (id, goods_id, spec_values, spec_text, code, pic, price, original_price, stock, sales) VALUES
(10040001, 10030001, '["曜石黑","8+256G"]', '颜色:曜石黑;存储:8+256G', 'PH-X9P-01', '/uploads/demo/g10030001_main.jpg', 3299.00, 3599.00, 404, 0),
(10040002, 10030001, '["曜石黑","12+512G"]', '颜色:曜石黑;存储:12+512G', 'PH-X9P-02', '/uploads/demo/g10030001_main.jpg', 3799.00, 4099.00, 277, 0),
(10040003, 10030001, '["月光白","8+256G"]', '颜色:月光白;存储:8+256G', 'PH-X9P-03', '/uploads/demo/g10030001_main.jpg', 3299.00, 3599.00, 150, 0),
(10040004, 10030001, '["月光白","12+512G"]', '颜色:月光白;存储:12+512G', 'PH-X9P-04', '/uploads/demo/g10030001_main.jpg', 3799.00, 4099.00, 23, 0),
(10040005, 10030001, '["远峰蓝","8+256G"]', '颜色:远峰蓝;存储:8+256G', 'PH-X9P-05', '/uploads/demo/g10030001_main.jpg', 3299.00, 3599.00, 377, 0),
(10040006, 10030001, '["远峰蓝","12+512G"]', '颜色:远峰蓝;存储:12+512G', 'PH-X9P-06', '/uploads/demo/g10030001_main.jpg', 3799.00, 4099.00, 250, 0),
(10040007, 10030002, '["星空灰","6+128G"]', '颜色:星空灰;存储:6+128G', 'PH-N12-01', '/uploads/demo/g10030002_main.jpg', 1299.00, 1499.00, 299, 0),
(10040008, 10030002, '["星空灰","8+256G"]', '颜色:星空灰;存储:8+256G', 'PH-N12-02', '/uploads/demo/g10030002_main.jpg', 1499.00, 1699.00, 172, 0),
(10040009, 10030002, '["薄荷绿","6+128G"]', '颜色:薄荷绿;存储:6+128G', 'PH-N12-03', '/uploads/demo/g10030002_main.jpg', 1299.00, 1499.00, 45, 0),
(10040010, 10030002, '["薄荷绿","8+256G"]', '颜色:薄荷绿;存储:8+256G', 'PH-N12-04', '/uploads/demo/g10030002_main.jpg', 1499.00, 1699.00, 399, 0),
(10040011, 10030003, '["象牙白"]', '颜色:象牙白', 'AU-AB3-01', '/uploads/demo/g10030003_main.jpg', 399.00, 499.00, 229, 0),
(10040012, 10030003, '["石墨黑"]', '颜色:石墨黑', 'AU-AB3-02', '/uploads/demo/g10030003_main.jpg', 399.00, 499.00, 102, 0),
(10040013, 10030004, '["深海蓝"]', '颜色:深海蓝', 'AU-CUBE-01', '/uploads/demo/g10030004_main.jpg', 259.00, 319.00, 194, 0),
(10040014, 10030004, '["珊瑚红"]', '颜色:珊瑚红', 'AU-CUBE-02', '/uploads/demo/g10030004_main.jpg', 259.00, 319.00, 67, 0),
(10040015, 10030004, '["岩石灰"]', '颜色:岩石灰', 'AU-CUBE-03', '/uploads/demo/g10030004_main.jpg', 259.00, 319.00, 421, 0),
(10040016, 10030005, '["午夜黑","氟橡胶"]', '颜色:午夜黑;表带:氟橡胶', 'WT-S2-01', '/uploads/demo/g10030005_main.jpg', 899.00, 1099.00, 382, 0),
(10040017, 10030005, '["午夜黑","米兰尼斯"]', '颜色:午夜黑;表带:米兰尼斯', 'WT-S2-02', '/uploads/demo/g10030005_main.jpg', 999.00, 1199.00, 255, 0),
(10040018, 10030005, '["晨雾粉","氟橡胶"]', '颜色:晨雾粉;表带:氟橡胶', 'WT-S2-03', '/uploads/demo/g10030005_main.jpg', 899.00, 1099.00, 128, 0),
(10040019, 10030005, '["晨雾粉","米兰尼斯"]', '颜色:晨雾粉;表带:米兰尼斯', 'WT-S2-04', '/uploads/demo/g10030005_main.jpg', 999.00, 1199.00, 482, 0),
(10040020, 10030006, '["i5/16G/512G","皓月银"]', '配置:i5/16G/512G;颜色:皓月银', 'NB-B14-01', '/uploads/demo/g10030006_main.jpg', 4999.00, 5499.00, 312, 0),
(10040021, 10030006, '["i5/16G/512G","深空灰"]', '配置:i5/16G/512G;颜色:深空灰', 'NB-B14-02', '/uploads/demo/g10030006_main.jpg', 4999.00, 5499.00, 185, 0),
(10040022, 10030006, '["i7/16G/1TB","皓月银"]', '配置:i7/16G/1TB;颜色:皓月银', 'NB-B14-03', '/uploads/demo/g10030006_main.jpg', 5899.00, 6399.00, 58, 0),
(10040023, 10030006, '["i7/16G/1TB","深空灰"]', '配置:i7/16G/1TB;颜色:深空灰', 'NB-B14-04', '/uploads/demo/g10030006_main.jpg', 5899.00, 6399.00, 412, 0),
(10040024, 10030007, '["红轴"]', '轴体:红轴', 'KB-K87-01', '/uploads/demo/g10030007_main.jpg', 299.00, 379.00, 242, 0),
(10040025, 10030007, '["茶轴"]', '轴体:茶轴', 'KB-K87-02', '/uploads/demo/g10030007_main.jpg', 299.00, 379.00, 115, 0),
(10040026, 10030007, '["青轴"]', '轴体:青轴', 'KB-K87-03', '/uploads/demo/g10030007_main.jpg', 299.00, 379.00, 469, 0),
(10040027, 10030008, '["曜石黑"]', '颜色:曜石黑', 'MS-M3-01', '/uploads/demo/g10030008_main.jpg', 79.00, 99.00, 430, 0),
(10040028, 10030008, '["珍珠白"]', '颜色:珍珠白', 'MS-M3-02', '/uploads/demo/g10030008_main.jpg', 79.00, 99.00, 303, 0),
(10040029, 10030008, '["樱花粉"]', '颜色:樱花粉', 'MS-M3-03', '/uploads/demo/g10030008_main.jpg', 79.00, 99.00, 176, 0),
(10040030, 10030009, '[]', '', 'ST-A5-01', '/uploads/demo/g10030009_main.jpg', 29.90, 39.90, 137, 0),
(10040031, 10030010, '["奶油白"]', '颜色:奶油白', 'KT-AF5-01', '/uploads/demo/g10030010_main.jpg', 299.00, 399.00, 360, 0),
(10040032, 10030010, '["薄荷绿"]', '颜色:薄荷绿', 'KT-AF5-02', '/uploads/demo/g10030010_main.jpg', 299.00, 399.00, 233, 0),
(10040033, 10030011, '[]', '', 'KT-BL1-01', '/uploads/demo/g10030011_main.jpg', 499.00, 699.00, 325, 0),
(10040034, 10030012, '["标准版"]', '版本:标准版', 'HA-S7-01', '/uploads/demo/g10030012_main.jpg', 1599.00, 1999.00, 67, 0),
(10040035, 10030012, '["自动集尘版"]', '版本:自动集尘版', 'HA-S7-02', '/uploads/demo/g10030012_main.jpg', 2299.00, 2699.00, 421, 0),
(10040036, 10030013, '["云朵白"]', '颜色:云朵白', 'HA-HT2-01', '/uploads/demo/g10030013_main.jpg', 159.00, 199.00, 32, 0),
(10040037, 10030013, '["浅灰"]', '颜色:浅灰', 'HA-HT2-02', '/uploads/demo/g10030013_main.jpg', 159.00, 199.00, 386, 0),
(10040038, 10030014, '["雾霾紫"]', '颜色:雾霾紫', 'PC-HD1-01', '/uploads/demo/g10030014_main.jpg', 269.00, 349.00, 478, 0),
(10040039, 10030014, '["冰川白"]', '颜色:冰川白', 'PC-HD1-02', '/uploads/demo/g10030014_main.jpg', 269.00, 349.00, 351, 0),
(10040040, 10030015, '["白色","M"]', '颜色:白色;尺码:M', 'MW-T01-01', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 443, 0),
(10040041, 10030015, '["白色","L"]', '颜色:白色;尺码:L', 'MW-T01-02', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 316, 0),
(10040042, 10030015, '["白色","XL"]', '颜色:白色;尺码:XL', 'MW-T01-03', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 189, 0),
(10040043, 10030015, '["黑色","M"]', '颜色:黑色;尺码:M', 'MW-T01-04', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 62, 0),
(10040044, 10030015, '["黑色","L"]', '颜色:黑色;尺码:L', 'MW-T01-05', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 416, 0),
(10040045, 10030015, '["黑色","XL"]', '颜色:黑色;尺码:XL', 'MW-T01-06', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 289, 0),
(10040046, 10030015, '["藏青","M"]', '颜色:藏青;尺码:M', 'MW-T01-07', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 162, 0),
(10040047, 10030015, '["藏青","L"]', '颜色:藏青;尺码:L', 'MW-T01-08', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 35, 0),
(10040048, 10030015, '["藏青","XL"]', '颜色:藏青;尺码:XL', 'MW-T01-09', '/uploads/demo/g10030015_main.jpg', 69.00, 99.00, 389, 0),
(10040049, 10030016, '["经典黑","L"]', '颜色:经典黑;尺码:L', 'MW-D02-01', '/uploads/demo/g10030016_main.jpg', 399.00, 599.00, 45, 0),
(10040050, 10030016, '["经典黑","XL"]', '颜色:经典黑;尺码:XL', 'MW-D02-02', '/uploads/demo/g10030016_main.jpg', 399.00, 599.00, 399, 0),
(10040051, 10030016, '["经典黑","XXL"]', '颜色:经典黑;尺码:XXL', 'MW-D02-03', '/uploads/demo/g10030016_main.jpg', 399.00, 599.00, 272, 0),
(10040052, 10030016, '["橄榄绿","L"]', '颜色:橄榄绿;尺码:L', 'MW-D02-04', '/uploads/demo/g10030016_main.jpg', 399.00, 599.00, 145, 0),
(10040053, 10030016, '["橄榄绿","XL"]', '颜色:橄榄绿;尺码:XL', 'MW-D02-05', '/uploads/demo/g10030016_main.jpg', 399.00, 599.00, 499, 0),
(10040054, 10030016, '["橄榄绿","XXL"]', '颜色:橄榄绿;尺码:XXL', 'MW-D02-06', '/uploads/demo/g10030016_main.jpg', 399.00, 599.00, 372, 0),
(10040055, 10030017, '["碎花蓝","S"]', '颜色:碎花蓝;尺码:S', 'WW-D01-01', '/uploads/demo/g10030017_main.jpg', 189.00, 289.00, 421, 0),
(10040056, 10030017, '["碎花蓝","M"]', '颜色:碎花蓝;尺码:M', 'WW-D01-02', '/uploads/demo/g10030017_main.jpg', 189.00, 289.00, 294, 0),
(10040057, 10030017, '["碎花蓝","L"]', '颜色:碎花蓝;尺码:L', 'WW-D01-03', '/uploads/demo/g10030017_main.jpg', 189.00, 289.00, 167, 0),
(10040058, 10030017, '["碎花粉","S"]', '颜色:碎花粉;尺码:S', 'WW-D01-04', '/uploads/demo/g10030017_main.jpg', 189.00, 289.00, 40, 0),
(10040059, 10030017, '["碎花粉","M"]', '颜色:碎花粉;尺码:M', 'WW-D01-05', '/uploads/demo/g10030017_main.jpg', 189.00, 289.00, 394, 0),
(10040060, 10030017, '["碎花粉","L"]', '颜色:碎花粉;尺码:L', 'WW-D01-06', '/uploads/demo/g10030017_main.jpg', 189.00, 289.00, 267, 0),
(10040061, 10030018, '["燕麦色","S"]', '颜色:燕麦色;尺码:S', 'WW-C02-01', '/uploads/demo/g10030018_main.jpg', 259.00, 379.00, 316, 0),
(10040062, 10030018, '["燕麦色","M"]', '颜色:燕麦色;尺码:M', 'WW-C02-02', '/uploads/demo/g10030018_main.jpg', 259.00, 379.00, 189, 0),
(10040063, 10030018, '["燕麦色","L"]', '颜色:燕麦色;尺码:L', 'WW-C02-03', '/uploads/demo/g10030018_main.jpg', 259.00, 379.00, 62, 0),
(10040064, 10030018, '["雾霾蓝","S"]', '颜色:雾霾蓝;尺码:S', 'WW-C02-04', '/uploads/demo/g10030018_main.jpg', 259.00, 379.00, 416, 0),
(10040065, 10030018, '["雾霾蓝","M"]', '颜色:雾霾蓝;尺码:M', 'WW-C02-05', '/uploads/demo/g10030018_main.jpg', 259.00, 379.00, 289, 0),
(10040066, 10030018, '["雾霾蓝","L"]', '颜色:雾霾蓝;尺码:L', 'WW-C02-06', '/uploads/demo/g10030018_main.jpg', 259.00, 379.00, 162, 0),
(10040067, 10030019, '["黑白","40"]', '颜色:黑白;尺码:40', 'SH-R01-01', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 211, 0),
(10040068, 10030019, '["黑白","41"]', '颜色:黑白;尺码:41', 'SH-R01-02', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 84, 0),
(10040069, 10030019, '["黑白","42"]', '颜色:黑白;尺码:42', 'SH-R01-03', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 438, 0),
(10040070, 10030019, '["黑白","43"]', '颜色:黑白;尺码:43', 'SH-R01-04', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 311, 0),
(10040071, 10030019, '["荧光绿","40"]', '颜色:荧光绿;尺码:40', 'SH-R01-05', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 184, 0),
(10040072, 10030019, '["荧光绿","41"]', '颜色:荧光绿;尺码:41', 'SH-R01-06', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 57, 0),
(10040073, 10030019, '["荧光绿","42"]', '颜色:荧光绿;尺码:42', 'SH-R01-07', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 411, 0),
(10040074, 10030019, '["荧光绿","43"]', '颜色:荧光绿;尺码:43', 'SH-R01-08', '/uploads/demo/g10030019_main.jpg', 459.00, 599.00, 284, 0),
(10040075, 10030020, '["36"]', '尺码:36', 'SH-W02-01', '/uploads/demo/g10030020_main.jpg', 299.00, 399.00, 71, 0),
(10040076, 10030020, '["37"]', '尺码:37', 'SH-W02-02', '/uploads/demo/g10030020_main.jpg', 299.00, 399.00, 425, 0),
(10040077, 10030020, '["38"]', '尺码:38', 'SH-W02-03', '/uploads/demo/g10030020_main.jpg', 299.00, 399.00, 298, 0),
(10040078, 10030020, '["39"]', '尺码:39', 'SH-W02-04', '/uploads/demo/g10030020_main.jpg', 299.00, 399.00, 171, 0),
(10040079, 10030020, '["40"]', '尺码:40', 'SH-W02-05', '/uploads/demo/g10030020_main.jpg', 299.00, 399.00, 44, 0),
(10040080, 10030021, '["雅士黑","20 寸"]', '颜色:雅士黑;尺寸:20 寸', 'BG-L01-01', '/uploads/demo/g10030021_main.jpg', 499.00, 699.00, 224, 0),
(10040081, 10030021, '["雅士黑","24 寸"]', '颜色:雅士黑;尺寸:24 寸', 'BG-L01-02', '/uploads/demo/g10030021_main.jpg', 599.00, 799.00, 97, 0),
(10040082, 10030021, '["奶茶色","20 寸"]', '颜色:奶茶色;尺寸:20 寸', 'BG-L01-03', '/uploads/demo/g10030021_main.jpg', 499.00, 699.00, 451, 0),
(10040083, 10030021, '["奶茶色","24 寸"]', '颜色:奶茶色;尺寸:24 寸', 'BG-L01-04', '/uploads/demo/g10030021_main.jpg', 599.00, 799.00, 324, 0),
(10040084, 10030022, '["石墨黑"]', '颜色:石墨黑', 'BG-B02-01', '/uploads/demo/g10030022_main.jpg', 199.00, 279.00, 154, 0),
(10040085, 10030022, '["烟灰色"]', '颜色:烟灰色', 'BG-B02-02', '/uploads/demo/g10030022_main.jpg', 199.00, 279.00, 27, 0),
(10040086, 10030023, '["30ml"]', '规格:30ml', 'BT-S01-01', '/uploads/demo/g10030023_main.jpg', 199.00, 279.00, 119, 0),
(10040087, 10030023, '["50ml"]', '规格:50ml', 'BT-S01-02', '/uploads/demo/g10030023_main.jpg', 299.00, 379.00, 473, 0),
(10040088, 10030024, '[]', '', 'BT-C02-01', '/uploads/demo/g10030024_main.jpg', 69.00, 89.00, 84, 0),
(10040089, 10030025, '["#01 正红"]', '色号:#01 正红', 'BT-L03-01', '/uploads/demo/g10030025_main.jpg', 129.00, 169.00, 307, 0),
(10040090, 10030025, '["#02 豆沙"]', '色号:#02 豆沙', 'BT-L03-02', '/uploads/demo/g10030025_main.jpg', 129.00, 169.00, 180, 0),
(10040091, 10030025, '["#03 枫叶棕"]', '色号:#03 枫叶棕', 'BT-L03-03', '/uploads/demo/g10030025_main.jpg', 129.00, 169.00, 53, 0),
(10040092, 10030026, '["500ml"]', '规格:500ml', 'BT-H04-01', '/uploads/demo/g10030026_main.jpg', 59.00, 79.00, 495, 0),
(10040093, 10030026, '["750ml"]', '规格:750ml', 'BT-H04-02', '/uploads/demo/g10030026_main.jpg', 79.00, 99.00, 368, 0),
(10040094, 10030027, '["15 袋装"]', '规格:15 袋装', 'FD-N01-01', '/uploads/demo/g10030027_main.jpg', 69.00, 99.00, 460, 0),
(10040095, 10030027, '["30 袋装"]', '规格:30 袋装', 'FD-N01-02', '/uploads/demo/g10030027_main.jpg', 129.00, 159.00, 333, 0),
(10040096, 10030028, '["原味"]', '口味:原味', 'FD-J02-01', '/uploads/demo/g10030028_main.jpg', 39.90, 54.90, 425, 0),
(10040097, 10030028, '["香辣"]', '口味:香辣', 'FD-J02-02', '/uploads/demo/g10030028_main.jpg', 39.90, 54.90, 298, 0),
(10040098, 10030028, '["五香"]', '口味:五香', 'FD-J02-03', '/uploads/demo/g10030028_main.jpg', 39.90, 54.90, 171, 0),
(10040099, 10030029, '[]', '', 'FD-T03-01', '/uploads/demo/g10030029_main.jpg', 168.00, 238.00, 132, 0),
(10040100, 10030030, '["10 颗装"]', '规格:10 颗装', 'FD-C04-01', '/uploads/demo/g10030030_main.jpg', 49.00, 69.00, 355, 0),
(10040101, 10030030, '["20 颗装"]', '规格:20 颗装', 'FD-C04-02', '/uploads/demo/g10030030_main.jpg', 89.00, 109.00, 228, 0),
(10040102, 10030031, '["5 斤装"]', '规格:5 斤装', 'FR-O01-01', '/uploads/demo/g10030031_main.jpg', 39.90, 59.90, 320, 0),
(10040103, 10030031, '["10 斤装"]', '规格:10 斤装', 'FR-O01-02', '/uploads/demo/g10030031_main.jpg', 69.90, 89.90, 193, 0),
(10040104, 10030032, '["月白","1.5m 床"]', '颜色:月白;尺寸:1.5m 床', 'HM-B01-01', '/uploads/demo/g10030032_main.jpg', 399.00, 599.00, 285, 0),
(10040105, 10030032, '["月白","1.8m 床"]', '颜色:月白;尺寸:1.8m 床', 'HM-B01-02', '/uploads/demo/g10030032_main.jpg', 459.00, 659.00, 158, 0),
(10040106, 10030032, '["雾霾蓝","1.5m 床"]', '颜色:雾霾蓝;尺寸:1.5m 床', 'HM-B01-03', '/uploads/demo/g10030032_main.jpg', 399.00, 599.00, 31, 0),
(10040107, 10030032, '["雾霾蓝","1.8m 床"]', '颜色:雾霾蓝;尺寸:1.8m 床', 'HM-B01-04', '/uploads/demo/g10030032_main.jpg', 459.00, 659.00, 385, 0),
(10040108, 10030033, '["原木色"]', '颜色:原木色', 'HM-L02-01', '/uploads/demo/g10030033_main.jpg', 259.00, 349.00, 215, 0),
(10040109, 10030033, '["胡桃色"]', '颜色:胡桃色', 'HM-L02-02', '/uploads/demo/g10030033_main.jpg', 259.00, 349.00, 88, 0),
(10040110, 10030034, '[]', '', 'HM-S03-01', '/uploads/demo/g10030034_main.jpg', 89.00, 129.00, 180, 0),
(10040111, 10030035, '["10kg"]', '重量:10kg', 'SP-D01-01', '/uploads/demo/g10030035_main.jpg', 299.00, 399.00, 403, 0),
(10040112, 10030035, '["20kg"]', '重量:20kg', 'SP-D01-02', '/uploads/demo/g10030035_main.jpg', 499.00, 599.00, 276, 0),
(10040113, 10030036, '["2 人"]', '人数:2 人', 'OD-T01-01', '/uploads/demo/g10030036_main.jpg', 459.00, 619.00, 368, 0),
(10040114, 10030036, '["3-4 人"]', '人数:3-4 人', 'OD-T01-02', '/uploads/demo/g10030036_main.jpg', 659.00, 819.00, 241, 0),
(10040115, 10030037, '[]', '', 'SP-B01-01', '/uploads/demo/g10030037_main.jpg', 149.00, 199.00, 333, 0);

-- ======================== 首页轮播 ========================
INSERT INTO mkt_banner (id, title, image, link_type, link_value, position, sort, status) VALUES
(10050001, '星澜 X9 Pro 新品首发', '/uploads/demo/banner_1.jpg', 'GOODS', '10030001', 'HOME', 1, 1),
(10050002, '秋冬焕新季 女装上新', '/uploads/demo/banner_2.jpg', 'CATEGORY', '10010402', 'HOME', 2, 1),
(10050003, '零食狂欢节', '/uploads/demo/banner_3.jpg', 'CATEGORY', '10010601', 'HOME', 3, 1);

-- ======================== 公告 ========================
INSERT INTO mkt_notice (id, title, content, status, sort) VALUES
(10060001, '双 11 狂欢季活动说明', '<p>双 11 狂欢季火热进行中！活动期间全场商品参与满减，领取「满 99 减 10」「满 299 减 40」优惠券后下单立享优惠。</p><p>每日 10:00 准时开启限量秒杀，秒杀商品每人限购，售完即止。</p><p>优惠券与秒杀价不可叠加使用，活动最终解释权归本商城所有。</p>', 1, 1),
(10060002, '发货与售后服务说明', '<p>现货商品付款后 48 小时内发货，节假日顺延；偏远地区物流时效可能延长 1~3 天。</p><p>签收后 7 天内支持无理由退换（商品需保持完好、不影响二次销售），生鲜食品类除外。</p><p>如遇商品质量问题，请在「我的订单」中申请售后并上传照片，客服将在 24 小时内处理。</p>', 1, 2);

-- ======================== 优惠券 ========================
INSERT INTO mkt_coupon (id, name, type, threshold, amount, discount, max_amount, scope, scope_ids, total, issued, used, per_limit, valid_type, start_at, end_at, valid_days, status, description) VALUES
(10070001, '满99减10', 'FULL_REDUCTION', 99.00, 10.00, 1.00, 0.00, 'ALL', NULL, 1000, 0, 0, 2, 'DAYS', NULL, NULL, 30, 1, '全场通用，订单商品金额满 99 元可用，领取后 30 天内有效，每人限领 2 张'),
(10070002, '满299减40', 'FULL_REDUCTION', 299.00, 40.00, 1.00, 0.00, 'ALL', NULL, 500, 0, 0, 1, 'FIXED', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY, 0, 1, '全场通用，订单商品金额满 299 元可用，限活动期内使用，每人限领 1 张'),
(10070003, '无门槛5元', 'NO_THRESHOLD', 0.00, 5.00, 1.00, 0.00, 'ALL', NULL, 0, 0, 0, 1, 'DAYS', NULL, NULL, 7, 1, '全场无门槛立减 5 元，领取后 7 天内有效，每人限领 1 张'),
(10070004, '全场9折最高减50', 'DISCOUNT', 0.00, 0.00, 0.90, 50.00, 'ALL', NULL, 300, 0, 0, 1, 'FIXED', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 0, 1, '全场商品 9 折，单笔最高优惠 50 元，限活动期内使用，每人限领 1 张');

-- ======================== 秒杀 ========================
INSERT INTO mkt_seckill (id, title, goods_id, sku_id, seckill_price, stock, sold, limit_per_member, start_at, end_at, status) VALUES
(10080001, '星澜 X9 Pro 限时秒杀', 10030001, 10040001, 2799.00, 50, 0, 1, NOW() - INTERVAL 1 HOUR, NOW() + INTERVAL 7 DAY, 1),
(10080002, '每日坚果 明日 10 点秒杀', 10030027, 10040095, 79.00, 100, 0, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 10 HOUR, DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 10 HOUR, 1);
