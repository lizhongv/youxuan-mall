package com.platform.cloud.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 轮播广告。会进入首页缓存，保持普通 JavaBean。
 */
@Data
@Schema(description = "轮播广告")
public class BannerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String image;
    @Schema(description = "NONE / GOODS / CATEGORY / URL")
    private String linkType;
    private String linkValue;
    private String position;
    private Integer sort;
    private Integer status;
}
