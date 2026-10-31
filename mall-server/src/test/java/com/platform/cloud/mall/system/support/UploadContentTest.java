package com.platform.cloud.mall.system.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 上传文件头校验：改了扩展名的 HTML / SVG 不能冒充图片。
 */
class UploadContentTest {

    private static byte[] bytes(int... v) {
        byte[] b = new byte[Math.max(v.length, UploadRules.MAGIC_HEADER_LENGTH)];
        for (int i = 0; i < v.length; i++) {
            b[i] = (byte) v[i];
        }
        return b;
    }

    @Test
    void realImagesPass() {
        assertThatCode(() -> UploadRules.checkContent(bytes(0xFF, 0xD8, 0xFF, 0xE0), "jpg")).doesNotThrowAnyException();
        assertThatCode(() -> UploadRules.checkContent(bytes(0xFF, 0xD8, 0xFF, 0xE1), "jpeg")).doesNotThrowAnyException();
        assertThatCode(() -> UploadRules.checkContent(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A), "png"))
                .doesNotThrowAnyException();
        assertThatCode(() -> UploadRules.checkContent("GIF89a......".getBytes(StandardCharsets.US_ASCII), "gif"))
                .doesNotThrowAnyException();
        assertThatCode(() -> UploadRules.checkContent("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII), "webp"))
                .doesNotThrowAnyException();
    }

    @Test
    void htmlRenamedToJpgIsRejected() {
        byte[] html = "<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> UploadRules.checkContent(html, "jpg"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内容与扩展名不符");
    }

    @Test
    void mismatchedTypeIsRejected() {
        byte[] png = bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
        assertThatThrownBy(() -> UploadRules.checkContent(png, "gif")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> UploadRules.checkContent("RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII),
                "webp")).isInstanceOf(BusinessException.class);
    }

    @Test
    void tinyFileIsRejected() {
        assertThatThrownBy(() -> UploadRules.checkContent(new byte[]{(byte) 0xFF}, "jpg"))
                .isInstanceOf(BusinessException.class);
    }
}
