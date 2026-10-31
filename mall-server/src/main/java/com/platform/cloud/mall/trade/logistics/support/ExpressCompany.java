package com.platform.cloud.mall.trade.logistics.support;

/**
 * 快递公司。
 *
 * @param code 快递100 公司编码，如 shunfeng
 * @param name 中文名称，如 顺丰速运
 */
public record ExpressCompany(String code, String name) {

    /** 顺丰速运 / 顺丰快运查询必须带收件人或寄件人手机号（快递100 要求），其他公司不需要 */
    public boolean requiresPhone() {
        return "shunfeng".equals(code) || "shunfengkuaiyun".equals(code);
    }
}
