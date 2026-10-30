package com.platform.cloud.mall.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信验证码配置（mall.sms.*）。
 * <p>
 * 验证码长度、有效期、发送间隔、错误次数是接口契约规定的，写成常量见 SmsCodeServiceImpl；这里只放部署相关的开关。
 */
@Data
@ConfigurationProperties(prefix = "mall.sms")
public class SmsProperties {

    /** 开发模式：接口直接返回验证码（devCode），未接短信渠道时便于演示；生产必须设为 false */
    private boolean devMode = true;

    /** 同一手机号每天最多发送次数：防止被当作短信轰炸工具，10 次对正常用户足够 */
    private int dailyLimit = 10;
}
