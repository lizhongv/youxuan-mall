package com.platform.cloud.bootstrap.goods.category.converter;

import com.platform.cloud.bootstrap.goods.category.dto.CategoryUpsertRequest;
import com.platform.cloud.bootstrap.goods.category.entity.CategoryEntity;
import com.platform.cloud.bootstrap.goods.category.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryConverter {

    CategoryEntity toEntity(CategoryUpsertRequest req);

    @Mappings({})
    CategoryVO toVO(CategoryEntity entity);

    List<CategoryVO> toVOList(List<CategoryEntity> entities);

    /** 部分更新：仅覆盖请求中非 null 字段 */
    void updateEntity(CategoryUpsertRequest req, @MappingTarget CategoryEntity target);
}
