package com.platform.cloud.bootstrap.goods.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "分类视图")
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String slug;
    private String iconUrl;
    private String bannerUrl;
    private Integer sortOrder;
    private Integer visible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "子分类（仅 tree 接口返回）")
    private List<CategoryVO> children = new ArrayList<>();
}
