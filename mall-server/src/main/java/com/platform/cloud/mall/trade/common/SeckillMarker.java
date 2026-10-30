package com.platform.cloud.mall.trade.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 秒杀活动 ID 的记录与解析。
 * <p>
 * trd_order 没有 seckill_id 列（表结构已发布不能改），但关单时必须知道归还哪个活动的名额，
 * 所以在下单日志的 remark 里写入 {@code seckillId=<id>}，取消时从日志解析。
 * 后续若加列，只需改这里和下单处。
 */
public final class SeckillMarker {

    private static final Pattern PATTERN = Pattern.compile("seckillId=(\\d+)");

    private SeckillMarker() {
    }

    public static String mark(Long seckillId) {
        return "seckillId=" + seckillId;
    }

    /** 解析不到返回 null */
    public static Long parse(String remark) {
        if (remark == null) {
            return null;
        }
        Matcher m = PATTERN.matcher(remark);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }
}
