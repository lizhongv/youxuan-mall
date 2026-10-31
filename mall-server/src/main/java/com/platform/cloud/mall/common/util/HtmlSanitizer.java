package com.platform.cloud.mall.common.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 富文本 XSS 清洗（商品详情、公告等后台编辑、前台 v-html 渲染的内容）。
 * <p>
 * 只靠前端过滤不够：接口可以被直接调用，后台账号被盗后写入的脚本会在所有买家浏览器里执行（存储型 XSS）。
 * 规则：
 * <ul>
 *   <li>jsoup {@link Safelist#relaxed()} 白名单：常见排版标签保留，script / iframe / 事件属性（onerror 等）全部去掉。</li>
 *   <li>img 的 src、a 的 href 只允许 http / https 与站内相对路径（如 /uploads/...），javascript: / data: 一律去掉。</li>
 *   <li>允许 style 属性（富文本编辑器的对齐、颜色依赖它），但删除含 expression()、javascript:、behavior、
 *       -moz-binding、非 http(s) / 站内 url() 的声明。</li>
 * </ul>
 */
public final class HtmlSanitizer {

    /** 解析相对路径用的虚拟站点；只用于判断协议，输出时保留原始相对路径 */
    private static final String BASE_URI = "https://mall.invalid/";

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("span", "hr", "figure", "figcaption")
            .addAttributes(":all", "style")
            .addAttributes("img", "width", "height")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto", "tel")
            .preserveRelativeLinks(true);

    /** 危险的 CSS 片段（在去掉注释与转义之后匹配） */
    private static final Pattern DANGEROUS_CSS = Pattern.compile(
            "expression\\s*\\(|javascript:|vbscript:|behavior\\s*:|-moz-binding|@import", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_URL = Pattern.compile("url\\s*\\(\\s*['\"]?([^'\")]*)", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern CSS_ESCAPE = Pattern.compile("\\\\(?:([0-9a-fA-F]{1,6})\\s?|(.))", Pattern.DOTALL);

    private HtmlSanitizer() {
    }

    /** 清洗后的 HTML（body 内片段）；null 原样返回 */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        Document dirty = Jsoup.parseBodyFragment(html, BASE_URI);
        Document clean = new Cleaner(SAFELIST).clean(dirty);
        for (Element el : clean.select("[style]")) {
            String style = cleanStyle(el.attr("style"));
            if (style.isEmpty()) {
                el.removeAttr("style");
            } else {
                el.attr("style", style);
            }
        }
        clean.outputSettings().prettyPrint(false);
        return clean.body().html();
    }

    /** 逐条检查 CSS 声明，危险的整条丢弃 */
    static String cleanStyle(String style) {
        List<String> kept = new ArrayList<>();
        for (String decl : style.split(";")) {
            String d = decl.trim();
            if (d.isEmpty()) {
                continue;
            }
            // 去掉注释与反斜杠转义后再判断，防止 exp/**/ression、\65xpression 之类的绕过
            String normalized = decodeCssEscapes(CSS_COMMENT.matcher(d).replaceAll("")).toLowerCase(Locale.ROOT);
            if (DANGEROUS_CSS.matcher(normalized).find() || hasUnsafeUrl(normalized)) {
                continue;
            }
            kept.add(d);
        }
        return String.join("; ", kept);
    }

    /** CSS 转义还原：\65 → e（十六进制，最多 6 位，可带一个空白结尾），\x → x */
    static String decodeCssEscapes(String css) {
        Matcher m = CSS_ESCAPE.matcher(css);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String replacement;
            if (m.group(1) != null) {
                int cp = Integer.parseInt(m.group(1), 16);
                replacement = Character.isValidCodePoint(cp) ? new String(Character.toChars(cp)) : "";
            } else {
                replacement = m.group(2);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static boolean hasUnsafeUrl(String css) {
        Matcher m = CSS_URL.matcher(css);
        while (m.find()) {
            String url = m.group(1).trim();
            boolean safe = url.startsWith("http://") || url.startsWith("https://")
                    || (url.startsWith("/") && !url.startsWith("//"));
            if (!safe) {
                return true;
            }
        }
        return false;
    }
}
