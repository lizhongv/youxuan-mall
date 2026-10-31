package com.platform.cloud.bootstrap.goods.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "品牌视图")
public class BrandVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String logoUrl;
    private String description;
    private Integer sortOrder;
    private Integer visible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
