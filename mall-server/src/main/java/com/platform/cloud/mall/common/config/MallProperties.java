package com.platform.cloud.mall.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 商城基础设施配置（mall.*）。
 * <p>
 * 只放部署相关的值；运费、超时时间这类运营可调的业务参数放在 sys_config 表，后台可改。
 */
@Data
@ConfigurationProperties(prefix = "mall")
public class MallProperties {

    private Upload upload = new Upload();

    /** 首次启动时创建的超级管理员初始密码；上线后应立即在后台修改 */
    private String initAdminPassword = "admin123";

    @Data
    public static class Upload {
        /** 上传文件根目录，容器内挂载为数据卷 */
        private String dir = "./data/uploads";
        /** 对外访问前缀，由 Nginx 直接托管该目录 */
        private String urlPrefix = "/uploads";
        /** 单文件上限 5MB：商品图、评价图足够，避免被当成网盘 */
        private long maxSize = 5 * 1024 * 1024;
        /** 扩展名白名单 */
        private String allowedExt = "jpg,jpeg,png,gif,webp";
    }
}
