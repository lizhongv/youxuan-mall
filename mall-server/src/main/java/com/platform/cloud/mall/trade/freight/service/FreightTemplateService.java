package com.platform.cloud.mall.trade.freight.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateQuery;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest;
import com.platform.cloud.mall.trade.freight.vo.FreightTemplateOptionVO;
import com.platform.cloud.mall.trade.freight.vo.FreightTemplateVO;

import java.util.List;

/** 管理端运费模板维护。 */
public interface FreightTemplateService {

    PageVO<FreightTemplateVO> page(FreightTemplateQuery query);

    FreightTemplateVO get(Long id);

    Long create(FreightTemplateRequest req);

    void update(Long id, FreightTemplateRequest req);

    /** 有商品引用时不能删除，避免商品静默回退到默认模板导致运费变化 */
    void delete(Long id);

    /** 启用中的模板下拉（商品表单），默认模板排在最前 */
    List<FreightTemplateOptionVO> listOptions();
}
