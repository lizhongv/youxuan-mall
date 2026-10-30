package com.platform.cloud.mall.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 品牌。
 */
@Data
@Schema(description = "品牌")
public class BrandVO {

    private Long id;
    private String name;
    private String logo;
    private String firstLetter;
    private String description;
    private Integer sort;
    private Integer status;
}
