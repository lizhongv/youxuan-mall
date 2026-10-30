package com.platform.cloud.mall.member.config;

import com.platform.cloud.mall.member.support.LogSmsSender;
import com.platform.cloud.mall.member.support.SmsSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 没有提供真实短信渠道时使用日志实现。
 */
@Configuration
public class MemberSmsConfig {

    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    public SmsSender logSmsSender(SmsProperties props) {
        return new LogSmsSender(props.isDevMode());
    }
}
