package com.platform.cloud.mall.system.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.mall.common.config.JacksonConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 操作日志筛选 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQuery extends PageQuery {

    private String username;
    private String module;
    private Integer success;

    @DateTimeFormat(pattern = JacksonConfig.DATE_TIME_PATTERN)
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = JacksonConfig.DATE_TIME_PATTERN)
    private LocalDateTime endTime;
}
