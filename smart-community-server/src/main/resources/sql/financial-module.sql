-- ================================================
-- 智慧小区系统 - 财务与缴费管理模块 DDL
-- 数据库：MySQL 8.0+
-- ================================================

-- ----------------------------
-- 1. 计费标准表 (fee_standard)
-- ----------------------------
DROP TABLE IF EXISTS `fee_standard`;
CREATE TABLE `fee_standard` (
    `fee_standard_id`    BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '计费标准ID',
    `fee_name`           VARCHAR(100) NOT NULL                 COMMENT '费用名称（物业费、停车费、水电公摊等）',
    `unit_price`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '单价（元）',
    `calculation_method` TINYINT      NOT NULL DEFAULT 1       COMMENT '计算方式：1-按户固定，2-按面积',
    `status`             TINYINT      NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-启用',
    `remark`             VARCHAR(500)          DEFAULT NULL     COMMENT '备注',
    `deleted`            TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`fee_standard_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='计费标准表';

-- ----------------------------
-- 2. 账单表 (property_bill)
-- ----------------------------
DROP TABLE IF EXISTS `property_bill`;
CREATE TABLE `property_bill` (
    `bill_id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '账单ID',
    `person_id`          BIGINT       NOT NULL                 COMMENT '关联住户ID',
    `community_id`       BIGINT       NOT NULL                 COMMENT '关联小区ID',
    `house_no`           VARCHAR(50)           DEFAULT NULL     COMMENT '门牌号',
    `fee_standard_id`    BIGINT       NOT NULL                 COMMENT '关联计费标准ID',
    `fee_name`           VARCHAR(100) NOT NULL                 COMMENT '费用名称',
    `amount`             DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '应缴金额（元）',
    `bill_month`         VARCHAR(7)   NOT NULL                 COMMENT '账单月份（格式：YYYY-MM）',
    `due_date`           DATE         NOT NULL                 COMMENT '到期时间',
    `status`             TINYINT      NOT NULL DEFAULT 0       COMMENT '状态：0-待缴，1-已缴，2-逾期',
    `last_dunning_time`  DATETIME              DEFAULT NULL     COMMENT '最后催缴时间',
    `deleted`            TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`bill_id`),
    KEY `idx_person_id` (`person_id`),
    KEY `idx_community_id` (`community_id`),
    KEY `idx_bill_month` (`bill_month`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账单表';

-- ----------------------------
-- 3. 缴费流水表 (payment_record)
-- ----------------------------
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record` (
    `payment_id`          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '流水ID',
    `bill_id`             BIGINT       NOT NULL                 COMMENT '关联账单ID',
    `paid_amount`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '实缴金额（元）',
    `pay_method`          VARCHAR(50)  NOT NULL                 COMMENT '支付方式（支付宝/微信/现金/银行转账）',
    `pay_time`            DATETIME     NOT NULL                 COMMENT '支付时间',
    `operator`            VARCHAR(50)  NOT NULL DEFAULT 'system' COMMENT '操作人',
    `remark`              VARCHAR(500)          DEFAULT NULL     COMMENT '备注',
    `deleted`             TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`payment_id`),
    KEY `idx_bill_id` (`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='缴费流水表';

-- ----------------------------
-- 初始数据：插入示例计费标准
-- ----------------------------
INSERT INTO `fee_standard` (`fee_name`, `unit_price`, `calculation_method`, `status`, `remark`) VALUES
('物业费', 2.50, 2, 1, '按面积计费，单位：元/平方米/月'),
('停车费', 150.00, 1, 1, '按月固定收费，单位：元/月'),
('水电公摊', 30.00, 1, 1, '按户固定收费，单位：元/月'),
('垃圾清运费', 15.00, 1, 1, '按户固定收费，单位：元/月');
