package com.platform.cloud.mall.member.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 用 JDK HttpClient 直接调公众号接口。只用到「code 换 openid」一个接口，没必要引入整套 WxJava。
 * <p>
 * 请求 URL 里带 AppSecret，所以任何情况下都不打印 URL，只记录微信返回的 errcode / errmsg。
 */
@Slf4j
@Component
public class HttpWechatMpClient implements WechatMpClient {

    static final String OAUTH_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    /** 授权在用户等待页面中同步进行，5 秒拿不到结果宁可让用户重试 */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public HttpWechatMpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuthResult exchangeCode(String appId, String appSecret, String code) {
        String url = OAUTH_URL + "?appid=" + enc(appId) + "&secret=" + enc(appSecret) + "&code=" + enc(code)
                + "&grant_type=authorization_code";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
        String body;
        try {
            body = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        } catch (IOException e) {
            log.warn("调用微信网页授权接口失败：{}", e.getMessage());
            throw new BusinessException("微信授权失败，请重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("微信授权失败，请重试");
        }
        return parse(objectMapper, body);
    }

    /** 解析返回：成功含 openid，失败含 errcode（如 40029 code 无效、40163 code 已被使用） */
    static OAuthResult parse(ObjectMapper mapper, String body) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
        } catch (IOException e) {
            throw new BusinessException("微信授权失败，请重试");
        }
        String openid = node.path("openid").asText("");
        if (node.path("errcode").asInt(0) != 0 || openid.isEmpty()) {
            log.warn("微信网页授权失败 errcode={} errmsg={}", node.path("errcode").asText(),
                    node.path("errmsg").asText());
            throw new BusinessException("微信授权已失效，请重新进入");
        }
        return new OAuthResult(openid, node.path("unionid").asText(""));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
