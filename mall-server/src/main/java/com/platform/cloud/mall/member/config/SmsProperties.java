package com.platform.cloud.mall.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信验证码配置（mall.sms.*）。
 * <p>
 * 验证码长度、有效期、发送间隔、错误次数是接口契约规定的，写成常量见 SmsCodeServiceImpl；这里只放部署相关的开关与限额。
 */
@Data
@ConfigurationProperties(prefix = "mall.sms")
public class SmsProperties {

    /**
     * 开发模式：接口直接返回验证码（devCode），未接短信渠道时便于演示。
     * 默认 false；只有 demo 模式能打开，prod 模式被强制为 false（MallProfileEnvironmentPostProcessor）。
     */
    private boolean devMode = false;

    /** 同一手机号每天最多发送次数：防止被当作短信轰炸工具，10 次对正常用户足够 */
    private int dailyLimit = 10;

    /** 同一 IP 每小时最多发送次数：一个家庭 / 办公室出口 IP 下多人注册也够用，挡住单机脚本轮换手机号刷量 */
    private int ipHourlyLimit = 20;

    /**
     * 全站每分钟最多发送次数：最后一道「费用保险丝」，即使攻击者绕过图形验证码、持有大量 IP，
     * 每分钟花掉的短信费也有上限。按业务峰值调整，超出时提示稍后再试。
     */
    private int globalPerMinuteLimit = 60;
}
