# 商城接口约定

> 前后端唯一接口契约。后端实现与前端调用都以本文为准；如需调整，先改本文再改代码。
> 在线文档：demo 模式部署后访问 `/doc.html`（Knife4j，分「管理端」「商城端」两组）；prod 模式不提供接口文档。

## 0. 通用

| 项 | 约定 |
|---|---|
| 响应 | `{ code, msg, data, ts, traceId }`，`code = 0` 成功；`40100` 未登录 / 令牌过期 / 令牌已失效；`40300` 无权限；`40301` 请先修改初始密码（见 1.1）；`40002` 需要图形验证码（`data.needCaptcha = true`）；`42900` 请求过于频繁；`50300` 服务暂不可用（Redis / 数据库故障）；`60000` 业务提示（直接展示 msg） |
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
| 参数 `system/config` | `GET /list` 全部；`PUT /batch {items:[{configKey, configValue}]}` | `{id, configKey, configValue, name, remark}` |
| 操作日志 `system/oper-log` | `GET /page`（`username, module, success, startTime, endTime`） | `{id, username, module, action, method, uri, params, ip, success, errorMsg, costMs, createdAt}` |
| 登录日志 `system/login-log` | `GET /page`（`userType, username, success`） | `{id, userType, username, ip, userAgent, success, message, createdAt}` |
| 上传 `system/file/upload` | POST multipart `file`, `bizType` | `{id, url, originalName, size}`；只收 jpg/jpeg/png/gif/webp，文件头必须与扩展名一致 |

操作日志：管理端所有 POST/PUT/DELETE 请求自动记录（切面实现，密码字段脱敏）。

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
  "specItems": [{"name": "颜色", "values": ["红", "蓝"]}],
  "skus": [{"id": "可空", "specValues": ["红"], "code": "", "pic": "", "price": 99.00, "originalPrice": 129.00, "stock": 100}]
}
```
无规格商品：`specItems = []`，`skus` 只有一条且 `specValues = []`。保存时 SPU 的 price / originalPrice / stock 由 SKU 汇总。

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
| PUT | `/admin/trade/order/{id}/deliver` | `{deliveryCompany, deliveryNo}`，仅待发货 |
| PUT | `/admin/trade/order/{id}/price` | `{adjustAmount}` 改价，仅待支付，实付不得 < 0.01 |
| PUT | `/admin/trade/order/{id}/remark` | `{adminRemark}` |
| PUT | `/admin/trade/order/{id}/cancel` | `{reason}`，仅待支付 |
| GET | `/admin/trade/aftersale/page` | 筛选 `aftersaleNo, orderNo, status, type` |
| GET | `/admin/trade/aftersale/{id}` | 售后 + 订单明细快照 |
| PUT | `/admin/trade/aftersale/{id}/approve` | `{remark}`：仅退款 → 直接退款完成；退货退款 → WAIT_RETURN |
| PUT | `/admin/trade/aftersale/{id}/reject` | `{remark}` |
| PUT | `/admin/trade/aftersale/{id}/receive` | 确认收到退货并退款（RETURNING → REFUNDED） |
| GET | `/admin/trade/review/page` | 筛选 `goodsId, rating, status, hasImage` |
| PUT | `/admin/trade/review/{id}/reply` | `{reply}` |
| PUT | `/admin/trade/review/{id}/status?status=` | 显示 / 隐藏 |

订单 VO：
```
{id, orderNo, memberId, memberNickname, orderType, status, statusText, goodsAmount, freightAmount, couponAmount,
 discountAmount, adjustAmount, payAmount, payType, payTime, payExpireAt, receiverName, receiverMobile,
 receiverProvince, receiverCity, receiverDistrict, receiverDetail, remark, adminRemark, deliveryCompany,
 deliveryNo, deliveryTime, receiveTime, cancelTime, cancelReason, reviewed, createdAt,
 items: [{id, goodsId, skuId, goodsName, pic, specText, price, quantity, totalAmount, payAmount, aftersaleStatus, reviewed}]}
```
售后 VO：`{id, aftersaleNo, orderId, orderNo, orderItemId, memberId, memberNickname, type, status, statusText, reason, description, images[], amount, quantity, auditRemark, auditTime, returnCompany, returnNo, refundTime, createdAt, item: OrderItem}`
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
| GET | `/app/member/profile` | `{id, mobile(脱敏), nickname, avatar, gender, birthday, levelName, discount, points, growth, balance, couponCount, favoriteCount, orderCount: {PENDING_PAY, PENDING_DELIVERY, DELIVERED, TO_REVIEW, AFTERSALE}}` |
| PUT | `/app/member/profile` | `{nickname, avatar, gender, birthday}` |
| PUT | `/app/member/password` | `{oldPassword, newPassword}` → `{accessToken, refreshToken, expiresIn}`：其他设备上的登录全部失效，前端用返回的新令牌替换本地令牌 |
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
| GET | `/app/coupon/mine?status=UNUSED\|USED\|EXPIRED` | `[{id, couponId, name, type, threshold, amount, discount, maxAmount, scope, startAt, endAt, status, description}]` |

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
| POST | `/app/order/confirm` | 结算预览，入参 `OrderSubmit`，出参见下 |
| POST | `/app/order` | 提交订单（需幂等头），入参 `OrderSubmit`，出参 `{orderId, orderNo, payAmount, payExpireAt}` |
| GET | `/app/order/page?status=` | 我的订单；status 额外支持 `TO_REVIEW`（已完成未评价） |
| GET | `/app/order/{id}` | 订单详情（同管理端订单 VO，另含 `logs[]`） |
| POST | `/app/order/{id}/pay` | `{payType: BALANCE\|MOCK}`（需幂等头），返回 `{paid: true}` |
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
 address: Address | null, goodsAmount, freightAmount, couponAmount, discountAmount, payAmount,
 memberCouponId, coupons: [CouponOption], balance}
```

价格计算顺序：商品金额 → 会员折扣（discountAmount，秒杀商品不参与）→ 优惠券（按折后适用金额计算）→ 运费（按折后金额判断包邮）→ 应付 = 商品 − 折扣 − 券 + 运费（最低 0.01）。优惠按金额比例分摊到明细的 `payAmount`，尾差计入最后一行。

### 2.7 售后与评价

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/app/aftersale` | `{orderItemId, type, reason, description, images[], amount}`：待发货可仅退款；待收货、已完成（售后期内）可退款或退货退款 |
| GET | `/app/aftersale/page` | 我的售后 |
| GET | `/app/aftersale/{id}` | 详情 |
| PUT | `/app/aftersale/{id}/cancel` | 撤销（仅 APPLYING / WAIT_RETURN） |
| PUT | `/app/aftersale/{id}/return` | `{returnCompany, returnNo}` 填写退货物流（WAIT_RETURN → RETURNING） |
| POST | `/app/review` | `{orderItemId, rating, content, images[], anonymous}`，仅已完成订单的明细，每条只能评一次；全部明细已评则订单 reviewed=1 |

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
- 待发货订单全部明细退款完成 → 订单 CANCELLED。
- 支付成功：按实付金额赠送积分与成长值（sys_config 规则），确认收货时发放。

**售后**

```
APPLYING ─同意(仅退款)─▶ REFUNDED
APPLYING ─同意(退货退款)─▶ WAIT_RETURN ─买家填物流─▶ RETURNING ─商家确认收货─▶ REFUNDED
APPLYING ─拒绝─▶ REJECTED        APPLYING / WAIT_RETURN ─买家撤销─▶ CANCELLED
```
- 退款原路退回：余额支付退回余额；MOCK 支付视为线下退款，只记录。退款成功回补库存（未发货时）。
- 明细 `aftersaleStatus`：申请中 APPLYING，退款完成 REFUNDED，被拒或撤销恢复 NONE。

**定时任务**（每分钟）：关闭超时未支付订单；自动确认收货；过期优惠券置为 EXPIRED。

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
| | 评价管理 `/trade/review` | `trade:review:view` | `trade:review:edit` |
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

字典类型（种子数据）：`express_company` 快递公司（顺丰、中通、圆通、韵达、申通、京东、EMS、极兔）、`aftersale_reason` 售后原因（不想要了、商品与描述不符、质量问题、少件/漏发、发错货、其他）、`cancel_reason` 取消原因（不想买了、信息填写错误、重复下单、其他）。

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
| 运费 | 是否包邮按会员折扣后、用券前的金额判断，用券不会导致重新收运费 |
| 改价 | `adjustAmount` 为订单总调整额（覆盖上次），按比例重新分摊到明细 |
| 售后与发货 | 有进行中售后的订单不能发货，也暂停自动确认收货 |
| 支付 | `pay` 返回 `{paid}`；接入微信/支付宝时新增 `PayChannel` 实现，回调走 `/app/public/pay/notify/{channel}` 并调用 `PayService.onPaid(payNo, channelNo)`（重复通知幂等） |
| 会员资料 | 额外返回 `hasPassword`；验证码注册的会员首次设置密码时可不传 `oldPassword` |
| 收藏 / 足迹项 | `{id(商品ID), name, pic, price, originalPrice, sales, onSale, favoritedAt / browsedAt}`；收藏上限 500，足迹保留最近 200 条 |
| 令牌时效 | access 2 小时、refresh 7 天且每次刷新换新（旧 refresh 立即作废，重放返回 40100） |
| 令牌失效 | 令牌带版本号 `tv`：会员改密 / 重置密码 / 被冻结，管理员改密 / 被重置密码 / 被停用 / 角色调整后，该用户所有已签发令牌**立即**失效（40100）；仅修改角色的菜单权限仍在下次刷新令牌时生效 |
| 富文本 | 公告内容、商品详情保存时在服务端用 `HtmlSanitizer` 白名单清洗（去掉脚本、事件属性、javascript: 链接、危险 CSS），前端仍可 v-html 渲染 |
