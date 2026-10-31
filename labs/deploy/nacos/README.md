# Nacos 本地开发说明

## 默认配置
- 模式：standalone（内嵌 derby）
- 控制台：http://localhost:8848/nacos
- 用户：`nacos` / `nacos`（首次登录会强制改密）

## 切换到 MySQL 持久化

如果希望用 MySQL 存储 Nacos 配置/服务数据，编辑 `docker-compose.yml` 的 `nacos` 服务，加入以下环境变量：

```yaml
environment:
  MODE: standalone
  SPRING_DATASOURCE_PLATFORM: mysql
  MYSQL_SERVICE_HOST: mysql
  MYSQL_SERVICE_PORT: 3306
  MYSQL_SERVICE_DB_NAME: nacos_config
  MYSQL_SERVICE_USER: ${MYSQL_USER}
  MYSQL_SERVICE_PASSWORD: ${MYSQL_PASSWORD}
```

并提前在 MySQL 内创建 `nacos_config` 库 + 导入 [nacos-mysql.sql](https://github.com/alibaba/nacos/blob/develop/distribution/conf/mysql-schema.sql)。
