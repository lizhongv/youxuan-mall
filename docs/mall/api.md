# 商城接口约定

> 前后端唯一接口契约。后端实现与前端调用都以本文为准；如需调整，先改本文再改代码。
> 在线文档：demo 模式部署后访问 `/doc.html`（Knife4j，分「管理端」「商城端」两组）；prod 模式不提供接口文档。

## 0. 通用

| 项 | 约定 |
|---|---|
| 响应 | `{ code, msg, data, ts, traceId }`，`code = 0` 成功；`40100` 未登录 / 令牌过期 / 令牌已失效；`40300` 无权限；`40301` 请先修改初始密码（见 1.1）；`40002` 需要图形验证码（`data.needCaptcha = true`）；`42900` 请求过于频繁；`50300` 服务暂不可用（Redis / 数据库故障）；`60000` 业务提示（直接展示 msg）；`60010` / `60011` / `60012` 余额支付密码（见 §2.6「余额支付密码」） |
| 分页入参 | query：`page`（从 1 开始）、`size`（默认 20，最大 200）、`keyword` 及各接口筛选字段 |
| 分页出参 | `{ records: [], total, page, size, pages }` |
| ID | 所有 ID 为**字符串**（后端 Long 序列化为字符串） |
| 金额 | 数字，两位小数（如 `99.90`） |
| 时间 | 字符串 `yyyy-MM-dd HH:mm:ss` |
| 鉴权 | 请求头 `Authorization: Bearer <accessToken>`；管理端令牌和商城端令牌互不通用 |
| 幂等 | 下单、支付接口要求请求头 `X-Idempotent-Key: <uuid>` |
| 状态开关 | `status` 为 `1/0` 的启停字段统一用整数 |

**通用 CRUD 模式**（下文写作「CRUD」时即指这五个接口，`{res}` 为资源路径）：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/{res}/page` | 分页列表 |
| GET | `/admin/{res}/{id}` | 详情 |
| POST | `/admin/{res}` | 新增，body 为表单对象，返回新 ID |
| PUT | `/admin/{res}/{id}` | 修改 |
| DELETE | `/admin/{res}/{id}` | 删除（逻辑删除） |

---

## 1. 管理端 `/admin/**`

### 1.1 认证与个人

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| GET | `/admin/auth/captcha` | – | `{captchaId, image}`；image 为 `data:image/png;base64,...`，2 分钟有效、一次性（demo 模式另返回 `devCode` 答案） |
| POST | `/admin/auth/login` | `{username, password, captchaId?, captchaCode?}` | `{accessToken, refreshToken, expiresIn}` |
| POST | `/admin/auth/refresh` | `{refreshToken}` | 同上；每个 refreshToken 只能用一次 |
| POST | `/admin/auth/logout` | `{refreshToken?}`（请求头照常带 access 令牌） | – ；access 与 refresh 都作废 |
| GET | `/admin/auth/me` | – | `{id, username, nickname, avatar, roles:[code], permissions:[code], menus: MenuTree[], mustChangePassword}` |
| PUT | `/admin/auth/profile` | `{nickname, avatar, mobile, email}` | – |
| PUT | `/admin/auth/password` | `{oldPassword, newPassword}` | `{accessToken, refreshToken, expiresIn}`：改密后该账号所有旧令牌失效，前端用返回的新令牌替换本地令牌 |

`MenuTree = {id, parentId, type, name, path, component, icon, sort, visible, children: MenuTree[]}`（只含 DIR/MENU，按钮权限在 permissions 中）。
登录防爆破：
- 同一 IP 每分钟最多 20 次登录尝试，超出返回 `42900`。
- 同一账号在同一 IP **连续失败 2 次后**必须带图形验证码：缺少或错误时返回 `{code: 40002, msg, data: {needCaptcha: true}}`；
  密码错误的响应（`60000`）也带 `data.needCaptcha`，为 true 时前端应先 `GET /admin/auth/captcha` 再显示验证码输入框。
  同一账号在所有 IP 上累计失败 10 次后，任何 IP 登录都需要验证码。
- 同一账号在同一 IP 连续失败 5 次锁定 15 分钟；只锁该 IP，其他 IP 上的正确登录不受影响。

强制修改初始密码（prod 模式）：内置 admin 首次登录、被他人新建或重置密码的账号，`me` 返回 `mustChangePassword: true`，
此时除 `me`、`PUT /admin/auth/password`、`logout` 外的管理端接口一律返回 `{code: 40301, msg: "请先修改初始密码"}`，前端应跳转改密页。

### 1.2 系统管理（权限前缀 `system:`）

| 资源 | 接口 | 字段 / 说明 |
|---|---|---|
| 管理员 `system/admin` | CRUD；`PUT /{id}/status?status=`；`PUT /{id}/password {password}` 重置密码 | `{id, username, nickname, avatar, mobile, email, status, roleIds:[], roleNames:[], lastLoginAt, remark, createdAt}`；新增需 `password`；不能停用/删除自己和 admin；**非超级管理员**不能分配或移除 super_admin 角色、不能修改 / 停用 / 删除超级管理员账号、不能重置 admin 与超级管理员的密码；新建或重置密码后对方首次登录需改密；停用、调整角色、重置密码后对方已登录的令牌立即失效 |
| 角色 `system/role` | CRUD；`GET /list` 全部启用角色；`GET /{id}/menus` → `[menuId]`；`PUT /{id}/menus {menuIds:[]}` | `{id, name, code, sort, status, remark, createdAt}` |
| 菜单 `system/menu` | `GET /tree` 全部菜单树（含按钮）；POST / PUT / DELETE 同 CRUD | 字段同 MenuTree + `permission, status`；有子节点不能删 |
| 字典 `system/dict` | CRUD（page 支持 `dictType` 筛选）；`GET /types` → `[dictType]` | `{id, dictType, label, value, sort, status, remark}` |
| 参数 `system/config` | `GET /list` 全部；`PUT /batch {items:[{configKey, configValue}]}` | `{id, configKey, configValue, name, remark, sensitive}`；`sensitive=true` 的参数（私钥、密钥）已设置时 `configValue` 返回掩码 `******`、未设置返回空串，保存时提交 `******` 表示不修改；开关类参数只能为 `true/false`，`pay.notify_base_url` 须 `https://` 开头。前端按键前缀分组：`mall.`/`trade.`/`member.` 运营参数、`pay.` 支付配置、`wechat.` 微信登录（说明见 [payment.md](payment.md)） |
| 操作日志 `system/oper-log` | `GET /page`（`username, module, success, startTime, endTime`） | `{id, username, module, action, method, uri, params, ip, success, errorMsg, costMs, createdAt}` |
| 登录日志 `system/login-log` | `GET /page`（`userType, username, success`） | `{id, userType, username, ip, userAgent, success, message, createdAt}` |
| 上传 `system/file/upload` | POST multipart `file`, `bizType` | `{id, url, originalName, size}`；对象存储模式下 `url` 为完整 CDN / 桶域名地址 |
| 短信测试 `system/sms/test` | `POST {mobile}`，权限 `system:config:edit` | 用验证码模板发一条随机验证码（不可用于登录），返回 `{provider}`；失败时 `msg` 为渠道错误码，如 `阿里云短信发送失败：isv.SMS_SIGNATURE_ILLEGAL` |
| 存储测试 `system/storage/test` | `POST`，权限 `system:config:edit` | 用已保存的配置上传并删除一个探测文件，返回 `{provider, sampleUrl, elapsedMs, message}`；本地存储直接返回「无需测试」 |
| 上传 `system/file/upload` | POST multipart `file`, `bizType` | `{id, url, originalName, size}`；只收 jpg/jpeg/png/gif/webp，文件头必须与扩展名一致 |

操作日志：管理端所有 POST/PUT/DELETE 请求自动记录（切面实现，密码字段脱敏；系统参数中键名含 `private_key`、`api_v3_key`、`secret` 的值脱敏）。

第三方基础设施参数（sys_config，后台「系统参数」修改后即时生效，无需重启；接入步骤见 [integration.md](integration.md)）：

| 分组 | 键 | 说明 |
|---|---|---|
| 短信 | `sms.provider` | `LOG`（仅 `mall.sms.dev-mode=true` 时可用，生产选 LOG 发送会失败）/ `ALIYUN` / `TENCENT` |
| | `sms.access_key_id`、`sms.access_key_secret`（敏感） | 阿里云 AccessKey 或腾讯云 SecretId / SecretKey |
| | `sms.sign_name`、`sms.region`、`sms.tencent.sdk_app_id` | 签名；地域（腾讯云默认 ap-guangzhou）；腾讯云 SdkAppId |
| | `sms.template.code` | 验证码模板（阿里云变量名 `code`；腾讯云只含 `{1}`） |
| | `sms.template.pay_success` / `delivered` / `refund_success` | 通知模板，留空不发送 |
| 存储 | `storage.provider` | `LOCAL` / `S3`（OSS、COS、MinIO、七牛 Kodo 均走 S3 协议），只影响新上传 |
| | `storage.s3.endpoint`、`region`、`bucket`、`access_key`、`secret_key`（敏感）、`path_style` | S3 连接参数；OSS 必须 `path_style=false`，MinIO 一般为 `true` |
| | `storage.public_base_url`、`storage.path_prefix` | 访问域名（CDN）与对象键前缀（默认 `mall`） |
| 物流 | `logistics.provider` | `NONE` / `KUAIDI100` |
| | `logistics.kuaidi100.customer`、`logistics.kuaidi100.key`（敏感） | 快递100 授权信息 |

### 1.3 会员（权限前缀 `member:`）

| 资源 | 接口 | 字段 |
|---|---|---|
| 会员 `member/member` | `GET /page`（`keyword` 手机/昵称, `levelId, status`）；`GET /{id}`；`PUT /{id}/status?status=`；`POST /{id}/adjust {accountType: BALANCE\|POINTS, amount, remark}` | `{id, mobile(脱敏), nickname, avatar, gender, birthday, levelId, levelName, points, growth, balance, status, lastLoginAt, createdAt}`；详情另含 `addresses[]` |
| 账户流水 `member/account-log` | `GET /page`（`memberId, accountType`） | `{id, memberId, accountType, changeValue, afterValue, bizType, bizNo, remark, createdAt}` |
| 等级 `member/level` | CRUD；`GET /list` | `{id, name, minGrowth, discount, icon, sort}` |

### 1.4 商品（权限前缀 `goods:`）

| 资源 | 接口 | 字段 |
|---|---|---|
| 类目 `goods/category` | `GET /tree`；POST / PUT / DELETE | `{id, parentId, name, icon, sort, status, children[]}`；最多两级；有子类目或商品不能删 |
| 品牌 `goods/brand` | CRUD；`GET /list` | `{id, name, logo, firstLetter, description, sort, status}` |
| 商品 `goods/goods` | CRUD；`PUT /status {ids:[], status: ON_SALE\|OFF_SALE}`；`PUT /{id}/flags {isNew, isHot, isRecommend}` | 见下 |

商品分页筛选：`keyword, categoryId, brandId, status`。列表项：
`{id, name, pic, categoryId, categoryName, brandName, price, originalPrice, stock, sales, status, isNew, isHot, isRecommend, sort, createdAt}`

商品表单（新增 / 修改 / 详情）：
```json
{
  "id": "…", "categoryId": "…", "brandId": "…", "name": "", "subtitle": "", "pic": "", "album": ["url"],
  "detail": "<p>富文本</p>", "unit": "件", "keywords": "", "virtualSales": 0, "sort": 0,
  "status": "ON_SALE", "isNew": 0, "isHot": 0, "isRecommend": 0,
  "freightTemplateId": "可空，空表示使用默认运费模板",
  "specItems": [{"name": "颜色", "values": ["红", "蓝"]}],
  "skus": [{"id": "可空", "specValues": ["红"], "code": "", "pic": "", "price": 99.00, "originalPrice": 129.00,
            "stock": 100, "originStock": 80, "weight": 0.500}]
}
```
无规格商品：`specItems = []`，`skus` 只有一条且 `specValues = []`。保存时 SPU 的 price / originalPrice 由 SKU 汇总，SPU stock 按 SKU 当前库存重算。

- `freightTemplateId`：运费模板（`GET /admin/trade/freight-template/list` 取下拉），空表示默认模板；默认模板也没有时沿用系统参数的一口价 + 满额包邮。
- `skus[].weight`：单件重量（kg，3 位小数），按重量计费的模板使用，默认 0。
- **库存差量调整**：修改已有 SKU（带 `id`）时，`stock` 是目标库存，`originStock` 必须原样回传编辑页打开时详情接口返回的 `stock`；
  服务端执行 `stock = stock + (stock目标 − originStock)` 的条件更新（结果不得 < 0，否则提示「库存已被订单占用…请刷新后重试」），
  编辑期间的下单扣减 / 取消回补不会被表单旧值覆盖。不传 `originStock` 表示不调整该 SKU 库存；新增 SKU（无 `id`）直接用 `stock`。
  价格、编码等其他字段照常覆盖，销量永远不由表单修改。

### 1.5 营销（权限前缀 `marketing:`）

| 资源 | 接口 | 字段 |
|---|---|---|
| 轮播 `marketing/banner` | CRUD | `{id, title, image, linkType: NONE\|GOODS\|CATEGORY\|URL, linkValue, position, sort, status}` |
| 公告 `marketing/notice` | CRUD | `{id, title, content, sort, status, createdAt}` |
| 优惠券 `marketing/coupon` | CRUD；`PUT /{id}/status?status=`；`GET /{id}/records`（分页，领取记录） | 字段同 mkt_coupon（驼峰）；记录 `{id, memberId, memberMobile, status, startAt, endAt, usedAt, orderId, createdAt}` |
| 秒杀 `marketing/seckill` | CRUD | `{id, title, goodsId, goodsName, goodsPic, skuId, specText, seckillPrice, stock, sold, limitPerMember, startAt, endAt, status, state: NOT_STARTED\|ONGOING\|ENDED}` |

### 1.6 交易（权限前缀 `trade:`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/trade/order/page` | 筛选 `orderNo, receiverMobile, status, orderType, startTime, endTime` |
| GET | `/admin/trade/order/{id}` | 详情：订单 + `items[]` + `logs[]` + `member{id,nickname,mobile}` |
| GET | `/admin/trade/order/status-count` | `{PENDING_PAY: n, PENDING_DELIVERY: n, DELIVERED: n, COMPLETED: n, CANCELLED: n}` |
| PUT | `/admin/trade/order/{id}/deliver` | `{deliveryCompany, deliveryNo}`，仅待发货；`deliveryCompany` 传字典 label（中文名）或 value（快递100 编码）均可 |
| GET | `/admin/trade/order/{id}/logistics` | 物流轨迹，权限 `trade:order:view`，出参见下方「物流 VO」 |
| PUT | `/admin/trade/order/{id}/price` | `{adjustAmount}` 改价，仅待支付，实付不得 < 0.01 |
| PUT | `/admin/trade/order/{id}/remark` | `{adminRemark}` |
| PUT | `/admin/trade/order/{id}/cancel` | `{reason}`，仅待支付 |
| GET | `/admin/trade/aftersale/page` | 筛选 `aftersaleNo, orderNo, status, type` |
| GET | `/admin/trade/aftersale/{id}` | 售后 + 订单明细快照 |
| PUT | `/admin/trade/aftersale/{id}/approve` | `{remark}`：仅退款 → 原路退款（REFUNDING → REFUNDED，见 §3）；退货退款 → WAIT_RETURN（退货地址三项都未配置时返回业务提示「请先在系统参数中设置退货地址」） |
| PUT | `/admin/trade/aftersale/{id}/reject` | `{remark}` |
| PUT | `/admin/trade/aftersale/{id}/receive` | 确认收到退货并退款（RETURNING → REFUNDING → REFUNDED） |
| GET | `/admin/trade/payment/page` | 支付流水，筛选 `payNo, orderNo, channel, status, startTime, endTime`（权限 `trade:payment:view`） |
| POST | `/admin/trade/payment/{id}/sync` | 向渠道查单并同步结果（回调丢失、对账；权限 `trade:payment:edit`），返回最新流水 |
| GET | `/admin/trade/review/page` | 筛选 `goodsId, rating, status, hasImage` |
| PUT | `/admin/trade/review/{id}/reply` | `{reply}` |
| PUT | `/admin/trade/review/{id}/status?status=` | 显示 / 隐藏 |
| GET | `/admin/trade/freight-template/page` | 运费模板分页，筛选 `keyword`（名称）, `chargeType`, `status`；默认模板排最前 |
| GET | `/admin/trade/freight-template/list` | 启用中的模板下拉 `[{id, name, chargeType, isDefault}]`（`trade:freight:view` 或 `goods:goods:view`） |
| GET | `/admin/trade/freight-template/{id}` | 详情 `FreightTemplate` |
| POST | `/admin/trade/freight-template` | 新增，入参见下，返回 ID（`trade:freight:edit`） |
| PUT | `/admin/trade/freight-template/{id}` | 修改，规则与包邮条件整体替换（`trade:freight:edit`） |
| DELETE | `/admin/trade/freight-template/{id}` | 删除；有商品引用时拒绝（`trade:freight:edit`） |

订单 VO：
```
{id, orderNo, memberId, memberNickname, orderType, status, statusText, goodsAmount, freightAmount, couponAmount,
 discountAmount, adjustAmount, payAmount, payType, payTime, payExpireAt, receiverName, receiverMobile,
 receiverProvince, receiverCity, receiverDistrict, receiverDetail, remark, adminRemark, deliveryCompany,
 deliveryNo, deliveryTime, receiveTime, cancelTime, cancelReason, reviewed, createdAt,
 items: [{id, goodsId, skuId, goodsName, pic, specText, price, quantity, totalAmount, payAmount, aftersaleStatus, reviewed}]}
```
售后 VO：`{id, aftersaleNo, orderId, orderNo, orderItemId, memberId, memberNickname, type, status, statusText, reason, description, images[], amount, quantity, auditRemark, auditTime, returnCompany, returnNo, refundTime, createdAt, item: OrderItem, returnAddress: {name, mobile, address} | null}`（`returnAddress` 规则见 §2.7）
运费模板 `FreightTemplate`（入参同结构，不含 id / createdAt）：
```json
{
  "id": "…", "name": "普通快递", "chargeType": "BY_COUNT | BY_WEIGHT", "isDefault": 1, "status": 1,
  "rules": [
    {"provinces": [], "firstUnit": 1, "firstFee": 8.00, "nextUnit": 1, "nextFee": 2.00},
    {"provinces": ["新疆维吾尔自治区", "西藏自治区"], "firstUnit": 1, "firstFee": 20.00, "nextUnit": 1, "nextFee": 10.00}
  ],
  "frees": [{"provinces": ["广东省"], "minCount": 3, "minAmount": 0}],
  "createdAt": "…"
}
```
- `rules`：至少一条；`provinces` 为省份名数组，空数组表示「默认地区（全国其他地区）」，最多一条；同一省份不能出现在两条规则里（「广西」与「广西壮族自治区」视为同一省）。
  按件时单位为件，按重量时单位为 kg；`nextUnit = 0` 表示超出首件 / 首重后不再续费。
- `frees`：指定地区（空数组 = 全部地区）满 `minCount` 件**或**满 `minAmount` 元（会员折扣后金额）包邮，两者至少设一个。
- `isDefault = 1` 时其他模板自动取消默认；停用的模板视同不存在，引用它的商品回退到默认模板。
物流 VO（管理端、商城端相同）：
```
{company: "shunfeng",            // 快递100 编码，无法识别时为空
 companyName: "顺丰速运", trackingNo: "SF100…",
 state: "SIGNED", stateText: "已签收",
 traces: [{time: "2026-10-31 18:30:00", context: "已签收…", location: "上海市"}],   // 时间倒序
 message: null}                  // 未开通 / 未发货 / 无结果 / 查询失败时的提示，此时 traces = []
```
`state` 取值：`NONE` 暂无物流信息、`COLLECTED` 已揽收、`IN_TRANSIT` 运输中、`DELIVERING` 派送中、`SIGNED` 已签收、`PROBLEM` 疑难件、`RETURN_SIGNED` 退件签收、`RETURNING` 退回中、`TRANSFERRED` 转投中、`CUSTOMS` 清关中、`REJECTED` 已拒签。
`message` 取值：`未开通物流查询`、`订单尚未发货`、`暂不支持查询该快递公司的物流`、`暂无物流轨迹，快递公司可能尚未揽收，请稍后再查`、`物流信息暂时无法查询，请稍后再试`。物流查询失败不影响订单详情接口。
结果缓存 Redis：在途 30 分钟、已签收 1 天、无结果 10 分钟；其他失败不缓存。

支付流水 VO：`{id, payNo, orderId, orderNo, memberId, channel(BALANCE|WECHAT|ALIPAY|MOCK), channelText, tradeType(WECHAT_JSAPI|WECHAT_H5|ALIPAY_WAP|空), amount, status(WAITING|SUCCESS|CLOSED|REFUNDED|REFUNDED_AFTER_CLOSE), statusText, channelNo, paidAt, remark, createdAt}`
评价 VO：`{id, orderId, goodsId, goodsName, goodsPic, memberId, nickname, avatar, specText, rating, content, images[], anonymous, reply, replyTime, status, createdAt}`

### 1.7 统计 `/admin/stats`（权限 `stats:view`）

| 方法 | 路径 | 出参 |
|---|---|---|
| GET | `/admin/stats/overview` | `{todaySales, todayOrders, todayMembers, yesterdaySales, yesterdayOrders, totalMembers, onSaleGoods, pendingDelivery, pendingAftersale, pendingPay}` |
| GET | `/admin/stats/trend?days=7\|30` | `[{date: "2026-10-31", sales, orders, members}]` |
| GET | `/admin/stats/top-goods?days=30&limit=10` | `[{goodsId, goodsName, pic, quantity, amount}]` |
| GET | `/admin/stats/order-status` | `[{status, statusText, count}]` |

---

## 2. 商城端 `/app/**`

### 2.1 认证 `/app/auth`（公开）

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| GET | `/app/auth/captcha` | – | `{captchaId, image}`，同 `/admin/auth/captcha`（两端验证码互不通用） |
| POST | `/app/auth/sms-code` | `{mobile, scene: REGISTER\|LOGIN\|RESET, captchaId, captchaCode}` | `{devCode}`（仅 demo 模式且未接短信渠道时返回验证码；prod 恒为空） |
| POST | `/app/auth/register` | `{mobile, code, password, nickname?}` | `{accessToken, refreshToken, expiresIn}` |
| POST | `/app/auth/login` | `{mobile, password, captchaId?, captchaCode?}` | 同上；失败 2 次后需要图形验证码，规则同管理端（`40002` + `needCaptcha`） |
| POST | `/app/auth/login-sms` | `{mobile, code}`（未注册自动注册） | 同上 |
| POST | `/app/auth/refresh` | `{refreshToken}` | 同上；每个 refreshToken 只能用一次 |
| POST | `/app/auth/reset-password` | `{mobile, code, password}` | – |

短信验证码 6 位，5 分钟有效，同一手机号 60 秒内只能发一次、每天 10 次，错误 5 次作废。
发送前必须先 `GET /app/auth/captcha` 并带上 `captchaId, captchaCode`（图形验证码一次性，发送失败也要重新获取）；
同一 IP 每小时 20 次、全站每分钟 60 次（可配置），超出返回 `42900`。
为防止手机号枚举，**发送接口对已注册 / 未注册手机号一律返回成功**：注册时手机号已注册、重置密码时手机号未注册，在提交 `register` / `reset-password` 时提示。
重置密码成功后该会员所有已登录的令牌失效。

**微信公众号网页授权登录**（仅微信内置浏览器；需配置 `wechat.mp.app_id / app_secret`）：

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| GET | `/app/auth/wechat/authorize-url?redirect=` | `redirect`：授权后回跳的商城页面完整地址（域名须在公众号「网页授权域名」中） | `{url, state}`，前端 `location.href = url` |
| POST | `/app/auth/wechat/login` | `{code, state}`（回跳 URL 上的参数；state 一次性、10 分钟有效） | 已绑定：`{needBind: false, accessToken, refreshToken, expiresIn}`；未绑定：`{needBind: true, bindToken}`（10 分钟有效） |
| POST | `/app/auth/wechat/bind` | `{bindToken, mobile, code}`，验证码用 `scene=LOGIN` 发送；手机号未注册自动注册 | `{accessToken, refreshToken, expiresIn}` |

使用 `snsapi_base` 静默授权，只取 openid（存 `mbr_social`，同时用于公众号 JSAPI 支付）；一个微信只能绑一个账号、一个账号只能绑一个微信。

### 2.2 公开浏览 `/app/public`

| 方法 | 路径 | 出参 |
|---|---|---|
| GET | `/app/public/home` | `{banners[], notices[], categories[](一级，含 icon), seckills[](进行中及即将开始，最多 4), newGoods[], hotGoods[], recommendGoods[]}`（商品各最多 10） |
| GET | `/app/public/category/tree` | 类目树（仅启用） |
| GET | `/app/public/goods/page` | 筛选 `keyword, categoryId(一级或二级), brandId, sort: DEFAULT\|SALES\|PRICE_ASC\|PRICE_DESC\|NEW` |
| GET | `/app/public/goods/{id}` | 商品详情：表单字段 + `skus[]` + `brandName` + `reviewCount, goodRate, latestReviews[](2 条)` + `favorited`(已登录时) + `seckill`(进行中的活动，可空) |
| GET | `/app/public/goods/{id}/reviews` | 分页，筛选 `type: ALL\|GOOD\|MEDIUM\|BAD\|IMAGE`；另 `GET /app/public/goods/{id}/review-stat` → `{total, good, medium, bad, image, goodRate}` |
| GET | `/app/public/coupon/available` | 可领取的券列表（已登录时含 `received` 已领数量） |
| GET | `/app/public/seckill/list` | 秒杀活动列表 `[SeckillVO]` |
| GET | `/app/public/config` | `{mallName, servicePhone, freightAmount, freightFreeThreshold}` |
| GET | `/app/public/notice/{id}` | 公告详情 |

商品列表项：`{id, name, subtitle, pic, price, originalPrice, sales(含虚拟销量), isNew, isHot}`

### 2.3 会员中心 `/app/member`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/app/member/profile` | `{id, mobile(脱敏), nickname, avatar, gender, birthday, levelName, discount, points, growth, balance, couponCount, favoriteCount, orderCount: {PENDING_PAY, PENDING_DELIVERY, DELIVERED, TO_REVIEW, AFTERSALE}, hasPassword, hasPayPassword}`；`hasPayPassword` 是否已设置余额支付密码 |
| PUT | `/app/member/profile` | `{nickname, avatar, gender, birthday}` |
| PUT | `/app/member/password` | `{oldPassword, newPassword}` → `{accessToken, refreshToken, expiresIn}`：其他设备上的登录全部失效，前端用返回的新令牌替换本地令牌 |
| POST | `/app/member/pay-password/sms-code` | `{captchaId, captchaCode}` → `{devCode?}`：给**当前会员自己的手机号**发送设置支付密码的验证码（场景 `PAY_PASSWORD`）；图形验证码与频控同 `/app/auth/sms-code`。公开的 `/app/auth/sms-code` **不接受** `PAY_PASSWORD` 场景，前端请用本接口 |
| PUT | `/app/member/pay-password` | `{code, payPassword}`：设置或修改余额支付密码（首次设置与修改相同，均凭短信验证码，不需要原支付密码）。`payPassword` 为 6 位数字，拒绝 6 位相同数字和逐位连续数字（123456、654321、012345 等）；成功后清零错误计数（解除锁定） |
| GET/POST/PUT/DELETE | `/app/member/address`、`/app/member/address/{id}` | 列表 / 新增 / 修改 / 删除；`PUT /app/member/address/{id}/default`；`GET /app/member/address/default` |
| GET | `/app/member/favorite/page` | 收藏商品分页（商品列表项） |
| POST | `/app/member/favorite/{goodsId}` / DELETE 同路径 | 收藏 / 取消 |
| GET | `/app/member/history/page` | 足迹分页；`DELETE /app/member/history` 清空 |
| GET | `/app/member/account-log/page` | `accountType` 筛选 |
| POST | `/app/member/upload` | 上传图片（头像、评价图），同 1.2 上传；每个会员每分钟 20 次、每天 200 次 |
| POST | `/app/member/logout` | `{refreshToken?}`：access 与 refresh 都作废 |

浏览商品详情时若已登录，自动记录足迹（由 `/app/public/goods/{id}` 写入）。

### 2.4 优惠券 `/app/coupon`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/app/coupon/{couponId}/receive` | 领取（校验库存、限领、状态） |
| GET | `/app/coupon/mine?status=UNUSED\|USED\|EXPIRED` | `[{id, couponId, name, type, threshold, amount, discount, maxAmount, scope, scopeIds, startAt, endAt, status, description}]` |

### 2.5 购物车 `/app/cart`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/app/cart` | `[{id, goodsId, skuId, goodsName, pic, specText, price, quantity, checked, stock, valid(在售且有库存)}]` |
| POST | `/app/cart` | `{skuId, quantity}`，已存在则累加 |
| PUT | `/app/cart/{id}` | `{quantity}` |
| PUT | `/app/cart/check` | `{ids: [], checked: true}`（ids 为空表示全部） |
| DELETE | `/app/cart` | `{ids: []}` |
| GET | `/app/cart/count` | 数量合计 |

### 2.6 订单 `/app/order`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/app/order/confirm` | 结算预览，入参 `OrderSubmit`，出参见下；`addressId` 可不传：回退到会员默认地址，无默认地址取最近修改的一个，再没有则 `address=null` 并按默认地区估算运费 |
| POST | `/app/order` | 提交订单（需幂等头），入参 `OrderSubmit`（**必须**传 `addressId`，前端用 confirm 返回的 `address.id`），出参 `{orderId, orderNo, payAmount, payExpireAt}` |
| GET | `/app/order/page?status=` | 我的订单；status 额外支持 `TO_REVIEW`（已完成未评价） |
| GET | `/app/order/{id}` | 订单详情（同管理端订单 VO，另含 `logs[]`） |
| GET | `/app/order/{id}/logistics` | 物流轨迹（只能查自己的订单），出参同管理端「物流 VO」 |
| POST | `/app/order/{id}/pay` | `{payType: BALANCE\|WECHAT_JSAPI\|WECHAT_H5\|ALIPAY_WAP\|MOCK, payPassword?}`（需幂等头），`payType=BALANCE` 时 `payPassword` 必填；出参见下「支付」 |
| GET | `/app/order/{id}/pay-status` | 支付结果轮询 `{orderId, status, statusText, paid, payType, payTime}`；本地未支付时服务端主动向渠道查单（同一支付单 3 秒内只查一次） |
| PUT | `/app/order/{id}/cancel` | `{reason}`，仅待支付 |
| PUT | `/app/order/{id}/receive` | 确认收货，仅待收货 |
| DELETE | `/app/order/{id}` | 删除订单（仅已完成 / 已取消，只对买家隐藏） |

`OrderSubmit`：
```json
{
  "source": "CART | BUY_NOW",
  "cartIds": ["…"],
  "items": [{"skuId": "…", "quantity": 1}],
  "seckillId": "可空，立即购买秒杀商品时传",
  "addressId": "…",
  "memberCouponId": "可空；confirm 时不传则自动选最优券，传 0 表示不用券",
  "remark": ""
}
```
`confirm` 出参：
```
{items: [{goodsId, skuId, goodsName, pic, specText, price, quantity, totalAmount}],
 address: {id, name, mobile, province, city, district, detail} | null, goodsAmount, freightAmount, couponAmount, discountAmount, payAmount,
 memberCouponId, coupons: [CouponOption], balance}
```

价格计算顺序：商品金额 → 会员折扣（discountAmount，秒杀商品不参与）→ 优惠券（按折后适用金额计算）→ 运费（见下）→ 应付 = 商品 − 折扣 − 券 + 运费（最低 0.01）。优惠按金额比例分摊到明细的 `payAmount`，尾差计入最后一行；运费不分摊到明细。

**运费规则**（结算预览与提交共用，金额一律用会员折扣后、用券前的金额，用券不会导致重新收运费）：
1. 全局满额包邮优先：系统参数 `trade.freight_free_threshold` > 0 且整单折后金额 ≥ 门槛 → 运费 0。
2. 商品按运费模板分组：商品指定的模板（启用中）→ 默认模板 → 都没有则按系统参数一口价 `trade.freight_amount`（门槛 ≤ 0 时这部分包邮）。
3. 同一模板的商品**合并**计件 / 计重：按收货省份匹配规则（指定省份优先，其次默认地区）；件数 / 重量 ≤ 首件（首重）收首费，超出部分按续件（续重）**向上取整**每档收一次续费。
4. 模板的指定条件包邮：收货省份命中且满足满件或满额之一 → 该模板运费 0。
5. 多个模板（及一口价部分）的运费**相加**。
6. 收货省份不在某模板的配送范围（无匹配规则且无默认地区规则）时，提交订单报错「收货地址不在「模板名」的配送范围内」；结算预览未选地址时按默认地区规则预估，无默认地区规则的模板按 0 预估。
**支付**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/app/pay/channels?orderId=&inWechat=` | 收银台可选支付方式 `[{type, name, icon, enabled, reason}]`；`inWechat` 由前端按 UA 是否含 `MicroMessenger` 传入 |
| POST | `/app/order/{id}/pay` | 发起支付 |
| GET | `/app/order/{id}/pay-status` | 从渠道返回后轮询 |

渠道列表规则：后台开关关闭的方式不返回；开启但不可用的返回 `enabled=false` 与 `reason`。
`BALANCE` 余额不足时不可用；`WECHAT_JSAPI` 须在微信内且会员已微信授权（有 openid）；`WECHAT_H5` 在微信内不可用；`ALIPAY_WAP` 在微信内不可用（提示在浏览器打开）；`MOCK` 仅 `MALL_PAY_MOCK_ENABLED=true` 时返回。`icon` 取值 `balance / wechat / alipay / mock`。

`pay` 出参 `{paid, payType, payNo?, payParams?}`：

| payType | 出参 | 前端调起 |
|---|---|---|
| `BALANCE` / `MOCK` | `{paid: true}` | 直接跳支付成功 |
| `WECHAT_JSAPI` | `{paid: false, payNo, payParams: {appId, timeStamp, nonceStr, package, signType, paySign}}` | `WeixinJSBridge.invoke('getBrandWCPayRequest', payParams, cb)`，回调后轮询 `pay-status` |
| `WECHAT_H5` | `{paid: false, payNo, payParams: {h5Url}}` | `location.href = h5Url + '&redirect_url=' + encodeURIComponent(结果页)`，回到结果页后轮询 |
| `ALIPAY_WAP` | `{paid: false, payNo, payParams: {payUrl}}` | `location.href = payUrl`，支付后支付宝跳回 `pay.return_url`，结果页轮询 |

- 同一订单重复发起：同方式、同金额、4 分钟内发起过的预支付单直接复用（返回同一 `payNo` 与参数）；其余待支付单先向渠道查单（已付款则直接落账并提示「订单已支付」），再关闭渠道订单。
- 渠道订单失效时间 = 订单支付截止时间 `payExpireAt`。

**余额支付密码**（`payType=BALANCE`，在扣余额之前校验；其他支付方式忽略 `payPassword`）：

| code | msg | 前端处理 |
|---|---|---|
| `60010` | 请先设置支付密码 | 引导到设置支付密码页（`PUT /app/member/pay-password`） |
| `60011` | 支付密码错误，还可尝试 N 次 | 展示 msg，清空输入重新输入 |
| `60012` | 支付密码错误次数过多，请 30 分钟后再试 | 展示 msg；可引导「忘记支付密码」凭短信重设（重设成功即解锁） |

- 连续错误 5 次锁定 30 分钟（第 5 次错误直接返回 `60012`）；锁定期间不校验密码直接返回 `60012`；校验正确或重设支付密码后清零。计数在 Redis（`mall:pay-pwd-fail:{memberId}`），从最后一次错误起 30 分钟过期。
- 校验失败时幂等 key 会被释放，可用同一个 `X-Idempotent-Key` 重试。

**支付回调**（公开，渠道调用；地址 = `pay.notify_base_url` + 路径，必须 HTTPS）：

| 路径 | 说明 | 响应 |
|---|---|---|
| `POST /app/public/pay/notify/wechat` | 微信支付结果通知（APIv3，SDK 验签 + 解密，核对商户号、AppID、金额） | 成功 204 无应答体；失败 4XX/5XX + `{"code":"FAIL","message":"…"}` |
| `POST /app/public/pay/notify/wechat-refund` | 微信退款结果通知 | 同上 |
| `POST /app/public/pay/notify/alipay` | 支付宝异步通知（RSA2 验签，核对 app_id、金额） | 纯文本 `success` / `fail` |

价格计算顺序：商品金额 → 会员折扣（discountAmount，秒杀商品不参与）→ 优惠券（按折后适用金额计算）→ 运费（按折后金额判断包邮）→ 应付 = 商品 − 折扣 − 券 + 运费（最低 0.01）。优惠按金额比例分摊到明细的 `payAmount`，尾差计入最后一行。

### 2.7 售后与评价

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/app/aftersale` | `{orderItemId, type, reason, description, images[], amount}`：待发货可仅退款；待收货、已完成（售后期内）可退款或退货退款 |
| GET | `/app/aftersale/page` | 我的售后 |
| GET | `/app/aftersale/{id}` | 详情 |
| PUT | `/app/aftersale/{id}/cancel` | 撤销（仅 APPLYING / WAIT_RETURN） |
| PUT | `/app/aftersale/{id}/return` | `{returnCompany, returnNo}` 填写退货物流（WAIT_RETURN → RETURNING）；寄往售后 VO 的 `returnAddress` |
| POST | `/app/review` | `{orderItemId, rating, content, images[], anonymous}`，仅已完成订单的明细，每条只能评一次；全部明细已评则订单 reviewed=1 |

售后 VO（商城端与管理端相同，见 §1.6）新增 `returnAddress: {name, mobile, address} | null`：仅 `type=RETURN_REFUND` 且 `status` 为 `WAIT_RETURN / RETURNING / REFUNDING / REFUNDED` 时返回，取自系统参数 `trade.return_receiver_name` / `trade.return_receiver_mobile` / `trade.return_address`（当前值，不做快照）；三项都为空时为 `null`。

评价、售后的 `images[]` 只接受本站上传接口返回的路径（`/uploads/...`），外链会被拒绝。
| GET | `/app/review/mine` | 我的评价分页 |

---

## 3. 状态机

**订单**

```
PENDING_PAY ──支付──▶ PENDING_DELIVERY ──发货──▶ DELIVERED ──确认收货/超时自动──▶ COMPLETED
     │
     └──买家取消 / 后台取消 / 超时关单──▶ CANCELLED（回补库存、退还优惠券、归还秒杀名额）
```
- 待发货订单全部明细退款完成 → 订单 CANCELLED，并与取消订单一致：额外原路退还运费（退款单号 = 售后单号 + `F`）、退还优惠券（已过期置 EXPIRED）、归还秒杀名额。
- 确认收货后发生退款：按「退款额 / 发放基数」比例回收确认收货时发放的积分与成长值（向下取整），积分不足扣到 0 并写流水；会员等级不降。
- 支付成功：按实付金额赠送积分与成长值（sys_config 规则），确认收货时发放。

**售后**

```
APPLYING ─同意(仅退款)─▶ REFUNDING ─退款到账─▶ REFUNDED
APPLYING ─同意(退货退款)─▶ WAIT_RETURN ─买家填物流─▶ RETURNING ─商家确认收货─▶ REFUNDING ─退款到账─▶ REFUNDED
APPLYING ─拒绝─▶ REJECTED        APPLYING / WAIT_RETURN ─买家撤销─▶ CANCELLED
```
- `REFUNDING` 退款中：已向渠道发起退款、等待结果；占用可退额度，不能撤销，订单不能发货。
- 退款原路退回（按订单 `payType`）：
  - `BALANCE` 退回余额，同一事务内直接 `REFUNDED`；
  - `ALIPAY` 调 `alipay.trade.refund`，同步返回资金已变动即 `REFUNDED`，否则保持 `REFUNDING` 由补偿任务查询；
  - `WECHAT` 调 v3 退款，通常 `PROCESSING`，由退款回调 `/app/public/pay/notify/wechat-refund` 或补偿任务推进为 `REFUNDED`；渠道退款失败（如 ABNORMAL）保持 `REFUNDING`，审核备注追加「【退款失败：原因】」，需到商户平台处理；
  - `MOCK` 不经渠道，直接 `REFUNDED`，审核备注追加「【需线下处理】」。
- 渠道退款单号 = 售后单号，重复发起时渠道按单号幂等。退款完成（`REFUNDED`）时回补库存（未发货时）。
- 明细 `aftersaleStatus`：申请中 APPLYING，退款完成 REFUNDED，被拒或撤销恢复 NONE。

**支付流水**（`trd_payment.status`）

```
WAITING ─回调 / 查单确认已付─▶ SUCCESS ─整单退完─▶ REFUNDED
WAITING ─换支付方式 / 订单关闭后补偿关单 / 渠道已关闭─▶ CLOSED
WAITING / CLOSED ─到账时订单已关闭、已被其他支付单付清或金额已变更─▶ REFUNDED_AFTER_CLOSE（自动原路退款）
```

**定时任务**（每分钟）：关闭超时未支付订单；自动确认收货；过期优惠券置为 EXPIRED；
支付补偿（第 45 秒，分布式锁）：5 分钟前发起仍 `WAITING` 的微信 / 支付宝流水主动查单（已付 → 落账或关单后到账自动退款；订单已关闭仍未付 → 关闭渠道订单），退款中超过 5 分钟的售后单查询渠道退款结果。

---

## 4. 管理端权限码与前端路由

后台前端使用**静态路由**，每个路由在 `meta.permission` 声明查看权限，菜单按 `/admin/auth/me` 返回的 `permissions` 过滤显示（超级管理员角色 `super_admin` 全部可见）；
`sys_menu` 表维护同一棵树，用于角色授权。查询接口要求 `*:view`，写接口要求 `*:edit`（个别见备注）。

| 目录 | 菜单（前端路由） | 查看 | 操作 |
|---|---|---|---|
| 首页 | 数据概览 `/dashboard` | `stats:view` | – |
| 商品 | 商品列表 `/goods/list`、发布商品 `/goods/edit/:id?`（隐藏） | `goods:goods:view` | `goods:goods:edit` |
| | 商品类目 `/goods/category` | `goods:category:view` | `goods:category:edit` |
| | 品牌管理 `/goods/brand` | `goods:brand:view` | `goods:brand:edit` |
| 订单 | 订单列表 `/trade/order`、订单详情 `/trade/order/:id`（隐藏） | `trade:order:view` | `trade:order:edit`（改价/备注/取消）、`trade:order:deliver`（发货） |
| | 售后处理 `/trade/aftersale` | `trade:aftersale:view` | `trade:aftersale:audit` |
| | 支付流水 `/trade/payment` | `trade:payment:view` | `trade:payment:edit`（手动查单） |
| | 评价管理 `/trade/review` | `trade:review:view` | `trade:review:edit` |
| | 运费模板 `/trade/freight` | `trade:freight:view` | `trade:freight:edit` |
| 营销 | 优惠券 `/marketing/coupon` | `marketing:coupon:view` | `marketing:coupon:edit` |
| | 秒杀活动 `/marketing/seckill` | `marketing:seckill:view` | `marketing:seckill:edit` |
| | 轮播广告 `/marketing/banner` | `marketing:banner:view` | `marketing:banner:edit` |
| | 商城公告 `/marketing/notice` | `marketing:notice:view` | `marketing:notice:edit` |
| 会员 | 会员列表 `/member/list` | `member:member:view` | `member:member:edit`（启停、调账） |
| | 会员等级 `/member/level` | `member:level:view` | `member:level:edit` |
| 系统 | 管理员 `/system/admin` | `system:admin:view` | `system:admin:edit` |
| | 角色权限 `/system/role` | `system:role:view` | `system:role:edit` |
| | 菜单管理 `/system/menu` | `system:menu:view` | `system:menu:edit` |
| | 数据字典 `/system/dict` | `system:dict:view` | `system:dict:edit` |
| | 系统参数 `/system/config` | `system:config:view` | `system:config:edit` |
| | 操作日志 `/system/oper-log`、登录日志 `/system/login-log` | `system:log:view` | – |

上传接口 `/admin/system/file/upload` 只需登录。

字典类型（种子数据）：`express_company` 快递公司（label 中文名，value 为快递100 编码：顺丰速运 `shunfeng`、中通快递 `zhongtong`、圆通速递 `yuantong`、韵达快递 `yunda`、申通快递 `shentong`、京东物流 `jd`、EMS `ems`、极兔速递 `jtexpress`；V7 起由 SF/ZTO… 改为快递100 编码，新增快递公司时 value 填快递100 编码（快递100 企业后台「快递公司编码」表）），详见 [integration.md](integration.md)。订单 `deliveryCompany` 历史上保存 label，查询物流时按 value → label → 旧编码依次匹配，旧订单无需迁移；`aftersale_reason` 售后原因（不想要了、商品与描述不符、质量问题、少件/漏发、发错货、其他）、`cancel_reason` 取消原因（不想买了、信息填写错误、重复下单、其他）。

---

## 5. 实现补充（v1.0 落地时确定）

| 项 | 约定 |
|---|---|
| 新增接口返回值 | `POST` 新增类接口返回新记录 ID；领取优惠券返回会员券 ID |
| 字典下拉 | `GET /admin/system/dict/data/{dictType}`（只需登录）、`GET /app/public/dict/{dictType}`（公开），返回 `[{label, value}]` |
| 商城端登出 | `POST /app/member/logout {refreshToken?}`，作废当前 access 令牌与提交的 refresh 令牌 |
| 售后类型 | `REFUND` 仅退款、`RETURN_REFUND` 退货退款；待发货订单的退款必须为该明细剩余可退全额（并回补库存） |
| 订单日志 | `logs[] = {fromStatus, toStatus, operator, remark, createdAt}`，operator 为 `member:<id>` / `admin:<id>` / `system` |
| 好评率 | `goodRate` 为 0–100 的整数（4–5 星占比），无评价时为 100 |
| 秒杀库存 | `stock` 为活动划拨总量，剩余 = `stock − sold`；同一 SKU 启用中的活动时间不可重叠；秒杀订单不可用券、不享会员折扣 |
| 运费 | 是否包邮按会员折扣后、用券前的金额判断，用券不会导致重新收运费；运费模板规则见 §2.6 |
| 改价 | `adjustAmount` 为订单总调整额（覆盖上次），按比例重新分摊到明细（每件先预留 0.01）；改价后应付不得低于「运费 + 0.01 × 商品件数」；与支付并发时以金额条件互斥，支付侧提示「订单金额已变化，请刷新后重试」 |
| 售后与发货 | 有进行中售后的订单不能发货，也暂停自动确认收货（自动收货任务直接排除这类订单）；发货与售后申请都先锁订单行，串行执行 |
| 统计口径 | 销售额 = 实付 − 已退款，按支付时间归属日期；支付后整单退款取消的订单不计入销售额与订单数；热销商品金额扣除明细退款，全额退款的明细不计销量 |
| 秒杀限购 | 个人限购以「该会员该活动未取消订单的件数」兜底，Redis 计数丢失不会放开限购 |
| 支付 | `pay` 返回 `{paid}`；接入微信/支付宝时新增 `PayChannel` 实现，回调走 `/app/public/pay/notify/{channel}` 并调用 `PayService.onPaid(payNo, channelNo)`（重复通知幂等） |
| 运费 | 是否包邮按会员折扣后、用券前的金额判断，用券不会导致重新收运费 |
| 改价 | `adjustAmount` 为订单总调整额（覆盖上次），按比例重新分摊到明细 |
| 售后与发货 | 有进行中售后的订单不能发货，也暂停自动确认收货 |
| 支付 | 渠道实现 `PayChannel`（同步）/ `AsyncPayChannel`（下单、查单、关单、查退款）；回调、轮询查单、补偿任务、后台查单统一走 `PaySettlementService.settle`：金额以分比较、流水终态幂等、订单待付款时调用 `PayService.onPaid` 落账，否则自动原路退款并标记 `REFUNDED_AFTER_CLOSE`（错误日志 + 订单日志）。支付配置存 `sys_config`（`pay.*`、`wechat.*`），后台修改后不重启生效（SDK 客户端按配置快照重建），详见 [payment.md](payment.md) |
| 微信登录 | 公众号网页授权 `snsapi_base`；openid 存 `mbr_social(platform=WECHAT_MP)`，唯一索引 `platform+openid`、`member_id+platform`；令牌与密码登录同一签发方法 |
| 会员资料 | 额外返回 `hasPassword`、`hasPayPassword`；验证码注册的会员首次设置密码时可不传 `oldPassword` |
| 退货地址 | 系统参数 `trade.return_receiver_name`（退货收件人）、`trade.return_receiver_mobile`（退货联系电话）、`trade.return_address`（退货地址），默认空串（V8 迁移）；后台在「系统参数」中维护，售后 VO 按 §2.7 规则返回 |
| 支付密码 | `mbr_member.pay_password`（BCrypt，空串 = 未设置）；验证码场景 `PAY_PASSWORD` 只能经 `POST /app/member/pay-password/sms-code` 发给本人；错误码 `60010` 未设置 / `60011` 错误（含剩余次数）/ `60012` 锁定 30 分钟；操作日志对 `payPassword` 字段脱敏，服务日志不记录密码 |
| 结算默认地址 | `confirm` 未传 `addressId` 时按「默认地址 → 最近修改的地址 → null」回退并在 `address.id` 返回；`POST /app/order` 仍必须传 `addressId` |
| 收藏 / 足迹项 | `{id(商品ID), name, pic, price, originalPrice, sales, onSale, favoritedAt / browsedAt}`；收藏上限 500，足迹保留最近 200 条 |
| 令牌时效 | access 2 小时、refresh 7 天且每次刷新换新（旧 refresh 立即作废，重放返回 40100） |
| 令牌失效 | 令牌带版本号 `tv`：会员改密 / 重置密码 / 被冻结，管理员改密 / 被重置密码 / 被停用 / 角色调整后，该用户所有已签发令牌**立即**失效（40100）；仅修改角色的菜单权限仍在下次刷新令牌时生效 |
| 富文本 | 公告内容、商品详情保存时在服务端用 `HtmlSanitizer` 白名单清洗（去掉脚本、事件属性、javascript: 链接、危险 CSS），前端仍可 v-html 渲染 |
