package com.platform.cloud.mall.common.util;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会员提交的图片只能是本站上传路径。
 */
class UploadUrlValidatorTest {

    @Test
    void acceptsUploadedPaths() {
        assertThat(UploadUrlValidator.isLocal("/uploads/2026/10/31/9f8e7d.jpg", "/uploads")).isTrue();
        assertThat(UploadUrlValidator.isLocal("/uploads/demo/goods_1.png", "/uploads/")).isTrue();
    }

    @Test
    void rejectsExternalAndTrickyUrls() {
        String p = "/uploads";
        assertThat(UploadUrlValidator.isLocal("https://evil.com/a.jpg", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal("//evil.com/uploads/a.jpg", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal("javascript:alert(1)", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal("/uploads/../admin/x.jpg", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal("/uploads//evil.com/a.jpg", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal("/uploads/a.jpg?x=<script>", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal("/uploadsX/a.jpg", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal("/uploads/noext", p)).isFalse();
        assertThat(UploadUrlValidator.isLocal(null, p)).isFalse();
    }

    @Test
    void requireLocalThrowsOnFirstBadUrl() {
        UploadUrlValidator.requireLocal(null, "/uploads");
        UploadUrlValidator.requireLocal(List.of("/uploads/a/b.webp"), "/uploads");
        assertThatThrownBy(() -> UploadUrlValidator.requireLocal(List.of("/uploads/a.jpg", "http://x/y.jpg"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片地址无效");
    }
}
