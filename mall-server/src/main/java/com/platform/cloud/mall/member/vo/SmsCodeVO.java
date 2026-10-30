package com.platform.cloud.mall.member.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发送验证码结果。devCode 只在开发模式返回，接入真实短信渠道后为空 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsCodeVO {

    private String devCode;
}
