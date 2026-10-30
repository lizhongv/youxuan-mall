package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.system.dto.OperLogQuery;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.entity.SysOperLog;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import com.platform.cloud.mall.system.mapper.SysOperLogMapper;
import com.platform.cloud.mall.system.service.SysOperLogService;
import com.platform.cloud.mall.system.support.AsyncLogWriter;
import com.platform.cloud.mall.system.vo.OperLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志。
 */
@Slf4j
@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    private final SysOperLogMapper operLogMapper;
    private final SysAdminMapper adminMapper;
    private final AsyncLogWriter logWriter;

    public SysOperLogServiceImpl(SysOperLogMapper operLogMapper, SysAdminMapper adminMapper,
                                 AsyncLogWriter logWriter) {
        this.operLogMapper = operLogMapper;
        this.adminMapper = adminMapper;
        this.logWriter = logWriter;
    }

    @Override
    public PageVO<OperLogVO> page(OperLogQuery q) {
        LambdaQueryWrapper<SysOperLog> w = new LambdaQueryWrapper<SysOperLog>()
                .like(StringUtils.hasText(q.getUsername()), SysOperLog::getUsername, q.getUsername())
                .eq(StringUtils.hasText(q.getModule()), SysOperLog::getModule, q.getModule())
                .eq(q.getSuccess() != null, SysOperLog::getSuccess, q.getSuccess())
                .ge(q.getStartTime() != null, SysOperLog::getCreatedAt, q.getStartTime())
                .le(q.getEndTime() != null, SysOperLog::getCreatedAt, q.getEndTime())
                .orderByDesc(SysOperLog::getCreatedAt);
        return PageVO.of(operLogMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w),
                SysOperLogServiceImpl::toVO);
    }

    @Override
    public void recordAsync(SysOperLog operLog) {
        logWriter.submit(() -> {
            try {
                // 令牌里只有管理员 ID，用户名在异步线程里补齐，不占用业务请求的时间
                if (!StringUtils.hasText(operLog.getUsername()) && operLog.getAdminId() != null) {
                    SysAdmin admin = adminMapper.selectById(operLog.getAdminId());
                    operLog.setUsername(admin == null ? "" : admin.getUsername());
                }
                if (operLog.getUsername() == null) {
                    operLog.setUsername("");
                }
                operLog.setCreatedBy(operLog.getAdminId());
                operLog.setUpdatedBy(operLog.getAdminId());
                operLogMapper.insert(operLog);
            } catch (Exception e) {
                log.warn("写操作日志失败 uri={}: {}", operLog.getUri(), e.getMessage());
            }
        });
    }

    private static OperLogVO toVO(SysOperLog l) {
        OperLogVO vo = new OperLogVO();
        vo.setId(l.getId());
        vo.setUsername(l.getUsername());
        vo.setModule(l.getModule());
        vo.setAction(l.getAction());
        vo.setMethod(l.getMethod());
        vo.setUri(l.getUri());
        vo.setParams(l.getParams());
        vo.setIp(l.getIp());
        vo.setSuccess(l.getSuccess());
        vo.setErrorMsg(l.getErrorMsg());
        vo.setCostMs(l.getCostMs());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
