package com.platform.cloud.mall.system.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.system.dto.DictQuery;
import com.platform.cloud.mall.system.dto.DictSaveRequest;
import com.platform.cloud.mall.system.vo.DictItemVO;
import com.platform.cloud.mall.system.vo.DictVO;

import java.util.List;

/**
 * 数据字典。
 */
public interface SysDictService {

    PageVO<DictVO> page(DictQuery query);

    DictVO detail(Long id);

    Long create(DictSaveRequest req);

    void update(Long id, DictSaveRequest req);

    void delete(Long id);

    /** 全部字典类型 */
    List<String> types();

    /** 某类型下启用的字典项，按排序返回，供下拉框使用 */
    List<DictItemVO> items(String dictType);
}
