package com.platform.cloud.bootstrap.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        R<Void> body = R.fail(ResultCode.FORBIDDEN);
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
