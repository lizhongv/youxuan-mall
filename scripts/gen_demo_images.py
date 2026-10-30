#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成商城演示数据所需的图片（与 V3__seed_goods_marketing.sql 中的数据一一对应）。

输出目录：<repo>/deploy/mall/seed/uploads/demo/
  - g<goodsId>_main.jpg / _1.jpg / _2.jpg  商品主图 + 两张轮播图（800x800）
  - cat_<categoryId>.png                    类目图标（160x160）
  - brand_<brandId>.png                     品牌 Logo（200x200）
  - banner_1.jpg .. banner_3.jpg            首页轮播（750x340）

用法：python scripts/gen_demo_images.py   （可在任意目录执行，可重复执行，会覆盖旧文件）
依赖：Pillow
"""
import colorsys
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
OUT_DIR = os.path.join(ROOT, 'deploy', 'mall', 'seed', 'uploads', 'demo')

# ---------------------------------------------------------------- 数据（需与 SQL 保持一致）

# 一级类目：id -> (名称, 主色, 图标形状)
TOP_CATEGORIES = [
    (10010001, '手机数码', '#3D7BFF', 'phone'),
    (10010002, '电脑办公', '#6C5CE7', 'laptop'),
    (10010003, '家用电器', '#00A896', 'airfryer'),
    (10010004, '服饰鞋包', '#FF6B6B', 'shirt'),
    (10010005, '美妆个护', '#E84393', 'serum'),
    (10010006, '食品生鲜', '#F39C12', 'orange'),
    (10010007, '家居家装', '#C08552', 'sofa'),
    (10010008, '运动户外', '#27AE60', 'ball'),
]

# 二级类目：(id, 父 id, 名称, 图标形状)
SUB_CATEGORIES = [
    (10010101, 10010001, '手机', 'phone'),
    (10010102, 10010001, '耳机音箱', 'earbuds'),
    (10010103, 10010001, '智能穿戴', 'watch'),
    (10010201, 10010002, '笔记本', 'laptop'),
    (10010202, 10010002, '键鼠外设', 'keyboard'),
    (10010203, 10010002, '办公文具', 'notebook'),
    (10010301, 10010003, '厨房电器', 'airfryer'),
    (10010302, 10010003, '生活电器', 'robot'),
    (10010303, 10010003, '个护电器', 'hairdryer'),
    (10010401, 10010004, '男装', 'shirt'),
    (10010402, 10010004, '女装', 'dress'),
    (10010403, 10010004, '运动鞋', 'shoe'),
    (10010404, 10010004, '箱包', 'luggage'),
    (10010501, 10010005, '护肤', 'serum'),
    (10010502, 10010005, '彩妆', 'lipstick'),
    (10010503, 10010005, '洗护', 'pump'),
    (10010601, 10010006, '休闲零食', 'nuts'),
    (10010602, 10010006, '茶饮冲调', 'coffee'),
    (10010603, 10010006, '生鲜水果', 'orange'),
    (10010701, 10010007, '家纺', 'bed'),
    (10010702, 10010007, '灯具', 'lamp'),
    (10010703, 10010007, '收纳', 'crate'),
    (10010801, 10010008, '健身器材', 'dumbbell'),
    (10010802, 10010008, '户外装备', 'tent'),
    (10010803, 10010008, '球类', 'ball'),
]

# 品牌：(id, 中文名, 英文名, 首字母, 主色, 简介)
BRANDS = [
    (10020001, '青禾', 'Qinghe', 'Q', '#4CAF50', '青禾专注天然好食材与日常文具，坚持产地直采、简单配方，让每一天的吃喝用度都更安心。'),
    (10020002, '北极星', 'Polaris', 'B', '#2D3E8C', '北极星 Polaris 是专注效率办公与智能家电的科技品牌，以稳定可靠的品质陪伴每一位认真生活的人。'),
    (10020003, '星澜', 'Xinglan', 'X', '#3D7BFF', '星澜科技致力于打造兼具性能与设计的智能手机与可穿戴设备，让科技触手可及。'),
    (10020004, '拾光', 'Shiguang', 'S', '#E84393', '拾光以温和有效的成分科学为核心，提供护肤、彩妆与个护电器，记录每一段美好时光。'),
    (10020005, '木野', 'Muye', 'M', '#A0703C', '木野倡导自然、简约的北欧生活方式，产品覆盖家纺、灯具、收纳与出行箱包。'),
    (10020006, '远山', 'Yuanshan', 'Y', '#2E8B57', '远山户外为城市里的探索者设计专业而轻量的运动与露营装备，向山而行。'),
    (10020007, '简屿', 'Jianyu', 'J', '#FF6B6B', '简屿服饰以舒适面料与利落剪裁为本，打造四季百搭的基础款衣橱。'),
    (10020008, '栗子厨房', 'Lizi Kitchen', 'L', '#F39C12', '栗子厨房专注小家电研发，用更聪明的厨房电器把下厨变成一件轻松愉快的事。'),
    (10020009, '赫兹', 'Hertz Audio', 'H', '#5B4BDB', '赫兹声学深耕音频技术十余年，以纯净还原的音质与舒适佩戴赢得口碑。'),
    (10020010, '迪越', 'Diyue Sport', 'D', '#00A8E8', '迪越运动为跑者与球类爱好者打造专业运动装备，科技缓震，步步轻盈。'),
]

# 商品：(id, 二级类目 id, 品牌 id, 名称, 卖点(subtitle), 三条特性, 图片形状)
GOODS = [
    (10030001, 10010101, 10020003, '星澜 X9 Pro 5G 旗舰手机', '2K 曲面屏 · 5000 万像素三摄 · 100W 闪充',
     ('6.78 英寸 2K 曲面屏', '5000 万像素影像系统', '100W 有线闪充'), 'phone'),
    (10030002, 10010101, 10020003, '星澜 Note 12 5G 手机', '大电池长续航 · 120Hz 高刷护眼屏',
     ('6000mAh 大电池', '120Hz 高刷护眼屏', '轻薄 7.9mm 机身'), 'phone'),
    (10030003, 10010102, 10020009, '赫兹 AirBuds 3 主动降噪耳机', '-45dB 深度降噪 · 36 小时总续航',
     ('-45dB 智能降噪', '36 小时超长续航', '蓝牙 5.3 低延迟'), 'earbuds'),
    (10030004, 10010102, 10020009, '赫兹 Cube 便携蓝牙音箱', 'IP67 防水防尘 · 360° 环绕立体声',
     ('360° 环绕声场', 'IP67 防水防尘', '20 小时续航'), 'speaker'),
    (10030005, 10010103, 10020003, '星澜 Watch S2 智能手表', '血氧心率监测 · 14 天超长续航',
     ('全天候健康监测', '100+ 运动模式', '14 天超长续航'), 'watch'),
    (10030006, 10010201, 10020002, '北极星 Polaris Book 14 轻薄本', '1.2kg 全金属机身 · 2.8K 高色域屏',
     ('2.8K 120Hz 高色域屏', '1.2kg 全金属机身', '65W 快充 · 续航 14h'), 'laptop'),
    (10030007, 10010202, 10020002, '北极星 K87 三模机械键盘', '热插拔轴座 · 三模连接 · RGB 背光',
     ('有线/蓝牙/2.4G 三模', '全键热插拔', 'RGB 炫彩背光'), 'keyboard'),
    (10030008, 10010202, 10020002, '北极星 M3 无线静音鼠标', '静音微动 · 人体工学 · 一年续航',
     ('静音微动 降噪 90%', '人体工学握感', '一节电池用一年'), 'mouse'),
    (10030009, 10010203, 10020001, '青禾 A5 活页笔记本套装', '100g 道林纸 · 不洇墨 · 自由增减内页',
     ('100g 加厚道林纸', '书写顺滑不洇墨', '活页自由增减'), 'notebook'),
    (10030010, 10010301, 10020008, '栗子厨房 可视空气炸锅 5L', '可视窗口 · 免翻面 · 少油更健康',
     ('可视烹饪窗口', '360° 热风免翻面', '5L 大容量'), 'airfryer'),
    (10030011, 10010301, 10020008, '栗子厨房 静音破壁料理机', '低噪破壁 · 冷热双打 · 一键清洗',
     ('低噪 8 叶刀头', '冷热双打 豆浆米糊', '一键自清洁'), 'blender'),
    (10030012, 10010302, 10020002, '北极星 S7 扫拖一体机器人', '激光导航 · 4000Pa 大吸力 · 自动集尘',
     ('LDS 激光导航', '4000Pa 强劲吸力', '扫拖一体 自动集尘'), 'robot'),
    (10030013, 10010302, 10020002, '北极星 桌面暖风机', '2 秒速热 · 倾倒断电 · 低噪运行',
     ('PTC 陶瓷 2 秒速热', '倾倒自动断电', '35dB 低噪运行'), 'heater'),
    (10030014, 10010303, 10020004, '拾光 高速负离子吹风机', '11 万转高速马达 · 负离子护发',
     ('11 万转高速马达', '2 亿负离子护发', '智能恒温不伤发'), 'hairdryer'),
    (10030015, 10010401, 10020007, '简屿 男士精梳棉圆领 T 恤', '新疆长绒棉 · 亲肤透气 · 不易变形',
     ('新疆精梳长绒棉', '亲肤透气 不闷汗', '领口加固 不变形'), 'shirt'),
    (10030016, 10010401, 10020007, '简屿 男士轻薄白鸭绒羽绒服', '90% 白鸭绒 · 轻至 280g · 可收纳',
     ('90% 白鸭绒填充', '轻至 280g', '防泼水面料'), 'jacket'),
    (10030017, 10010402, 10020007, '简屿 法式碎花收腰连衣裙', '法式方领 · 收腰显瘦 · 垂感雪纺',
     ('法式复古方领', '收腰显瘦版型', '垂感雪纺面料'), 'dress'),
    (10030018, 10010402, 10020007, '简屿 女士羊毛针织开衫', '30% 羊毛混纺 · 软糯亲肤 · 百搭外搭',
     ('30% 羊毛混纺', '软糯亲肤不扎', '春秋百搭外搭'), 'cardigan'),
    (10030019, 10010403, 10020010, '迪越 云感缓震跑步鞋', '超临界发泡中底 · 轻弹缓震 · 透气网面',
     ('超临界发泡中底', '轻至 230g', '飞织透气鞋面'), 'shoe'),
    (10030020, 10010403, 10020010, '迪越 经典复古小白鞋', '头层牛皮鞋面 · 软弹鞋垫 · 百搭易打理',
     ('头层牛皮鞋面', '软弹记忆鞋垫', '百搭易打理'), 'sneaker'),
    (10030021, 10010404, 10020005, '木野 铝框万向轮拉杆箱', 'PC 镜面箱体 · 铝合金框 · 静音万向轮',
     ('铝合金加固框架', '静音万向飞机轮', 'TSA 海关密码锁'), 'luggage'),
    (10030022, 10010404, 10020005, '木野 通勤防泼水双肩包', '15.6 寸电脑仓 · 防泼水 · 减压背带',
     ('15.6 寸独立电脑仓', '防泼水面料', '透气减压背带'), 'backpack'),
    (10030023, 10010501, 10020004, '拾光 玻尿酸水润保湿精华', '5 重玻尿酸 · 深层补水 · 清爽不黏',
     ('5 重玻尿酸复配', '24 小时长效保湿', '清爽质地不黏腻'), 'serum'),
    (10030024, 10010501, 10020004, '拾光 氨基酸温和洁面乳', '氨基酸表活 · 弱酸性 · 洗后不紧绷',
     ('氨基酸温和表活', '弱酸性亲肤配方', '绵密泡沫 不紧绷'), 'tube'),
    (10030025, 10010502, 10020004, '拾光 丝绒雾面哑光口红', '丝绒雾面 · 显色持久 · 不拔干',
     ('丝绒雾面质地', '一抹显色 持久不脱', '添加植物油 不拔干'), 'lipstick'),
    (10030026, 10010503, 10020001, '青禾 山茶花控油洗发水', '山茶花精萃 · 控油蓬松 · 无硅油',
     ('山茶花植萃', '控油蓬松 72h', '无硅油配方'), 'pump'),
    (10030027, 10010601, 10020001, '青禾 每日坚果混合装', '6 种坚果果干 · 科学配比 · 独立小袋',
     ('6 种坚果果干', '每日 25g 科学配比', '独立充氮小包装'), 'nuts'),
    (10030028, 10010601, 10020001, '青禾 风干手撕牛肉干', '精选牛后腿肉 · 高蛋白 · 越嚼越香',
     ('精选牛后腿肉', '高蛋白 低脂肪', '传统风干工艺'), 'jerky'),
    (10030029, 10010602, 10020001, '青禾 明前西湖龙井 250g', '明前头采 · 豆香馥郁 · 罐装密封',
     ('明前头采嫩芽', '豆香馥郁 回甘', '铁罐密封保鲜'), 'tea'),
    (10030030, 10010602, 10020001, '青禾 冷萃冻干挂耳咖啡', '-40°C 冻干 · 3 秒速溶 · 0 蔗糖',
     ('-40°C 冷萃冻干', '3 秒冷热速溶', '0 蔗糖 0 植脂末'), 'coffee'),
    (10030031, 10010603, 10020001, '青禾 赣南脐橙 新鲜当季', '产地直发 · 果大汁多 · 坏果包赔',
     ('赣南原产地直发', '果大皮薄 汁水多', '坏果包赔'), 'orange'),
    (10030032, 10010701, 10020005, '木野 60 支长绒棉四件套', '60 支长绒棉 · 贡缎工艺 · 丝滑亲肤',
     ('60 支新疆长绒棉', '贡缎工艺 丝滑', 'A 类婴幼儿标准'), 'bed'),
    (10030033, 10010702, 10020005, '木野 北欧实木落地灯', '实木灯杆 · 布艺灯罩 · 三档调光',
     ('榉木实木灯杆', '亚麻布艺灯罩', '三档无极调光'), 'lamp'),
    (10030034, 10010703, 10020005, '木野 可折叠收纳箱 3 只装', '一秒折叠 · 可叠放 · 承重 30kg',
     ('一秒折叠省空间', '多层叠放', '加厚 PP 承重 30kg'), 'crate'),
    (10030035, 10010801, 10020006, '远山 可调节家用哑铃', '一秒调重 · 防滑握把 · 安全卡扣',
     ('旋钮一秒调重', '防滑橡胶握把', '安全锁扣设计'), 'dumbbell'),
    (10030036, 10010802, 10020006, '远山 双层防暴雨露营帐篷', '3000mm 防水 · 双层通风 · 快速搭建',
     ('3000mm 防水指数', '双层通风防结露', '3 分钟快速搭建'), 'tent'),
    (10030037, 10010803, 10020010, '迪越 7 号室内外比赛篮球', 'PU 吸湿面料 · 手感细腻 · 耐磨防滑',
     ('PU 吸湿超纤面料', '标准 7 号比赛用球', '深沟设计 好控球'), 'ball'),
]

# 首页轮播：(文件序号, 主标题, 副标题, 一级类目 id(配色), 形状)
BANNERS = [
    (1, '星澜 X9 Pro 新品首发', '2K 曲面屏 · 100W 闪充  限时直降 300', 10010001, 'phone'),
    (2, '秋冬焕新季', '女装新品 低至 5 折  满 299 减 40', 10010004, 'dress'),
    (3, '零食狂欢节', '坚果零食 第 2 件半价  满 99 减 10', 10010006, 'nuts'),
]

SCENE_TEXT = {
    10010001: '多色可选 · 质感随行',
    10010002: '高效办公 · 灵感随行',
    10010003: '智能家电 · 品质生活',
    10010004: '多色多码 · 百搭穿搭',
    10010005: '温和配方 · 精致日常',
    10010006: '新鲜好味 · 安心入口',
    10010007: '温暖居所 · 简约美学',
    10010008: '向山而行 · 自在运动',
}

# ---------------------------------------------------------------- 工具函数

WHITE = (255, 255, 255)
BLACK = (0, 0, 0)


def hex2rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(c1, c2, t):
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(c1, c2))


def shift_hue(c, dh, dl=0.0, ds=0.0):
    h, l, s = colorsys.rgb_to_hls(*[v / 255 for v in c])
    h = (h + dh) % 1.0
    l = min(1.0, max(0.0, l + dl))
    s = min(1.0, max(0.0, s + ds))
    return tuple(int(round(v * 255)) for v in colorsys.hls_to_rgb(h, l, s))


def palette(base):
    if isinstance(base, str):
        base = hex2rgb(base)
    return {
        'base': base,
        'light': mix(base, WHITE, 0.82),
        'soft': mix(base, WHITE, 0.55),
        'dark': mix(base, BLACK, 0.35),
        'deep': mix(base, BLACK, 0.62),
        'accent': shift_hue(base, 0.12, 0.05, 0.1),
        'white': WHITE,
        'grey': (226, 230, 236),
        'ink': mix(base, BLACK, 0.72),
    }


def _font_candidates(bold):
    win = 'C:/Windows/Fonts/'
    names = (['msyhbd.ttc', 'msyh.ttc'] if bold else ['msyh.ttc', 'msyhbd.ttc']) + ['simhei.ttf', 'simsun.ttc']
    cands = [win + n for n in names]
    cands += [
        '/System/Library/Fonts/PingFang.ttc',
        '/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc' if bold else '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc',
        '/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc',
        '/usr/share/fonts/truetype/wqy/wqy-microhei.ttc',
    ]
    return cands


_FONT_CACHE = {}


def font(size, bold=False):
    key = (size, bold)
    if key in _FONT_CACHE:
        return _FONT_CACHE[key]
    f = None
    for path in _font_candidates(bold):
        if os.path.exists(path):
            try:
                f = ImageFont.truetype(path, size)
                break
            except OSError:
                continue
    if f is None:
        try:
            f = ImageFont.load_default(size)
        except TypeError:
            f = ImageFont.load_default()
    _FONT_CACHE[key] = f
    return f


def text_w(draw, text, f):
    b = draw.textbbox((0, 0), text, font=f)
    return b[2] - b[0]


def fit_lines(draw, text, max_w, size, min_size, bold=False, max_lines=2):
    """返回 (font, lines)：优先缩小字号单行放下，放不下则按空格/字符折行。"""
    s = size
    while s >= min_size:
        f = font(s, bold)
        if text_w(draw, text, f) <= max_w:
            return f, [text]
        s -= 2
    # 折行：在最小字号基础上略放大，按字符贪心折行
    s = max(min_size, int(size * 0.8))
    while True:
        f = font(s, bold)
        lines, cur = [], ''
        for ch in text:
            if text_w(draw, cur + ch, f) <= max_w:
                cur += ch
            else:
                lines.append(cur.rstrip())
                cur = ch.lstrip()
        if cur:
            lines.append(cur)
        if len(lines) <= max_lines or s <= min_size:
            return f, lines[:max_lines]
        s -= 2


def draw_text_block(draw, text, center_x, top_y, max_w, size, min_size, fill, bold=False, max_lines=2, spacing=1.25):
    f, lines = fit_lines(draw, text, max_w, size, min_size, bold, max_lines)
    lh = int(f.size * spacing)
    y = top_y
    for ln in lines:
        draw.text((center_x, y), ln, font=f, fill=fill, anchor='mt')
        y += lh
    return y


def gradient(size, c1, c2, direction='v'):
    w, h = size
    mask = Image.linear_gradient('L')
    if direction == 'd':
        # 对角渐变：横向与纵向渐变各取一半
        h_mask = mask.rotate(90, expand=True).transpose(Image.FLIP_LEFT_RIGHT)
        mask = Image.blend(mask, h_mask, 0.5)
    elif direction == 'h':
        mask = mask.rotate(90, expand=True).transpose(Image.FLIP_LEFT_RIGHT)
    mask = mask.resize((w, h), Image.BICUBIC)
    return Image.composite(Image.new('RGB', (w, h), c2), Image.new('RGB', (w, h), c1), mask)


# ---------------------------------------------------------------- 形状绘制（单位坐标 0~1）

class Pen:
    def __init__(self, img):
        self.d = ImageDraw.Draw(img)
        self.s = img.size[0]

    def _b(self, x0, y0, x1, y1):
        s = self.s
        return [x0 * s, y0 * s, x1 * s, y1 * s]

    def rr(self, x0, y0, x1, y1, r, fill, outline=None, width=0.0):
        r = min(r, (x1 - x0) / 2, (y1 - y0) / 2)
        self.d.rounded_rectangle(self._b(x0, y0, x1, y1), radius=r * self.s, fill=fill, outline=outline,
                                 width=max(1, int(width * self.s)) if outline else 0)

    def rect(self, x0, y0, x1, y1, fill):
        self.d.rectangle(self._b(x0, y0, x1, y1), fill=fill)

    def el(self, x0, y0, x1, y1, fill, outline=None, width=0.0):
        self.d.ellipse(self._b(x0, y0, x1, y1), fill=fill, outline=outline,
                       width=max(1, int(width * self.s)) if outline else 0)

    def circle(self, cx, cy, r, fill, outline=None, width=0.0):
        self.el(cx - r, cy - r, cx + r, cy + r, fill, outline, width)

    def poly(self, pts, fill, outline=None, width=0.0):
        self.d.polygon([(x * self.s, y * self.s) for x, y in pts], fill=fill, outline=outline,
                       width=max(1, int(width * self.s)) if outline else 0)

    def line(self, pts, fill, width):
        self.d.line([(x * self.s, y * self.s) for x, y in pts], fill=fill, width=max(1, int(width * self.s)),
                    joint='curve')

    def arc(self, x0, y0, x1, y1, a0, a1, fill, width):
        self.d.arc(self._b(x0, y0, x1, y1), a0, a1, fill=fill, width=max(1, int(width * self.s)))

    def pie(self, x0, y0, x1, y1, a0, a1, fill):
        self.d.pieslice(self._b(x0, y0, x1, y1), a0, a1, fill=fill)

    def chord(self, x0, y0, x1, y1, a0, a1, fill):
        self.d.chord(self._b(x0, y0, x1, y1), a0, a1, fill=fill)


def sh_phone(p, c):
    p.rr(.28, .05, .72, .95, .075, c['deep'])
    p.rr(.30, .075, .70, .925, .06, c['light'])
    p.rr(.45, .095, .55, .115, .01, c['deep'])
    p.rr(.33, .16, .67, .40, .035, c['base'])
    p.circle(.42, .25, .045, c['soft'])
    p.rr(.49, .23, .63, .25, .01, c['white'])
    p.rr(.49, .28, .60, .30, .01, c['soft'])
    cols = [c['accent'], c['soft'], c['dark'], c['base']]
    for row in range(3):
        for col in range(4):
            x = .335 + col * .085
            y = .45 + row * .11
            p.rr(x, y, x + .06, y + .06, .015, cols[(row + col) % 4])
    p.rr(.43, .885, .57, .895, .005, c['deep'])


def sh_earbuds(p, c):
    p.rr(.22, .50, .78, .88, .16, c['white'], c['soft'], .012)
    p.rect(.23, .62, .77, .627, c['grey'])
    p.circle(.50, .78, .018, c['accent'])
    for x, flip in ((.30, 1), (.58, -1)):
        p.rr(x + .045, .22, x + .095, .46, .025, c['white'], c['soft'], .01)
        p.el(x, .10, x + .14, .27, c['white'], c['soft'], .01)
        p.el(x + .035, .14, x + .105, .22, c['dark'])
        p.circle(x + .07, .45, .012, c['base'])


def sh_speaker(p, c):
    p.rr(.20, .18, .80, .84, .12, c['dark'])
    p.rr(.23, .21, .77, .81, .10, c['base'])
    for r, col in ((.20, c['deep']), (.15, c['dark']), (.08, c['deep']), (.04, c['soft'])):
        p.circle(.50, .52, r, col)
    for i in range(5):
        p.circle(.32 + i * .09, .27, .012, c['light'])
    p.rr(.30, .86, .70, .89, .015, c['deep'])


def sh_watch(p, c):
    p.rr(.38, .02, .62, .30, .05, c['soft'])
    p.rr(.38, .70, .62, .98, .05, c['soft'])
    for y in (.80, .86, .92):
        p.circle(.50, y, .012, c['base'])
    p.rr(.27, .22, .73, .78, .12, c['deep'])
    p.rr(.30, .25, .70, .75, .10, c['ink'])
    p.rr(.73, .42, .76, .54, .01, c['dark'])
    p.arc(.36, .31, .64, .59, -90, 200, c['accent'], .025)
    p.arc(.40, .35, .60, .55, -90, 120, c['soft'], .02)
    p.rr(.38, .62, .62, .65, .01, c['white'])
    p.rr(.42, .67, .58, .695, .01, c['soft'])


def sh_laptop(p, c):
    p.rr(.14, .16, .86, .66, .03, c['deep'])
    p.rr(.165, .185, .835, .635, .015, c['light'])
    p.rr(.20, .23, .52, .40, .02, c['base'])
    p.rr(.55, .23, .80, .30, .015, c['soft'])
    p.rr(.55, .33, .80, .40, .015, c['accent'])
    p.rr(.20, .44, .80, .59, .02, c['soft'])
    p.poly([(.10, .68), (.90, .68), (.97, .78), (.03, .78)], c['dark'])
    p.rr(.03, .78, .97, .80, .01, c['deep'])
    p.rr(.42, .69, .58, .71, .008, c['deep'])


def sh_keyboard(p, c):
    p.rr(.06, .30, .94, .72, .04, c['deep'])
    for row in range(5):
        y = .34 + row * .074
        n = 12 if row < 4 else 7
        w = .82 / n
        for i in range(n):
            x = .09 + i * w
            col = c['light']
            if row == 4 and i == 3:
                col = c['soft']
            if (row, i) in ((0, 0), (1, 11), (3, 11)):
                col = c['accent']
            p.rr(x + .004, y, x + w - .004, y + .058, .01, col)


def sh_mouse(p, c):
    p.el(.30, .14, .70, .88, c['base'])
    p.el(.33, .17, .67, .85, c['dark'])
    p.el(.30, .14, .70, .82, c['base'])
    p.line([(.50, .15), (.50, .42)], c['deep'], .012)
    p.rr(.48, .22, .52, .32, .02, c['light'])
    p.el(.40, .55, .60, .70, c['soft'])
    p.circle(.50, .73, .012, c['accent'])


def sh_notebook(p, c):
    p.rr(.30, .14, .78, .88, .03, c['dark'])
    p.rr(.26, .12, .74, .86, .03, c['base'])
    p.rr(.33, .22, .67, .34, .015, c['light'])
    for y in (.42, .48, .54):
        p.rr(.33, y, .67, y + .02, .01, c['soft'])
    for i in range(7):
        y = .17 + i * .1
        p.el(.22, y, .30, y + .05, None, c['grey'], .014)
    p.rr(.70, .62, .76, .90, .02, c['accent'])
    p.poly([(.70, .90), (.76, .90), (.73, .95)], c['ink'])


def sh_airfryer(p, c):
    p.rr(.22, .14, .78, .88, .14, c['white'], c['soft'], .01)
    p.rr(.30, .20, .70, .36, .05, c['deep'])
    p.circle(.50, .28, .05, c['ink'])
    p.arc(.45, .23, .55, .33, 200, 340, c['accent'], .012)
    p.rr(.26, .44, .74, .84, .08, c['base'])
    p.rr(.32, .50, .68, .64, .03, c['deep'])
    p.circle(.42, .57, .03, c['accent'])
    p.circle(.55, .58, .025, mix(c['accent'], BLACK, .2))
    p.rr(.40, .70, .60, .78, .04, c['dark'])


def sh_blender(p, c):
    p.poly([(.28, .14), (.72, .14), (.66, .62), (.34, .62)], c['light'], c['soft'], .012)
    p.poly([(.31, .40), (.69, .40), (.66, .61), (.34, .61)], c['accent'])
    p.rr(.26, .10, .74, .16, .02, c['deep'])
    p.rr(.72, .20, .80, .48, .03, None, c['soft'], .02)
    p.rr(.28, .62, .72, .88, .06, c['base'])
    p.circle(.50, .75, .06, c['deep'])
    p.circle(.50, .75, .03, c['white'])


def sh_robot(p, c):
    p.el(.18, .58, .82, .80, mix(c['grey'], BLACK, .1))
    p.circle(.50, .52, .32, c['white'], c['soft'], .01)
    p.circle(.50, .52, .25, c['light'])
    p.circle(.50, .40, .08, c['deep'])
    p.circle(.50, .40, .04, c['base'])
    p.arc(.18, .20, .82, .84, 200, 340, c['dark'], .03)
    p.rr(.38, .62, .62, .66, .02, c['soft'])
    p.circle(.44, .72, .015, c['accent'])
    p.circle(.56, .72, .015, c['base'])


def sh_heater(p, c):
    p.rr(.26, .14, .74, .82, .08, c['white'], c['soft'], .01)
    p.rr(.31, .20, .69, .62, .04, c['deep'])
    for i in range(7):
        y = .24 + i * .052
        p.rr(.34, y, .66, y + .022, .01, mix(c['accent'], (255, 120, 40), .5))
    p.circle(.42, .72, .035, c['base'])
    p.circle(.58, .72, .035, c['soft'])
    p.rr(.30, .82, .70, .88, .02, c['dark'])


def sh_hairdryer(p, c):
    p.poly([(.42, .48), (.56, .48), (.60, .90), (.46, .90)], c['dark'])
    p.rr(.45, .84, .61, .92, .03, c['deep'])
    p.rr(.20, .22, .78, .50, .14, c['base'])
    p.rr(.70, .27, .86, .45, .04, c['dark'])
    p.circle(.30, .36, .12, c['deep'])
    p.circle(.30, .36, .07, c['soft'])
    p.circle(.30, .36, .03, c['white'])
    p.rr(.48, .56, .52, .64, .015, c['accent'])


def _tee(p, c, long_sleeve=False, body=None, sleeve=None):
    body = body or c['base']
    sleeve = sleeve or body
    if long_sleeve:
        p.poly([(.22, .18), (.10, .30), (.04, .84), (.16, .86), (.24, .40)], sleeve)
        p.poly([(.78, .18), (.90, .30), (.96, .84), (.84, .86), (.76, .40)], sleeve)
    else:
        p.poly([(.22, .18), (.06, .34), (.16, .46), (.27, .38)], sleeve)
        p.poly([(.78, .18), (.94, .34), (.84, .46), (.73, .38)], sleeve)
    p.poly([(.22, .18), (.39, .12), (.61, .12), (.78, .18), (.75, .88), (.25, .88)], body)
    p.chord(.39, .04, .61, .22, 0, 180, c['light'])


def sh_shirt(p, c):
    _tee(p, c)
    p.rr(.40, .38, .60, .42, .01, c['light'])
    p.rr(.44, .45, .56, .47, .01, c['soft'])


def sh_jacket(p, c):
    _tee(p, c, long_sleeve=True)
    for y in (.30, .44, .58, .72):
        p.line([(.26, y), (.74, y)], c['dark'], .008)
    p.line([(.50, .20), (.50, .88)], c['deep'], .014)
    p.rr(.36, .10, .64, .18, .03, c['dark'])


def sh_cardigan(p, c):
    _tee(p, c, long_sleeve=True)
    p.poly([(.39, .12), (.50, .42), (.61, .12)], c['light'])
    p.line([(.50, .42), (.50, .88)], c['dark'], .008)
    for y in (.50, .60, .70, .80):
        p.circle(.53, y, .014, c['white'])
    for y in (.80, .84):
        p.line([(.26, y), (.74, y)], c['dark'], .006)


def sh_dress(p, c):
    p.poly([(.36, .08), (.42, .08), (.44, .20), (.56, .20), (.58, .08), (.64, .08), (.66, .38), (.34, .38)],
           c['base'])
    p.rr(.33, .36, .67, .42, .02, c['dark'])
    p.poly([(.34, .42), (.66, .42), (.86, .92), (.14, .92)], c['base'])
    for x, y in ((.40, .55), (.58, .62), (.30, .80), (.50, .76), (.70, .84), (.45, .88), (.63, .50)):
        p.circle(x, y, .022, c['white'])
        p.circle(x, y, .009, c['accent'])


def sh_shoe(p, c):
    p.poly([(.08, .62), (.12, .40), (.30, .34), (.44, .44), (.62, .48), (.86, .56), (.94, .66), (.92, .72),
            (.10, .72)], c['base'])
    p.rr(.06, .70, .95, .80, .04, c['white'], c['soft'], .008)
    p.rr(.06, .76, .95, .80, .02, c['accent'])
    for i in range(4):
        x = .38 + i * .06
        p.line([(x, .44 + i * .01), (x + .04, .52 + i * .01)], c['white'], .012)
    p.poly([(.50, .60), (.80, .60), (.70, .66)], c['dark'])
    p.el(.12, .36, .22, .44, c['dark'])


def sh_sneaker(p, c):
    p.poly([(.08, .62), (.10, .42), (.28, .38), (.46, .46), (.66, .50), (.88, .58), (.94, .68), (.10, .70)],
           c['white'], c['soft'], .008)
    p.rr(.06, .68, .95, .80, .03, c['light'], c['soft'], .008)
    p.poly([(.10, .42), (.28, .38), (.30, .62), (.10, .62)], c['base'])
    for i in range(4):
        x = .40 + i * .06
        p.circle(x, .50 + i * .01, .01, c['dark'])
    p.line([(.40, .64), (.86, .64)], c['soft'], .008)


def sh_luggage(p, c):
    p.rr(.44, .04, .56, .10, .02, c['deep'])
    p.rect(.46, .08, .48, .20, c['deep'])
    p.rect(.52, .08, .54, .20, c['deep'])
    p.rr(.26, .18, .74, .88, .06, c['base'])
    for x in (.36, .50, .64):
        p.rr(x - .015, .22, x + .015, .84, .015, c['dark'])
    p.rr(.26, .46, .74, .49, .0, c['soft'])
    p.rr(.40, .40, .60, .46, .015, c['deep'])
    for x in (.32, .68):
        p.circle(x, .92, .035, c['ink'])


def sh_backpack(p, c):
    p.arc(.38, .04, .62, .26, 180, 360, c['deep'], .03)
    p.rr(.22, .14, .78, .90, .14, c['base'])
    p.rr(.30, .52, .70, .84, .08, c['dark'])
    p.rr(.36, .58, .64, .61, .01, c['soft'])
    p.rr(.30, .24, .70, .30, .03, c['soft'])
    p.circle(.66, .27, .015, c['accent'])


def sh_serum(p, c):
    p.el(.40, .02, .60, .20, c['ink'])
    p.rr(.42, .14, .58, .28, .02, c['deep'])
    p.rr(.30, .26, .70, .90, .07, c['soft'])
    p.rr(.30, .52, .70, .90, .07, c['base'])
    p.rect(.30, .52, .70, .60, c['base'])
    p.rr(.36, .36, .64, .46, .015, c['white'])
    p.rr(.40, .40, .60, .42, .005, c['base'])
    p.rr(.34, .30, .38, .80, .02, c['light'])


def sh_tube(p, c):
    p.poly([(.30, .10), (.70, .10), (.64, .74), (.36, .74)], c['white'], c['soft'], .01)
    p.rect(.30, .10, .70, .16, c['soft'])
    p.rr(.38, .74, .62, .90, .03, c['base'])
    p.rr(.38, .30, .62, .44, .02, c['base'])
    p.rr(.40, .50, .60, .53, .01, c['soft'])
    p.rr(.42, .57, .58, .59, .01, c['soft'])


def sh_lipstick(p, c):
    p.rr(.36, .52, .64, .92, .02, c['deep'])
    p.rr(.36, .52, .64, .58, .0, mix(c['deep'], (212, 175, 55), .6))
    p.rr(.40, .34, .60, .54, .01, mix(c['white'], (212, 175, 55), .4))
    p.poly([(.42, .34), (.42, .14), (.58, .06), (.58, .34)], c['base'])
    p.poly([(.42, .14), (.58, .06), (.58, .10), (.42, .18)], c['dark'])


def sh_pump(p, c):
    p.rr(.47, .10, .53, .26, .01, c['deep'])
    p.rr(.30, .08, .54, .13, .02, c['deep'])
    p.rr(.40, .22, .60, .30, .02, c['deep'])
    p.rr(.28, .28, .72, .92, .1, c['base'])
    p.rr(.34, .44, .66, .74, .03, c['white'])
    p.el(.42, .50, .58, .62, c['accent'])
    p.rr(.40, .66, .60, .69, .01, c['soft'])


def sh_nuts(p, c):
    p.el(.16, .80, .84, .90, mix(c['grey'], BLACK, .1))
    nuts = [(.32, .44, (163, 105, 60)), (.44, .38, (196, 140, 80)), (.56, .42, (140, 85, 45)),
            (.68, .46, (196, 140, 80)), (.38, .52, (120, 70, 40)), (.62, .52, (222, 180, 110)),
            (.50, .48, (170, 110, 60))]
    for x, y, col in nuts:
        p.el(x - .07, y - .05, x + .07, y + .05, col)
        p.el(x - .04, y - .03, x + .01, y, mix(col, WHITE, .3))
    p.chord(.14, .26, .86, .88, 0, 180, c['base'])
    p.rr(.14, .55, .86, .59, .02, c['dark'])
    p.chord(.24, .58, .76, .84, 0, 180, mix(c['base'], WHITE, .15))


def sh_jerky(p, c):
    p.poly([(.24, .14), (.76, .14), (.80, .90), (.20, .90)], c['base'])
    p.rr(.22, .10, .78, .18, .01, c['dark'])
    for i in range(10):
        x = .24 + i * .056
        p.poly([(x, .10), (x + .028, .06), (x + .056, .10)], c['dark'])
    p.el(.32, .36, .68, .72, c['white'])
    p.poly([(.38, .58), (.44, .44), (.62, .46), (.60, .62), (.46, .66)], (150, 62, 40))
    p.line([(.44, .50), (.56, .58)], (190, 100, 70), .012)
    p.rr(.34, .78, .66, .82, .01, c['light'])


def sh_tea(p, c):
    p.rr(.28, .10, .72, .20, .03, c['deep'])
    p.rr(.30, .18, .70, .90, .04, c['base'])
    p.rr(.30, .36, .70, .70, .0, c['light'])
    p.el(.40, .42, .58, .64, (90, 160, 80))
    p.line([(.42, .62), (.56, .44)], (60, 120, 60), .01)
    p.el(.52, .46, .64, .56, (120, 185, 95))
    p.rr(.34, .76, .66, .79, .01, c['soft'])


def sh_coffee(p, c):
    for x in (.40, .50, .60):
        p.arc(x - .05, .08, x + .05, .22, 90, 270, c['soft'], .014)
        p.arc(x - .05, .20, x + .05, .34, 270, 450, c['soft'], .014)
    p.el(.14, .76, .86, .90, c['dark'])
    p.el(.18, .74, .82, .86, c['soft'])
    p.arc(.62, .44, .84, .66, -90, 90, c['white'], .04)
    p.chord(.22, .14, .74, .84, 0, 180, c['white'])
    p.rect(.22, .40, .74, .50, c['white'])
    p.el(.22, .34, .74, .46, (110, 70, 45))
    p.el(.40, .37, .56, .42, (160, 115, 80))
    p.rr(.30, .58, .66, .60, .01, c['soft'])


def sh_orange(p, c):
    orange = (255, 150, 30)
    p.el(.22, .78, .78, .90, mix(c['grey'], BLACK, .12))
    p.circle(.50, .52, .32, orange)
    p.circle(.42, .42, .10, (255, 190, 90))
    p.circle(.50, .21, .03, (110, 140, 40))
    p.poly([(.52, .20), (.66, .08), (.78, .12), (.68, .22)], (70, 160, 70))
    p.line([(.54, .19), (.70, .13)], (40, 120, 50), .008)
    for a in range(0, 360, 60):
        x = .60 + .05 * math.cos(math.radians(a))
        y = .66 + .05 * math.sin(math.radians(a))
        p.circle(x, y, .008, (230, 120, 20))


def sh_bed(p, c):
    p.rr(.08, .24, .92, .60, .04, c['dark'])
    p.rr(.12, .40, .44, .54, .05, c['white'])
    p.rr(.56, .40, .88, .54, .05, c['white'])
    p.rr(.06, .52, .94, .84, .04, c['base'])
    p.rr(.06, .52, .94, .60, .03, c['light'])
    for x in (.25, .50, .75):
        p.line([(x, .62), (x, .84)], c['soft'], .006)
    p.rect(.10, .84, .14, .92, c['deep'])
    p.rect(.86, .84, .90, .92, c['deep'])


def sh_lamp(p, c):
    p.el(.30, .86, .70, .94, c['deep'])
    p.rect(.485, .30, .515, .90, (176, 132, 90))
    p.poly([(.34, .08), (.66, .08), (.78, .36), (.22, .36)], c['soft'])
    p.poly([(.34, .08), (.42, .08), (.36, .36), (.22, .36)], c['light'])
    p.rr(.20, .34, .80, .38, .01, c['dark'])
    p.chord(.32, .26, .68, .52, 0, 180, mix(c['accent'], (255, 230, 150), .7))


def sh_crate(p, c):
    for (y0, y1, col) in ((.58, .92, c['dark']), (.32, .62, c['base']), (.10, .36, c['soft'])):
        p.rr(.20, y0, .80, y1, .03, col)
        h = y1 - y0
        p.rr(.42, y0 + h * .25, .58, y0 + h * .42, .02, mix(col, BLACK, .3))
        p.line([(.22, y0 + h * .7), (.78, y0 + h * .7)], mix(col, WHITE, .3), .006)


def sh_dumbbell(p, c):
    p.rr(.10, .46, .90, .54, .02, c['grey'])
    for x0, x1 in ((.14, .22), (.22, .30), (.70, .78), (.78, .86)):
        p.rr(x0, .26, x1, .74, .03, c['deep'])
        p.rr(x0 + .01, .28, x1 - .01, .72, .02, c['dark'])
    p.rr(.36, .44, .64, .56, .03, c['base'])
    for x in range(5):
        p.line([(.40 + x * .05, .45), (.40 + x * .05, .55)], c['dark'], .006)
    p.rr(.30, .38, .34, .62, .01, c['accent'])
    p.rr(.66, .38, .70, .62, .01, c['accent'])


def sh_tent(p, c):
    p.el(.04, .80, .96, .92, mix(c['grey'], BLACK, .1))
    p.poly([(.50, .12), (.92, .84), (.08, .84)], c['base'])
    p.poly([(.50, .12), (.62, .84), (.08, .84)], c['dark'])
    p.poly([(.50, .38), (.64, .84), (.36, .84)], c['deep'])
    p.poly([(.50, .38), (.54, .84), (.36, .84)], c['ink'])
    p.line([(.50, .12), (.50, .04)], c['deep'], .012)
    p.poly([(.50, .04), (.62, .07), (.50, .10)], c['accent'])


def sh_ball(p, c):
    ball = mix((235, 120, 40), c['base'], .15)
    p.el(.22, .80, .78, .90, mix(c['grey'], BLACK, .12))
    p.circle(.50, .48, .34, ball)
    line = mix(ball, BLACK, .55)
    p.line([(.16, .48), (.84, .48)], line, .014)
    p.line([(.50, .14), (.50, .82)], line, .014)
    p.arc(.02, .14, .38, .82, -60, 60, line, .014)
    p.arc(.62, .14, .98, .82, 120, 240, line, .014)
    p.circle(.40, .34, .06, mix(ball, WHITE, .25))


def sh_sofa(p, c):
    p.rr(.14, .26, .86, .62, .06, c['dark'])
    p.rr(.20, .30, .48, .56, .04, c['base'])
    p.rr(.52, .30, .80, .56, .04, c['base'])
    p.rr(.08, .44, .22, .80, .05, c['dark'])
    p.rr(.78, .44, .92, .80, .05, c['dark'])
    p.rr(.18, .56, .82, .76, .04, c['soft'])
    p.line([(.50, .58), (.50, .74)], c['base'], .008)
    p.rect(.14, .80, .18, .90, c['ink'])
    p.rect(.82, .80, .86, .90, c['ink'])
    p.rr(.58, .34, .72, .46, .03, c['accent'])


SHAPES = {k[3:]: v for k, v in globals().items() if k.startswith('sh_')}


def render_shape(name, pal, size, ss=3):
    """在 ss 倍画布上绘制再缩小，获得抗锯齿效果。"""
    big = Image.new('RGBA', (size * ss, size * ss), (0, 0, 0, 0))
    SHAPES[name](Pen(big), pal)
    return big.resize((size, size), Image.LANCZOS)


def soft_shadow(size, box, alpha=70, blur=18):
    layer = Image.new('RGBA', size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse(box, fill=(0, 0, 0, alpha))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def deco_circles(img, color, items):
    layer = Image.new('RGBA', img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for (cx, cy, r, a) in items:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (a,))
    img.alpha_composite(layer)


def pill(draw, center, text, f, bg, fg, pad_x=22, pad_y=10):
    w = text_w(draw, text, f)
    cx, cy = center
    h = f.size + pad_y * 2
    draw.rounded_rectangle([cx - w / 2 - pad_x, cy - h / 2, cx + w / 2 + pad_x, cy + h / 2], radius=h / 2, fill=bg)
    draw.text((cx, cy), text, font=f, fill=fg, anchor='mm')


def save_jpg(img, name, quality=82):
    img.convert('RGB').save(os.path.join(OUT_DIR, name), 'JPEG', quality=quality, optimize=True, progressive=True)


def save_png(img, name):
    img.save(os.path.join(OUT_DIR, name), 'PNG', optimize=True)


# ---------------------------------------------------------------- 各类图片

TOP_BY_ID = {t[0]: t for t in TOP_CATEGORIES}
SUB_BY_ID = {s[0]: s for s in SUB_CATEGORIES}
BRAND_BY_ID = {b[0]: b for b in BRANDS}


def top_of(cat_id):
    return TOP_BY_ID[SUB_BY_ID[cat_id][1]] if cat_id in SUB_BY_ID else TOP_BY_ID[cat_id]


def goods_main(g):
    gid, cat, brand, name, tagline, feats, shape = g
    top = top_of(cat)
    c = palette(top[2])
    S = 800
    img = gradient((S, S), c['light'], mix(c['soft'], WHITE, .35)).convert('RGBA')
    deco_circles(img, WHITE, [(400, 330, 270, 120), (680, 110, 90, 70), (110, 640, 60, 60)])
    img.alpha_composite(soft_shadow((S, S), (220, 520, 580, 580), 60, 20))
    img.alpha_composite(render_shape(shape, c, 440), (180, 100))
    d = ImageDraw.Draw(img)
    bname = BRAND_BY_ID[brand][1]
    pill(d, (90 + text_w(d, bname, font(26, True)) / 2, 60), bname, font(26, True), c['base'], WHITE, 18, 8)
    y = draw_text_block(d, name, S // 2, 590, 700, 48, 30, c['ink'], bold=True)
    draw_text_block(d, tagline, S // 2, y + 12, 700, 30, 20, c['dark'], max_lines=1)
    return img


def goods_detail(g):
    gid, cat, brand, name, tagline, feats, shape = g
    top = top_of(cat)
    c = palette(top[2])
    S = 800
    img = gradient((S, S), WHITE, c['light'], 'h').convert('RGBA')
    deco_circles(img, c['soft'], [(250, 420, 230, 90)])
    img.alpha_composite(soft_shadow((S, S), (90, 590, 410, 640), 55, 16))
    img.alpha_composite(render_shape(shape, c, 380), (60, 230))
    d = ImageDraw.Draw(img)
    d.rectangle([60, 70, 72, 150], fill=c['base'])
    d.text((96, 72), '核心卖点', font=font(46, True), fill=c['ink'])
    f_sub, lines = fit_lines(d, name, 640, 26, 18)
    d.text((98, 132), lines[0], font=f_sub, fill=c['dark'])
    fx0, fx1 = 470, 760
    for i, ft in enumerate(feats):
        y = 250 + i * 150
        d.rounded_rectangle([fx0, y, fx1, y + 118], radius=22, fill=WHITE, outline=c['soft'], width=2)
        d.ellipse([fx0 + 18, y + 18, fx0 + 62, y + 62], fill=c['base'])
        d.text((fx0 + 40, y + 40), str(i + 1), font=font(26, True), fill=WHITE, anchor='mm')
        f, ls = fit_lines(d, ft, fx1 - fx0 - 96, 30, 18, bold=True, max_lines=2)
        ty = y + 59 - (len(ls) * int(f.size * 1.25)) / 2
        for ln in ls:
            d.text((fx0 + 78, ty), ln, font=f, fill=c['ink'])
            ty += int(f.size * 1.25)
    d.text((S // 2, 730), tagline, font=fit_lines(d, tagline, 700, 26, 18)[0], fill=c['dark'], anchor='mm')
    return img


def goods_scene(g):
    gid, cat, brand, name, tagline, feats, shape = g
    top = top_of(cat)
    base = palette(top[2])
    alt = palette(shift_hue(base['base'], 0.07 if gid % 2 else -0.07, 0.02))
    S = 800
    img = gradient((S, S), base['deep'], base['dark']).convert('RGBA')
    deco_circles(img, WHITE, [(620, 300, 230, 22), (620, 300, 160, 26), (140, 140, 70, 18)])
    d = ImageDraw.Draw(img)
    d.rectangle([0, 600, S, S], fill=mix(base['deep'], BLACK, .25))
    img.alpha_composite(soft_shadow((S, S), (380, 570, 780, 630), 120, 18))
    img.alpha_composite(render_shape(shape, alt, 420), (370, 170))
    sm = render_shape(shape, base, 210).rotate(8, resample=Image.BICUBIC)
    img.alpha_composite(soft_shadow((S, S), (60, 585, 300, 615), 110, 12))
    img.alpha_composite(sm, (70, 390))
    d = ImageDraw.Draw(img)
    d.text((70, 90), SCENE_TEXT[top[0]], font=font(40, True), fill=WHITE)
    d.text((72, 150), top[1] + ' · ' + SUB_BY_ID[cat][2], font=font(26), fill=base['soft'])
    y = draw_text_block(d, name, S // 2, 652, 720, 36, 24, WHITE, bold=True, max_lines=1)
    draw_text_block(d, feats[0] + '  |  ' + feats[1], S // 2, y + 10, 720, 24, 18, base['soft'], max_lines=1)
    return img


def category_icon(cat_id, shape):
    top = top_of(cat_id)
    c = palette(top[2])
    S = 160
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    bg = gradient((S, S), mix(c['base'], WHITE, .25), c['dark'], 'd').convert('RGBA')
    mask = Image.new('L', (S * 4, S * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle([4, 4, S * 4 - 4, S * 4 - 4], radius=150, fill=255)
    mask = mask.resize((S, S), Image.LANCZOS)
    img.paste(bg, (0, 0), mask)
    deco_circles(img, WHITE, [(130, 30, 40, 40)])
    icon_pal = palette(mix(c['base'], WHITE, .1))
    icon_pal.update(white=WHITE, light=mix(c['base'], WHITE, .9), grey=mix(c['base'], WHITE, .75))
    img.alpha_composite(render_shape(shape, icon_pal, 116, ss=4), (22, 22))
    # 外圈遮罩，避免图形超出圆角
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def brand_logo(b):
    bid, name, en, letter, color, desc = b
    c = palette(color)
    S = 200
    ss = 4
    big = Image.new('RGBA', (S * ss, S * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.ellipse([8, 8, S * ss - 8, S * ss - 8], fill=c['base'])
    d.ellipse([40, 40, S * ss - 40, S * ss - 40], outline=WHITE, width=10)
    img = big.resize((S, S), Image.LANCZOS)
    grad = gradient((S, S), mix(c['base'], WHITE, .2), c['dark'], 'd').convert('RGBA')
    m = img.split()[3]
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.paste(grad, (0, 0), m)
    ring = img.copy()
    ring_mask = Image.eval(img.convert('L'), lambda v: 255 if v > 240 else 0)
    out.paste(WHITE, (0, 0), ring_mask)
    d = ImageDraw.Draw(out)
    title = name if len(name) <= 3 else name[:2]
    f, _ = fit_lines(d, title, 136, 64, 30, bold=True, max_lines=1)
    d.text((S / 2, 92), title, font=f, fill=WHITE, anchor='mm')
    fe, _ = fit_lines(d, en.upper(), 110, 18, 10, bold=True, max_lines=1)
    d.text((S / 2, 140), en.upper(), font=fe, fill=mix(c['base'], WHITE, .8), anchor='mm')
    return out


def banner(bn):
    idx, title, sub, top_id, shape = bn
    c = palette(TOP_BY_ID[top_id][2])
    W, H = 750, 340
    img = gradient((W, H), mix(c['base'], WHITE, .1), c['deep'], 'd').convert('RGBA')
    deco_circles(img, WHITE, [(590, 170, 170, 40), (590, 170, 120, 40), (40, 300, 80, 25), (720, 20, 50, 30)])
    img.alpha_composite(soft_shadow((W, H), (480, 270, 700, 300), 90, 10))
    img.alpha_composite(render_shape(shape, palette(mix(c['base'], WHITE, .15)), 250), (465, 40))
    d = ImageDraw.Draw(img)
    f, lines = fit_lines(d, title, 400, 50, 30, bold=True, max_lines=1)
    d.text((44, 80), lines[0], font=f, fill=WHITE)
    f2, l2 = fit_lines(d, sub, 400, 24, 16, max_lines=1)
    d.text((46, 160), l2[0], font=f2, fill=mix(c['base'], WHITE, .8))
    fb = font(24, True)
    bw = text_w(d, '立即抢购 >', fb)
    d.rounded_rectangle([46, 228, 46 + bw + 48, 276], radius=24, fill=WHITE)
    d.text((46 + (bw + 48) / 2, 252), '立即抢购 >', font=fb, fill=c['dark'], anchor='mm')
    return img


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    expected = set()
    for g in GOODS:
        gid = g[0]
        for suffix, fn in (('main', goods_main), ('1', goods_detail), ('2', goods_scene)):
            name = 'g%d_%s.jpg' % (gid, suffix)
            save_jpg(fn(g), name)
            expected.add(name)
    for t in TOP_CATEGORIES:
        name = 'cat_%d.png' % t[0]
        save_png(category_icon(t[0], t[3]), name)
        expected.add(name)
    for s in SUB_CATEGORIES:
        name = 'cat_%d.png' % s[0]
        save_png(category_icon(s[0], s[3]), name)
        expected.add(name)
    for b in BRANDS:
        name = 'brand_%d.png' % b[0]
        save_png(brand_logo(b), name)
        expected.add(name)
    for bn in BANNERS:
        name = 'banner_%d.jpg' % bn[0]
        save_jpg(banner(bn), name)
        expected.add(name)
    # 清理不再使用的旧文件，保证目录与 SQL 引用一致
    for f in os.listdir(OUT_DIR):
        if f not in expected and os.path.isfile(os.path.join(OUT_DIR, f)):
            os.remove(os.path.join(OUT_DIR, f))
    total = sum(os.path.getsize(os.path.join(OUT_DIR, f)) for f in expected)
    print('generated %d files into %s (%.2f MB)' % (len(expected), OUT_DIR, total / 1024 / 1024))


if __name__ == '__main__':
    sys.exit(main())
