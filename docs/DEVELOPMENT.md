# 开发规范

> 版本：v1.0（2026-10-31）
> 来源：沿用「快传 KuaiChuan」开发规范的结构，按 Java 后端 + Vue 前端的电商项目改写。
> 用法：开工前通读一遍，提交前逐项对照文末「检查清单」。

## 1. 目录结构

```
platform-cloud/
├── mall-server/                 商城服务端（模块化单体，可商用主产品）
│   └── src/main/java/com/platform/mall/
│       ├── MallApplication.java
│       ├── common/              跨领域基础设施：配置、安全、上下文、工具
│       └── <领域>/              system / member / goods / marketing / trade / stats
│           ├── controller/admin 管理端接口  /admin/**
│           ├── controller/app   商城端接口  /app/**
│           ├── service/         业务逻辑（接口 + impl）
│           ├── mapper/          MyBatis-Plus Mapper
│           ├── entity/          表映射对象
│           ├── dto/             入参（*Request / *Query）
│           └── vo/              出参（*VO）
├── web/mall-admin/              管理后台：Vue 3 + TS + Vite + Element Plus
├── web/mall-h5/                 移动商城：Vue 3 + TS + Vite + Vant 4
├── starter/                     公共 starter：统一响应、异常、MyBatis、JWT、缓存
├── service/ infra/              微服务演进实验（Nacos / Gateway / RocketMQ / ES），不参与商城部署
├── deploy/mall/                 商城一键部署：docker-compose + nginx + 脚本
├── scripts/                     冒烟测试、截图、发布脚本
└── docs/                        设计、接口、部署文档
```

- 领域之间只通过 **Service 接口** 调用，不跨领域直接使用对方的 Mapper。
- 领域划分与微服务拆分一致，日后可按领域整体迁出为独立服务。

## 2. 代码分层

| 层 | 职责 | 约束 |
|---|---|---|
| controller | 参数校验、鉴权注解、调用 service、返回 `R<T>` | 不写业务逻辑，不直接用 Mapper |
| service | 业务规则、事务、状态流转 | 写操作加 `@Transactional(rollbackFor = Exception.class)` |
| mapper | 数据访问 | 只用 MyBatis-Plus Wrapper 或 `#{}` 参数，**禁止 `${}` 拼接** |
| entity | 表结构映射 | 继承 `BaseEntity`（审计列、逻辑删除、乐观锁） |
| dto / vo | 入参 / 出参 | 实体不直接出现在接口上；金额用 `BigDecimal`，时间用 `LocalDateTime` |

- 业务失败抛 `BusinessException`，由全局异常处理转成统一响应，不在 controller 里 try/catch。
- 状态字段用枚举（如 `OrderStatus`），状态变化集中在一个方法里校验「从哪个状态能到哪个状态」。
- 金额计算统一 `setScale(2, RoundingMode.HALF_UP)`；库存扣减用带条件的 `UPDATE ... WHERE stock >= ?`，防止超卖。

## 3. 编码风格

- Java：Google Java Style，4 空格缩进，单行不超过 120 字符；import 不用通配符。
- TypeScript / Vue：`<script setup lang="ts">`，组件名大驼峰，接口类型集中在 `src/types`，请求集中在 `src/api`。
- 每个类、每个公开方法写简短中文注释，说明**为什么**：取值理由、边界、并发考虑；不写复述代码的注释。
- 不硬编码魔法数字：配置进 `application.yml`（`mall.*`）或常量类，并注释取值理由。

## 4. 接口约定

- 路径：管理端 `/admin/<领域>/<资源>`，商城端 `/app/<资源>`，公开接口 `/app/public/**`。
- 统一响应 `{code, msg, data, ts, traceId}`，`code=0` 成功；分页返回 `{records, total, current, size}`。
- 所有 `Long` 型 ID 序列化为字符串，避免前端 JS 精度丢失。
- 写接口必须有 `@Valid` 校验；下单、支付等关键接口带幂等令牌。
- 管理端权限：`@PreAuthorize("hasAuthority('goods:edit')")`，权限码在菜单表维护。
- 每个接口加 `@Operation(summary = ...)`，文档在 `/doc.html`。

## 5. 界面

- **设计规范集中管理**：颜色、圆角、阴影、间距只在 `src/styles/variables.scss`（后台）/ `theme.ts`（H5）定义；页面不硬编码颜色。
- **后台布局**：左侧菜单 + 顶栏（面包屑、主题切换、用户菜单）+ 标签页 + 内容卡片；列表页统一「搜索栏 + 工具栏 + 表格 + 分页」。
- **商城布局**：底部标签栏（首页 / 分类 / 购物车 / 我的），主色电商红；图片懒加载，列表下拉刷新、上拉加载。
- **状态可见**：所有请求有加载态，空列表有空状态图，失败有可读的提示；危险操作二次确认。
- **浅色 / 深色**：后台支持暗色模式；在 1366×768 和 375×667 两种尺寸下检查无截断、无横向滚动。

## 6. 配置与数据

- 环境相关配置全部走环境变量（`.env`），仓库只放 `.env.example`。
- 表结构只通过 Flyway 迁移变更：`V<序号>__<说明>.sql`，已发布的迁移脚本不得修改。
- 所有表含 `id, created_by, updated_by, created_at, updated_at, deleted, version`；金额 `DECIMAL(10,2)`；字符集 `utf8mb4`。

## 7. 安全

- 密码使用 BCrypt；JWT 密钥 ≥ 32 字节且仅从环境变量注入；登录失败多次锁定。
- 管理端与商城端令牌带 `utype` 声明，互不通用。
- 上传文件校验扩展名白名单与大小，文件名随机化，存储路径不可由用户控制。
- 手机号等敏感字段在后台列表中脱敏展示。
- 用户只能访问自己的订单、地址、优惠券：查询条件必须带当前用户 ID。

## 8. 测试

- JUnit 5 + Mockito：核心业务（价格计算、优惠券、订单状态机、库存扣减、权限）必须有单测。
- 冒烟测试 `scripts/smoke_test.py`：部署后从登录到下单、支付、发货、收货、评价、售后跑一遍完整链路。
- 测试不依赖计时与固定端口，不依赖外网。

## 9. 构建与发布

- 后端：`./mvnw -pl mall-server -am package`；前端：`npm ci && npm run build`。
- 一键部署 `deploy/mall/deploy.sh <主机>`：构建 → 测试 → 打包 → 上传 → `docker compose up -d` → 冒烟测试。
- 语义化版本；`CHANGELOG.md` 按「新增 / 修复 / 安全」记录。

## 10. Git

- 约定式提交：`feat(trade): 支持订单超时自动关闭`、`fix:`、`docs:`、`test:`、`chore:`；标题说做了什么，正文说为什么。
- `.gitattributes` 统一 `eol=lf`，`.ps1` 用 CRLF；`target/`、`node_modules/`、`dist/`、`.env`、日志不进仓库。

## 11. 交付前检查清单

- [ ] 目录、分层符合第 1、2 节；没有跨领域使用 Mapper
- [ ] 无 `${}` SQL 拼接；无硬编码密钥
- [ ] `mvn test` 全部通过；冒烟测试全链路通过
- [ ] 前端 `npm run build` 无类型错误；浅色、深色与手机尺寸检查无截断
- [ ] Flyway 迁移可在空库上从头执行
- [ ] README、接口文档、CHANGELOG 已更新
