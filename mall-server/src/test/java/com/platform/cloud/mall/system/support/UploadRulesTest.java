package com.platform.cloud.mall.system.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 上传扩展名、大小、文件名与路径生成规则。
 */
class UploadRulesTest {

    private static final String ALLOWED = "jpg,jpeg,png,gif,webp";

    @Test
    void extensionIsLowerCasedAndTakenFromLastDot() {
        assertThat(UploadRules.checkExtension("Photo.JPG", ALLOWED)).isEqualTo("jpg");
        assertThat(UploadRules.checkExtension("shell.php.png", ALLOWED)).isEqualTo("png");
    }

    @ParameterizedTest
    @ValueSource(strings = {"evil.jsp", "image.png.php", "noext", "trailingdot.", ".htaccess", "a.svg", ""})
    void rejectsExtensionsOutsideWhitelist(String name) {
        assertThatThrownBy(() -> UploadRules.checkExtension(name, ALLOWED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void rejectsNullFileName() {
        assertThatThrownBy(() -> UploadRules.checkExtension(null, ALLOWED)).isInstanceOf(BusinessException.class);
    }

    @Test
    void whitelistToleratesSpacesAndCase() {
        assertThat(UploadRules.checkExtension("a.webp", " JPG , WebP ")).isEqualTo("webp");
    }

    @Test
    void sizeLimit() {
        long max = 5L * 1024 * 1024;
        UploadRules.checkSize(max, max);
        assertThatThrownBy(() -> UploadRules.checkSize(max + 1, max)).hasMessageContaining("5MB");
        assertThatThrownBy(() -> UploadRules.checkSize(0, max)).hasMessageContaining("文件为空");
    }

    @Test
    void baseNameStripsClientDirectories() {
        assertThat(UploadRules.baseName("C:\\Users\\me\\..\\a.png")).isEqualTo("a.png");
        assertThat(UploadRules.baseName("../../etc/passwd.png")).isEqualTo("passwd.png");
    }

    @Test
    void storagePathIsDateDirectoryPlusRandomName() {
        String path = UploadRules.buildPath(LocalDate.of(2026, 10, 31), "png");
        assertThat(path).matches("2026/10/31/[0-9a-f]{32}\\.png");
        assertThat(UploadRules.buildPath(LocalDate.of(2026, 10, 31), "png")).isNotEqualTo(path);
    }

    @Test
    void invalidBizTypeFallsBackToCommon() {
        assertThat(UploadRules.normalizeBizType("goods")).isEqualTo("goods");
        assertThat(UploadRules.normalizeBizType("../x")).isEqualTo("common");
        assertThat(UploadRules.normalizeBizType(null)).isEqualTo("common");
    }
}
