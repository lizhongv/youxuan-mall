package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.common.util.MaskUtils;
import com.platform.cloud.mall.system.dto.LoginLogQuery;
import com.platform.cloud.mall.system.entity.SysLoginLog;
import com.platform.cloud.mall.system.mapper.SysLoginLogMapper;
import com.platform.cloud.mall.system.service.SysLoginLogService;
import com.platform.cloud.mall.system.support.AsyncLogWriter;
import com.platform.cloud.mall.system.support.RequestUtils;
import com.platform.cloud.mall.system.vo.LoginLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 登录日志。
 */
@Slf4j
@Service
public class SysLoginLogServiceImpl implements SysLoginLogService {

    /** 与表列宽一致 */
    private static final int USERNAME_MAX = 64;
    private static final int MESSAGE_MAX = 255;

    private final SysLoginLogMapper loginLogMapper;
    private final AsyncLogWriter logWriter;

    public SysLoginLogServiceImpl(SysLoginLogMapper loginLogMapper,
                                  AsyncLogWriter logWriter) {
        this.loginLogMapper = loginLogMapper;
        this.logWriter = logWriter;
    }

    @Override
    public PageVO<LoginLogVO> page(LoginLogQuery q) {
        LambdaQueryWrapper<SysLoginLog> w = new LambdaQueryWrapper<SysLoginLog>()
                .eq(StringUtils.hasText(q.getUserType()), SysLoginLog::getUserType, q.getUserType())
                .like(StringUtils.hasText(q.getUsername()), SysLoginLog::getUsername, q.getUsername())
                .eq(q.getSuccess() != null, SysLoginLog::getSuccess, q.getSuccess())
                .orderByDesc(SysLoginLog::getCreatedAt);
        return PageVO.of(loginLogMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w),
                SysLoginLogServiceImpl::toVO);
    }

    @Override
    public void record(UserType userType, Long userId, String username, boolean success, String message) {
        SysLoginLog l = new SysLoginLog();
        l.setUserType(userType.name());
        l.setUserId(userId);
        l.setUsername(RequestUtils.truncate(username, USERNAME_MAX));
        // IP 与 UA 只能在请求线程读取
        l.setIp(RequestUtils.clientIp());
        l.setUserAgent(RequestUtils.userAgent());
        l.setSuccess(success ? 1 : 0);
        l.setMessage(RequestUtils.truncate(message, MESSAGE_MAX));
        l.setCreatedBy(userId);
        l.setUpdatedBy(userId);
        logWriter.submit(() -> {
            try {
                loginLogMapper.insert(l);
            } catch (Exception e) {
                log.warn("写登录日志失败 username={}: {}", username, e.getMessage());
            }
        });
    }

    private static LoginLogVO toVO(SysLoginLog l) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(l.getId());
        vo.setUserType(l.getUserType());
        // 会员的登录名是手机号，列表里脱敏展示
        vo.setUsername(UserType.MEMBER.name().equals(l.getUserType())
                ? MaskUtils.mobile(l.getUsername()) : l.getUsername());
        vo.setIp(l.getIp());
        vo.setUserAgent(l.getUserAgent());
        vo.setSuccess(l.getSuccess());
        vo.setMessage(l.getMessage());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
