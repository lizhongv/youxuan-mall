package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.dto.AddressRequest;
import com.platform.cloud.mall.member.vo.AddressVO;

import java.util.List;

/**
 * 收货地址。所有操作都带会员 ID 条件，防止越权访问他人地址。
 */
public interface AddressService {

    /** 默认地址排第一，其余按最近修改 */
    List<AddressVO> list(Long memberId);

    AddressVO detail(Long memberId, Long id);

    Long create(Long memberId, AddressRequest req);

    void update(Long memberId, Long id, AddressRequest req);

    void delete(Long memberId, Long id);

    void setDefault(Long memberId, Long id);

    /** 默认地址；没有设置默认时返回最近修改的地址，一个都没有返回 null */
    AddressVO getDefault(Long memberId);
}
