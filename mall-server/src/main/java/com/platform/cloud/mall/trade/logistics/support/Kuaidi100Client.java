package com.platform.cloud.mall.trade.logistics.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.mall.trade.logistics.vo.LogisticsTraceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 快递100 实时快递查询 API（见快递100 企业版 API 文档「实时快递查询接口」）。
 * <p>
 * 请求：POST application/x-www-form-urlencoded，字段 customer、sign、param（JSON 字符串）；
 * 签名 sign = MD5(param + key + customer) 转大写。
 * 直接用 JDK HttpClient 调用：接口只有一个，引入官方 SDK 反而带来额外依赖。
 */
@Slf4j
@Component
public class Kuaidi100Client {

    static final String QUERY_URL = "https://poll.kuaidi100.com/poll/query.do";
    /** 快递100 返回「查询无结果」的错误码，常见于刚发货尚未揽收 */
    public static final String NO_RESULT = "500";
    /** 连接 3 秒、整体 5 秒：物流查询在订单详情页同步调用，宁可快速失败显示提示 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;

    @Autowired
    public Kuaidi100Client() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
    }

    Kuaidi100Client(HttpClient http) {
        this.http = http;
    }

    /**
     * 查询轨迹。网络异常、签名错误、无结果都以 {@link Result#ok()} = false 返回，不抛异常，
     * 由上层决定提示语，保证物流查询失败不影响订单详情。
     *
     * @param phone 收件人手机号后四位，仅顺丰需要，其他传 null
     */
    public Result query(String customer, String key, String com, String num, String phone) {
        String param = buildParam(com, num, phone);
        String form = "customer=" + encode(customer) + "&sign=" + encode(sign(param, key, customer))
                + "&param=" + encode(param);
        HttpRequest req = HttpRequest.newBuilder(URI.create(QUERY_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                log.error("快递100 HTTP 异常 status={} com={} num={}", resp.statusCode(), com, num);
                return Result.error("HTTP_" + resp.statusCode(), "物流服务暂不可用");
            }
            return parse(resp.body());
        } catch (IOException e) {
            log.error("快递100 请求失败 com={} num={} msg={}", com, num, e.getMessage());
            return Result.error("NETWORK", "物流服务连接失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("INTERRUPTED", "物流查询被中断");
        }
    }

    /** param 字段：resultv2=1 让快递100 返回行政区域解析（areaName），order=desc 最新轨迹在前 */
    static String buildParam(String com, String num, String phone) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("com", com);
        p.put("num", num);
        if (phone != null && !phone.isBlank()) {
            p.put("phone", phone);
        }
        p.put("resultv2", "1");
        p.put("show", "0");
        p.put("order", "desc");
        try {
            return JSON.writeValueAsString(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** sign = MD5(param + key + customer)，32 位大写十六进制 */
    static String sign(String param, String key, String customer) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest((param + key + customer).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 MD5", e);
        }
    }

    /**
     * 解析响应。成功：{"message":"ok","status":"200","state":"3","ischeck":"1","data":[{time,context,location,areaName}]}；
     * 失败：{"result":false,"returnCode":"500","message":"查询无结果，请隔段时间再查"}。
     */
    static Result parse(String body) {
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (IOException e) {
            log.error("快递100 响应不是合法 JSON");
            return Result.error("BAD_RESPONSE", "物流服务返回异常");
        }
        if (root == null || !"200".equals(root.path("status").asText())) {
            String code = root == null ? "" : root.path("returnCode").asText("");
            String msg = root == null ? "" : root.path("message").asText("");
            if (!NO_RESULT.equals(code)) {
                // 400 参数错误、503 签名错误、601 key 过期或余额不足等都需要运维处理，记 error
                log.error("快递100 查询失败 returnCode={} message={}", code, msg);
            }
            return Result.error(code.isEmpty() ? "UNKNOWN" : code, msg);
        }
        List<LogisticsTraceVO> traces = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            String location = n.path("location").asText("");
            if (location.isBlank()) {
                location = n.path("areaName").asText("");
            }
            traces.add(new LogisticsTraceVO(n.path("time").asText(""), n.path("context").asText(""), location));
        }
        // 时间格式统一为 yyyy-MM-dd HH:mm:ss，字符串倒序即时间倒序；不依赖对方一定按 order=desc 返回
        traces.sort(Comparator.comparing(LogisticsTraceVO::getTime, Comparator.nullsFirst(String::compareTo)).reversed());
        LogisticsState state = LogisticsState.fromKuaidi100(root.path("state").asText(""));
        boolean signed = "1".equals(root.path("ischeck").asText()) || state == LogisticsState.SIGNED;
        return new Result(true, state, signed, traces, null, null);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    /**
     * 查询结果。
     *
     * @param signed    是否已签收（决定缓存时长）
     * @param errorCode 失败时快递100 的 returnCode 或本地错误类型
     */
    public record Result(boolean ok, LogisticsState state, boolean signed, List<LogisticsTraceVO> traces,
                         String errorCode, String errorMessage) {

        static Result error(String code, String message) {
            return new Result(false, LogisticsState.NONE, false, List.of(), code, message);
        }
    }
}
