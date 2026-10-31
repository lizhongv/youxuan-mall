package com.platform.cloud.mall.trade.pay.support;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * 按配置快照缓存 SDK 客户端：配置不变时复用，后台改了任意一项（快照 equals 不同）就重建。
 * <p>
 * 为什么要缓存：微信 SDK 构建时会联网下载平台证书并启动定时更新，支付宝客户端持有连接池，每次请求重建代价大。
 * 构建失败（私钥格式错、APIv3 密钥错）时记住失败 {@link #FAILURE_BACKOFF_MILLIS} 毫秒，
 * 避免收银台每次打开都触发一次联网失败；日志只打印异常类型与消息，不打印配置内容（含密钥）。
 *
 * @param <S> 配置快照（record，toString 不含密钥）
 * @param <C> 客户端
 */
@Slf4j
public class ConfiguredClientCache<S, C> {

    /** 配置错误时 1 分钟内不重试构建；管理员改正配置后快照变化会立即重建，不受此限制 */
    static final long FAILURE_BACKOFF_MILLIS = 60_000L;

    private final String name;
    private final Function<S, C> builder;
    private final LongSupplier clock;
    private volatile Entry<S, C> entry;

    public ConfiguredClientCache(String name, Function<S, C> builder) {
        this(name, builder, System::currentTimeMillis);
    }

    ConfiguredClientCache(String name, Function<S, C> builder, LongSupplier clock) {
        this.name = name;
        this.builder = builder;
        this.clock = clock;
    }

    /** 当前配置对应的客户端；构建失败返回 empty */
    public Optional<C> get(S settings) {
        Entry<S, C> e = entry;
        if (e != null && Objects.equals(e.settings, settings) && fresh(e)) {
            return Optional.ofNullable(e.client);
        }
        synchronized (this) {
            e = entry;
            if (e != null && Objects.equals(e.settings, settings) && fresh(e)) {
                return Optional.ofNullable(e.client);
            }
            C client = null;
            try {
                client = builder.apply(settings);
                log.info("{} 客户端已按新配置构建 {}", name, settings);
            } catch (RuntimeException ex) {
                // 不打印堆栈里的参数，消息里也不会含密钥（SDK 只报格式错误）
                log.error("{} 客户端构建失败，请检查后台支付配置：{} {}", name, ex.getClass().getSimpleName(),
                        ex.getMessage());
            }
            entry = new Entry<>(settings, client, clock.getAsLong());
            return Optional.ofNullable(client);
        }
    }

    /** 成功的客户端一直有效；失败结果只缓存一段时间 */
    private boolean fresh(Entry<S, C> e) {
        return e.client != null || clock.getAsLong() - e.builtAt < FAILURE_BACKOFF_MILLIS;
    }

    private record Entry<S, C>(S settings, C client, long builtAt) {
    }
}
