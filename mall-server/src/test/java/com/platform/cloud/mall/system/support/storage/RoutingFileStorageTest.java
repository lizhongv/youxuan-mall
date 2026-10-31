package com.platform.cloud.mall.system.support.storage;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.api.InfraConfigKeys;
import com.platform.cloud.mall.system.support.LocalFileStorage;
import com.platform.cloud.mall.testsupport.FakeConfigApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 存储路由：LOCAL / S3 切换、配置不变复用客户端、配置变化重建并关闭旧客户端。
 */
class RoutingFileStorageTest {

    private LocalFileStorage local;
    private FakeConfigApi config;
    private List<S3FileStorage> built;
    private Function<S3Settings, S3FileStorage> factory;

    @BeforeEach
    void setUp() {
        local = mock(LocalFileStorage.class);
        config = new FakeConfigApi();
        built = new ArrayList<>();
        factory = s -> {
            S3FileStorage storage = mock(S3FileStorage.class);
            when(storage.settings()).thenReturn(s);
            when(storage.url(any())).thenAnswer(inv -> "https://cdn/" + inv.getArgument(0));
            built.add(storage);
            return storage;
        };
    }

    @Test
    void defaultsToLocal() throws IOException {
        RoutingFileStorage routing = new RoutingFileStorage(local, config, factory);
        routing.store("a.jpg", new ByteArrayInputStream(new byte[]{1}));
        verify(local).store(eq("a.jpg"), any());
        assertThat(built).isEmpty();
        assertThat(routing.currentProvider()).isEqualTo(StorageProvider.LOCAL);
    }

    @Test
    void switchesToS3AndRebuildsOnlyWhenConfigChanges() throws IOException {
        s3();
        RoutingFileStorage routing = new RoutingFileStorage(local, config, factory);

        routing.store("a.jpg", new ByteArrayInputStream(new byte[]{1}));
        assertThat(routing.url("a.jpg")).isEqualTo("https://cdn/a.jpg");
        assertThat(built).hasSize(1);

        config.put(InfraConfigKeys.STORAGE_S3_BUCKET, "other-bucket");
        routing.store("b.jpg", new ByteArrayInputStream(new byte[]{1}));
        assertThat(built).hasSize(2);
        assertThat(built.get(1).settings().bucket()).isEqualTo("other-bucket");
        verify(built.get(0)).close();

        // 切回本地：新上传回到本地磁盘
        config.put(InfraConfigKeys.STORAGE_PROVIDER, "LOCAL");
        routing.store("c.jpg", new ByteArrayInputStream(new byte[]{1}));
        verify(local).store(eq("c.jpg"), any());
    }

    @Test
    void incompleteS3ConfigFailsWithKeyNames() {
        config.put(InfraConfigKeys.STORAGE_PROVIDER, "S3").put(InfraConfigKeys.STORAGE_S3_SECRET_KEY, "sk-secret");
        RoutingFileStorage routing = new RoutingFileStorage(local, config, factory);
        assertThatThrownBy(() -> routing.url("a.jpg"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(InfraConfigKeys.STORAGE_S3_ENDPOINT)
                .hasMessageContaining(InfraConfigKeys.STORAGE_S3_BUCKET)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("sk-secret"));
    }

    @Test
    void unknownProviderAndRequireS3OnLocalAreRejected() {
        RoutingFileStorage routing = new RoutingFileStorage(local, config, factory);
        assertThatThrownBy(routing::requireS3).hasMessageContaining("不是 S3");
        config.put(InfraConfigKeys.STORAGE_PROVIDER, "FTP");
        assertThatThrownBy(() -> routing.url("a.jpg")).hasMessageContaining("FTP");
    }

    @Test
    void settingsToStringHidesSecret() {
        s3();
        assertThat(S3Settings.from(config).toString()).doesNotContain("sk-secret").doesNotContain("ak-id");
    }

    private void s3() {
        config.put(InfraConfigKeys.STORAGE_PROVIDER, "s3")
                .put(InfraConfigKeys.STORAGE_S3_ENDPOINT, "https://oss-cn-hangzhou.aliyuncs.com/")
                .put(InfraConfigKeys.STORAGE_S3_BUCKET, "mall-img")
                .put(InfraConfigKeys.STORAGE_S3_ACCESS_KEY, "ak-id")
                .put(InfraConfigKeys.STORAGE_S3_SECRET_KEY, "sk-secret")
                .put(InfraConfigKeys.STORAGE_PUBLIC_BASE_URL, "https://cdn.example.com/");
    }
}
