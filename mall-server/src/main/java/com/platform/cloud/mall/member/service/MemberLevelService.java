package com.platform.cloud.mall.member.service;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.dto.LevelRequest;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.vo.LevelVO;

import java.util.List;

/**
 * 会员等级。
 */
public interface MemberLevelService {

    PageVO<LevelVO> page(PageQuery query);

    /** 全部等级，按成长值门槛升序 */
    List<LevelVO> list();

    LevelVO detail(Long id);

    Long create(LevelRequest req);

    void update(Long id, LevelRequest req);

    void delete(Long id);

    /** 新会员的默认等级：门槛最低的等级；没有配置等级时返回 null */
    MbrLevel defaultLevel();

    /** 成长值对应的等级：门槛不超过 growth 的最高等级 */
    MbrLevel matchLevel(int growth);

    /** 按 ID 取等级，不存在返回 null */
    MbrLevel get(Long id);
}
