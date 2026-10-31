-- 第三方基础设施：短信、对象存储、物流查询的后台配置项；快递公司字典改为快递100 编码。
-- 依赖 V6 给 sys_config 增加的 sensitive 列（1 = 敏感，后台列表掩码展示）。
-- sensitive 是 MySQL 保留字，必须加反引号。
-- ID 段 27001 起专用于基础设施配置，避免与其他迁移插入的参数冲突。
-- 键与 com.platform.cloud.mall.system.api.InfraConfigKeys 一一对应；默认值保证升级后行为不变（LOG / LOCAL / NONE）。

INSERT INTO sys_config (id, config_key, config_value, name, remark, `sensitive`) VALUES
-- 短信
(27001, 'sms.provider',               'LOG',  '短信服务商',            'LOG（仅开发/演示模式可用）/ ALIYUN / TENCENT', 0),
(27002, 'sms.access_key_id',          '',     '短信 AccessKeyId',      '阿里云 AccessKeyId 或腾讯云 SecretId，建议使用仅授权短信的子账号', 0),
(27003, 'sms.access_key_secret',      '',     '短信 AccessKeySecret',  '阿里云 AccessKeySecret 或腾讯云 SecretKey', 1),
(27004, 'sms.sign_name',              '',     '短信签名',              '审核通过的签名名称，不含【】', 0),
(27005, 'sms.region',                 '',     '短信地域',              '腾讯云如 ap-guangzhou；阿里云可留空', 0),
(27006, 'sms.tencent.sdk_app_id',     '',     '腾讯云短信 SdkAppId',   '仅腾讯云需要，短信控制台「应用管理」中查看', 0),
(27007, 'sms.template.code',          '',     '验证码模板',            '阿里云模板 Code（变量名 code）或腾讯云模板 ID（仅一个变量 {1}）', 0),
(27008, 'sms.template.pay_success',   '',     '支付成功通知模板',      '留空不发送；变量顺序 orderNo, amount', 0),
(27009, 'sms.template.delivered',     '',     '发货通知模板',          '留空不发送；变量顺序 orderNo, company, trackingNo', 0),
(27010, 'sms.template.refund_success','',     '退款成功通知模板',      '留空不发送；变量顺序 orderNo, amount', 0),
-- 对象存储
(27101, 'storage.provider',           'LOCAL','存储方式',              'LOCAL 本地磁盘 / S3 对象存储（OSS、COS、MinIO、七牛均走 S3 协议）；切换只影响新上传', 0),
(27102, 'storage.s3.endpoint',        '',     'S3 Endpoint',           '如 https://oss-cn-hangzhou.aliyuncs.com、https://cos.ap-guangzhou.myqcloud.com', 0),
(27103, 'storage.s3.region',          '',     'S3 地域',               '如 oss-cn-hangzhou、ap-guangzhou；MinIO 可留空', 0),
(27104, 'storage.s3.bucket',          '',     'S3 桶名',               'COS 桶名包含 APPID 后缀，如 mall-1250000000', 0),
(27105, 'storage.s3.access_key',      '',     'S3 AccessKey',          '建议使用仅授权该桶读写的子账号', 0),
(27106, 'storage.s3.secret_key',      '',     'S3 SecretKey',          '', 1),
(27107, 'storage.s3.path_style',      'false','Path-Style 访问',       'MinIO 填 true；阿里云 OSS 必须 false（只支持虚拟主机风格）', 0),
(27108, 'storage.public_base_url',    '',     '文件访问域名',          'CDN 或桶访问域名，如 https://img.example.com；留空按 endpoint 与桶名推导', 0),
(27109, 'storage.path_prefix',        'mall', '存储路径前缀',          '对象键前缀，同一个桶放多个项目时区分目录', 0),
-- 物流
(27201, 'logistics.provider',         'NONE', '物流查询服务商',        'NONE 未开通 / KUAIDI100 快递100', 0),
(27202, 'logistics.kuaidi100.customer','',    '快递100 customer',      '快递100 企业管理后台「授权信息」中查看', 0),
(27203, 'logistics.kuaidi100.key',    '',     '快递100 授权 key',      '', 1);

-- 快递公司字典 value 改为快递100 编码，label 保持中文不变。
-- 历史订单 delivery_company 保存的是中文名称，查询物流时按 label 反查，旧数据无需迁移。
UPDATE sys_dict SET value = 'shunfeng',  remark = '快递100 编码' WHERE id = 10001 AND dict_type = 'express_company';
UPDATE sys_dict SET value = 'zhongtong', remark = '快递100 编码' WHERE id = 10002 AND dict_type = 'express_company';
UPDATE sys_dict SET value = 'yuantong',  remark = '快递100 编码' WHERE id = 10003 AND dict_type = 'express_company';
UPDATE sys_dict SET value = 'yunda',     remark = '快递100 编码' WHERE id = 10004 AND dict_type = 'express_company';
UPDATE sys_dict SET value = 'shentong',  remark = '快递100 编码' WHERE id = 10005 AND dict_type = 'express_company';
UPDATE sys_dict SET value = 'jd',        remark = '快递100 编码' WHERE id = 10006 AND dict_type = 'express_company';
UPDATE sys_dict SET value = 'ems',       remark = '快递100 编码' WHERE id = 10007 AND dict_type = 'express_company';
UPDATE sys_dict SET value = 'jtexpress', remark = '快递100 编码' WHERE id = 10008 AND dict_type = 'express_company';
