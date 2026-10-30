package com.platform.cloud.mall.member.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.dto.AccountLogQuery;
import com.platform.cloud.mall.member.vo.AccountLogVO;

import java.math.BigDecimal;

/**
 * 会员账户：余额、积分、成长值的变更与流水。所有变更都用条件更新保证并发安全，并在同一事务里写流水。
 */
public interface MemberAccountService {

    /** 变更余额，扣减后小于 0 抛「余额不足」 */
    void changeBalance(Long memberId, BigDecimal delta, String bizType, String bizNo, String remark);

    /** 变更积分，扣减后小于 0 抛「积分不足」 */
    void changePoints(Long memberId, int delta, String bizType, String bizNo, String remark);

    /** 增加成长值，达到更高等级门槛时自动升级（只升不降） */
    void addGrowth(Long memberId, int delta, String bizType, String bizNo, String remark);

    /** 流水分页；memberId 为空时查全部（仅后台） */
    PageVO<AccountLogVO> pageLogs(AccountLogQuery query);
}
