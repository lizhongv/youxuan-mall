package com.platform.cloud.mall.goods.support;

/**
 * 商城前台缓存名。首页和类目树是访问量最大、变化最少的两个接口，走多级缓存；
 * 写操作（商品、类目、轮播、公告）时整体失效，数据量小，没必要做细粒度失效。
 */
public final class GoodsCacheNames {

    /** 首页聚合（不含秒杀：秒杀状态随时间变化，每次实时查询） */
    public static final String HOME = "mall:home";
    /** 启用的类目树 */
    public static final String CATEGORY_TREE = "mall:category-tree";

    private GoodsCacheNames() {
    }
}
