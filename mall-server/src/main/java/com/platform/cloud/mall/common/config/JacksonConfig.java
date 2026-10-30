package com.platform.cloud.mall.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 序列化约定。
 * <ul>
 *   <li>包装类型 Long 输出为字符串（ID 都用包装类型；分页 total 等用基本类型 long，仍输出数字）：雪花 ID 超过 JS 安全整数 2^53，前端直接用数字会丢精度。</li>
 *   <li>时间统一 yyyy-MM-dd HH:mm:ss，前后端不用各自转换时区和格式。</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer mallJacksonCustomizer() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(fmt))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(fmt));
    }
}
