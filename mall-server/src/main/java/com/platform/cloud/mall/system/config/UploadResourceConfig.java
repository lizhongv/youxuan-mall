package com.platform.cloud.mall.system.config;

import com.platform.cloud.mall.common.config.MallProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 开发环境由应用直接托管上传目录，前端本地联调不必再起 Nginx。
 * <p>
 * 生产环境 Nginx 在请求到达应用前就处理了 /uploads/**，可设 {@code MALL_UPLOAD_SERVE_LOCAL=false} 关闭。
 * 只有 URL 前缀是站内路径（以 / 开头）时才注册；配置成 CDN 域名说明文件不由本机提供。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mall.upload", name = "serve-local", havingValue = "true", matchIfMissing = true)
public class UploadResourceConfig implements WebMvcConfigurer {

    private final MallProperties props;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = props.getUpload().getUrlPrefix();
        if (prefix == null || !prefix.startsWith("/")) {
            return;
        }
        String pattern = (prefix.endsWith("/") ? prefix : prefix + "/") + "**";
        Path dir = Paths.get(props.getUpload().getDir()).toAbsolutePath().normalize();
        registry.addResourceHandler(pattern).addResourceLocations(dir.toUri().toString());
    }
}
