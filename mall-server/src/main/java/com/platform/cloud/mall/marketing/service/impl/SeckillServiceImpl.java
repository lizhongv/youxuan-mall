package com.platform.cloud.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.goods.api.GoodsApi.SkuSnapshot;
import com.platform.cloud.mall.marketing.dto.SeckillQuery;
import com.platform.cloud.mall.marketing.dto.SeckillRequest;
import com.platform.cloud.mall.marketing.entity.Seckill;
import com.platform.cloud.mall.marketing.mapper.SeckillMapper;
import com.platform.cloud.mall.marketing.service.SeckillService;
import com.platform.cloud.mall.marketing.support.SeckillState;
import com.platform.cloud.mall.marketing.support.SeckillStockCache;
import com.platform.cloud.mall.marketing.support.TxCallbacks;
import com.platform.cloud.mall.marketing.vo.SeckillVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 秒杀活动维护与展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillMapper seckillMapper;
    private final SeckillStockCache stockCache;
    private final GoodsApi goodsApi;

    @Override
    public PageVO<SeckillVO> page(SeckillQuery q) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Seckill> w = new LambdaQueryWrapper<Seckill>()
                .like(StringUtils.hasText(q.getKeyword()), Seckill::getTitle, q.getKeyword())
                .eq(q.getStatus() != null, Seckill::getStatus, q.getStatus())
                .eq(q.getGoodsId() != null, Seckill::getGoodsId, q.getGoodsId())
                .orderByDesc(Seckill::getStartAt).orderByDesc(Seckill::getId);
        if (StringUtils.hasText(q.getState())) {
            switch (SeckillState.valueOf(q.getState())) {
                case NOT_STARTED -> w.gt(Seckill::getStartAt, now);
                case ONGOING -> w.le(Seckill::getStartAt, now).gt(Seckill::getEndAt, now);
                case ENDED -> w.le(Seckill::getEndAt, now);
                default -> { }
            }
        }
        Page<Seckill> page = seckillMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        Map<Long, SkuSnapshot> skus = snapshots(page.getRecords());
        return PageVO.of(page, s -> toVO(s, skus.get(s.getSkuId()), now));
    }

    @Override
    public SeckillVO get(Long id) {
        Seckill s = mustGet(id);
        return toVO(s, snapshots(List.of(s)).get(s.getSkuId()), LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SeckillRequest req) {
        Seckill s = new Seckill();
        s.setSold(0);
        validate(null, req, 0);
        apply(s, req);
        seckillMapper.insert(s);
        warmAfterCommit(s);
        return s.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SeckillRequest req) {
        Seckill s = mustGet(id);
        int sold = s.getSold() == null ? 0 : s.getSold();
        if (sold > 0 && !Objects.equals(s.getSkuId(), req.getSkuId())) {
            throw new BusinessException("活动已有人抢购，不能更换商品");
        }
        if (req.getStock() < sold) {
            throw new BusinessException("活动库存不能小于已抢数量 " + sold);
        }
        validate(id, req, sold);
        apply(s, req);
        if (seckillMapper.updateById(s) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
        warmAfterCommit(s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Seckill s = mustGet(id);
        boolean ongoing = SeckillState.of(s.getStartAt(), s.getEndAt(), LocalDateTime.now()) == SeckillState.ONGOING;
        if (ongoing && s.getSold() != null && s.getSold() > 0 && Objects.equals(s.getStatus(), 1)) {
            throw new BusinessException("进行中的活动已有人抢购，请先停用再删除");
        }
        seckillMapper.deleteById(id);
        TxCallbacks.afterCommit(() -> stockCache.remove(id));
    }

    @Override
    public List<SeckillVO> listPublic() {
        LocalDateTime now = LocalDateTime.now();
        List<Seckill> list = seckillMapper.selectList(new LambdaQueryWrapper<Seckill>()
                .eq(Seckill::getStatus, 1)
                .gt(Seckill::getEndAt, now)
                .orderByAsc(Seckill::getStartAt).orderByAsc(Seckill::getId));
        return toVOs(list, now);
    }

    @Override
    public List<SeckillVO> listForHome(int limit) {
        List<SeckillVO> all = listPublic();
        return new ArrayList<>(all.subList(0, Math.min(limit, all.size())));
    }

    @Override
    public SeckillVO findOngoing(Long goodsId) {
        LocalDateTime now = LocalDateTime.now();
        List<Seckill> list = seckillMapper.selectList(new LambdaQueryWrapper<Seckill>()
                .eq(Seckill::getGoodsId, goodsId)
                .eq(Seckill::getStatus, 1)
                .le(Seckill::getStartAt, now)
                .gt(Seckill::getEndAt, now)
                .orderByAsc(Seckill::getEndAt));
        if (list.isEmpty()) {
            return null;
        }
        Seckill s = list.get(0);
        return toVO(s, snapshots(List.of(s)).get(s.getSkuId()), now);
    }

    @Override
    public int warmUpAll() {
        List<Seckill> list = seckillMapper.selectList(new LambdaQueryWrapper<Seckill>()
                .gt(Seckill::getEndAt, LocalDateTime.now()));
        for (Seckill s : list) {
            stockCache.warm(s.getId(), remaining(s), s.getEndAt());
        }
        return list.size();
    }

    /**
     * 校验：时间段合法；SKU 属于所选商品；秒杀价低于 SKU 售价；活动库存不超过 SKU 当前库存
     * （秒杀成交后交易领域还要扣 SKU 真实库存，划拨超过就会出现抢到却下不了单）；
     * 同一 SKU 的启用活动时间不能重叠，否则前台不知道该用哪个价格。
     */
    private void validate(Long selfId, SeckillRequest req, int sold) {
        if (!req.getEndAt().isAfter(req.getStartAt())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        SkuSnapshot sku = goodsApi.getSkus(List.of(req.getSkuId())).get(req.getSkuId());
        if (sku == null) {
            throw new BusinessException("SKU 不存在");
        }
        if (!Objects.equals(sku.goodsId(), req.getGoodsId())) {
            throw new BusinessException("SKU 不属于所选商品");
        }
        if (req.getSeckillPrice().compareTo(sku.price()) >= 0) {
            throw new BusinessException("秒杀价必须低于商品售价 " + sku.price());
        }
        int skuStock = sku.stock() == null ? 0 : sku.stock();
        if (req.getStock() - sold > skuStock) {
            throw new BusinessException("活动剩余库存不能超过 SKU 当前库存 " + skuStock);
        }
        boolean overlap = seckillMapper.exists(new LambdaQueryWrapper<Seckill>()
                .eq(Seckill::getSkuId, req.getSkuId())
                .eq(Seckill::getStatus, 1)
                .ne(selfId != null, Seckill::getId, selfId)
                .lt(Seckill::getStartAt, req.getEndAt())
                .gt(Seckill::getEndAt, req.getStartAt()));
        if (overlap && (req.getStatus() == null || req.getStatus() == 1)) {
            throw new BusinessException("该 SKU 在同一时间段已有秒杀活动");
        }
    }

    private void warmAfterCommit(Seckill s) {
        Long id = s.getId();
        int remaining = remaining(s);
        LocalDateTime endAt = s.getEndAt();
        TxCallbacks.afterCommit(() -> stockCache.warm(id, remaining, endAt));
    }

    static int remaining(Seckill s) {
        return Math.max((s.getStock() == null ? 0 : s.getStock()) - (s.getSold() == null ? 0 : s.getSold()), 0);
    }

    private static void apply(Seckill s, SeckillRequest req) {
        s.setTitle(req.getTitle().trim());
        s.setGoodsId(req.getGoodsId());
        s.setSkuId(req.getSkuId());
        s.setSeckillPrice(req.getSeckillPrice().setScale(2, RoundingMode.HALF_UP));
        s.setStock(req.getStock());
        s.setLimitPerMember(req.getLimitPerMember() == null ? 1 : req.getLimitPerMember());
        s.setStartAt(req.getStartAt());
        s.setEndAt(req.getEndAt());
        s.setStatus(req.getStatus() == null ? 1 : req.getStatus());
    }

    private List<SeckillVO> toVOs(List<Seckill> list, LocalDateTime now) {
        Map<Long, SkuSnapshot> skus = snapshots(list);
        List<SeckillVO> result = new ArrayList<>();
        for (Seckill s : list) {
            result.add(toVO(s, skus.get(s.getSkuId()), now));
        }
        // 进行中的排在即将开始的前面
        result.sort(Comparator.comparing((SeckillVO v) -> SeckillState.ONGOING.name().equals(v.getState()) ? 0 : 1));
        return result;
    }

    private Map<Long, SkuSnapshot> snapshots(List<Seckill> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        return goodsApi.getSkus(list.stream().map(Seckill::getSkuId).distinct().toList());
    }

    private Seckill mustGet(Long id) {
        Seckill s = seckillMapper.selectById(id);
        if (s == null) {
            throw new BusinessException("秒杀活动不存在");
        }
        return s;
    }

    static SeckillVO toVO(Seckill s, SkuSnapshot sku, LocalDateTime now) {
        SeckillVO vo = new SeckillVO();
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setGoodsId(s.getGoodsId());
        vo.setSkuId(s.getSkuId());
        if (sku != null) {
            vo.setGoodsName(sku.goodsName());
            vo.setGoodsPic(sku.pic());
            vo.setSpecText(sku.specText());
            vo.setPrice(sku.price());
        }
        vo.setSeckillPrice(s.getSeckillPrice());
        vo.setStock(s.getStock());
        vo.setSold(s.getSold());
        vo.setLimitPerMember(s.getLimitPerMember());
        vo.setStartAt(s.getStartAt());
        vo.setEndAt(s.getEndAt());
        vo.setStatus(s.getStatus());
        vo.setState(SeckillState.of(s.getStartAt(), s.getEndAt(), now).name());
        return vo;
    }
}
