package com.platform.cloud.mall.marketing.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.marketing.dto.CouponQuery;
import com.platform.cloud.mall.marketing.dto.CouponRecordQuery;
import com.platform.cloud.mall.marketing.dto.CouponRequest;
import com.platform.cloud.mall.marketing.vo.AvailableCouponVO;
import com.platform.cloud.mall.marketing.vo.CouponRecordVO;
import com.platform.cloud.mall.marketing.vo.CouponVO;
import com.platform.cloud.mall.marketing.vo.MyCouponVO;

import java.util.List;

/**
 * 优惠券：模板维护（管理端）与领取、我的券（商城端）。下单相关的计算与核销见 CouponApi。
 */
public interface CouponService {

    PageVO<CouponVO> page(CouponQuery q);

    CouponVO get(Long id);

    Long create(CouponRequest req);

    /** 已有会员领取后，只能改名称、发行量、限领、状态、说明，避免已领的券优惠规则被悄悄改变 */
    void update(Long id, CouponRequest req);

    /** 已有会员领取的券不能删除，只能停发 */
    void delete(Long id);

    void updateStatus(Long id, Integer status);

    PageVO<CouponRecordVO> records(Long couponId, CouponRecordQuery q);

    /** 领取，返回会员券 ID */
    Long receive(Long couponId, Long memberId);

    /** 我的券，status 为空时查未使用 */
    List<MyCouponVO> mine(Long memberId, String status);

    /** 领券中心：可领取的券；memberId 非空时带上已领数量 */
    List<AvailableCouponVO> available(Long memberId);
}
