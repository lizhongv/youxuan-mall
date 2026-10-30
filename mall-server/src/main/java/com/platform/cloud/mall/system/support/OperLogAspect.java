package com.platform.cloud.mall.system.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.system.entity.SysOperLog;
import com.platform.cloud.mall.system.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 管理端操作日志切面：拦截 /admin/** 的 POST / PUT / DELETE。
 * <p>
 * 切在所有领域的 controller.admin 包上，新增后台接口无需额外注解即可被审计；
 * 模块名取类上的 {@code @Tag(name)}，动作取方法上的 {@code @Operation(summary)}，与接口文档保持一致。
 * 登录、刷新令牌不记：它们有专门的登录日志，且请求体里是明文密码。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    private static final Set<String> EXCLUDED_URIS = Set.of("/admin/auth/login", "/admin/auth/refresh");
    private static final int ERROR_MSG_MAX = 500;
    private static final int URI_MAX = 255;

    private final SysOperLogService operLogService;
    private final ObjectMapper objectMapper;

    @Around("within(com.platform.cloud.mall..controller.admin..*)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest req = RequestUtils.currentRequest();
        if (req == null || !shouldLog(req.getMethod(), req.getRequestURI())) {
            return pjp.proceed();
        }
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            record(pjp, req, start, error);
        }
    }

    static boolean shouldLog(String method, String uri) {
        return method != null && WRITE_METHODS.contains(method.toUpperCase())
                && uri != null && uri.startsWith("/admin/") && !EXCLUDED_URIS.contains(uri);
    }

    private void record(ProceedingJoinPoint pjp, HttpServletRequest req, long start, Throwable error) {
        Class<?> targetClass = AopUtils.getTargetClass(pjp.getTarget());
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Tag tag = AnnotatedElementUtils.findMergedAnnotation(targetClass, Tag.class);
        Operation op = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);

        SysOperLog l = new SysOperLog();
        l.setAdminId(SecurityUtils.currentUserIdOrNull());
        l.setModule(tag == null ? targetClass.getSimpleName() : tag.name());
        l.setAction(op == null ? method.getName() : RequestUtils.truncate(op.summary(), 128));
        l.setMethod(req.getMethod());
        l.setUri(RequestUtils.truncate(req.getRequestURI(), URI_MAX));
        l.setParams(OperLogParams.serialize(objectMapper, pjp.getArgs()));
        l.setIp(RequestUtils.clientIp());
        l.setSuccess(error == null ? 1 : 0);
        l.setErrorMsg(error == null ? "" : RequestUtils.truncate(errorText(error), ERROR_MSG_MAX));
        l.setCostMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
        operLogService.recordAsync(l);
    }

    private static String errorText(Throwable t) {
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
