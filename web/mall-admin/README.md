# mall-admin 商城管理后台

商城的运营后台：数据概览、商品、订单 / 售后 / 支付流水 / 评价 / 运费模板、营销、会员、系统管理（含支付、短信、存储、物流等基础设施参数）。接口契约见 [`docs/mall/api.md`](../../docs/mall/api.md)。

## 技术栈

Vue 3.5 · TypeScript · Vite 5 · Element Plus（中文、暗黑 css-vars）· Pinia · Vue Router 4 · axios · ECharts 5（按需引入）· wangEditor 5 · dayjs · nprogress · sass · unplugin-auto-import / unplugin-vue-components

## 目录

```
src/
├── api/            按领域封装的接口（auth / system / goods / trade / marketing / member / stats）
├── types/          接口类型（与契约字段一一对应）
├── utils/          request（统一响应 + 刷新令牌）、storage、format、constants（枚举与状态色）、message
├── stores/         user（登录态 / 权限）、app（折叠 / 主题 / 移动端）、tabs（多标签页）
├── router/         routes.ts 静态路由 + 菜单定义（单一来源），index.ts 守卫
├── directives/     v-perm 按钮权限
├── composables/    useTable（分页 / 搜索 / 重置）、useDict、useEcharts
├── components/     ImageUpload、RichEditor、StatusTag、PageHeader、Pagination、EmptyState、GoodsPicker、IconSelect
├── layout/         侧边菜单、顶栏、多标签页
├── styles/         variables.scss（设计变量，唯一定义处）、element.scss、index.scss
└── views/          dashboard / goods / trade（含 payment、freight）/ marketing / member / system / profile / login（含强制改密）/ error
```

## 开发

```bash
npm install
npm run dev                                    # http://localhost:5173/console/
VITE_PROXY_TARGET=http://localhost:8081 VITE_PROXY_DIRECT=1 npm run dev   # 直连本地服务端
```

开发代理把 `/api` 与 `/uploads` 转发到 `VITE_PROXY_TARGET`，默认本机部署的 Nginx `http://localhost:8090`；直连本地服务端时加 `VITE_PROXY_DIRECT=1` 去掉 `/api` 前缀。

## 构建

```bash
npm ci && npm run build    # vue-tsc 类型检查 + vite build，产物在 dist/
```

## 约定

- **部署路径**：`base` 为 `/console/`，路由 `createWebHistory('/console/')`；Nginx 需对 `/console/` 做 `try_files ... /console/index.html`。
- **接口**：axios `baseURL = '/api'`，Nginx 去掉 `/api` 后转发后端。响应 `{code,msg,data}`，`code≠0` 统一提示 `msg`（`ApiError` 带 `code` 与 `data`）。所有 ID 为字符串。
- **登录态**（`utils/request.ts`）：
  - `40100` 先用 refreshToken 刷新一次再重放请求。同一标签页的并发请求共享同一次刷新；多标签页用 Web Locks（不支持时用 localStorage 锁）互斥，拿到锁后若令牌已被其他标签页换新则直接使用，避免一次性 refreshToken 被重复使用。
  - 只有刷新接口明确返回 `40100` / HTTP 401 才清空登录态并带 `redirect` 跳登录页；网络错误、超时、5xx 保留登录态并提示重试（进入页面时拉取 `me` 失败进入「网络异常」页，可重试）。
  - 其他标签页退出登录时本页同步回到登录页；登出接口带 `{refreshToken}`。
  - 登录响应 `data.needCaptcha=true`（或 `40002`）时显示图形验证码（点击换一张；一次性，任何失败后自动换新；demo 模式返回的 `devCode` 自动填入）。
  - `me.mustChangePassword=true` 或任意接口返回 `40301` → 全屏「首次登录请修改密码」页 `/change-password`（守卫拦截其他页面，无法绕过）；改密接口返回新令牌后直接替换本地令牌，无需重新登录。
- **权限**：路由 `meta.permission` 控制页面（无权限进 403），菜单按 `/admin/auth/me` 的 `permissions` 过滤，`roles` 含 `super_admin` 全部可见；按钮用 `v-perm="'goods:goods:edit'"`。新增页面只需在 `router/routes.ts` 的分组里加一项。
- **样式**：颜色、圆角、阴影、间距只在 `styles/variables.scss` 定义（CSS 变量，含深色模式），页面只引用变量；状态颜色统一走 `StatusTag` + `utils/constants.ts` 的 `tone`。
- **列表页**：搜索栏 + 工具栏 + 表格 + 分页，统一用 `useTable`；删除走 `confirmDelete` 二次确认。
  - 表格空状态统一写成 `<template #empty><EmptyState v-if="!loading" … /><div v-else class="table-placeholder" /></template>`，加载中不闪现空状态；`useTable` 自动加载时 `loading` 初始即为 true。
  - 1366×768 下不得出现横向滚动：表格各列宽之和不超过约 1070px（侧栏展开后的可用宽度）；次要信息合并为两行（如「排序 / 创建」「注册 / 最近登录」「类型 / 原因」），时间列可只显示日期并用 `title` 给出完整时间。商品列表的类目树可折叠（状态记忆在 localStorage）。
- **表格插槽**：Element Plus 2.14 的列插槽 `row` 不是泛型，写法统一为 `#default="{ row }: { row: any }"`。
- **自定义表单控件**：值变化后调用 `useFormItem().formItem?.validate('change')`（见 `ImageUpload`），否则所在 `el-form-item` 的校验提示不会消失。

## 业务要点（v1.1）

- **运费模板** `/trade/freight`：按件 / 按重量计费；「默认地区」规则固定在第一行，其他规则选择省份（全国 34 个省级行政区，按大区分组，已被其他规则占用的省份不可再选）；指定条件包邮为「满 N 件或满 X 元」。启停、设为默认通过整体 `PUT` 提交。
- **商品编辑**：运费模板下拉（`/admin/trade/freight-template/list`，空 = 默认模板）、SKU 重量（kg，3 位小数）。**库存差量**：编辑已有 SKU 时把打开页面时详情返回的 `stock` 作为 `originStock` 原样回传；服务端提示库存已被占用时可一键载入最新库存后重新确认。
- **支付流水** `/trade/payment`：按支付单号 / 订单号 / 渠道 / 状态 / 时间筛选；微信、支付宝流水可「查单」（`trade:payment:edit`）。
- **售后**：新增「退款中 REFUNDING」；退货退款单（WAIT_RETURN 及之后）显示商家退货地址（可复制），未配置时引导到「系统参数 › 交易」。
- **订单详情**：物流时间线（`GET /admin/trade/order/{id}/logistics`，最新在上并高亮，超过 5 条可展开，无轨迹时显示 `message`）；发货时快递公司提交字典 value（快递100 编码）；手机号默认脱敏，可点「显示」；改价弹窗给出下限原因（运费 + 0.01 × 件数）。
- **系统参数**：按键前缀分为 基础 / 交易 / 会员 / 支付 / 微信 / 短信 / 存储 / 物流 八个标签页，支持 `?group=trade` 直达；`sensitive=true` 的项只显示「已设置 / 未设置」，输入框留空表示不修改（不提交该项，服务端保持原值）；私钥 / 公钥全文用多行文本框，`true/false` 用开关，`*.provider` 用下拉，未选用的服务商参数自动隐藏；说明文字统一在输入框下方；短信组「测试发送」、存储组「测试连接」使用已保存的配置。

## UI 自查

后端未部署新版本时，可用 `npm run build && npx vite preview` + Playwright 路由拦截 mock 接口截图检查（1366×768 / 1440×900，浅色与深色）：登录验证码、强制改密、各列表页无横向滚动、加载态不闪空状态、商品编辑、运费模板、支付流水、系统参数分组与测试按钮、订单物流时间线。
