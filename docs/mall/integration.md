# 第三方服务接入：短信、对象存储、物流查询

> 适用版本：V7 迁移之后。三项服务的参数都在 **管理后台 → 系统 → 系统参数** 中填写（需要 `system:config:edit` 权限），
> 保存后立即生效，不需要重启服务（多实例部署时其他节点最多 1 分钟后生效）。
> 密钥类参数（`sms.access_key_secret`、`storage.s3.secret_key`、`logistics.kuaidi100.key`）标记为敏感，列表中掩码展示。

默认值保持升级前的行为：短信 `LOG`、存储 `LOCAL`、物流 `NONE`。

---

## 1. 短信

### 1.1 选择服务商

| `sms.provider` | 说明 |
|---|---|
| `LOG` | 不发真实短信，只写日志。**仅在 `MALL_SMS_DEV_MODE=true`（`mall.sms.dev-mode`）时可用**；生产环境误选 LOG 时发送会失败并记录错误日志 |
| `ALIYUN` | 阿里云短信服务（dysmsapi 2017-05-25） |
| `TENCENT` | 腾讯云短信（API 2021-01-11） |

上线前务必把 `mall.sms.dev-mode` 设为 `false`：开发模式下验证码会直接在接口里返回（`devCode`）。

### 1.2 阿里云短信

1. 开通 [短信服务](https://dysms.console.aliyun.com/)，完成企业实名认证。
2. **申请签名**：国内消息 → 签名管理 → 添加签名（通常为商城名称或公司简称），审核约 2 小时。
3. **申请模板**：
   - 验证码模板，例：`您的验证码为${code}，5分钟内有效，请勿泄露。` 变量名必须是 **`code`**。
   - 通知模板（可选），变量名按下表：

     | 参数键 | 变量 | 示例内容 |
     |---|---|---|
     | `sms.template.pay_success` | `orderNo`、`amount` | 您的订单${orderNo}已支付${amount}元，我们将尽快发货。 |
     | `sms.template.delivered` | `orderNo`、`company`、`trackingNo` | 您的订单${orderNo}已由${company}发出，运单号${trackingNo}。 |
     | `sms.template.refund_success` | `orderNo`、`amount` | 您的订单${orderNo}已退款${amount}元，请注意查收。 |
4. **创建 AccessKey**：RAM 控制台新建子账号，只授予 `AliyunDysmsFullAccess`（或仅 `dysms:SendSms`），使用子账号的 AccessKey，不要用主账号。
5. 后台填写：

   | 键 | 值 |
   |---|---|
   | `sms.provider` | `ALIYUN` |
   | `sms.access_key_id` / `sms.access_key_secret` | 子账号 AccessKey |
   | `sms.sign_name` | 审核通过的签名名称（不含【】） |
   | `sms.template.code` | 验证码模板 Code，如 `SMS_123456789` |
   | `sms.region` | 可留空 |

### 1.3 腾讯云短信

1. 开通 [短信](https://console.cloud.tencent.com/smsv2)，在「应用管理」创建应用，记下 **SdkAppId**（1400 开头）。
2. **申请签名**与**正文模板**：
   - 验证码模板只能有 **一个变量 `{1}`**，例：`您的验证码为{1}，5分钟内有效，请勿泄露。`
   - 通知模板变量按位置填写，顺序必须与 1.2 表中的变量顺序一致，例：发货 `您的订单{1}已由{2}发出，运单号{3}。`
3. **创建密钥**：访问管理 CAM 新建子用户，授予 `QcloudSMSFullAccess`，获取 SecretId / SecretKey。
4. 后台填写：

   | 键 | 值 |
   |---|---|
   | `sms.provider` | `TENCENT` |
   | `sms.access_key_id` / `sms.access_key_secret` | SecretId / SecretKey |
   | `sms.tencent.sdk_app_id` | SdkAppId |
   | `sms.sign_name` | 签名内容 |
   | `sms.template.code` | 验证码模板 ID（纯数字） |
   | `sms.region` | 留空即 `ap-guangzhou`；也可填 `ap-beijing`、`ap-nanjing` |

### 1.4 验证

系统参数页「短信」分组点击 **测试发送**（`POST /admin/system/sms/test {mobile}`），输入自己的手机号。失败时提示中包含服务商错误码，常见：

| 错误码 | 原因 |
|---|---|
| `isv.SMS_SIGNATURE_ILLEGAL` / `FailedOperation.SignatureIncorrectOrUnapproved` | 签名未审核或名称填错 |
| `isv.SMS_TEMPLATE_ILLEGAL` / `FailedOperation.TemplateIncorrectOrUnapproved` | 模板 ID 错误或未审核 |
| `FailedOperation.TemplateParamSetNotMatchApprovedTemplate` | 腾讯云验证码模板变量不是恰好一个 |
| `isv.BUSINESS_LIMIT_CONTROL` / `LimitExceeded.*` | 触发服务商频控（同号码每分钟 / 每天上限） |
| `InvalidAccessKeyId.NotFound` / `AuthFailure.*` | 密钥错误或已禁用 |

测试短信里的验证码是随机生成的，不能用于登录。

---

## 2. 对象存储

### 2.1 原理与切换影响

使用 AWS S3 SDK v2，任何兼容 S3 协议的存储都可以接入。`storage.provider=S3` 之后：

- **新上传**写入对象存储，返回的 `url` 是完整地址（`storage.public_base_url` + `/` + 前缀 + `yyyy/MM/dd/uuid.ext`）；
- **已有文件**不迁移：它们的 URL（`/uploads/...`）已经保存在业务数据里，本地目录仍由 Nginx 托管，继续可用。如需全部上云，可用 `ossutil` / `coscli` / `mc mirror` 把上传目录同步到桶里，再在 Nginx 把 `/uploads/` 反向代理或重定向到 CDN。
- 切回 `LOCAL` 同理，只影响之后的上传。

### 2.2 参数

| 键 | 说明 |
|---|---|
| `storage.provider` | `LOCAL` / `S3` |
| `storage.s3.endpoint` | S3 接入点，**必须带 `https://`**（MinIO 内网可 `http://`） |
| `storage.s3.region` | 地域；MinIO 可留空 |
| `storage.s3.bucket` | 桶名 |
| `storage.s3.access_key` / `storage.s3.secret_key` | 建议使用只授权该桶读写删除的子账号 |
| `storage.s3.path_style` | `true` 为 `endpoint/bucket/key` 风格（MinIO），`false` 为 `bucket.endpoint/key` 风格（OSS、COS、七牛） |
| `storage.public_base_url` | 对外访问域名，推荐绑定 CDN，如 `https://img.example.com`；留空则按 endpoint 与桶名推导桶域名 |
| `storage.path_prefix` | 对象键前缀，默认 `mall` |

### 2.3 配置示例

**阿里云 OSS**（只支持虚拟主机风格，`path_style` 必须为 `false`）

```
storage.provider        = S3
storage.s3.endpoint     = https://oss-cn-hangzhou.aliyuncs.com
storage.s3.region       = oss-cn-hangzhou
storage.s3.bucket       = mall-img
storage.s3.path_style   = false
storage.public_base_url = https://img.example.com      # 绑定的 CDN 域名；不用 CDN 可留空
```

**腾讯云 COS**（桶名带 APPID 后缀）

```
storage.provider        = S3
storage.s3.endpoint     = https://cos.ap-guangzhou.myqcloud.com
storage.s3.region       = ap-guangzhou
storage.s3.bucket       = mall-img-1250000000
storage.s3.path_style   = false
storage.public_base_url = https://img.example.com
```

**MinIO**（自建）

```
storage.provider        = S3
storage.s3.endpoint     = http://minio:9000
storage.s3.region       =                              # 留空
storage.s3.bucket       = mall
storage.s3.path_style   = true
storage.public_base_url = https://files.example.com/mall   # 对外暴露的地址（Nginx 反代 MinIO）
```

**七牛 Kodo**

```
storage.provider        = S3
storage.s3.endpoint     = https://s3.cn-east-1.qiniucs.com
storage.s3.region       = cn-east-1
storage.s3.bucket       = mall-img
storage.s3.path_style   = false
storage.public_base_url = https://img.example.com      # 七牛必须绑定自定义域名才能长期访问
```

### 2.4 访问权限、跨域与 CDN

- **公开读**：程序不设置对象 ACL（各家对 `x-amz-acl` 的支持不一致），请把桶设为「公共读、私有写」，或保持私有并在 CDN 开启「回源鉴权 / 私有桶回源」。MinIO 用 `mc anonymous set download myminio/mall`。
- **跨域（CORS）**：上传经由服务端转发，浏览器不直接访问桶的写接口，所以**上传不需要配置 CORS**。若前端需要用 `fetch` / canvas 读取图片（如生成分享海报），在桶或 CDN 上允许来源为商城域名、方法 `GET, HEAD`。
- **CDN**：对象文件名为 UUID，内容永不变化，上传时已设置 `Cache-Control: public, max-age=31536000, immutable`，CDN 可放心长缓存。
- **防盗链**：建议在 CDN 配置 Referer 白名单（商城域名 + 允许空 Referer，便于 App 内打开）。

### 2.5 验证

系统参数页「存储」分组点击 **测试连接**（`POST /admin/system/storage/test`）：用已保存的配置上传一个 `_probe/*.txt` 再删除。返回的 `sampleUrl` 可用于核对域名拼接是否正确。常见错误：`AccessDenied`（密钥无写/删权限）、`NoSuchBucket`（桶名或地域不对）、`SignatureDoesNotMatch`（SecretKey 错误）、OSS 报 `SecondLevelDomainForbidden`（`path_style` 必须为 `false`）。

---

## 3. 物流轨迹（快递100）

### 3.1 开通

1. 在 [快递100 企业版](https://api.kuaidi100.com/) 注册并完成企业认证，购买「实时快递查询」套餐（按查询次数计费）。
2. 在「我的信息 → 授权信息」中获取 **customer** 与 **授权 key**。
3. 后台填写：

   | 键 | 值 |
   |---|---|
   | `logistics.provider` | `KUAIDI100` |
   | `logistics.kuaidi100.customer` | customer |
   | `logistics.kuaidi100.key` | 授权 key |

未开通或参数不全时，物流接口返回空轨迹和提示「未开通物流查询」，不影响订单详情。

### 3.2 快递公司编码

字典 `express_company` 的 value 是快递100 公司编码（V7 迁移已把种子数据改为 `shunfeng`、`zhongtong`、`yuantong`、`yunda`、`shentong`、`jd`、`ems`、`jtexpress`），label 是展示用中文名。新增快递公司时到 **系统 → 数据字典** 添加，value 填快递100 编码（在快递100 后台「快递公司编码」表中查询）。

历史订单里保存的是中文名称，查询时按 value → label → 旧编码（SF / ZTO …）→ 内置常用名称依次匹配，无需迁移数据。

顺丰速运查询需要收件人手机号后四位，程序自动从订单收货人手机号截取。

### 3.3 缓存与费用

同一运单的查询结果缓存在 Redis：在途 30 分钟、已签收 1 天、「查询无结果」10 分钟；签名错误、余额不足等失败不缓存，修好配置后立即可查。按 30 分钟缓存估算，每单从发货到签收通常消耗 5–20 次查询。

常见错误码（记录在服务端错误日志中）：`400` 参数错误（多为快递公司编码不对）、`500` 查询无结果、`503` 签名错误（customer 或 key 填错）、`601` key 过期或余额不足。

---

## 4. 前端配合

- **系统参数页**按前缀分组展示：`sms.*` 短信、`storage.*` 存储、`logistics.*` 物流；短信组放「测试发送」按钮（弹框输入手机号），存储组放「测试连接」按钮。先保存再测试。
- **订单详情**（后台与 H5）调用 `GET .../order/{id}/logistics` 展示物流时间线；`traces` 为空时显示 `message`。
- **发货弹框**的快递公司下拉 option 的 value 现在是快递100 编码；当前前端提交的是 label（中文名），后端两种都支持，可以保持不变。
