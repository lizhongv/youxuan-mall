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

## 上线前检查

- [ ] 修改后台 `admin` 初始密码（`.env` 中的 `MALL_INIT_ADMIN_PASSWORD` 只在首次建库时生效）
- [ ] 接入短信渠道后把 `MALL_SMS_DEV_MODE` 改为 `false`（否则验证码会返回给前端）
- [ ] 接入微信 / 支付宝后把 `MALL_PAY_MOCK_ENABLED` 改为 `false`
- [ ] 在 `系统参数` 中把「注册赠送余额」改为 0（演示数据默认赠送 200 元）
- [ ] 前面加 HTTPS（云负载均衡或在 Nginx 中配置证书）
- [ ] 防火墙只开放 HTTP(S) 端口；MySQL 只绑定在 127.0.0.1
- [ ] 每日备份数据库与 `data/uploads`
