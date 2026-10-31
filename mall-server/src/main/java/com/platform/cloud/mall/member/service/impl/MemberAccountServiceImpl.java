package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.dto.AccountLogQuery;
import com.platform.cloud.mall.member.entity.MbrAccountLog;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrAccountLogMapper;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.MemberAccountService;
import com.platform.cloud.mall.member.service.MemberLevelService;
import com.platform.cloud.mall.member.support.AccountType;
import com.platform.cloud.mall.member.vo.AccountLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 会员账户变更。
 * <p>
 * 先做「原值 + 增量」的条件更新（数据库行锁保证原子，扣成负数时影响 0 行），再在同一事务里读出变更后的值写流水。
 * 由于本事务已持有该行的写锁，读到的就是本次变更后的值，不会被并发事务干扰。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAccountServiceImpl implements MemberAccountService {

    /** 与 mbr_account_log 列宽一致 */
    private static final int BIZ_NO_MAX = 64;
    private static final int REMARK_MAX = 255;

    private final MbrMemberMapper memberMapper;
    private final MbrAccountLogMapper accountLogMapper;
    private final MemberLevelService levelService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeBalance(Long memberId, BigDecimal delta, String bizType, String bizNo, String remark) {
        if (delta == null || delta.signum() == 0) {
            return;
        }
        BigDecimal amount = delta.setScale(2, RoundingMode.HALF_UP);
        if (memberMapper.changeBalance(memberId, amount) == 0) {
            throw failure(memberId, amount.signum() < 0, "余额不足");
        }
        MbrMember m = memberMapper.selectById(memberId);
        writeLog(memberId, AccountType.BALANCE, amount, m.getBalance(), bizType, bizNo, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePoints(Long memberId, int delta, String bizType, String bizNo, String remark) {
        if (delta == 0) {
            return;
        }
        if (memberMapper.changePoints(memberId, delta) == 0) {
            throw failure(memberId, delta < 0, "积分不足");
        }
        MbrMember m = memberMapper.selectById(memberId);
        writeLog(memberId, AccountType.POINTS, BigDecimal.valueOf(delta), BigDecimal.valueOf(m.getPoints()),
                bizType, bizNo, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGrowth(Long memberId, int delta, String bizType, String bizNo, String remark) {
        if (delta <= 0) {
            // 本方法只负责增加；退款回收走 reclaimReward（扣到 0 为止、不降级）
            return;
        }
        if (memberMapper.addGrowth(memberId, delta) == 0) {
            throw new BusinessException("会员不存在");
        }
        MbrMember m = memberMapper.selectById(memberId);
        writeLog(memberId, AccountType.GROWTH, BigDecimal.valueOf(delta), BigDecimal.valueOf(m.getGrowth()),
                bizType, bizNo, remark);
        upgradeIfNeeded(m);
    }

    /**
     * 先 SELECT ... FOR UPDATE 锁住会员行读出当前积分 / 成长值，按 min(计划, 当前) 扣减，
     * 这样「扣到 0」与写流水的 change / after 值在并发下也一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reclaimReward(Long memberId, int points, int growth, String bizType, String bizNo, String remark) {
        if (points <= 0 && growth <= 0) {
            return;
        }
        MbrMember m = memberMapper.selectAccountForUpdate(memberId);
        if (m == null) {
            throw new BusinessException("会员不存在");
        }
        int curPoints = m.getPoints() == null ? 0 : m.getPoints();
        int curGrowth = m.getGrowth() == null ? 0 : m.getGrowth();
        int p = Math.min(Math.max(points, 0), curPoints);
        int g = Math.min(Math.max(growth, 0), curGrowth);
        if (p == 0 && g == 0) {
            log.info("会员 {} 积分 / 成长值已为 0，无需回收 bizNo={}", memberId, bizNo);
            return;
        }
        memberMapper.deductReward(memberId, p, g);
        if (p > 0) {
            writeLog(memberId, AccountType.POINTS, BigDecimal.valueOf(-p), BigDecimal.valueOf(curPoints - p),
                    bizType, bizNo, p < points ? remark + "（积分不足，按剩余 " + p + " 扣回）" : remark);
        }
        if (g > 0) {
            writeLog(memberId, AccountType.GROWTH, BigDecimal.valueOf(-g), BigDecimal.valueOf(curGrowth - g),
                    bizType, bizNo, remark);
        }
    }

    @Override
    public PageVO<AccountLogVO> pageLogs(AccountLogQuery q) {
        LambdaQueryWrapper<MbrAccountLog> w = new LambdaQueryWrapper<MbrAccountLog>()
                .eq(q.getMemberId() != null, MbrAccountLog::getMemberId, q.getMemberId())
                .eq(StringUtils.hasText(q.getAccountType()), MbrAccountLog::getAccountType, q.getAccountType())
                .orderByDesc(MbrAccountLog::getCreatedAt).orderByDesc(MbrAccountLog::getId);
        return PageVO.of(accountLogMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w),
                MemberAccountServiceImpl::toVO);
    }

    /** 只升不降：等级门槛下调或会员被手工降级后，不会因为一次加成长值被「升」回低等级 */
    void upgradeIfNeeded(MbrMember m) {
        MbrLevel target = levelService.matchLevel(m.getGrowth());
        if (target == null || target.getId().equals(m.getLevelId())) {
            return;
        }
        MbrLevel current = levelService.get(m.getLevelId());
        if (current != null && target.getMinGrowth() <= current.getMinGrowth()) {
            return;
        }
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .set(MbrMember::getLevelId, target.getId())
                .eq(MbrMember::getId, m.getId()));
        log.info("会员 {} 升级为 {}", m.getId(), target.getName());
    }

    private BusinessException failure(Long memberId, boolean deducting, String insufficientMsg) {
        Long exists = memberMapper.selectCount(new LambdaQueryWrapper<MbrMember>().eq(MbrMember::getId, memberId));
        if (exists == null || exists == 0) {
            return new BusinessException("会员不存在");
        }
        return new BusinessException(deducting ? insufficientMsg : "账户变更失败，请重试");
    }

    private void writeLog(Long memberId, AccountType type, BigDecimal change, BigDecimal after,
                          String bizType, String bizNo, String remark) {
        MbrAccountLog l = new MbrAccountLog();
        l.setMemberId(memberId);
        l.setAccountType(type.name());
        l.setChangeValue(change);
        l.setAfterValue(after);
        l.setBizType(Objects.requireNonNull(bizType, "bizType"));
        l.setBizNo(truncate(bizNo, BIZ_NO_MAX));
        l.setRemark(truncate(remark, REMARK_MAX));
        accountLogMapper.insert(l);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    static AccountLogVO toVO(MbrAccountLog l) {
        AccountLogVO vo = new AccountLogVO();
        vo.setId(l.getId());
        vo.setMemberId(l.getMemberId());
        vo.setAccountType(l.getAccountType());
        vo.setChangeValue(l.getChangeValue());
        vo.setAfterValue(l.getAfterValue());
        vo.setBizType(l.getBizType());
        vo.setBizNo(l.getBizNo());
        vo.setRemark(l.getRemark());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
