package com.platform.cloud.mall.marketing.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.marketing.dto.BannerQuery;
import com.platform.cloud.mall.marketing.dto.BannerRequest;
import com.platform.cloud.mall.marketing.vo.BannerVO;

import java.util.List;

/**
 * 轮播广告。
 */
public interface BannerService {

    PageVO<BannerVO> page(BannerQuery q);

    BannerVO get(Long id);

    Long create(BannerRequest req);

    void update(Long id, BannerRequest req);

    void delete(Long id);

    /** 某位置下启用的轮播，按 sort 升序（首页用） */
    List<BannerVO> listEnabled(String position);
}
