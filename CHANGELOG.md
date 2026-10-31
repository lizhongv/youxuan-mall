# 更新日志

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)，格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [未发布]

### 新增
- 短信接入阿里云、腾讯云（后台 `sms.*` 参数切换，即时生效），后台「测试发送」，通知短信能力 `NotifySmsService`。
- 对象存储：S3 协议兼容阿里云 OSS、腾讯云 COS、MinIO、七牛 Kodo（`storage.*`），后台「测试连接」；已有本地文件链接保持可用。
- 物流轨迹：接入快递100 实时查询（`logistics.*`），商城端 `GET /app/order/{id}/logistics`、管理端 `GET /admin/trade/order/{id}/logistics`，Redis 缓存。
- 接入文档 `docs/mall/integration.md`。

### 变更
- 快递公司字典 `express_company` 的 value 改为快递100 编码（label 不变），历史订单按中文名兼容。
- 生产模式（`mall.sms.dev-mode=false`）下短信渠道为 LOG 时发送失败并记错误日志，不再静默丢弃。

## [1.0.0] - 2026-10-31

首个可商用版本：移动商城 + 管理后台 + 服务端 + 一键部署。

### 新增
- 服务端 mall-server：模块化单体，系统、会员、商品、营销、交易、统计六个领域，295 个单元测试。
- 移动商城：首页导购、类目、搜索、商品详情与多规格选择、购物车、结算、收银台、订单、售后、评价、优惠券、秒杀、会员中心等 36 个页面。
- 管理后台：数据概览、商品（多规格 SKU 组合）、订单、售后、评价、优惠券、秒杀、轮播、公告、会员、管理员、角色权限、菜单、字典、系统参数、日志，支持深色模式。
- 订单状态机与定时任务：超时关单、自动确认收货、优惠券过期。
- 一键部署 `deploy/mall/deploy.sh` 与全链路冒烟测试 `scripts/smoke_test.py`；演示数据 8 个类目、37 个商品。
- 开发规范 `docs/DEVELOPMENT.md` 与接口契约 `docs/mall/api.md`。

### 安全
- BCrypt 密码、登录失败锁定、JWT 角色隔离、按钮级权限、操作日志（密码脱敏）。
- 库存、余额、优惠券、秒杀名额均用条件更新防并发超扣；下单与支付幂等。
- 首次部署自动生成随机密钥；演示开关（验证码回显、模拟支付）可通过环境变量关闭。
