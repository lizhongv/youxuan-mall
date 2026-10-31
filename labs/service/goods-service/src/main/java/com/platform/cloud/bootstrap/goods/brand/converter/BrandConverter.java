package com.platform.cloud.bootstrap.goods.brand.converter;

import com.platform.cloud.bootstrap.goods.brand.dto.BrandUpsertRequest;
import com.platform.cloud.bootstrap.goods.brand.entity.BrandEntity;
import com.platform.cloud.bootstrap.goods.brand.vo.BrandVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BrandConverter {

    BrandEntity toEntity(BrandUpsertRequest req);

    BrandVO toVO(BrandEntity entity);

    void updateEntity(BrandUpsertRequest req, @MappingTarget BrandEntity target);
}
