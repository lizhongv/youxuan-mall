package com.platform.cloud.bootstrap.common.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

@Data
@Schema(description = "统一分页响应")
public class PageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总数")
    private long total;

    @Schema(description = "当前页")
    private long page;

    @Schema(description = "每页大小")
    private long size;

    @Schema(description = "总页数")
    private long pages;

    public static <S, T> PageVO<T> of(IPage<S> source, Function<S, T> mapper) {
        PageVO<T> vo = new PageVO<>();
        vo.records = source.getRecords().stream().map(mapper).toList();
        vo.total = source.getTotal();
        vo.page = source.getCurrent();
        vo.size = source.getSize();
        vo.pages = source.getPages();
        return vo;
    }
}
