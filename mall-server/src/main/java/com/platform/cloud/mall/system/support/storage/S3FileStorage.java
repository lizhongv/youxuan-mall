package com.platform.cloud.mall.system.support.storage;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.support.FileStorage;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * S3 协议对象存储，一套代码兼容阿里云 OSS、腾讯云 COS、MinIO、七牛 Kodo。
 * <p>
 * 兼容性要点（都是第三方 S3 实现与 AWS 的差异）：
 * <ul>
 *   <li>OSS 只支持 virtual-hosted 风格（bucket.endpoint），MinIO 默认用 path 风格，由 {@code storage.s3.path_style} 控制；</li>
 *   <li>SDK 2.30 起默认给上传加 CRC 校验和并使用 aws-chunked 编码，OSS / COS / 旧版 MinIO 会拒绝，
 *       因此把校验和设为 WHEN_REQUIRED 并关闭 chunked 编码；</li>
 *   <li>不设置对象 ACL：各家对 x-amz-acl 支持不一，公开读统一通过桶策略或 CDN 回源授权实现。</li>
 * </ul>
 */
@Slf4j
public class S3FileStorage implements FileStorage, AutoCloseable {

    /** 文件名是 UUID，内容永不变，CDN 与浏览器可以长期缓存 */
    static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    /** 上传 5MB 图片在慢速出口带宽下也足够 */
    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(30);

    /** 按扩展名给 Content-Type：桶里对象没有正确类型时浏览器会下载而不是显示图片 */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp",
            "txt", "text/plain; charset=utf-8",
            "mp4", "video/mp4",
            "pdf", "application/pdf");

    private final S3Settings settings;
    private final S3Client client;
    private final String baseUrl;

    S3FileStorage(S3Settings settings, S3Client client) {
        this.settings = settings;
        this.client = client;
        this.baseUrl = resolveBaseUrl(settings);
    }

    /** 按配置构建客户端；只用同步的 url-connection-client，不引入 netty */
    public static S3FileStorage create(S3Settings s) {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(s.endpoint()))
                .region(Region.of(s.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s.accessKey(), s.secretKey())))
                .forcePathStyle(s.pathStyle())
                .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.builder()
                        .connectionTimeout(CONNECT_TIMEOUT)
                        .socketTimeout(SOCKET_TIMEOUT)
                        .build())
                .build();
        return new S3FileStorage(s, client);
    }

    @Override
    public void store(String relativePath, InputStream in) throws IOException {
        String key = objectKey(relativePath);
        // 上游已限制单文件大小（mall.upload.max-size，默认 5MB），整块读入内存可以拿到准确的 Content-Length，
        // 这样不需要 chunked 编码，对各家 S3 兼容实现最稳妥
        byte[] bytes = in.readAllBytes();
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(settings.bucket())
                .key(key)
                .contentType(contentType(key))
                .contentLength((long) bytes.length)
                .cacheControl(CACHE_CONTROL)
                .build();
        try {
            client.putObject(req, RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            String code = e.awsErrorDetails() == null ? String.valueOf(e.statusCode()) : e.awsErrorDetails().errorCode();
            log.error("对象存储上传失败 bucket={} key={} status={} code={}", settings.bucket(), key, e.statusCode(), code);
            throw new IOException("对象存储上传失败：" + code, e);
        } catch (SdkException e) {
            log.error("对象存储上传失败 bucket={} key={} msg={}", settings.bucket(), key, e.getMessage());
            throw new IOException("对象存储上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String url(String relativePath) {
        return baseUrl + "/" + objectKey(relativePath);
    }

    /** 删除对象，后台「测试连接」用来清理探测文件 */
    public void delete(String relativePath) {
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(settings.bucket())
                .key(objectKey(relativePath))
                .build());
    }

    public S3Settings settings() {
        return settings;
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * 对象键 = 前缀 + 相对路径。相对路径本由服务端生成，这里仍拒绝 ../、绝对路径和反斜杠作为纵深防御，
     * 与本地存储的目录穿越检查保持一致。
     */
    String objectKey(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.startsWith("/")
                || relativePath.contains("\\") || relativePath.contains(":")) {
            throw new BusinessException("非法的文件路径");
        }
        for (String seg : relativePath.split("/")) {
            if (seg.isEmpty() || seg.equals(".") || seg.equals("..")) {
                throw new BusinessException("非法的文件路径");
            }
        }
        return settings.pathPrefix().isEmpty() ? relativePath : settings.pathPrefix() + "/" + relativePath;
    }

    static String contentType(String key) {
        int dot = key.lastIndexOf('.');
        String ext = dot < 0 ? "" : key.substring(dot + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    /**
     * 访问域名：优先用配置的 CDN / 自定义域名；否则按访问风格推导桶域名
     * （virtual-hosted：https://bucket.endpoint-host，path：https://endpoint/bucket）。
     */
    static String resolveBaseUrl(S3Settings s) {
        if (!s.publicBaseUrl().isEmpty()) {
            return s.publicBaseUrl();
        }
        if (s.pathStyle()) {
            return s.endpoint() + "/" + s.bucket();
        }
        URI uri = URI.create(s.endpoint());
        String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        return scheme + "://" + s.bucket() + "." + uri.getHost() + port;
    }
}
