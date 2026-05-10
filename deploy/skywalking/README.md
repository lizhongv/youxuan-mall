# SkyWalking 链路追踪部署 (Phase 5.2)

OAP server + UI 走 Docker，Java agent 在 Windows 宿主直接挂到 jar 启动参数。

## 1. 启动 OAP + UI 容器

```bash
# OAP server: gRPC :11800 (agent 上报) / GraphQL :12800 (UI/查询)
docker run -d --name skywalking-oap \
  -p 11800:11800 -p 12800:12800 \
  -e SW_STORAGE=h2 \
  -e SW_HEALTH_CHECKER=default \
  apache/skywalking-oap-server:9.7.0

# UI: :8088
docker run -d --name skywalking-ui \
  --link skywalking-oap:oap \
  -p 8088:8080 \
  -e SW_OAP_ADDRESS=http://oap:12800 \
  apache/skywalking-ui:9.7.0
```

UI 访问: http://localhost:8088

## 2. Agent 安装

下载 `apache-skywalking-java-agent-9.4.0.tgz`，解压到：

```
%USERPROFILE%\.platform-cloud\sw-agent\skywalking-agent\
```

## 3. 给每个 Java 服务挂 agent

```powershell
java -javaagent:%USERPROFILE%\.platform-cloud\sw-agent\skywalking-agent\skywalking-agent.jar `
     -Dskywalking.agent.service_name=goods-service `
     -Dskywalking.collector.backend_service=localhost:11800 `
     -jar service/goods-service/target/goods-service.jar
```

`service_name` 分别填: `gateway`, `auth-service`, `goods-service`, `order-service`。

`backend_service` 必须用 `localhost:11800`（Docker 端口映射），**不要**用 `172.22.80.1`（WSL vEthernet IP 从 Windows Java 进程访问会失败）。

## 4. 验证

- 访问 UI 看 Service 列表是否出现 4 个 Java 服务 + MySQL/Redis
- 触发任意请求（如 gateway → goods → mysql 链路），UI 的 "Trace" 页应能看到 trace 树

## 备注

- 生产建议把 OAP 存储从 `h2` 换成 `elasticsearch`，否则 trace 数据保留窗口短
- Agent 9.4 对部分新版库（mybatis-plus、shardingsphere 5.5）会打 "Witness class does not exist"，无害可忽略，HTTP/JDBC/Redis 主线追踪正常
