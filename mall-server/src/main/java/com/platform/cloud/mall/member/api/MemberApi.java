package com.platform.cloud.mall.member.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * 会员领域对其他领域开放的能力。其他领域只能通过本接口访问会员数据，不直接使用会员的 Mapper。
 */
public interface MemberApi {

    /** 会员概要；不存在返回 null */
    MemberBrief getMember(Long memberId);

    /** 批量取会员概要（后台订单列表展示买家） */
    Map<Long, MemberBrief> getMembers(Collection<Long> memberIds);

    /** 取会员自己的地址；地址不属于该会员时抛业务异常，防止越权使用他人地址 */
    AddressSnapshot getAddress(Long memberId, Long addressId);

    /**
     * 变更余额。delta 为负表示扣减，余额不足抛业务异常「余额不足」。
     * 同时写入账户流水，调用方需在自己的事务内调用以保证一致。
     *
     * @param bizType 见 mbr_account_log.biz_type，如 ORDER_PAY / ORDER_REFUND
     */
    void changeBalance(Long memberId, BigDecimal delta, String bizType, String bizNo, String remark);

    /** 变更积分，规则同余额 */
    void changePoints(Long memberId, int delta, String bizType, String bizNo, String remark);

    /** 增加成长值并按等级表自动升级 */
    void addGrowth(Long memberId, int delta, String bizType, String bizNo, String remark);

    /**
     * 回收已发放的订单奖励（确认收货后又退款）：积分、成长值各自最多扣到 0，不因余额不足而失败——
     * 积分可能已被会员花掉，退款不能因此卡住。实际扣减数各写一条流水（扣减为 0 的不写）。
     * 会员等级不随成长值下降而降级，避免退款导致等级频繁跳动（等级只升不降，与发放规则一致）。
     *
     * @param points 计划回收的积分（≥ 0）
     * @param growth 计划回收的成长值（≥ 0）
     */
    void reclaimReward(Long memberId, int points, int growth, String bizType, String bizNo, String remark);

    /** 是否已收藏该商品 */
    boolean isFavorited(Long memberId, Long goodsId);

    /** 记录浏览足迹：同一商品只保留一条，刷新浏览时间 */
    void recordBrowse(Long memberId, Long goodsId);

    /** 统计用：时间段内新注册会员数 */
    long countRegistered(LocalDateTime from, LocalDateTime to);

    /** 统计用：[from, to) 内按注册日期（yyyy-MM-dd）分组的新会员数，一次查询；没有注册的日期不返回 */
    Map<String, Long> countRegisteredDaily(LocalDateTime from, LocalDateTime to);

    record MemberBrief(Long id, String mobile, String nickname, String avatar, Long levelId, String levelName,
                       BigDecimal discount, BigDecimal balance, Integer points, Integer status) {
    }

    record AddressSnapshot(String name, String mobile, String province, String city, String district, String detail) {
    }
}
