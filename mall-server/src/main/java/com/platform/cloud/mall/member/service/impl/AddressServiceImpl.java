package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.dto.AddressRequest;
import com.platform.cloud.mall.member.entity.MbrAddress;
import com.platform.cloud.mall.member.mapper.MbrAddressMapper;
import com.platform.cloud.mall.member.service.AddressService;
import com.platform.cloud.mall.member.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址。默认地址每个会员至多一个：设默认时先把其他地址清掉再设置，在同一事务内完成。
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    /** 每个会员最多 20 个地址，与主流电商一致，避免结算页地址列表过长 */
    static final int MAX_ADDRESSES = 20;

    private static final int YES = 1;
    private static final int NO = 0;

    private final MbrAddressMapper addressMapper;

    @Override
    public List<AddressVO> list(Long memberId) {
        return addressMapper.selectList(new LambdaQueryWrapper<MbrAddress>()
                        .eq(MbrAddress::getMemberId, memberId)
                        .orderByDesc(MbrAddress::getIsDefault)
                        .orderByDesc(MbrAddress::getUpdatedAt))
                .stream().map(AddressServiceImpl::toVO).toList();
    }

    @Override
    public AddressVO detail(Long memberId, Long id) {
        return toVO(requireOwned(memberId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long memberId, AddressRequest req) {
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<MbrAddress>().eq(MbrAddress::getMemberId, memberId));
        long existing = count == null ? 0 : count;
        if (existing >= MAX_ADDRESSES) {
            throw new BusinessException("最多保存 " + MAX_ADDRESSES + " 个收货地址");
        }
        // 第一个地址自动成为默认地址，省去用户再手动设置
        boolean asDefault = existing == 0 || Integer.valueOf(YES).equals(req.getIsDefault());
        if (asDefault) {
            clearDefault(memberId);
        }
        MbrAddress a = new MbrAddress();
        a.setMemberId(memberId);
        apply(a, req);
        a.setIsDefault(asDefault ? YES : NO);
        addressMapper.insert(a);
        return a.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long memberId, Long id, AddressRequest req) {
        MbrAddress old = requireOwned(memberId, id);
        boolean asDefault = req.getIsDefault() == null
                ? Integer.valueOf(YES).equals(old.getIsDefault())
                : req.getIsDefault() == YES;
        if (asDefault) {
            clearDefault(memberId);
        }
        MbrAddress a = new MbrAddress();
        apply(a, req);
        addressMapper.update(null, new LambdaUpdateWrapper<MbrAddress>()
                .set(MbrAddress::getName, a.getName())
                .set(MbrAddress::getMobile, a.getMobile())
                .set(MbrAddress::getProvince, a.getProvince())
                .set(MbrAddress::getCity, a.getCity())
                .set(MbrAddress::getDistrict, a.getDistrict())
                .set(MbrAddress::getDetail, a.getDetail())
                .set(MbrAddress::getIsDefault, asDefault ? YES : NO)
                .eq(MbrAddress::getId, id)
                .eq(MbrAddress::getMemberId, memberId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long id) {
        MbrAddress old = requireOwned(memberId, id);
        addressMapper.delete(new LambdaQueryWrapper<MbrAddress>()
                .eq(MbrAddress::getId, id)
                .eq(MbrAddress::getMemberId, memberId));
        if (Integer.valueOf(YES).equals(old.getIsDefault())) {
            // 删掉默认地址后把最近修改的地址顶上，结算页仍有默认可选
            List<MbrAddress> rest = addressMapper.selectList(new LambdaQueryWrapper<MbrAddress>()
                    .eq(MbrAddress::getMemberId, memberId)
                    .orderByDesc(MbrAddress::getUpdatedAt));
            if (!rest.isEmpty()) {
                markDefault(memberId, rest.get(0).getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long memberId, Long id) {
        requireOwned(memberId, id);
        clearDefault(memberId);
        markDefault(memberId, id);
    }

    @Override
    public AddressVO getDefault(Long memberId) {
        List<AddressVO> all = list(memberId);
        return all.isEmpty() ? null : all.get(0);
    }

    private void clearDefault(Long memberId) {
        addressMapper.update(null, new LambdaUpdateWrapper<MbrAddress>()
                .set(MbrAddress::getIsDefault, NO)
                .eq(MbrAddress::getMemberId, memberId)
                .eq(MbrAddress::getIsDefault, YES));
    }

    private void markDefault(Long memberId, Long id) {
        addressMapper.update(null, new LambdaUpdateWrapper<MbrAddress>()
                .set(MbrAddress::getIsDefault, YES)
                .eq(MbrAddress::getId, id)
                .eq(MbrAddress::getMemberId, memberId));
    }

    private MbrAddress requireOwned(Long memberId, Long id) {
        MbrAddress a = addressMapper.selectOne(new LambdaQueryWrapper<MbrAddress>()
                .eq(MbrAddress::getId, id)
                .eq(MbrAddress::getMemberId, memberId));
        if (a == null) {
            throw new BusinessException("收货地址不存在");
        }
        return a;
    }

    private static void apply(MbrAddress a, AddressRequest req) {
        a.setName(req.getName().trim());
        a.setMobile(req.getMobile().trim());
        a.setProvince(req.getProvince().trim());
        a.setCity(req.getCity().trim());
        a.setDistrict(req.getDistrict().trim());
        a.setDetail(req.getDetail().trim());
    }

    static AddressVO toVO(MbrAddress a) {
        AddressVO vo = new AddressVO();
        vo.setId(a.getId());
        vo.setName(a.getName());
        vo.setMobile(a.getMobile());
        vo.setProvince(a.getProvince());
        vo.setCity(a.getCity());
        vo.setDistrict(a.getDistrict());
        vo.setDetail(a.getDetail());
        vo.setIsDefault(a.getIsDefault());
        return vo;
    }
}
