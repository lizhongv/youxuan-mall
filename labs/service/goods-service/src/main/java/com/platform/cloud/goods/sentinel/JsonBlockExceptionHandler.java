package com.platform.cloud.goods.sentinel;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 把 Sentinel 限流/熔断异常转成 R 格式响应，复用现有错误码体系。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonBlockExceptionHandler implements BlockExceptionHandler {

    private final ObjectMapper mapper;

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String resource, BlockException ex)
            throws IOException {
        log.warn("Sentinel blocked: resource={}, type={}", resource, ex.getClass().getSimpleName());
        resp.setStatus(429); // Too Many Requests（Jakarta Servlet 6 没有常量）
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");

        String msg;
        if (ex instanceof FlowException)            msg = "请求过于频繁，已限流: " + resource;
        else if (ex instanceof DegradeException)    msg = "服务降级中（熔断）: " + resource;
        else if (ex instanceof ParamFlowException)  msg = "热点参数限流: " + resource;
        else                                         msg = "Sentinel 拦截: " + ex.getClass().getSimpleName();

        R<Void> body = R.fail(ResultCode.DUPLICATE_REQUEST, msg);
        resp.getWriter().write(mapper.writeValueAsString(body));
    }
}
