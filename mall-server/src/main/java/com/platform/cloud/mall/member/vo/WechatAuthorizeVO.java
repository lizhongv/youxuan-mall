package com.platform.cloud.mall.member.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 微信网页授权地址；前端 location.href 跳转，回跳时 URL 带 code 与 state */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatAuthorizeVO {

    private String url;
    private String state;
}
