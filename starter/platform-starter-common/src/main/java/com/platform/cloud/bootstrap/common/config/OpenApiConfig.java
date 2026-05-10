package com.platform.cloud.bootstrap.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI platformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("platform-cloud API")
                        .version("0.1.0")
                        .description("现代化电商平台 API 文档（bootstrap-app 单体阶段）")
                        .contact(new Contact().name("lizhongv").url("https://github.com/lizhongv/youxuan-mall"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("点击 Authorize 后，输入 'Bearer <token>'")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
