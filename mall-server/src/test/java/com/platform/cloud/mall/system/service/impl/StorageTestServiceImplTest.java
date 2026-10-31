package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.support.storage.RoutingFileStorage;
import com.platform.cloud.mall.system.support.storage.S3FileStorage;
import com.platform.cloud.mall.system.support.storage.StorageProvider;
import com.platform.cloud.mall.system.vo.StorageTestVO;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 存储测试连接：本地存储直接返回；S3 上传后必须删除探测文件。
 */
class StorageTestServiceImplTest {

    private final RoutingFileStorage routing = mock(RoutingFileStorage.class);
    private final S3FileStorage s3 = mock(S3FileStorage.class);
    private final StorageTestServiceImpl service = new StorageTestServiceImpl(routing);

    @Test
    void localNeedsNoTest() {
        when(routing.currentProvider()).thenReturn(StorageProvider.LOCAL);
        StorageTestVO vo = service.test();
        assertThat(vo.getProvider()).isEqualTo("LOCAL");
        verify(routing, never()).requireS3();
    }

    @Test
    void s3UploadsThenDeletesProbe() throws IOException {
        when(routing.currentProvider()).thenReturn(StorageProvider.S3);
        when(routing.requireS3()).thenReturn(s3);
        when(s3.url(anyString())).thenReturn("https://cdn/x");
        StorageTestVO vo = service.test();
        assertThat(vo.getProvider()).isEqualTo("S3");
        assertThat(vo.getSampleUrl()).isEqualTo("https://cdn/x");
        verify(s3).store(startsWith(StorageTestServiceImpl.PROBE_DIR + "/"), any());
        verify(s3).delete(startsWith(StorageTestServiceImpl.PROBE_DIR + "/"));
    }

    @Test
    void uploadAndDeleteFailuresAreReadable() throws IOException {
        when(routing.currentProvider()).thenReturn(StorageProvider.S3);
        when(routing.requireS3()).thenReturn(s3);
        doThrow(new IOException("对象存储上传失败：AccessDenied")).when(s3).store(anyString(), any());
        assertThatThrownBy(service::test).isInstanceOf(BusinessException.class).hasMessageContaining("AccessDenied");

        S3FileStorage s3b = mock(S3FileStorage.class);
        when(routing.requireS3()).thenReturn(s3b);
        doThrow(S3Exception.builder().statusCode(403).build()).when(s3b).delete(anyString());
        assertThatThrownBy(service::test).hasMessageContaining("删除失败");
    }
}
