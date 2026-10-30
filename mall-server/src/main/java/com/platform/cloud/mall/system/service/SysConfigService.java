package com.platform.cloud.mall.system.service;

import com.platform.cloud.mall.system.dto.ConfigBatchRequest;
import com.platform.cloud.mall.system.vo.ConfigVO;

import java.util.List;

/**
 * 系统参数后台维护。读取请用 {@link com.platform.cloud.mall.system.api.ConfigApi}（带缓存）。
 */
public interface SysConfigService {

    List<ConfigVO> list();

    void batchUpdate(List<ConfigBatchRequest.Item> items);
}
