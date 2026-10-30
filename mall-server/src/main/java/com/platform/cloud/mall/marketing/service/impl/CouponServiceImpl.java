package com.platform.cloud.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.marketing.dto.CouponQuery;
import com.platform.cloud.mall.marketing.dto.CouponRecordQuery;
import com.platform.cloud.mall.marketing.dto.CouponRequest;
import com.platform.cloud.mall.marketing.entity.Coupon;
import com.platform.cloud.mall.marketing.entity.MemberCoupon;
import com.platform.cloud.mall.marketing.mapper.CouponMapper;
import com.platform.cloud.mall.marketing.mapper.MemberCouponMapper;
import com.platform.cloud.mall.marketing.service.CouponService;
import com.platform.cloud.mall.marketing.support.CouponScope;
import com.platform.cloud.mall.marketing.support.CouponType;
import com.platform.cloud.mall.marketing.support.CouponValidType;
import com.platform.cloud.mall.marketing.support.MemberCouponStatus;
import com.platform.cloud.mall.marketing.vo.AvailableCouponVO;
import com.platform.cloud.mall.marketing.vo.CouponRecordVO;
import com.platform.cloud.mall.marketing.vo.CouponVO;
import com.platform.cloud.mall.marketing.vo.MyCouponVO;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券服务。
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    /** 「我的券」单次最多返回条数：个人券量通常几十张，设上限防止异常数据拖垮接口 */
    static final int MY_COUPON_LIMIT = 200;

    private final CouponMapper couponMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final MemberApi memberApi;

    @Override
    public PageVO<CouponVO> page(CouponQuery q) {
        LambdaQueryWrapper<Coupon> w = new LambdaQueryWrapper<Coupon>()
                .like(StringUtils.hasText(q.getKeyword()), Coupon::getName, q.getKeyword())
                .eq(StringUtils.hasText(q.getType()), Coupon::getType, q.getType())
                .eq(q.getStatus() != null, Coupon::getStatus, q.getStatus())
                .orderByDesc(Coupon::getId);
        return PageVO.of(couponMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), CouponServiceImpl::toVO);
    }

    @Override
    public CouponVO get(Long id) {
        return toVO(mustGet(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CouponRequest req) {
        Coupon c = new Coupon();
        c.setIssued(0);
        c.setUsed(0);
        apply(c, req);
        couponMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CouponRequest req) {
        Coupon c = mustGet(id);
        int issued = c.getIssued() == null ? 0 : c.getIssued();
        if (req.getTotal() != 0 && req.getTotal() < issued) {
            throw new BusinessException("发行量不能小于已领取数量 " + issued);
        }
        Coupon updated = new Coupon();
        apply(updated, req);
        if (issued > 0 && ruleChanged(c, updated)) {
            throw new BusinessException("已有会员领取，只能修改名称、发行量、限领、状态和说明");
        }
        apply(c, req);
        if (couponMapper.updateById(c) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Coupon c = mustGet(id);
        if (c.getIssued() != null && c.getIssued() > 0) {
            throw new BusinessException("已有会员领取，不能删除，可以停止发放");
        }
        couponMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态只能是 0 或 1");
        }
        Coupon c = mustGet(id);
        c.setStatus(status);
        if (couponMapper.updateById(c) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
    }

    @Override
    public PageVO<CouponRecordVO> records(Long couponId, CouponRecordQuery q) {
        mustGet(couponId);
        Page<MemberCoupon> page = memberCouponMapper.selectPage(new Page<>(q.getPage(), q.getSize()),
                new LambdaQueryWrapper<MemberCoupon>()
                        .eq(MemberCoupon::getCouponId, couponId)
                        .eq(StringUtils.hasText(q.getStatus()), MemberCoupon::getStatus, q.getStatus())
                        .eq(q.getMemberId() != null, MemberCoupon::getMemberId, q.getMemberId())
                        .orderByDesc(MemberCoupon::getId));
        List<Long> memberIds = page.getRecords().stream().map(MemberCoupon::getMemberId).distinct().toList();
        Map<Long, MemberBrief> members = memberIds.isEmpty() ? Map.of() : memberApi.getMembers(memberIds);
        return PageVO.of(page, mc -> {
            CouponRecordVO vo = new CouponRecordVO();
            vo.setId(mc.getId());
            vo.setMemberId(mc.getMemberId());
            MemberBrief m = members == null ? null : members.get(mc.getMemberId());
            vo.setMemberMobile(m == null ? null : maskMobile(m.mobile()));
            vo.setStatus(mc.getStatus());
            vo.setStartAt(mc.getStartAt());
            vo.setEndAt(mc.getEndAt());
            vo.setUsedAt(mc.getUsedAt());
            vo.setOrderId(mc.getOrderId());
            vo.setCreatedAt(mc.getCreatedAt());
            return vo;
        });
    }

    /**
     * 领取顺序很关键：先用条件更新占用发行量（拿到模板行锁，同一张券的领取串行化），
     * 再当前读统计该会员已领数量；超出限领就抛异常回滚，占用的发行量随事务一起撤销。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long receive(Long couponId, Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        Coupon c = couponMapper.selectById(couponId);
        checkReceivable(c, now);
        if (couponMapper.increaseIssued(couponId) == 0) {
            throw new BusinessException("优惠券已领完");
        }
        int perLimit = c.getPerLimit() == null || c.getPerLimit() < 1 ? 1 : c.getPerLimit();
        if (memberCouponMapper.countReceivedForUpdate(couponId, memberId) >= perLimit) {
            throw new BusinessException("每人限领 " + perLimit + " 张，您已领过了");
        }
        MemberCoupon mc = new MemberCoupon();
        mc.setMemberId(memberId);
        mc.setCouponId(couponId);
        mc.setStatus(MemberCouponStatus.UNUSED.name());
        LocalDateTime[] period = validPeriod(c, now);
        mc.setStartAt(period[0]);
        mc.setEndAt(period[1]);
        memberCouponMapper.insert(mc);
        return mc.getId();
    }

    /** 领取前的业务校验（状态、有效期、库存），纯内存判断，便于单测 */
    static void checkReceivable(Coupon c, LocalDateTime now) {
        if (c == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (c.getStatus() == null || c.getStatus() != 1) {
            throw new BusinessException("优惠券已停止发放");
        }
        if (CouponValidType.FIXED.name().equals(c.getValidType()) && (c.getEndAt() == null || !c.getEndAt().isAfter(now))) {
            throw new BusinessException("优惠券已过期");
        }
        int total = c.getTotal() == null ? 0 : c.getTotal();
        if (total > 0 && c.getIssued() != null && c.getIssued() >= total) {
            throw new BusinessException("优惠券已领完");
        }
    }

    /** 会员券有效期：固定时间段用模板时间；领取后 N 天从领取时刻起算 */
    static LocalDateTime[] validPeriod(Coupon c, LocalDateTime now) {
        if (CouponValidType.DAYS.name().equals(c.getValidType())) {
            int days = c.getValidDays() == null || c.getValidDays() < 1 ? 1 : c.getValidDays();
            return new LocalDateTime[]{now, now.plusDays(days)};
        }
        return new LocalDateTime[]{c.getStartAt(), c.getEndAt()};
    }

    @Override
    public List<MyCouponVO> mine(Long memberId, String status) {
        LocalDateTime now = LocalDateTime.now();
        String s = StringUtils.hasText(status) ? status : MemberCouponStatus.UNUSED.name();
        LambdaQueryWrapper<MemberCoupon> w = new LambdaQueryWrapper<MemberCoupon>().eq(MemberCoupon::getMemberId, memberId);
        if (MemberCouponStatus.UNUSED.name().equals(s)) {
            w.eq(MemberCoupon::getStatus, s).gt(MemberCoupon::getEndAt, now).orderByAsc(MemberCoupon::getEndAt);
        } else if (MemberCouponStatus.USED.name().equals(s)) {
            w.eq(MemberCoupon::getStatus, s).orderByDesc(MemberCoupon::getUsedAt);
        } else if (MemberCouponStatus.EXPIRED.name().equals(s)) {
            // 定时任务每分钟才跑一次，未及时处理的过期券也归到「已过期」
            w.and(x -> x.eq(MemberCoupon::getStatus, s)
                            .or(y -> y.eq(MemberCoupon::getStatus, MemberCouponStatus.UNUSED.name())
                                    .le(MemberCoupon::getEndAt, now)))
                    .orderByDesc(MemberCoupon::getEndAt);
        } else {
            throw new BusinessException("状态只能是 UNUSED / USED / EXPIRED");
        }
        List<MemberCoupon> list = memberCouponMapper.selectPage(new Page<>(1, MY_COUPON_LIMIT, false), w).getRecords();
        Map<Long, Coupon> templates = loadTemplates(list.stream().map(MemberCoupon::getCouponId).toList());
        List<MyCouponVO> result = new ArrayList<>();
        for (MemberCoupon mc : list) {
            Coupon c = templates.get(mc.getCouponId());
            if (c == null) {
                continue;
            }
            MyCouponVO vo = new MyCouponVO();
            vo.setId(mc.getId());
            vo.setCouponId(c.getId());
            vo.setName(c.getName());
            vo.setType(c.getType());
            vo.setThreshold(c.getThreshold());
            vo.setAmount(c.getAmount());
            vo.setDiscount(c.getDiscount());
            vo.setMaxAmount(c.getMaxAmount());
            vo.setScope(c.getScope());
            vo.setStartAt(mc.getStartAt());
            vo.setEndAt(mc.getEndAt());
            boolean lateExpired = MemberCouponStatus.UNUSED.name().equals(mc.getStatus()) && !mc.getEndAt().isAfter(now);
            vo.setStatus(lateExpired ? MemberCouponStatus.EXPIRED.name() : mc.getStatus());
            vo.setDescription(c.getDescription());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AvailableCouponVO> available(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .and(x -> x.eq(Coupon::getValidType, CouponValidType.DAYS.name()).or().gt(Coupon::getEndAt, now))
                // 常量 SQL 片段，不含入参：比较同一行的两个列，Lambda 条件无法表达
                .and(x -> x.eq(Coupon::getTotal, 0).or().apply("issued < total"))
                .orderByDesc(Coupon::getId));
        Map<Long, Integer> received = new HashMap<>();
        if (memberId != null && !coupons.isEmpty()) {
            memberCouponMapper.selectList(new LambdaQueryWrapper<MemberCoupon>()
                            .select(MemberCoupon::getId, MemberCoupon::getCouponId)
                            .eq(MemberCoupon::getMemberId, memberId)
                            .in(MemberCoupon::getCouponId, coupons.stream().map(Coupon::getId).toList()))
                    .forEach(mc -> received.merge(mc.getCouponId(), 1, Integer::sum));
        }
        List<AvailableCouponVO> result = new ArrayList<>();
        for (Coupon c : coupons) {
            AvailableCouponVO vo = new AvailableCouponVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setType(c.getType());
            vo.setThreshold(c.getThreshold());
            vo.setAmount(c.getAmount());
            vo.setDiscount(c.getDiscount());
            vo.setMaxAmount(c.getMaxAmount());
            vo.setScope(c.getScope());
            vo.setValidType(c.getValidType());
            vo.setStartAt(c.getStartAt());
            vo.setEndAt(c.getEndAt());
            vo.setValidDays(c.getValidDays());
            vo.setPerLimit(c.getPerLimit());
            vo.setDescription(c.getDescription());
            if (c.getTotal() != null && c.getTotal() > 0) {
                vo.setRemaining(Math.max(c.getTotal() - (c.getIssued() == null ? 0 : c.getIssued()), 0));
            }
            if (memberId != null) {
                vo.setReceived(received.getOrDefault(c.getId(), 0));
            }
            result.add(vo);
        }
        return result;
    }

    private Map<Long, Coupon> loadTemplates(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return new HashMap<>();
        }
        return couponMapper.selectBatchIds(distinct).stream().collect(Collectors.toMap(Coupon::getId, Function.identity()));
    }

    private Coupon mustGet(Long id) {
        Coupon c = couponMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("优惠券不存在");
        }
        return c;
    }

    /**
     * 按类型规整并校验表单，写入实体。不同类型只保留各自有意义的字段，避免脏数据参与计算。
     */
    static void apply(Coupon c, CouponRequest req) {
        CouponType type = CouponType.valueOf(req.getType());
        BigDecimal threshold = money(req.getThreshold());
        BigDecimal amount = money(req.getAmount());
        BigDecimal discount = BigDecimal.ONE;
        BigDecimal maxAmount = BigDecimal.ZERO.setScale(2);
        switch (type) {
            case FULL_REDUCTION -> {
                if (amount.signum() <= 0) {
                    throw new BusinessException("请填写减免金额");
                }
                if (threshold.compareTo(amount) <= 0) {
                    throw new BusinessException("满减券的门槛必须大于减免金额");
                }
            }
            case NO_THRESHOLD -> {
                if (amount.signum() <= 0) {
                    throw new BusinessException("请填写减免金额");
                }
                threshold = BigDecimal.ZERO.setScale(2);
            }
            case DISCOUNT -> {
                if (req.getDiscount() == null || req.getDiscount().signum() <= 0
                        || req.getDiscount().compareTo(BigDecimal.ONE) >= 0) {
                    throw new BusinessException("折扣率必须在 0 到 1 之间，如 0.90 表示 9 折");
                }
                discount = req.getDiscount().setScale(2, RoundingMode.HALF_UP);
                maxAmount = money(req.getMaxAmount());
                amount = BigDecimal.ZERO.setScale(2);
            }
            default -> throw new BusinessException("不支持的优惠券类型");
        }

        CouponScope scope = StringUtils.hasText(req.getScope()) ? CouponScope.valueOf(req.getScope()) : CouponScope.ALL;
        List<Long> scopeIds = null;
        if (scope != CouponScope.ALL) {
            scopeIds = req.getScopeIds() == null ? List.of()
                    : req.getScopeIds().stream().filter(Objects::nonNull).distinct().toList();
            if (scopeIds.isEmpty()) {
                throw new BusinessException(scope == CouponScope.CATEGORY ? "请选择适用类目" : "请选择适用商品");
            }
            scopeIds = new ArrayList<>(scopeIds);
        }

        CouponValidType validType = CouponValidType.valueOf(req.getValidType());
        LocalDateTime startAt = null;
        LocalDateTime endAt = null;
        int validDays = 0;
        if (validType == CouponValidType.FIXED) {
            if (req.getStartAt() == null || req.getEndAt() == null) {
                throw new BusinessException("请填写有效期");
            }
            if (!req.getEndAt().isAfter(req.getStartAt())) {
                throw new BusinessException("结束时间必须晚于开始时间");
            }
            startAt = req.getStartAt();
            endAt = req.getEndAt();
        } else {
            if (req.getValidDays() == null || req.getValidDays() < 1) {
                throw new BusinessException("请填写领取后有效天数");
            }
            validDays = req.getValidDays();
        }

        c.setName(req.getName().trim());
        c.setType(type.name());
        c.setThreshold(threshold);
        c.setAmount(amount);
        c.setDiscount(discount);
        c.setMaxAmount(maxAmount);
        c.setScope(scope.name());
        c.setScopeIds(scopeIds);
        c.setTotal(req.getTotal());
        c.setPerLimit(req.getPerLimit() == null ? 1 : req.getPerLimit());
        c.setValidType(validType.name());
        c.setStartAt(startAt);
        c.setEndAt(endAt);
        c.setValidDays(validDays);
        c.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        c.setDescription(req.getDescription() == null ? "" : req.getDescription());
    }

    /** 影响已领券优惠结果或有效期的字段是否被修改 */
    static boolean ruleChanged(Coupon old, Coupon updated) {
        return !Objects.equals(old.getType(), updated.getType())
                || cmp(old.getThreshold(), updated.getThreshold())
                || cmp(old.getAmount(), updated.getAmount())
                || cmp(old.getDiscount(), updated.getDiscount())
                || cmp(old.getMaxAmount(), updated.getMaxAmount())
                || !Objects.equals(old.getScope(), updated.getScope())
                || !Objects.equals(normalizeIds(old.getScopeIds()), normalizeIds(updated.getScopeIds()))
                || !Objects.equals(old.getValidType(), updated.getValidType())
                || !Objects.equals(old.getStartAt(), updated.getStartAt())
                || !Objects.equals(old.getEndAt(), updated.getEndAt())
                || !Objects.equals(old.getValidDays(), updated.getValidDays());
    }

    private static List<Long> normalizeIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().sorted().toList();
    }

    private static boolean cmp(BigDecimal a, BigDecimal b) {
        BigDecimal x = a == null ? BigDecimal.ZERO : a;
        BigDecimal y = b == null ? BigDecimal.ZERO : b;
        return x.compareTo(y) != 0;
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    /** 后台列表手机号脱敏：138****1234 */
    static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7 || mobile.contains("*")) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    static CouponVO toVO(Coupon c) {
        CouponVO vo = new CouponVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setType(c.getType());
        vo.setThreshold(c.getThreshold());
        vo.setAmount(c.getAmount());
        vo.setDiscount(c.getDiscount());
        vo.setMaxAmount(c.getMaxAmount());
        vo.setScope(c.getScope());
        vo.setScopeIds(c.getScopeIds() == null ? new ArrayList<>() : c.getScopeIds());
        vo.setTotal(c.getTotal());
        vo.setIssued(c.getIssued());
        vo.setUsed(c.getUsed());
        vo.setPerLimit(c.getPerLimit());
        vo.setValidType(c.getValidType());
        vo.setStartAt(c.getStartAt());
        vo.setEndAt(c.getEndAt());
        vo.setValidDays(c.getValidDays());
        vo.setStatus(c.getStatus());
        vo.setDescription(c.getDescription());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }
}
