package com.platform.cloud.mall.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 富文本清洗：去脚本、事件属性、伪协议与危险 CSS，保留正常排版与站内 / http(s) 图片。
 */
class HtmlSanitizerTest {

    @Test
    void keepsNormalFormatting() {
        String html = "<p style=\"text-align: center; color: red\"><strong>新品</strong>上架</p>"
                + "<img src=\"/uploads/2026/10/31/a.jpg\" alt=\"图\"><img src=\"https://cdn.example.com/b.png\">";
        String out = HtmlSanitizer.sanitize(html);
        assertThat(out).contains("<strong>新品</strong>")
                .contains("text-align: center")
                .contains("src=\"/uploads/2026/10/31/a.jpg\"")
                .contains("src=\"https://cdn.example.com/b.png\"");
    }

    @Test
    void removesScriptsAndEventHandlers() {
        String out = HtmlSanitizer.sanitize("<p onclick=\"alert(1)\">x</p><script>alert(1)</script>"
                + "<img src=x onerror=alert(1)><iframe src=\"https://evil\"></iframe>");
        assertThat(out).doesNotContain("script").doesNotContain("onclick").doesNotContain("onerror")
                .doesNotContain("iframe");
    }

    @Test
    void removesJavascriptAndDataUrls() {
        String out = HtmlSanitizer.sanitize("<a href=\"javascript:alert(1)\">a</a>"
                + "<img src=\"data:image/svg+xml;base64,PHN2Zz4=\"><img src=\"JaVaScRiPt:alert(1)\">");
        assertThat(out.toLowerCase()).doesNotContain("javascript").doesNotContain("data:");
    }

    @Test
    void removesDangerousCss() {
        String out = HtmlSanitizer.sanitize("<div style=\"color: blue; width: expression(alert(1))\">a</div>"
                + "<div style=\"background: url(javascript:alert(1))\">b</div>"
                + "<div style=\"background: url('/uploads/a.png'); behavior: url(x.htc)\">c</div>"
                + "<div style=\"width: exp/**/ression(alert(1))\">d</div>"
                + "<div style=\"width: \\65 xpression(alert(1))\">e</div>");
        assertThat(out).contains("color: blue")
                .contains("url('/uploads/a.png')")
                .doesNotContain("expression")
                .doesNotContain("javascript")
                .doesNotContain("behavior")
                .doesNotContain("ression");
    }

    @Test
    void nullAndEmptyAreReturnedAsIs() {
        assertThat(HtmlSanitizer.sanitize(null)).isNull();
        assertThat(HtmlSanitizer.sanitize("")).isEmpty();
    }

    @Test
    void cssDeclarationWithExternalUrlIsKept() {
        assertThat(HtmlSanitizer.cleanStyle("background:url(https://cdn.example.com/a.png)")).contains("https://");
        assertThat(HtmlSanitizer.cleanStyle("background:url(//evil.com/a.png)")).isEmpty();
    }
}
