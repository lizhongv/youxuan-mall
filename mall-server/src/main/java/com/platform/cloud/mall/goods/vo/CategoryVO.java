package com.platform.cloud.mall.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 类目树节点。会进入多级缓存（Redis 用带类型信息的 JSON），所以保持普通可变 JavaBean。
 */
@Data
@Schema(description = "类目")
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
    @Schema(description = "子类目；首页一级类目列表不返回")
    private List<CategoryVO> children;
}
