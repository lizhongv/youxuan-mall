package com.platform.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.order.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {
}
