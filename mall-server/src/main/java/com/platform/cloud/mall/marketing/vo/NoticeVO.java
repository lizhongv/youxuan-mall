package com.platform.cloud.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告。首页列表不带 content（为空不输出），详情接口才返回正文。
 */
@Data
@Schema(description = "公告")
public class NoticeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
}
