package com.platform.cloud.mall.trade.review.service.impl;

import com.platform.cloud.mall.system.api.FileApi;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.review.dto.ReviewCreateRequest;
import com.platform.cloud.mall.trade.review.entity.Review;
import com.platform.cloud.mall.trade.review.mapper.ReviewMapper;
import com.platform.cloud.mall.trade.review.mapper.row.ReviewStatRow;
import com.platform.cloud.mall.trade.review.vo.ReviewStatVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceImplTest {

    private static final Long MEMBER = 1001L;
    private static final Long ORDER_ID = 5001L;
    private static final Long ITEM_ID = 6001L;

    @Mock
    ReviewMapper reviewMapper;
    @Mock
    OrderMapper orderMapper;
    @Mock
    OrderItemMapper orderItemMapper;
    @Mock
    MemberApi memberApi;
    @Mock
    FileApi fileApi;
    @InjectMocks
    ReviewServiceImpl service;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Order.class, OrderItem.class, Review.class);
    }

    @BeforeEach
    void setUp() {
        when(orderItemMapper.selectOne(any())).thenReturn(item(0));
        when(orderMapper.selectOne(any())).thenReturn(order("COMPLETED"));
        when(orderItemMapper.update(isNull(), any())).thenReturn(1);
        when(memberApi.getMember(MEMBER)).thenReturn(new MemberBrief(MEMBER, "138****0000", "小明", "a.png", 1L,
                "", BigDecimal.ONE, BigDecimal.ZERO, 0, 1));
    }

    private static OrderItem item(int reviewed) {
        OrderItem i = new OrderItem();
        i.setId(ITEM_ID);
        i.setOrderId(ORDER_ID);
        i.setGoodsId(300L);
        i.setSpecText("红色");
        i.setReviewed(reviewed);
        return i;
    }

    private static Order order(String status) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setMemberId(MEMBER);
        o.setStatus(status);
        return o;
    }

    private static ReviewCreateRequest req(boolean anonymous) {
        ReviewCreateRequest r = new ReviewCreateRequest();
        r.setOrderItemId(ITEM_ID);
        r.setRating(5);
        r.setContent("很好");
        r.setImages(List.of("1.png"));
        r.setAnonymous(anonymous);
        return r;
    }

    @Test
    void 已评价的明细不能重复评价() {
        when(orderItemMapper.selectOne(any())).thenReturn(item(1));
        assertThatThrownBy(() -> service.create(MEMBER, req(false))).hasMessage("该商品已评价");
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void 并发重复评价被条件更新拦截() {
        when(orderItemMapper.update(isNull(), any())).thenReturn(0);
        assertThatThrownBy(() -> service.create(MEMBER, req(false))).hasMessage("该商品已评价");
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void 唯一索引兜底() {
        when(reviewMapper.insert(any(Review.class))).thenThrow(new DuplicateKeyException("uk_item"));
        assertThatThrownBy(() -> service.create(MEMBER, req(false))).hasMessage("该商品已评价");
    }

    @Test
    void 未完成订单不能评价() {
        when(orderMapper.selectOne(any())).thenReturn(order("DELIVERED"));
        assertThatThrownBy(() -> service.create(MEMBER, req(false))).hasMessageContaining("订单完成后");
    }

    @Test
    void 他人明细视为不存在() {
        when(orderItemMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.create(MEMBER, req(false))).hasMessage("订单商品不存在");
    }

    @Test
    void 实名评价快照昵称头像_全部评完订单置为已评价() {
        when(orderItemMapper.selectCount(any())).thenReturn(0L);

        service.create(MEMBER, req(false));

        ArgumentCaptor<Review> r = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(r.capture());
        assertThat(r.getValue().getNickname()).isEqualTo("小明");
        assertThat(r.getValue().getAvatar()).isEqualTo("a.png");
        assertThat(r.getValue().getAnonymous()).isZero();
        assertThat(r.getValue().getSpecText()).isEqualTo("红色");
        assertThat(r.getValue().getGoodsId()).isEqualTo(300L);
        verify(orderMapper).update(isNull(), any());
    }

    @Test
    void 匿名评价显示匿名用户_还有未评明细时订单不变() {
        when(orderItemMapper.selectCount(any())).thenReturn(1L);

        service.create(MEMBER, req(true));

        ArgumentCaptor<Review> r = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(r.capture());
        assertThat(r.getValue().getNickname()).isEqualTo("匿名用户");
        assertThat(r.getValue().getAvatar()).isEmpty();
        assertThat(r.getValue().getAnonymous()).isEqualTo(1);
        verify(memberApi, never()).getMember(anyLong());
        verify(orderMapper, never()).update(isNull(), any());
    }

    @Test
    void 好评率() {
        assertThat(ReviewServiceImpl.goodRate(0, 0)).isEqualTo(100);
        assertThat(ReviewServiceImpl.goodRate(2, 3)).isEqualTo(67);
        assertThat(ReviewServiceImpl.goodRate(0, 5)).isZero();
    }

    @Test
    void 统计结果为空时全为0且好评率100() {
        when(reviewMapper.statByGoods(300L)).thenReturn(new ReviewStatRow());
        ReviewStatVO stat = service.stat(300L);
        assertThat(stat.total()).isZero();
        assertThat(stat.goodRate()).isEqualTo(100);
    }
}
