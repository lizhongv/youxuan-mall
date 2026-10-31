package com.platform.cloud.mall.trade.pay.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额换算。与渠道交互、核对金额一律用「分」的整数：
 * 微信接口本身以分为单位，支付宝的元字符串也先转成分再比较，避免 88.8 与 88.80 这类 scale 差异误判。
 */
public final class PayAmounts {

    private PayAmounts() {
    }

    /** 元 → 分；订单金额已是两位小数，多余小数位说明数据异常，直接抛错而不是四舍五入掩盖 */
    public static long toFen(BigDecimal yuan) {
        return yuan.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }

    /** 渠道返回的元字符串（如支付宝 total_amount）→ 分；格式不对返回 -1，必然与流水金额不符 */
    public static long yuanTextToFen(String yuan) {
        if (yuan == null || yuan.isBlank()) {
            return -1;
        }
        try {
            return toFen(new BigDecimal(yuan.trim()));
        } catch (ArithmeticException | NumberFormatException e) {
            return -1;
        }
    }

    /** 两位小数的元字符串（支付宝 total_amount / refund_amount 格式） */
    public static String toYuanText(BigDecimal yuan) {
        return yuan.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
