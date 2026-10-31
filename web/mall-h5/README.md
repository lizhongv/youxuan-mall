# mall-h5 移动端商城

面向会员的 H5 商城前端，接口契约见 `docs/mall/api.md`（§0、§2、§3）。

## 技术栈

Vue 3.5 + TypeScript + Vite 5 + Vant 4（`@vant/auto-import-resolver` 按需引入）+ Pinia + Vue Router 4 + axios + `@vant/area-data` + dayjs + sass。

## 目录

```
src/
├── api/            按领域分文件：auth / public / member / coupon / cart / order（含支付渠道、支付轮询、物流）/ aftersale / review；request.ts 为统一请求层
├── types/          接口类型（与契约字段一一对应）
├── stores/         user（令牌、会员信息）/ cart（角标）/ app（商城配置、结算参数）
├── composables/    usePageList（分页+下拉刷新）、useAsync（加载/失败/重试状态）、useDict（公开字典+内置回退）、
│                   useOrderActions（订单按钮与操作）、useCoupon、useAuthRedirect
├── components/     Price / GoodsCard / GoodsWaterfall / CountDown / EmptyState / LoadState / CouponCard / SkuPopup /
│                   OrderCard / NavBar / TabBar / ReviewItem / ImageUploader / SmsCodeButton / CaptchaInput /
│                   AgreementCheck / CancelSheet
├── styles/         theme.ts（唯一的设计变量来源 + Vant themeVars）/ variables.scss / global.scss
├── utils/          工具函数（fmtSpec 规格文案、num、compressImage、isWechat…）、字典文案（dict.ts）、本地存储、
│                   chunkError.ts（分片加载失败自刷新）、pay.ts（微信 JSAPI 调起、跳出支付前记录订单）
├── router/         路由表与登录守卫（meta.auth）
└── views/          页面：home / category / search / goods / cart / order / aftersale / review / mine / address / coupon / auth

## 页面补充（v1.1）

| 路由 | 页面 | 说明 |
|---|---|---|
| `/pay/:orderId` | 收银台 | 渠道来自 `GET /app/pay/channels`（不可用置灰并显示 reason，不默认选模拟支付）；余额支付弹 6 位密码键盘，60010 引导设置、60011 显示剩余次数、60012 锁定提示；JSAPI / H5 / 支付宝按 `payParams` 调起 |
| `/pay-result` | 支付结果 | `poll=1`（或从渠道回跳）时每 2 秒轮询 `pay-status`，最多 60 秒，超时给「我已支付 / 重新支付」 |
| `/order/:id/logistics` | 物流详情 | 时间线（最新在上高亮），无轨迹时显示服务端 `message` |
| `/pay-password` | 支付密码 | 图形验证码 → `POST /app/member/pay-password/sms-code` → 两次输入 6 位数字；带 `redirect` 时完成后返回 |
| `/password` | 登录密码 | `hasPassword=false` 时为「设置密码」，不显示原密码；改密返回的新令牌替换本地令牌 |
| `/agreement/user`、`/agreement/privacy` | 用户协议 / 隐私政策 | 通用模板，商城名与客服电话取 `/app/public/config` |
| `/auth/wechat`、`/auth/bind` | 微信登录回跳 / 绑定手机号 | 仅微信内显示「微信一键登录」；`needBind` 时用 bindToken + 短信（scene=LOGIN）绑定 |
```

## 开发

```bash
npm install
# 后端地址默认 http://localhost:8090，可覆盖：
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
- **加载状态**：页面数据请求必须区分「加载中 / 失败 / 空」，失败用 `<LoadState>`（或 `useAsync`）显示错误原因与「重新加载」，不能把失败当成空数据。
- **错误对象**：`ApiError` 带 `code` 与 `data`（如 `{needCaptcha}`）；`needCaptcha(e)` 判断是否要显示图形验证码，`errorMessage(e)` 取文案。由页面自行展示错误的接口（登录、结算预览、支付）用 `silent`。
- **验证码**：发短信统一用 `<SmsCodeButton>`，点击先弹图形验证码（一次性，失败自动换新，demo 的 `devCode` 自动填入）；密码登录返回 `needCaptcha` 时在表单内显示 `<CaptchaInput>`。
- **协议**：登录 / 注册的协议勾选默认不勾选（`<AgreementCheck>`），未勾选时提交会弹窗请求同意。
- **数字判断**：后端 Long 序列化为字符串（`"0"` 为真值），计数 / 金额判断一律 `num(v) > 0`，不要直接用作布尔。
- **规格文案**：展示 `specText` 一律经 `fmtSpec`（「颜色:星空灰;存储:8+256G」→「星空灰 / 8+256G」）。
- **字典**：取消原因、售后原因、快递公司用 `useDict(type, 内置回退)` 取 `GET /app/public/dict/{type}`。
- **图片上传**：`ImageUploader` 在读取前压缩（长边 1600px、JPEG 0.8），压缩后再校验 5MB。
- **分片加载失败**：`router.onError` / `unhandledrejection` 捕获动态 import 失败后自动整页刷新一次（sessionStorage 标记防循环），再失败显示「页面加载失败，点击重试」。
- **支付回跳**：跳到微信 H5 / 支付宝前把订单写入 sessionStorage（`utils/pay.ts`），回跳地址不带 `orderId` 时结果页从中恢复。
