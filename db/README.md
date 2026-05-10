# 数据库迁移（Flyway）

## 实际位置（重要）
**Phase 1 阶段，迁移脚本放在 `bootstrap-app/src/main/resources/db/migration/` 下**（让单体应用启动时自动执行）。

`db/migration/` 这个根目录留作 Phase 2 起的全局参考目录，到那时再决定是：
- (a) 各微服务各自管理自己的 migration
- (b) 抽出独立的 `platform-cloud-migrations/` 模块统一管理

## 命名规范
```
V1__init_baseline.sql           基线
V2__add_user_table.sql          新增表
V2_1__add_user_index.sql        修订（用 underscore + 数字）
R__refresh_view.sql             可重复执行（每次校验和变化都重跑）
```

## 启动时自动执行
`application-dev.yml` 已配置：
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

## Maven 手动执行
```bash
mvn flyway:migrate -pl bootstrap-app \
  -Dflyway.url=jdbc:mysql://localhost:3306/platform_cloud \
  -Dflyway.user=platform \
  -Dflyway.password=platform123
```

## 注意事项
- 脚本一旦发布**不可修改**（Flyway 校验 checksum）。变更已发布脚本时，新增 `Vn__fix_xxx.sql`。
- 大表 DDL 评估锁表时间，必要时用 `pt-online-schema-change` / `gh-ost`。
- 多人协作版本号冲突时，建议按模块切版本号区段（user 模块用 V1xxx、order 模块用 V2xxx）。
