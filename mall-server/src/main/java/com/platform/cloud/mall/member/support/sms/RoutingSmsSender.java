package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.mall.common.util.MaskUtils;
import com.platform.cloud.mall.member.config.SmsProperties;
import com.platform.cloud.mall.member.support.LogSmsSender;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.InfraConfigKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * 按 sys_config 中的 {@code sms.provider} 把短信分发到阿里云 / 腾讯云 / 日志实现。
 * <p>
 * 标为 @Primary：SmsCodeServiceImpl 等注入 {@code SmsSender} 的地方无需改动即可走真实渠道。
 * 每次发送都读一次配置快照，与上次不同就重建 SDK 客户端，后台改完参数不用重启。
 * <p>
 * LOG 渠道只在 {@code mall.sms.dev-mode=true} 时允许；生产误选 LOG 时直接发送失败并记错误日志，
 * 否则用户收不到验证码却不知道原因，运维也很难察觉。
 */
@Slf4j
@Primary
@Component
public class RoutingSmsSender implements SmsChannel {

    private final ConfigApi config;
    private final SmsProperties props;
    private final Function<SmsSettings, SmsChannel> factory;
    private final LogSmsSender logSender;

    /** 最近一次构建的渠道及其配置，配置不变时复用，避免每条短信都新建 HTTP 客户端 */
    private SmsSettings cachedSettings;
    private SmsChannel cachedChannel;

    @Autowired
    public RoutingSmsSender(ConfigApi config, SmsProperties props) {
        this(config, props, RoutingSmsSender::createChannel);
    }

    RoutingSmsSender(ConfigApi config, SmsProperties props, Function<SmsSettings, SmsChannel> factory) {
        this.config = config;
        this.props = props;
        this.factory = factory;
        this.logSender = new LogSmsSender(props.isDevMode());
    }

    @Override
    public void sendCode(String mobile, SmsScene scene, String code) {
        SmsSettings s = SmsSettings.from(config);
        if (s.provider() == SmsProvider.LOG) {
            requireDevMode();
            logSender.sendCode(mobile, scene, code);
            return;
        }
        channel(s).sendCode(mobile, scene, code);
    }

    @Override
    public void send(String mobile, String templateId, LinkedHashMap<String, String> params) {
        SmsSettings s = SmsSettings.from(config);
        if (s.provider() == SmsProvider.LOG) {
            requireDevMode();
            // 通知短信参数里没有验证码，开发模式下打出模板与参数便于联调
            log.info("[模拟短信] mobile={} template={} params={}", MaskUtils.mobile(mobile), templateId, params);
            return;
        }
        channel(s).send(mobile, templateId, params);
    }

    @Override
    public String codeTemplate() {
        return SmsSettings.from(config).codeTemplate();
    }

    /** 当前生效的渠道，供后台测试接口展示 */
    public SmsProvider currentProvider() {
        return SmsSettings.from(config).provider();
    }

    private void requireDevMode() {
        if (!props.isDevMode()) {
            log.error("短信渠道为 LOG 但 mall.sms.dev-mode=false：短信不会真正发出，请在后台系统参数中配置 {}",
                    InfraConfigKeys.SMS_PROVIDER);
            throw new SmsSendException("短信渠道未开通，请在系统参数中配置短信服务商");
        }
    }

    /** synchronized：配置切换瞬间多个请求并发进来时只构建一次客户端 */
    synchronized SmsChannel channel(SmsSettings s) {
        if (s.provider() == null) {
            log.error("无法识别的短信渠道 {}={}", InfraConfigKeys.SMS_PROVIDER, s.rawProvider());
            throw new SmsSendException("短信渠道配置错误：" + s.rawProvider() + "（可选 LOG / ALIYUN / TENCENT）");
        }
        if (s.equals(cachedSettings) && cachedChannel != null) {
            return cachedChannel;
        }
        List<String> missing = s.missingFields();
        if (!missing.isEmpty()) {
            log.error("短信配置不完整 provider={} 缺少 {}", s.provider(), missing);
            throw new SmsSendException("短信配置不完整，缺少：" + String.join("、", missing));
        }
        SmsChannel created = factory.apply(s);
        log.info("短信渠道已切换 {}", s);
        cachedSettings = s;
        cachedChannel = created;
        return created;
    }

    static SmsChannel createChannel(SmsSettings s) {
        return switch (s.provider()) {
            case ALIYUN -> AliyunSmsSender.create(s);
            case TENCENT -> TencentSmsSender.create(s);
            case LOG -> throw new IllegalStateException("LOG 渠道不经过工厂");
        };
    }
}
