# mall-admin 商城管理后台

商城的运营后台：数据概览、商品、订单 / 售后 / 评价、营销、会员、系统管理。接口契约见 [`docs/mall/api.md`](../../docs/mall/api.md)。

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
└── views/          dashboard / goods / trade / marketing / member / system / profile / login / error
```

## 开发

```bash
npm install
npm run dev                                    # http://localhost:5173/console/
VITE_PROXY_TARGET=http://localhost:8081 VITE_PROXY_DIRECT=1 npm run dev   # 直连本地服务端
```

开发代理把 `/api` 与 `/uploads` 转发到 `VITE_PROXY_TARGET`，默认已部署环境的 Nginx `http://192.168.0.100:8090`；直连本地服务端时加 `VITE_PROXY_DIRECT=1` 去掉 `/api` 前缀。

## 构建

```bash
npm ci && npm run build    # vue-tsc 类型检查 + vite build，产物在 dist/
```

## 约定

- **部署路径**：`base` 为 `/console/`，路由 `createWebHistory('/console/')`；Nginx 需对 `/console/` 做 `try_files ... /console/index.html`。
- **接口**：axios `baseURL = '/api'`，Nginx 去掉 `/api` 后转发后端。响应 `{code,msg,data}`，`code≠0` 统一提示 `msg`；`40100` 先用 refreshToken 刷新一次（并发请求共享同一次刷新），失败清空登录态并带 `redirect` 跳登录页。所有 ID 为字符串。
- **权限**：路由 `meta.permission` 控制页面（无权限进 403），菜单按 `/admin/auth/me` 的 `permissions` 过滤，`roles` 含 `super_admin` 全部可见；按钮用 `v-perm="'goods:goods:edit'"`。新增页面只需在 `router/routes.ts` 的分组里加一项。
- **样式**：颜色、圆角、阴影、间距只在 `styles/variables.scss` 定义（CSS 变量，含深色模式），页面只引用变量；状态颜色统一走 `StatusTag` + `utils/constants.ts` 的 `tone`。
- **列表页**：搜索栏 + 工具栏 + 表格 + 分页，统一用 `useTable`；删除走 `confirmDelete` 二次确认。
- **表格插槽**：Element Plus 2.14 的列插槽 `row` 不是泛型，写法统一为 `#default="{ row }: { row: any }"`。
