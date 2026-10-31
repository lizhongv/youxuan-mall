# 商城部署

单机 Docker Compose 部署，4 个容器，约 1.5GB 内存即可运行。

```
          :8090
┌───────────────────────────┐
│ nginx                     │  /            移动商城（web/h5）
│                           │  /console/    管理后台（web/console）
│                           │  /uploads/    上传文件（直接读磁盘）
│                           │  /api/  ──────┐
└───────────────────────────┘               ▼
                                   ┌──────────────────┐
                                   │ server :8081     │ mall-server.jar（JRE 17）
                                   └──────┬─────┬─────┘
                                          ▼     ▼
                                     mysql 8   redis 7
```

## 一键发布

在开发机（Git Bash）执行：

```bash
bash deploy/mall/deploy.sh <ssh 主机>            # 构建 + 单测 + 打包 + 上传 + 启动 + 冒烟测试
bash deploy/mall/deploy.sh <ssh 主机> --skip-web # 只更新服务端
```

目标主机要求：Docker（含 compose 插件），可通过 SSH 免密登录。Windows 主机安装到 `C:\mall`，Linux 主机安装到 `/opt/mall`。
首次发布会自动生成 `.env`（随机数据库、Redis、JWT 密钥），之后保留不变。

## 发布包结构

```
mall/
├── docker-compose.yml
├── .env                 首次自动生成，含全部密钥，勿外传
├── nginx/mall.conf
├── app/mall-server.jar
├── web/h5  web/console  前端构建产物
├── seed/uploads/demo    演示商品图片（每次发布同步到 data/uploads）
├── scripts/remote-up.*  启动脚本
└── data/                运行数据：uploads、logs（数据库在 Docker 卷 mall_mysql-data）
```

## 常用运维

| 操作 | 命令（在安装目录执行） |
|---|---|
| 查看状态 | `docker compose ps` |
| 服务端日志 | `docker compose logs -f --tail 200 server` |
| 重启服务端 | `docker compose restart server` |
| 备份数据库 | `docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" mall' > mall-$(date +%F).sql` |
| 停止 | `docker compose down`（数据卷保留；加 `-v` 会删除数据库，慎用） |

## 运行模式（MALL_PROFILE）

服务端有两种运行模式，由 `.env` 中的 `MALL_PROFILE` 决定（compose 传给服务端，对应 Spring profile）。**未设置时按 `prod` 处理**。

| | `prod`（默认，真实营业） | `demo`（演示机、冒烟测试） |
|---|---|---|
| 模拟支付 `MALL_PAY_MOCK_ENABLED` | **强制关闭**（.env 里写 true 也无效） | 按 .env，演示机设为 `true` |
| 短信验证码回显 `MALL_SMS_DEV_MODE` | **强制关闭** | 按 .env，未接短信渠道时设为 `true` |
| 图形验证码答案回显（供冒烟脚本） | 关闭 | 开启 |
| 接口文档 `/doc.html`、`/v3/api-docs` | **关闭**（后端不再提供，Nginx 的转发规则保留） | 可用 |
| admin 初始密码 | 首次登录**强制修改**（其他接口返回 40301） | 不强制，演示账号可直接用 |
| 启动检查 | JWT 密钥为仓库默认值 / 占位值 / < 32 字节，或数据库、Redis 密码为 `change-me…` / 弱口令时**拒绝启动**，日志里逐条列出原因 | 只打印警告 |

- 新装主机：`remote-up.sh / remote-up.ps1` 首次运行会生成随机密钥写入 `.env`，`MALL_PROFILE=prod`，可直接通过启动检查。
- **演示机**：在其 `.env` 中设置 `MALL_PROFILE=demo`、`MALL_SMS_DEV_MODE=true`、`MALL_PAY_MOCK_ENABLED=true`，然后 `docker compose up -d server`。
  旧版 `.env` 没有 `MALL_PROFILE`，升级后会按 prod 运行（模拟支付与验证码回显失效、冒烟测试失败），这是有意的安全默认值。
- demo 模式的默认值（演示 JWT 密钥等）是公开的，**不要用 demo 模式对公网营业**。
- 本机开发：`MALL_PROFILE=demo` 启动即可使用 `application-demo.yml` 中的开发默认值。

启动被拒绝时，`docker compose logs server` 开头会看到类似：

```
商城服务端拒绝在 prod 模式下启动，请修正以下配置：
  1. MALL_JWT_SECRET 仍是仓库中的公开默认值，任何人都能伪造登录令牌，请替换为随机串
```

## 上线前检查

- [ ] `.env` 中 `MALL_PROFILE=prod`（或不写）；`docker compose logs server | grep 启动检查` 显示「运行模式: prod」
- [ ] `MALL_JWT_SECRET`、`MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD` 为随机值（自动生成的 `.env` 已满足）
- [ ] 首次登录后台按提示修改 `admin` 初始密码（`MALL_INIT_ADMIN_PASSWORD` 只在首次建库时生效）
- [ ] 接入真实短信渠道（prod 下验证码不回显，未接渠道时用户收不到验证码）
- [ ] 接入微信 / 支付宝（prod 下模拟支付不可用）
- [ ] 「系统参数」中「注册赠送余额」默认已为 0（V5 迁移）；如需拉新活动再按预算调整
- [ ] 前面加 HTTPS（云负载均衡或在 Nginx 中配置证书）；若前面还有一层负载均衡，需让它把真实客户端 IP 传给本 Nginx（`set_real_ip_from` + `real_ip_header`），否则限流与登录日志里看到的都是负载均衡的地址
- [ ] 防火墙只开放 HTTP(S) 端口；服务端 8081、MySQL、Redis 不对外（compose 默认如此）
- [ ] `/doc.html` 返回非 200（prod 下接口文档关闭）
- [ ] 每日备份数据库与 `data/uploads`

## 安全相关的默认防护

| 防护 | 位置 | 说明 |
|---|---|---|
| 认证接口限流 | Nginx `mall_auth` / `mall_login` | 每 IP 5 次/秒；登录、发短信等 30 次/分钟，超出返回 429 |
| 登录防爆破 | 服务端 | 每 IP 每分钟 20 次；同一账号在同一 IP 失败 2 次后要图形验证码、5 次锁定该 IP 15 分钟（其他 IP 上的正确登录不受影响）；同一账号累计失败 10 次后所有 IP 都要验证码 |
| 短信防刷 | 服务端 | 发送前必须过图形验证码；手机号 60 秒 / 每天 10 次、IP 每小时 20 次、全站每分钟 60 次（`MALL_SMS_IP_HOURLY_LIMIT`、`MALL_SMS_GLOBAL_PER_MINUTE_LIMIT` 可调） |
| 令牌失效 | 服务端 | 登出拉黑 access 与 refresh；改密、重置密码、冻结会员、停用管理员、调整管理员角色后，该用户所有旧令牌立即失效 |
| 上传 | 服务端 + Nginx | 校验文件头与扩展名一致；会员每分钟 20 次、每天 200 次；`/uploads/` 带 `nosniff` 与 `default-src 'none'` 的 CSP |
| 页面安全头 | Nginx | `X-Frame-Options: SAMEORIGIN`、`Referrer-Policy`、`nosniff` |
| Redis 故障 | 服务端 | 缓存读写失败回源数据库；已登录用户的 access 令牌继续可用；登录、刷新令牌、发短信暂时不可用（返回 50300） |
