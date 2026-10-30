# 参与贡献

## 提交 Issue

- **Bug**：说明部署方式、浏览器 / 手机型号、复现步骤、期望与实际结果，附截图和响应中的 `traceId`（便于在日志中定位）。
- **功能建议**：说明使用场景和期望效果。

## 提交 Pull Request

1. 从 `master` 创建分支：`feat/拼团活动`、`fix/运费计算` 等。
2. 修改接口时先更新 [docs/mall/api.md](docs/mall/api.md)，前后端都以它为准。
3. 补充单元测试，确保 `./mvnw -pl mall-server -am test` 全部通过；前端 `npm run build` 无类型错误。
4. 改表结构只新增 Flyway 迁移脚本，不修改已发布的脚本。
5. 涉及界面改动时更新 `docs/images` 截图；在 `CHANGELOG.md` 的未发布版本下记录变更。
6. 提交信息使用 [约定式提交](https://www.conventionalcommits.org/zh-hans/)：`feat(trade): 支持部分发货`、`fix(member): 修复验证码重复发送`。

## 代码规范

完整规范见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)，要点：

- 领域之间只通过 `*/api/*Api` 接口调用，不跨领域使用 Mapper。
- 业务失败抛 `BusinessException("给用户看的提示")`，不在 controller 中 try/catch。
- 禁止 `${}` 拼接 SQL；会员相关查询必须带当前用户 ID。
- 注释用中文，说明「为什么」；颜色、间距只在前端的主题变量文件中定义。
