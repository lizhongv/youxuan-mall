-- Flyway 基线迁移（占位）
-- 文件命名：V{版本}__{说明}.sql；脚本一旦发布不可修改（Flyway 会校验 checksum）
-- Phase 1 内会逐步把 platform/_sql/mall.sql 的表结构按域拆分迁入

CREATE TABLE IF NOT EXISTS `app_health_probe` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_name`    VARCHAR(64) NOT NULL                COMMENT '应用名',
    `boot_time`   DATETIME    NOT NULL                COMMENT '启动时间',
    `note`        VARCHAR(255) DEFAULT NULL           COMMENT '备注',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_app_name` (`app_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '应用健康探针记录表';
