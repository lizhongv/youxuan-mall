package com.platform.cloud.order.mq;

/**
 * RocketMQ Topic / Tag 常量。
 * 约定 topic = 业务域，tag = 事件名。
 */
public final class OrderMessages {

    public static final String TOPIC_ORDER = "order_event";

    public static final String TAG_CREATED = "created";
    public static final String TAG_PAID    = "paid";
    public static final String TAG_CANCELLED = "cancelled";

    private OrderMessages() {}
}
