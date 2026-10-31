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

    private Security security = new Security();

    /** 首次启动时创建的超级管理员初始密码；首次登录后强制修改（prod 模式） */
    private String initAdminPassword = "admin123";

    /** 认证相关的部署开关与阈值（mall.security.*） */
    @Data
    public static class Security {
        /**
         * 管理员仍在使用初始 / 被重置的密码时，除个人信息、改密码、登出外的管理端接口返回 40301。
         * prod 模式强制为 true；demo 模式默认关闭，演示账号 admin/admin123 可以直接体验。
         */
        private boolean forceChangeInitialPassword = true;

        /** 图形验证码在响应中回显答案（devCode），仅供 demo 模式的冒烟测试脚本使用；prod 模式强制关闭 */
        private boolean captchaEcho = false;

        /** 同一 IP 每分钟最多尝试登录次数（管理端、商城端各自计数）：正常人工登录远低于此值，脚本爆破会被挡住 */
        private int loginIpPerMinute = 20;
    }

    @Data
    public static class Upload {
        /** 上传文件根目录，容器内挂载为数据卷 */
        private String dir = "./data/uploads";
        /** 对外访问前缀，由 Nginx 直接托管该目录 */
        private String urlPrefix = "/uploads";
        /** 单文件上限 5MB：商品图、评价图足够，避免被当成网盘 */
        private long maxSize = 5 * 1024 * 1024;
        /** 扩展名白名单；文件头（magic number）也必须与扩展名一致，见 UploadRules#checkContent */
        private String allowedExt = "jpg,jpeg,png,gif,webp";
        /** 单个会员每分钟最多上传次数：一次评价最多 9 张图，20 次足够，防止脚本刷存储 */
        private int memberPerMinute = 20;
        /** 单个会员每天最多上传次数：正常用户一天几十张已是上限 */
        private int memberDailyLimit = 200;
    }
}
