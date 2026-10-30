package com.platform.cloud.mall.system.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.common.config.MallProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地存储：正常落盘与目录穿越防护。
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        MallProperties props = new MallProperties();
        props.getUpload().setDir(tempDir.resolve("uploads").toString());
        props.getUpload().setUrlPrefix("/uploads/");
        storage = new LocalFileStorage(props);
    }

    @Test
    void storesUnderRootAndBuildsUrl() throws Exception {
        storage.store("2026/10/31/abc.png", new ByteArrayInputStream("img".getBytes(StandardCharsets.UTF_8)));

        Path saved = tempDir.resolve("uploads/2026/10/31/abc.png");
        assertThat(Files.readString(saved)).isEqualTo("img");
        assertThat(storage.url("2026/10/31/abc.png")).isEqualTo("/uploads/2026/10/31/abc.png");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../evil.png", "2026/../../evil.png", "a/../../../etc/passwd", "", " "})
    void rejectsPathsEscapingRoot(String path) {
        assertThatThrownBy(() -> storage.store(path, new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法的文件路径");
    }

    @Test
    void rejectsAbsolutePath() {
        String absolute = tempDir.resolve("outside.png").toAbsolutePath().toString();
        assertThatThrownBy(() -> storage.store(absolute, new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOf(BusinessException.class);
        assertThat(Files.exists(tempDir.resolve("outside.png"))).isFalse();
    }

    @Test
    void innerDotDotThatStaysInsideRootIsAllowed() {
        assertThat(storage.resolveSafe("2026/x/../abc.png"))
                .isEqualTo(storage.root().resolve("2026/abc.png"));
    }
}
