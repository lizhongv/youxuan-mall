package com.platform.cloud.mall.marketing.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.marketing.dto.NoticeQuery;
import com.platform.cloud.mall.marketing.dto.NoticeRequest;
import com.platform.cloud.mall.marketing.vo.NoticeVO;

import java.util.List;

/**
 * 商城公告。
 */
public interface NoticeService {

    PageVO<NoticeVO> page(NoticeQuery q);

    NoticeVO get(Long id);

    Long create(NoticeRequest req);

    void update(Long id, NoticeRequest req);

    void delete(Long id);

    /** 首页公告条：启用的前 N 条，不含正文 */
    List<NoticeVO> listLatest(int limit);

    /** 商城端公告详情：停用的公告视为不存在 */
    NoticeVO getPublished(Long id);
}
