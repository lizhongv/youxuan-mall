package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.common.config.MallProperties;
import com.platform.cloud.mall.common.security.RedisCounter;
import com.platform.cloud.mall.system.mapper.SysFileMapper;
import com.platform.cloud.mall.system.support.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 上传：文件头校验在落盘之前；会员上传限频。
 */
class FileServiceImplTest {

    private FileStorage storage;
    private RedisCounter counter;
    private FileServiceImpl service;

    @BeforeEach
    void setUp() {
        storage = mock(FileStorage.class);
        counter = mock(RedisCounter.class);
        service = new FileServiceImpl(storage, mock(SysFileMapper.class), new MallProperties(), counter);
        when(counter.incrementInWindow(anyString(), any())).thenReturn(1L);
    }

    @Test
    void fakeImageIsRejectedBeforeStoring() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "x.jpg", "image/jpeg",
                "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.upload(f, "goods")).isInstanceOf(BusinessException.class);
        verify(storage, never()).store(anyString(), any());
    }

    @Test
    void memberPerMinuteQuota() {
        when(counter.incrementInWindow(startsWith("mall:upload:member:min:"), any())).thenReturn(21L);
        assertThatThrownBy(() -> service.checkMemberQuota(7L)).hasMessageContaining("频繁");
    }

    @Test
    void memberDailyQuota() {
        when(counter.incrementInWindow(startsWith("mall:upload:member:day:"), any())).thenReturn(201L);
        assertThatThrownBy(() -> service.checkMemberQuota(7L)).hasMessageContaining("上限");
    }

    @Test
    void quotaFailsOpenWhenRedisIsDown() {
        when(counter.incrementInWindow(anyString(), any())).thenThrow(new RedisConnectionFailureException("down"));
        assertThatCode(() -> service.checkMemberQuota(7L)).doesNotThrowAnyException();
    }
}
