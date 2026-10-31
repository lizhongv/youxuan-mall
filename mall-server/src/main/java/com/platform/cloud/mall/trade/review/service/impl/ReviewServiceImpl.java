package com.platform.cloud.mall.trade.review.service.impl;

import com.platform.cloud.mall.system.api.FileApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.api.TradeApi.ReviewBrief;
import com.platform.cloud.mall.trade.api.TradeApi.ReviewSummary;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.review.dto.AdminReviewPageQuery;
import com.platform.cloud.mall.trade.review.dto.PublicReviewPageQuery;
import com.platform.cloud.mall.trade.review.dto.ReviewCreateRequest;
import com.platform.cloud.mall.trade.review.entity.Review;
import com.platform.cloud.mall.trade.review.mapper.ReviewMapper;
import com.platform.cloud.mall.trade.review.mapper.row.ReviewStatRow;
import com.platform.cloud.mall.trade.review.service.ReviewService;
import com.platform.cloud.mall.trade.review.vo.ReviewStatVO;
import com.platform.cloud.mall.trade.review.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品评价。
 * <p>
 * 「每条明细只能评一次」由明细 reviewed 的条件更新（0 → 1）保证，trd_review 的唯一索引兜底；
 * 全部可评明细（已退款的除外）评完后，订单 reviewed = 1，退出「待评价」列表。
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    static final String ANONYMOUS_NAME = "匿名用户";
    /** 商品详情页展示的最新评价条数（TradeApi 约定为 2） */
    static final int LATEST_SIZE = 2;

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MemberApi memberApi;
    private final FileApi fileApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long memberId, ReviewCreateRequest req) {
        OrderItem item = orderItemMapper.selectOne(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getId, req.getOrderItemId())
                .eq(OrderItem::getMemberId, memberId));
        if (item == null) {
            throw new BusinessException("订单商品不存在");
        }
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, item.getOrderId())
                .eq(Order::getMemberId, memberId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException("订单完成后才能评价");
        }
        if (item.getReviewed() != null && item.getReviewed() == 1) {
            throw new BusinessException("该商品已评价");
        }
        int rows = orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                .eq(OrderItem::getId, item.getId())
                .eq(OrderItem::getReviewed, 0)
                .set(OrderItem::getReviewed, 1));
        if (rows == 0) {
            throw new BusinessException("该商品已评价");
        }

        boolean anonymous = Boolean.TRUE.equals(req.getAnonymous());
        MemberBrief member = anonymous ? null : memberApi.getMember(memberId);
        Review r = new Review();
        r.setOrderId(order.getId());
        r.setOrderItemId(item.getId());
        r.setGoodsId(item.getGoodsId());
        r.setMemberId(memberId);
        // 昵称头像在评价时快照；匿名时不存真实昵称，避免被后续展示逻辑误泄露
        r.setNickname(anonymous ? ANONYMOUS_NAME : nz(member == null ? null : member.nickname()));
        r.setAvatar(anonymous ? "" : nz(member == null ? null : member.avatar()));
        r.setSpecText(nz(item.getSpecText()));
        r.setRating(req.getRating());
        r.setContent(req.getContent() == null ? "" : req.getContent().trim());
        // 会员提交的图片只允许本站上传地址：防外链钓鱼、追踪像素与伪协议
        fileApi.requireUploadedImages(req.getImages());
        r.setImages(req.getImages() == null ? List.of() : req.getImages());
        r.setAnonymous(anonymous ? 1 : 0);
        r.setReply("");
        r.setStatus(1);
        try {
            reviewMapper.insert(r);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该商品已评价");
        }

        long pending = orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId())
                .eq(OrderItem::getReviewed, 0)
                .ne(OrderItem::getAftersaleStatus, OrderItem.AS_REFUNDED));
        if (pending == 0) {
            orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                    .eq(Order::getId, order.getId())
                    .set(Order::getReviewed, 1));
        }
        return r.getId();
    }

    @Override
    public PageVO<ReviewVO> minePage(Long memberId, PageQuery q) {
        Page<Review> page = reviewMapper.selectPage(new Page<>(q.getPage(), q.getSize()),
                Wrappers.<Review>lambdaQuery()
                        .eq(Review::getMemberId, memberId)
                        .orderByDesc(Review::getCreatedAt).orderByDesc(Review::getId));
        return toPage(page, true);
    }

    @Override
    public PageVO<ReviewVO> publicPage(Long goodsId, PublicReviewPageQuery q) {
        LambdaQueryWrapper<Review> w = Wrappers.<Review>lambdaQuery()
                .eq(Review::getGoodsId, goodsId)
                .eq(Review::getStatus, 1);
        String type = StringUtils.hasText(q.getType()) ? q.getType() : "ALL";
        switch (type) {
            case "GOOD" -> w.ge(Review::getRating, 4);
            case "MEDIUM" -> w.eq(Review::getRating, 3);
            case "BAD" -> w.le(Review::getRating, 2);
            // 常量片段，无用户输入
            case "IMAGE" -> w.apply("images IS NOT NULL AND JSON_LENGTH(images) > 0");
            default -> {
            }
        }
        w.orderByDesc(Review::getCreatedAt).orderByDesc(Review::getId);
        PageVO<ReviewVO> page = toPage(reviewMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), false);
        page.getRecords().forEach(v -> v.setMemberId(null));
        return page;
    }

    @Override
    public ReviewStatVO stat(Long goodsId) {
        ReviewStatRow row = reviewMapper.statByGoods(goodsId);
        long total = row == null ? 0 : nz(row.getTotal());
        long good = row == null ? 0 : nz(row.getGood());
        long medium = row == null ? 0 : nz(row.getMedium());
        long bad = row == null ? 0 : nz(row.getBad());
        long image = row == null ? 0 : nz(row.getImage());
        return new ReviewStatVO(total, good, medium, bad, image, goodRate(good, total));
    }

    @Override
    public ReviewSummary summary(Long goodsId) {
        ReviewStatVO stat = stat(goodsId);
        List<ReviewBrief> latest = stat.total() == 0 ? List.of()
                : reviewMapper.selectPage(new Page<>(1, LATEST_SIZE, false), Wrappers.<Review>lambdaQuery()
                                .eq(Review::getGoodsId, goodsId)
                                .eq(Review::getStatus, 1)
                                .orderByDesc(Review::getCreatedAt).orderByDesc(Review::getId))
                        .getRecords().stream()
                        .map(r -> new ReviewBrief(r.getId(), r.getNickname(), r.getAvatar(), r.getRating(),
                                r.getContent(), r.getImages() == null ? List.of() : r.getImages(), r.getSpecText(),
                                r.getCreatedAt()))
                        .toList();
        return new ReviewSummary(stat.total(), stat.goodRate(), latest);
    }

    @Override
    public PageVO<ReviewVO> adminPage(AdminReviewPageQuery q) {
        LambdaQueryWrapper<Review> w = Wrappers.<Review>lambdaQuery()
                .eq(q.getGoodsId() != null, Review::getGoodsId, q.getGoodsId())
                .eq(q.getRating() != null, Review::getRating, q.getRating())
                .eq(q.getStatus() != null, Review::getStatus, q.getStatus());
        if (q.getHasImage() != null) {
            if (q.getHasImage() == 1) {
                w.apply("images IS NOT NULL AND JSON_LENGTH(images) > 0");
            } else {
                w.apply("(images IS NULL OR JSON_LENGTH(images) = 0)");
            }
        }
        w.orderByDesc(Review::getCreatedAt).orderByDesc(Review::getId);
        return toPage(reviewMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long adminId, Long id, String reply) {
        requireReview(id);
        reviewMapper.update(null, Wrappers.<Review>lambdaUpdate()
                .eq(Review::getId, id)
                .set(Review::getReply, reply.trim())
                .set(Review::getReplyTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long adminId, Long id, int status) {
        if (status != 0 && status != 1) {
            throw new BusinessException("状态只能是 0 或 1");
        }
        requireReview(id);
        reviewMapper.update(null, Wrappers.<Review>lambdaUpdate()
                .eq(Review::getId, id)
                .set(Review::getStatus, status));
    }

    /** 好评率：4-5 星占比，四舍五入到整数百分比；无评价时 100，避免新品显示 0% 好评 */
    static int goodRate(long good, long total) {
        if (total <= 0) {
            return 100;
        }
        return (int) Math.round(good * 100.0 / total);
    }

    private void requireReview(Long id) {
        if (reviewMapper.selectById(id) == null) {
            throw new BusinessException("评价不存在");
        }
    }

    private PageVO<ReviewVO> toPage(Page<Review> page, boolean withMemberId) {
        List<Review> records = page.getRecords();
        Map<Long, OrderItem> items = records.isEmpty() ? Map.of()
                : orderItemMapper.selectBatchIds(records.stream().map(Review::getOrderItemId)
                        .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        return PageVO.of(page, r -> toVO(r, items.get(r.getOrderItemId()), withMemberId));
    }

    private static ReviewVO toVO(Review r, OrderItem item, boolean withMemberId) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setOrderId(r.getOrderId());
        vo.setGoodsId(r.getGoodsId());
        vo.setGoodsName(item == null ? null : item.getGoodsName());
        vo.setGoodsPic(item == null ? null : item.getPic());
        vo.setMemberId(withMemberId ? r.getMemberId() : null);
        vo.setNickname(r.getNickname());
        vo.setAvatar(r.getAvatar());
        vo.setSpecText(r.getSpecText());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        vo.setImages(r.getImages() == null ? List.of() : r.getImages());
        vo.setAnonymous(r.getAnonymous());
        vo.setReply(r.getReply());
        vo.setReplyTime(r.getReplyTime());
        vo.setStatus(r.getStatus());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
