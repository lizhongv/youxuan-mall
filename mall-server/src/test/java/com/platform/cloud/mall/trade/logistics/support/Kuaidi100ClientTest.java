package com.platform.cloud.mall.trade.logistics.support;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 快递100：签名算法、请求表单、响应解析（mock JDK HttpClient，不访问外网）。
 */
class Kuaidi100ClientTest {

    private static final String SIGNED_BODY = """
            {"message":"ok","nu":"SF100","ischeck":"1","com":"shunfeng","status":"200","state":"3",
             "data":[
               {"time":"2026-10-30 08:00:00","context":"已揽收","location":"","areaName":"广东,深圳市"},
               {"time":"2026-10-31 18:30:00","context":"已签收，签收人：本人","location":"上海市"},
               {"time":"2026-10-31 09:10:00","context":"派送中","location":"上海市"}
             ]}
            """;

    @Test
    void signIsUpperCaseMd5OfParamKeyCustomer() {
        // MD5("abc") 是公开的测试向量
        assertThat(Kuaidi100Client.sign("a", "b", "c")).isEqualTo("900150983CD24FB0D6963F7D28E17F72");
    }

    @Test
    void paramIncludesPhoneOnlyWhenGiven() {
        assertThat(Kuaidi100Client.buildParam("shunfeng", "SF1", "5678"))
                .isEqualTo("{\"com\":\"shunfeng\",\"num\":\"SF1\",\"phone\":\"5678\",\"resultv2\":\"1\","
                        + "\"show\":\"0\",\"order\":\"desc\"}");
        assertThat(Kuaidi100Client.buildParam("zhongtong", "ZT1", null)).doesNotContain("phone");
    }

    @Test
    void parseSortsTracesDescAndFallsBackToAreaName() {
        Kuaidi100Client.Result r = Kuaidi100Client.parse(SIGNED_BODY);
        assertThat(r.ok()).isTrue();
        assertThat(r.state()).isEqualTo(LogisticsState.SIGNED);
        assertThat(r.signed()).isTrue();
        assertThat(r.traces()).extracting("time")
                .containsExactly("2026-10-31 18:30:00", "2026-10-31 09:10:00", "2026-10-30 08:00:00");
        assertThat(r.traces().get(2).getLocation()).isEqualTo("广东,深圳市");
        assertThat(r.traces().get(0).getContext()).startsWith("已签收");
    }

    @Test
    void parseErrorKeepsReturnCode() {
        Kuaidi100Client.Result r = Kuaidi100Client.parse(
                "{\"result\":false,\"returnCode\":\"500\",\"message\":\"查询无结果，请隔段时间再查\"}");
        assertThat(r.ok()).isFalse();
        assertThat(r.errorCode()).isEqualTo(Kuaidi100Client.NO_RESULT);
        assertThat(r.traces()).isEmpty();

        assertThat(Kuaidi100Client.parse("not json").errorCode()).isEqualTo("BAD_RESPONSE");
        assertThat(Kuaidi100Client.parse("{\"status\":\"200\",\"state\":\"0\",\"ischeck\":\"0\",\"data\":[]}").signed())
                .isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryPostsSignedFormToOfficialEndpoint() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn(SIGNED_BODY);
        doReturn(resp).when(http).send(any(), any());

        Kuaidi100Client.Result r = new Kuaidi100Client(http).query("CUST", "KEY", "shunfeng", "SF100", "5678");
        assertThat(r.ok()).isTrue();

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any());
        HttpRequest req = captor.getValue();
        assertThat(req.uri().toString()).isEqualTo(Kuaidi100Client.QUERY_URL);
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.headers().firstValue("Content-Type")).hasValueSatisfying(
                v -> assertThat(v).startsWith("application/x-www-form-urlencoded"));

        Map<String, String> form = parseForm(readBody(req));
        String expectedParam = Kuaidi100Client.buildParam("shunfeng", "SF100", "5678");
        assertThat(form).containsEntry("customer", "CUST")
                .containsEntry("param", expectedParam)
                .containsEntry("sign", Kuaidi100Client.sign(expectedParam, "KEY", "CUST"));
        // 授权 key 只参与签名，绝不明文出现在请求里
        assertThat(readBody(req)).doesNotContain("KEY");
    }

    @Test
    @SuppressWarnings("unchecked")
    void networkAndHttpErrorsReturnFailureInsteadOfThrowing() throws Exception {
        HttpClient http = mock(HttpClient.class);
        doThrow(new IOException("timeout")).when(http).send(any(), any());
        assertThat(new Kuaidi100Client(http).query("c", "k", "yunda", "1", null).errorCode()).isEqualTo("NETWORK");

        HttpClient http2 = mock(HttpClient.class);
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(502);
        doReturn(resp).when(http2).send(any(), any());
        assertThat(new Kuaidi100Client(http2).query("c", "k", "yunda", "1", null).ok()).isFalse();
    }

    /** 订阅 BodyPublisher 读出表单内容 */
    private static String readBody(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        req.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                sb.append(StandardCharsets.UTF_8.decode(item));
            }

            @Override
            public void onError(Throwable t) {
                throw new IllegalStateException(t);
            }

            @Override
            public void onComplete() {
            }
        });
        return sb.toString();
    }

    private static Map<String, String> parseForm(String body) {
        return Arrays.stream(body.split("&"))
                .map(kv -> kv.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> URLDecoder.decode(a[1], StandardCharsets.UTF_8)));
    }
}
