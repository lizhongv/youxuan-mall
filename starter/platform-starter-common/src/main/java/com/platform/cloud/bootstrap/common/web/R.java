package com.platform.cloud.bootstrap.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应封装。所有 controller 必须返回 R&lt;T&gt;。
 */
@Data
@Schema(description = "统一响应体")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务码，0 表示成功")
    private int code;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "数据负载")
    private T data;

    @Schema(description = "服务端时间戳（毫秒）")
    private long ts;

    @Schema(description = "链路追踪 ID")
    private String traceId;

    public static <T> R<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> fail(ResultCode rc) {
        return build(rc.getCode(), rc.getMsg(), null);
    }

    public static <T> R<T> fail(ResultCode rc, String detail) {
        return build(rc.getCode(), detail, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    /** 失败但需要带结构化提示给前端，如登录失败时的 {needCaptcha: true} */
    public static <T> R<T> fail(int code, String msg, T data) {
        return build(code, msg, data);
    }

    private static <T> R<T> build(int code, String msg, T data) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        r.ts = System.currentTimeMillis();
        r.traceId = MDC.get("traceId");
        return r;
    }
}
