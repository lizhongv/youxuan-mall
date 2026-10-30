package com.platform.cloud.mall.marketing.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.marketing.dto.SeckillQuery;
import com.platform.cloud.mall.marketing.dto.SeckillRequest;
import com.platform.cloud.mall.marketing.vo.SeckillVO;

import java.util.List;

/**
 * 秒杀活动维护与展示。抢购名额的扣减与归还见 SeckillApi。
 */
public interface SeckillService {

    PageVO<SeckillVO> page(SeckillQuery q);

    SeckillVO get(Long id);

    /** 保存后把剩余库存预热到 Redis */
    Long create(SeckillRequest req);

    void update(Long id, SeckillRequest req);

    /** 进行中且已有人抢购的活动不能删除，需先停用 */
    void delete(Long id);

    /** 商城端：启用且未结束的活动，进行中在前，按开始时间升序 */
    List<SeckillVO> listPublic();

    /** 首页：进行中及即将开始的活动，最多 limit 个 */
    List<SeckillVO> listForHome(int limit);

    /** 商品详情：该商品正在进行的秒杀活动，没有返回 null */
    SeckillVO findOngoing(Long goodsId);

    /** 应用启动时预热全部未结束活动的剩余库存 */
    int warmUpAll();
}
