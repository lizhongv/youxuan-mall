package com.platform.cloud.bootstrap.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        R<Void> body = R.fail(ResultCode.UNAUTHORIZED);
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
