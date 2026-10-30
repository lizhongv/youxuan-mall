package com.platform.cloud.mall.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录 / 刷新结果。expiresIn 为 accessToken 有效秒数，前端据此提前刷新；
 * 用 Integer 而非 Long：全局把 Long 序列化成字符串（防 ID 精度丢失），这里需要输出数字 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
}
