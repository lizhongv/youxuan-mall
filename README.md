# platform-cloud

> 现代化电商平台脚手架，对标主流电商技术栈：Spring Boot 3 / Spring Cloud Alibaba / RocketMQ / Elasticsearch / ShardingSphere / K8s。
>
> 重构起源：[`platform/`](../platform) 老版微同商城（Spring Boot 2.7 + Vue 2）。本仓库从零搭建，业务模型参考老项目。
>
> 重构计划详见 [`../platform/docs/重构分析与计划.md`](../platform/docs/重构分析与计划.md)。

---

## 当前阶段：Phase 0 + 进入 Phase 1

| Phase | 状态 | 说明 |
|---|---|---|
| 0 · 准备 | 🟢 进行中 | monorepo 骨架 + Docker 依赖栈 + Flyway baseline |
| 1 · 单体现代化 | ⬜ 待启动 | Spring Boot 3.3 + JDK 17 + Spring Security 6 |
| 2 · 微服务化 | ⬜ | Nacos / Gateway / Sentinel / Seata / RocketMQ |
| 3 · 数据架构 | ⬜ | ShardingSphere / Redis Cluster / ES / Canal |
| 4 · 高并发场景 | ⬜ | 秒杀 / 库存 / 幂等 / JMeter / Arthas |
| 5 · 可观测性 + DevOps | ⬜ | SkyWalking / Prometheus / ELK / K8s |
| 6 · 前端 + 业务补全 | ⬜ | Vue3 + TS + Vite / qiankun |
| 7 · 整理与延伸 | ⬜ | 文档站 / 选做 |

---

## 目录结构

```
platform-cloud/
├── pom.xml                          父 pom（版本锁定 BOM）
├── docker-compose.yml               依赖栈（profiles: core / mq / search / observability）
├── .env.example                     环境变量模板
├── .gitignore / .editorconfig
├── deploy/                          中间件配置
│   ├── nacos/
│   ├── prometheus/
│   └── rocketmq/
├── db/
│   └── README.md                    Flyway 迁移说明
├── docs/
│   └── README.md                    架构图/ER 图占位
└── bootstrap-app/                   Phase 1 单体应用
    ├── pom.xml
    └── src/
        ├── main/java/com/platform/cloud/bootstrap/
        │   ├── BootstrapApplication.java
        │   ├── config/SecurityConfig.java
        │   └── web/HealthController.java
        ├── main/resources/
        │   ├── application.yml
        │   ├── application-dev.yml
        │   └── db/migration/V1__init_baseline.sql
        └── test/...

# Phase 2 起加入：
# infra/{gateway,auth-service}
# domain/{user,goods,stock,cart,order,pay,promo,search}-service
# bff/{admin-bff,shop-bff}
# ops/job-service
```

---

## 版本矩阵

| 组件 | 版本 | 备注 |
|---|---|---|
| JDK | 17 | 你本机 OpenJDK 17.0.7 |
| Spring Boot | 3.3.5 | 已升级到 jakarta.* 命名空间 |
| Spring Cloud | 2023.0.3 (Leyton) | |
| Spring Cloud Alibaba | 2023.0.3.2 | 兼容 SB 3.3.x |
| MyBatis-Plus | 3.5.7 | 用 mybatis-plus-spring-boot3-starter |
| Druid | 1.2.23 | druid-spring-boot-3-starter |
| Flyway | 10.20.0 | 自动迁移 |
| ShardingSphere-JDBC | 5.5.0 | Phase 3 用 |
| Redisson | 3.36.0 | Phase 3 替换原 RedisLock |
| RocketMQ Client | 5.x（spring-starter 2.3.1）| Phase 2 用 |
| Elasticsearch | 8.15.3 | Phase 3 用 |
| Knife4j | 4.5.0 | OpenAPI 3 / Jakarta |
| JJWT | 0.12.6 | |
| MapStruct | 1.6.3 | |
| Lombok | 1.18.34 | |
| Hutool | 5.8.32 | |
| Testcontainers | 1.20.3 | 集成测试 |

---

## 快速上手

### 1. 起本地依赖

```bash
# 复制环境变量
cp .env.example .env

# Phase 1 只需核心三件套（MySQL + Redis + Nacos）
docker compose --profile core up -d

# 检查
docker compose ps
```

依赖端口对照：

| 服务 | 端口 | 凭据 / 入口 |
|---|---|---|
| MySQL | 3306 | platform / platform123，库名 platform_cloud |
| Redis | 6379 | password=platform123 |
| Nacos | 8848 | http://localhost:8848/nacos （nacos/nacos，首次会强制改密）|
| RocketMQ NameSrv | 9876 | （`--profile mq`） |
| RocketMQ Dashboard | 18080 | http://localhost:18080 |
| Elasticsearch | 9200 | （`--profile search`） |
| Kibana | 5601 | http://localhost:5601 |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3000 | admin / platform123 |
| SkyWalking UI | 8088 | http://localhost:8088 |

### 2. 编译并启动 bootstrap-app

```bash
# 编译
mvn -T 1C clean package -DskipTests

# 启动
java -jar bootstrap-app/target/bootstrap-app.jar

# 或在 IDEA 里直接运行 BootstrapApplication.main()
```

### 3. 验证

```bash
# 健康探针（自定义）
curl http://localhost:8080/api/ping

# Spring Actuator
curl http://localhost:8080/actuator/health

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus

# Knife4j API 文档
# 浏览器打开 http://localhost:8080/doc.html
```

### 4. Flyway 迁移已自动执行

启动后查看 MySQL 是否产生 `flyway_schema_history` 表 + `app_health_probe` 表：

```sql
USE platform_cloud;
SELECT * FROM flyway_schema_history;
SHOW TABLES;
```

---

## Phase 0 验收清单

- [x] monorepo 目录骨架建好（`platform-cloud/` + `bootstrap-app/`）
- [x] 父 pom 锁定 Spring Boot 3.3.5 / Cloud 2023.0.3 / Cloud Alibaba 2023.0.3.2 / JDK 17
- [x] `docker-compose.yml` 含 4 个 profile（core / mq / search / observability）
- [x] Flyway baseline 脚本就位（`V1__init_baseline.sql`）
- [x] bootstrap-app 含 Spring Security 6 + Actuator + Prometheus + Knife4j 依赖
- [x] 烟雾测试：`BootstrapApplicationTests#contextLoads`
- [ ] **本地实跑**：`docker compose --profile core up -d` + `mvn package` + 访问 `/api/ping`
- [ ] 用 DBeaver 或类似工具从 `platform/_sql/mall.sql` 导出 ER 图至 `docs/er.png`

> 完成最后两项后即可 commit Phase 0，并进入 Phase 1。

---

## 下一步（Phase 1 预告）

1. 接入 **logback-spring.xml**：JSON 输出 + 全链路 traceId（MDC）
2. 加全局异常处理器 + 统一响应封装 `RestResponse<T>`
3. 加 `@Idempotent` 注解 + Redis 实现幂等
4. **Spring Security 6 + JWT** 实战：登录接口、JwtAuthenticationFilter、白名单
5. **接入 Testcontainers**：跑 MySQL/Redis 真实集成测试
6. 把老项目的 **商品域**（Goods/SKU/Category/Brand）按现代风格迁移到 bootstrap-app
