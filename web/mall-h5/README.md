# mall-h5 移动端商城

面向会员的 H5 商城前端，接口契约见 `docs/mall/api.md`（§0、§2、§3）。

## 技术栈

Vue 3.5 + TypeScript + Vite 5 + Vant 4（`@vant/auto-import-resolver` 按需引入）+ Pinia + Vue Router 4 + axios + `@vant/area-data` + dayjs + sass。

## 目录

```
src/
├── api/            按领域分文件：auth / public / member / coupon / cart / order / aftersale / review；request.ts 为统一请求层
├── types/          接口类型（与契约字段一一对应）
├── stores/         user（令牌、会员信息）/ cart（角标）/ app（商城配置、结算参数）
├── composables/    usePageList（分页+下拉刷新）、useOrderActions（订单按钮与操作）、useCoupon、useAuthRedirect
├── components/     Price / GoodsCard / GoodsWaterfall / CountDown / EmptyState / CouponCard / SkuPopup /
│                   OrderCard / NavBar / TabBar / ReviewItem / ImageUploader / SmsCodeButton / CancelSheet
├── styles/         theme.ts（唯一的设计变量来源 + Vant themeVars）/ variables.scss / global.scss
├── utils/          工具函数、字典文案（dict.ts）、本地存储
├── router/         路由表与登录守卫（meta.auth）
└── views/          页面：home / category / search / goods / cart / order / aftersale / review / mine / address / coupon / auth
```

## 开发

```bash
npm install
# 后端地址默认 http://192.168.0.100:8090，可覆盖：
VITE_PROXY_TARGET=http://localhost:8081 VITE_PROXY_DIRECT=1 npm run dev   # 直连本地服务端（默认转发到已部署环境 Nginx）
```

开发服务器把 `/api/**`（去掉 `/api` 前缀）与 `/uploads/**` 代理到后端。

## 构建

```bash
npm run build      # vue-tsc -b && vite build，产物在 dist/
```

部署时 Nginx 以 `/` 为根托管 `dist/`，配置 history 回退（`try_files $uri /index.html`），`/api/` 去前缀转发后端，`/uploads/` 直接转发。

## 约定

- 请求统一 `axios.create({ baseURL: '/api' })`，解包 `{code,msg,data}`；`code≠0` 自动 Toast（`silent` 可关闭）。
- `40100`：先用 refreshToken 刷新一次（并发请求共享同一次刷新），失败清除登录态并跳登录页（带 `redirect`）。
- 下单、支付带 `X-Idempotent-Key`（UUID）；同一份参数的重复提交复用同一个键。
- 颜色、圆角只在 `styles/theme.ts` 定义，运行时写入 `--mall-*` CSS 变量，SCSS 通过 `variables.scss` 别名引用；页面不写死颜色。
- 桌面浏览器内容区最大 480px 居中；底部固定栏适配 `safe-area-inset-bottom`。
- 富文本 `v-html` 前经 `sanitizeHtml` 清洗（移除 script/iframe 等标签与 `on*` 属性、`javascript:` 链接）。
