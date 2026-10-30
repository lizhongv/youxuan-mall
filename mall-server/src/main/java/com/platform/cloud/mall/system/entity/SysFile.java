package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 上传文件记录，便于日后清理无主文件或迁移到对象存储 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    private String originalName;
    private String path;
    private String url;
    private Long size;
    private String contentType;
    private String bizType;
}
