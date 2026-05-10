package com.platform.cloud.goods.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品搜索文档（反范式：把 SPU 关键字段聚合到一处，便于 ES 全文检索）。
 * Phase 后期接 Canal 后由 binlog → MQ → 同步更新这里。
 */
@Data
@Document(indexName = "goods", createIndex = true)
public class GoodsSearchDoc implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String goodsNo;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String name;

    @Field(type = FieldType.Text)
    private String subtitle;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Keyword)
    private String coverImage;

    @Field(type = FieldType.Double)
    private BigDecimal minPrice;

    @Field(type = FieldType.Double)
    private BigDecimal maxPrice;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Integer)
    private Integer sortOrder;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
