# labs：微服务演进实验

> 这里是商城从单体走向微服务的技术实验（2026-05，Phase 0–5），**不参与商城部署**，仅作为架构演进参考。
> 商城产品本身是根目录的 `mall-server`（模块化单体）+ `web/`。规划背景见 [docs/重构分析与计划.md](../docs/重构分析与计划.md)。

| 目录 | 内容 |
|---|---|
| `bootstrap-app/` | Phase 0–1 单体起点（Spring Boot 3 + Security 6 + Flyway） |
| `infra/gateway/` | Spring Cloud Gateway + Sentinel 限流 |
| `service/` | auth / goods / order 微服务：Nacos、OpenFeign、Seata、RocketMQ、Elasticsearch、Canal、ShardingSphere、秒杀 Lua |
| `deploy/` | Nacos、RocketMQ、Canal、Prometheus、SkyWalking 配置 |
| `docker-compose.yml` | 中间件依赖栈（profiles：core / mq / search / observability） |

以下为当时的文档占位记录：

# 项目文档

## 待补充

- [ ] `er.png` —— 数据库 ER 图（可用 DBeaver、草料、PowerDesigner 任选生成；建议从老项目 `platform/_sql/mall.sql` 导出）
- [ ] `architecture.png` —— 总体架构图（Phase 2 微服务拆分后绘制）
- [ ] `deployment.png` —— 部署架构图（Phase 5 K8s 化后绘制）
- [ ] 各 Phase 的设计决策记录（ADR：Architecture Decision Records）

## 推荐工具
- 架构图：[draw.io](https://app.diagrams.net/) / [Excalidraw](https://excalidraw.com/) / Mermaid
- ER 图：DBeaver 自带导出 / [dbdiagram.io](https://dbdiagram.io/)
- API 文档：Knife4j（运行时自动生成）
