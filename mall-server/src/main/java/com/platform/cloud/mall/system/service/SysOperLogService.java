package com.platform.cloud.mall.system.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.system.dto.OperLogQuery;
import com.platform.cloud.mall.system.entity.SysOperLog;
import com.platform.cloud.mall.system.vo.OperLogVO;

/**
 * 操作日志。
 */
public interface SysOperLogService {

    PageVO<OperLogVO> page(OperLogQuery query);

    /** 异步落库，不拖慢业务请求；写日志失败只打告警，不影响业务结果 */
    void recordAsync(SysOperLog log);
}
