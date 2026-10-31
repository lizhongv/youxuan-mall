# 支付与微信登录接入指南

> 适用：商城 H5（公众号内 + 手机浏览器）。接口契约见 [api.md](api.md) §2.1、§2.6、§3。
> 实现：`mall-server/.../trade/pay/`（渠道、收银台、结算、补偿）、`member/.../MemberWechatAuthServiceImpl`（网页授权登录）。
> SDK：微信支付官方 `com.github.wechatpay-apiv3:wechatpay-java:0.2.17`；支付宝官方 `com.alipay.sdk:alipay-sdk-java:4.40.1061.ALL`。

## 1. 支持的支付方式

| payType | 渠道 | 场景 | 需要 |
|---|---|---|---|
| `BALANCE` | 余额 | 任意 | – |
| `WECHAT_JSAPI` | 微信支付 | 微信内打开商城（公众号支付） | 已认证服务号 + 商户号，会员已微信授权登录 |
| `WECHAT_H5` | 微信支付 | 手机浏览器（非微信）唤起微信 | 商户平台单独开通「H5 支付」并配置 H5 域名 |
| `ALIPAY_WAP` | 支付宝 | 手机浏览器（微信内不可用） | 开放平台应用签约「手机网站支付」 |
| `MOCK` | 模拟 | 演示 / 冒烟测试 | `MALL_PAY_MOCK_ENABLED=true`，**生产必须关闭** |

## 2. 前置条件

1. **域名与备案**：商城域名完成 ICP 备案，配置 HTTPS（有效证书，TLS1.2+）。微信要求回调地址为 HTTPS、不能带参数、外网可直接访问；支付宝同样建议 HTTPS。
2. **回调根地址** `pay.notify_base_url`：Nginx 把 `/api/` 反代到服务端（见 `deploy/mall/nginx/mall.conf`），因此填 `https://你的域名/api`。系统据此拼出：
   - `https://你的域名/api/app/public/pay/notify/wechat`（微信支付结果）
   - `https://你的域名/api/app/public/pay/notify/wechat-refund`（微信退款结果）
   - `https://你的域名/api/app/public/pay/notify/alipay`（支付宝）
   这些路径是公开接口，不要在网关 / WAF 上拦截，也不要做跳转（301/302 会导致回调失败）。
3. **服务器时间**准确（NTP），微信回调验签会校验时间戳。

## 3. 微信公众号与微信支付

### 3.1 申请

1. 注册并认证**服务号**（订阅号不能网页授权、不能支付）。
2. 微信支付商户平台 <https://pay.weixin.qq.com> 申请商户号，在「产品中心」开通 **JSAPI 支付**（需要 H5 支付时另开通 **H5 支付** 并填写 H5 支付域名）。
3. 商户平台「产品中心 / 开发配置」：
   - **JSAPI 支付授权目录**：填商城 H5 页面所在目录，如 `https://你的域名/`；
   - 「AppID 账号管理」关联上面的服务号 AppID。
4. 公众号后台「设置与开发 / 公众号设置 / 功能设置」：**网页授权域名**填商城域名（需上传校验文件到站点根目录）。

### 3.2 证书与密钥（商户平台「账户中心 / API 安全」）

| 项 | 说明 | 后台参数 |
|---|---|---|
| 商户 API 证书 | 用证书工具申请，得到 `apiclient_key.pem`（私钥）与证书序列号 | `pay.wechat.private_key`（粘贴 pem 全文）、`pay.wechat.mch_serial_no` |
| APIv3 密钥 | 自行设置的 32 位字符串，用于回调报文解密 | `pay.wechat.api_v3_key` |
| 微信支付公钥 | 2024 年下半年起新商户默认使用「微信支付公钥」验签，下载 `pub_key.pem` 并记下公钥 ID（`PUB_KEY_ID_` 开头） | `pay.wechat.public_key_id`、`pay.wechat.public_key` |
| 平台证书 | 老商户使用；系统用 APIv3 密钥自动下载并每 60 分钟更新，无需手工配置 | 两个公钥参数留空即为此模式 |

### 3.3 后台配置（系统 / 系统参数，「支付配置」分组）

| 键 | 填写 |
|---|---|
| `pay.wechat.enabled` | `true` 开启公众号支付 |
| `pay.wechat.h5_enabled` | `true` 开启 H5 支付（须已在商户平台开通） |
| `pay.wechat.app_id` | 服务号 AppID；留空则使用 `wechat.mp.app_id`（支付与登录必须是同一公众号，因为 openid 按公众号区分） |
| `pay.wechat.mch_id` | 商户号 |
| `pay.wechat.mch_serial_no` | 商户 API 证书序列号 |
| `pay.wechat.api_v3_key` | APIv3 密钥（敏感） |
| `pay.wechat.private_key` | `apiclient_key.pem` 全文（敏感，含 BEGIN/END 行） |
| `pay.wechat.public_key_id` / `pay.wechat.public_key` | 微信支付公钥模式时填写 |
| `wechat.mp.app_id` / `wechat.mp.app_secret` | 公众号 AppID / AppSecret（公众号后台「基本配置」，AppSecret 敏感），用于网页授权登录 |

## 4. 支付宝

1. 支付宝开放平台 <https://open.alipay.com> 创建「网页 / 移动应用」，签约 **手机网站支付**。
2. 「开发设置 / 接口加签方式」选 **公钥** 模式：用支付宝密钥工具生成 RSA2 密钥对，上传**应用公钥**，平台返回**支付宝公钥**。（证书模式只在资金转出类接口强制要求，本系统用不到。）
3. 后台配置：

| 键 | 填写 |
|---|---|
| `pay.alipay.enabled` | `true` |
| `pay.alipay.app_id` | 应用 AppID |
| `pay.alipay.private_key` | 应用私钥（PKCS8，Java 版密钥工具生成的那一串，敏感） |
| `pay.alipay.public_key` | **支付宝公钥**（不是应用公钥，填错会导致验签全部失败） |
| `pay.alipay.gateway` | 正式 `https://openapi.alipay.com/gateway.do`；沙箱 `https://openapi-sandbox.dl.alipaydev.com/gateway.do` |
| `pay.return_url` | 支付完成后跳回的商城结果页，`{orderId}` 会被替换，如 `https://你的域名/pay/result?orderId={orderId}` |

## 5. 配置生效与安全

- 参数保存后本节点立即生效，其他节点最多 1 分钟（系统参数本地缓存周期）；配置任一项变化时自动重建 SDK 客户端，无需重启。
- SDK 初始化失败（私钥格式错、APIv3 密钥错、平台证书下载失败）时，对应支付方式在收银台显示「暂不可用」，服务端日志有 `微信支付 客户端构建失败` 一行说明原因（不含密钥），1 分钟内不重复尝试；改正配置后立即重试。
- 敏感参数（私钥、APIv3 密钥、AppSecret）列表只显示 `******`，提交 `******` 表示不修改；操作日志对这些值脱敏；服务端日志只打印商户号、单号、金额。
- 回调只信任验签结果：微信由 SDK 验签并解密后再核对商户号与 AppID，支付宝验签后核对 `app_id`；金额一律换算成「分」与支付流水比较，不符拒绝落账并记错误日志。
- 上线后务必设置 `MALL_PAY_MOCK_ENABLED=false`。

## 6. 业务规则速览

- **发起**：每次发起写一条 `trd_payment`（`WAITING`）。同一订单 4 分钟内用同一方式再次发起时复用预支付单；换方式或超时后先向渠道查单（已付款则直接落账）再关闭旧渠道订单。渠道订单失效时间 = 订单支付截止时间。
- **落账**：回调、前端轮询 `pay-status`、补偿任务、后台「查单」都走同一个幂等方法；重复通知直接返回成功。
- **关单后到账**：订单已超时关闭（或已用其他方式付清、或后台改价后旧单到账）时，系统自动原路退款，流水标记 `REFUNDED_AFTER_CLOSE`，并写错误日志 `【关单后到账】` 与订单日志，便于客服解释。退款单号 `R + 支付单号`。
- **补偿任务**（每分钟第 45 秒，多实例只跑一个）：5 分钟前发起仍待支付的流水主动查单；退款中的售后单查询退款结果。
- **售后退款**：原路退回。余额、支付宝通常即时完成；微信退款为异步，售后单先进入 `REFUNDING`，收到退款回调后变为 `REFUNDED`；模拟支付直接完成并在备注注明「需线下处理」。渠道退款失败时售后单保持 `REFUNDING`，备注写明原因，请到商户平台处理（如微信「异常退款」）。

## 7. 测试方法

1. **单元测试**（不依赖外网）：`./mvnw -pl mall-server -am test`，渠道 SDK 客户端均为 mock，覆盖下单参数、回调验签失败 / 金额不符 / 重复通知、查单补偿、关单后到账自动退款、原路退款分发、渠道可用性、微信登录绑定。
2. **支付宝沙箱**：开放平台「沙箱环境」获取沙箱 AppID、密钥与沙箱买家账号，网关改为沙箱地址，用手机浏览器打开商城下单，选择支付宝，用沙箱买家账号付款。
3. **微信**：微信支付没有公开沙箱，使用正式商户号小额（0.01 元）测试：后台改价到 0.01 → 公众号内打开商城 → 微信授权登录 → 选择微信支付 → 支付 → 观察订单变为待发货；再申请售后仅退款，观察售后单 `REFUNDING → REFUNDED`。
4. **回调连通性**：在服务器外网执行 `curl -i -X POST https://你的域名/api/app/public/pay/notify/alipay`，应返回 `fail`（验签不通过属正常），说明路径可达；返回 404 / 302 需检查 Nginx。
5. **补偿**：临时在防火墙屏蔽回调后支付，5 分钟内订单应由补偿任务（或支付结果页轮询）自动变为已支付；后台「订单 / 支付流水」可点「查单」立即同步。
6. **关单后到账**：把 `trade.pay_timeout_minutes` 调为 1，下单后打开支付页等订单关闭再付款，应收到原路退款，流水状态为「关单后到账已退款」。
