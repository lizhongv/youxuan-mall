package com.platform.cloud.mall.goods.vo;

import com.platform.cloud.mall.marketing.vo.BannerVO;
import com.platform.cloud.mall.marketing.vo.NoticeVO;
import com.platform.cloud.mall.marketing.vo.SeckillVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页聚合。除 seckills 外整体缓存；seckills 的状态随时间变化，每次请求实时填充到副本上。
 */
@Data
@Schema(description = "首页聚合")
public class HomeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<BannerVO> banners = new ArrayList<>();
    private List<NoticeVO> notices = new ArrayList<>();
    @Schema(description = "一级类目（含图标）")
    private List<CategoryVO> categories = new ArrayList<>();
    @Schema(description = "进行中及即将开始的秒杀，最多 4 个")
    private List<SeckillVO> seckills = new ArrayList<>();
    private List<GoodsItemVO> newGoods = new ArrayList<>();
    private List<GoodsItemVO> hotGoods = new ArrayList<>();
    private List<GoodsItemVO> recommendGoods = new ArrayList<>();

    /** 浅拷贝：缓存里的对象被多个请求共享，不能直接在上面写 seckills */
    public HomeVO copy() {
        HomeVO vo = new HomeVO();
        vo.banners = banners;
        vo.notices = notices;
        vo.categories = categories;
        vo.newGoods = newGoods;
        vo.hotGoods = hotGoods;
        vo.recommendGoods = recommendGoods;
        return vo;
    }
}
