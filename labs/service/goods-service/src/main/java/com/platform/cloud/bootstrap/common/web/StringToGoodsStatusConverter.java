package com.platform.cloud.bootstrap.common.web;

import com.platform.cloud.bootstrap.goods.spu.GoodsStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * URL 参数 ?status=1 → GoodsStatus.ON_SALE
 * 也支持 ?status=ON_SALE 形式（按 name 匹配）
 */
@Component
public class StringToGoodsStatusConverter implements Converter<String, GoodsStatus> {

    @Override
    public GoodsStatus convert(@NonNull String source) {
        if (source.isBlank()) return null;
        try {
            return GoodsStatus.of(Integer.parseInt(source));
        } catch (NumberFormatException ignored) {
            return GoodsStatus.valueOf(source.toUpperCase());
        }
    }
}
