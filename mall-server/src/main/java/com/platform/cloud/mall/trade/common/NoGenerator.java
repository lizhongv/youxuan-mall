package com.platform.cloud.mall.trade.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务单号生成：yyyyMMddHHmmss + 6 位随机数字。
 * <p>
 * 单号要给用户和客服看，所以用可读的时间前缀而不是雪花 ID；同一秒内 100 万种组合碰撞概率很低，
 * 极端情况下由数据库唯一索引兜底，调用方捕获重复键后重试。
 */
public final class NoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private NoGenerator() {
    }

    public static String orderNo() {
        return LocalDateTime.now().format(FMT) + random6();
    }

    /** 售后单号加前缀 A，与订单号一眼区分 */
    public static String aftersaleNo() {
        return "A" + orderNo();
    }

    /** 支付单号加前缀 P，发给支付渠道作为商户订单号 */
    public static String payNo() {
        return "P" + orderNo();
    }

    private static String random6() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
