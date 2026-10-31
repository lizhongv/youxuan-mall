package com.platform.cloud.mall.system.support.storage;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S3 存储：对象键（前缀 + 路径）、Content-Type、URL 拼接（CDN / 虚拟主机 / path 风格）与错误转换。
 */
class S3FileStorageTest {

    private static S3Settings settings(String endpoint, boolean pathStyle, String publicBaseUrl, String prefix) {
        return new S3Settings(StorageProvider.S3, "S3", endpoint, "oss-cn-hangzhou", "mall-img", "ak", "sk",
                pathStyle, publicBaseUrl, S3Settings.normalizePrefix(prefix));
    }

    @Test
    void storePutsObjectUnderPrefixWithContentTypeAndLength() throws IOException {
        S3Client client = mock(S3Client.class);
        S3FileStorage storage = new S3FileStorage(
                settings("https://oss-cn-hangzhou.aliyuncs.com", false, "https://img.example.com", "/mall/"), client);

        storage.store("2026/10/31/abc.JPG", new ByteArrayInputStream(new byte[]{1, 2, 3}));

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest req = captor.getValue();
        assertThat(req.bucket()).isEqualTo("mall-img");
        assertThat(req.key()).isEqualTo("mall/2026/10/31/abc.JPG");
        assertThat(req.contentType()).isEqualTo("image/jpeg");
        assertThat(req.contentLength()).isEqualTo(3L);
        assertThat(req.cacheControl()).isEqualTo(S3FileStorage.CACHE_CONTROL);
        assertThat(storage.url("2026/10/31/abc.JPG")).isEqualTo("https://img.example.com/mall/2026/10/31/abc.JPG");
    }

    @Test
    void urlFallsBackToBucketDomainByAddressingStyle() {
        S3Client client = mock(S3Client.class);
        // OSS / COS：虚拟主机风格
        assertThat(new S3FileStorage(settings("https://oss-cn-hangzhou.aliyuncs.com/", false, "", ""), client)
                .url("a/b.png")).isEqualTo("https://mall-img.oss-cn-hangzhou.aliyuncs.com/a/b.png");
        // MinIO：path 风格，带端口
        assertThat(new S3FileStorage(settings("http://minio.local:9000", true, "", "mall"), client)
                .url("a/b.png")).isEqualTo("http://minio.local:9000/mall-img/mall/a/b.png");
        // 自定义域名末尾斜杠不会产生双斜杠（from() 已去除，这里模拟同样的结果）
        assertThat(new S3FileStorage(settings("https://cos.ap-guangzhou.myqcloud.com", false,
                "https://cdn.example.com", ""), client).url("x.webp")).isEqualTo("https://cdn.example.com/x.webp");
    }

    @Test
    void rejectsTraversalAndAbsolutePaths() {
        S3FileStorage storage = new S3FileStorage(settings("https://s3.example.com", false, "", "mall"),
                mock(S3Client.class));
        for (String bad : new String[]{"../etc/passwd", "/abs.jpg", "a/../../b.jpg", "a\\b.jpg", "C:/x.jpg", "a//b.jpg", ""}) {
            assertThatThrownBy(() -> storage.objectKey(bad)).as(bad).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void s3ErrorBecomesIoExceptionWithErrorCode() {
        S3Client client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(
                S3Exception.builder().statusCode(403)
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build()).build());
        S3FileStorage storage = new S3FileStorage(settings("https://s3.example.com", false, "", ""), client);
        assertThatThrownBy(() -> storage.store("a.png", new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("AccessDenied");
    }

    @Test
    void deleteUsesSameObjectKey() {
        S3Client client = mock(S3Client.class);
        new S3FileStorage(settings("https://s3.example.com", false, "", "p"), client).delete("_probe/x.txt");
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("p/_probe/x.txt");
        assertThat(captor.getValue().bucket()).isEqualTo("mall-img");
    }

    @Test
    void contentTypeByExtension() {
        assertThat(S3FileStorage.contentType("a/b.webp")).isEqualTo("image/webp");
        assertThat(S3FileStorage.contentType("a/b.png")).isEqualTo("image/png");
        assertThat(S3FileStorage.contentType("a/b.bin")).isEqualTo("application/octet-stream");
    }

    @Test
    void createBuildsRealClientWithoutNetworkAccess() {
        // 只验证 SDK 构建参数合法（不发请求），防止升级 SDK 后构建器 API 变化没被发现
        try (S3FileStorage s = S3FileStorage.create(settings("https://oss-cn-hangzhou.aliyuncs.com", false, "", ""))) {
            assertThat(s.settings().bucket()).isEqualTo("mall-img");
        }
    }
}
