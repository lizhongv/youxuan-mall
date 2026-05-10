package com.platform.cloud.bootstrap.goods.spu.converter;

import com.platform.cloud.bootstrap.goods.spu.dto.GoodsCreateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsUpdateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.SkuUpsertRequest;
import com.platform.cloud.bootstrap.goods.spu.entity.GoodsEntity;
import com.platform.cloud.bootstrap.goods.spu.entity.SkuEntity;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsDetailVO;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsVO;
import com.platform.cloud.bootstrap.goods.spu.vo.SkuVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface GoodsConverter {

    /** 注意：req.skus 在 service 层单独处理，不会映射到 entity */
    GoodsEntity toEntity(GoodsCreateRequest req);

    void updateEntity(GoodsUpdateRequest req, @MappingTarget GoodsEntity target);

    SkuEntity toSkuEntity(SkuUpsertRequest req);

    void updateSkuEntity(SkuUpsertRequest req, @MappingTarget SkuEntity target);

    GoodsVO toVO(GoodsEntity entity);

    SkuVO toSkuVO(SkuEntity entity);

    List<SkuVO> toSkuVOList(List<SkuEntity> entities);

    GoodsDetailVO toDetailVO(GoodsEntity entity);
}
