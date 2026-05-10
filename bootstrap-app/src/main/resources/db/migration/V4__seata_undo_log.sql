-- Phase 2.6: Seata AT 模式必需的回滚日志表
-- 每个 RM（资源管理器，即业务 service 的数据库）都要建
-- 单库共享时只建一次即可
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context, default serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT          NOT NULL COMMENT '0:normal, 1:GlobalFinished',
    `log_created`   DATETIME(6)  NOT NULL,
    `log_modified`  DATETIME(6)  NOT NULL,
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT 模式回滚表';
