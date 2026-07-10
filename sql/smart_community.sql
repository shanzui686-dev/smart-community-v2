-- =============================================
-- 智慧小区管理系统 - 数据库初始化脚本
-- MySQL 8.0+ | InnoDB | utf8mb4
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS smart_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_community;

-- =============================================
-- 1. 系统管理模块
-- =============================================

-- 用户表
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role_menu;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS menu;

CREATE TABLE user (
    user_id         BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    username        VARCHAR(64)     NOT NULL                 COMMENT '用户名',
    password        VARCHAR(128)    NOT NULL                 COMMENT '密码（加密）',
    real_name       VARCHAR(64)     DEFAULT NULL             COMMENT '真实姓名',
    mobile          VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    avatar          VARCHAR(255)    DEFAULT NULL             COMMENT '头像URL',
    email           VARCHAR(128)    DEFAULT NULL             COMMENT '邮箱',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-启用',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status),
    KEY idx_deleted (deleted),
    KEY idx_mobile (mobile),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE role (
    role_id         BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '角色ID',
    role_name       VARCHAR(64)     NOT NULL                 COMMENT '角色名称',
    role_code       VARCHAR(64)     NOT NULL                 COMMENT '角色编码',
    description     VARCHAR(255)    DEFAULT NULL             COMMENT '角色描述',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-启用',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_status (status),
    KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 菜单表
CREATE TABLE menu (
    menu_id         BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '菜单ID',
    menu_name       VARCHAR(64)     NOT NULL                 COMMENT '菜单名称',
    parent_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '父菜单ID，0为顶级',
    path            VARCHAR(255)    DEFAULT NULL             COMMENT '路由路径',
    component       VARCHAR(255)    DEFAULT NULL             COMMENT '前端组件路径',
    icon            VARCHAR(64)     DEFAULT NULL             COMMENT '菜单图标',
    sort            INT             NOT NULL DEFAULT 0       COMMENT '排序',
    type            TINYINT         NOT NULL DEFAULT 1       COMMENT '菜单类型：1-目录，2-菜单，3-按钮',
    permission      VARCHAR(128)    DEFAULT NULL             COMMENT '权限标识',
    visible         TINYINT         NOT NULL DEFAULT 1       COMMENT '是否可见：0-隐藏，1-可见',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-启用',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (menu_id),
    KEY idx_parent_id (parent_id),
    KEY idx_sort (sort),
    KEY idx_type (type),
    KEY idx_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- 用户角色关联表
CREATE TABLE user_role (
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色菜单关联表
CREATE TABLE role_menu (
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    menu_id         BIGINT          NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_menu_id (menu_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES menu(menu_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 操作日志表
CREATE TABLE operation_log (
    log_id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    user_id         BIGINT          DEFAULT NULL             COMMENT '操作用户ID',
    username        VARCHAR(64)     DEFAULT NULL             COMMENT '操作用户名',
    module          VARCHAR(64)     DEFAULT NULL             COMMENT '操作模块',
    operation       VARCHAR(64)     DEFAULT NULL             COMMENT '操作类型',
    method          VARCHAR(255)    DEFAULT NULL             COMMENT '请求方法',
    params          TEXT            DEFAULT NULL             COMMENT '请求参数',
    result          TEXT            DEFAULT NULL             COMMENT '返回结果',
    ip              VARCHAR(64)     DEFAULT NULL             COMMENT '请求IP',
    duration        BIGINT          DEFAULT 0                COMMENT '耗时（毫秒）',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '操作状态：0-失败，1-成功',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (log_id),
    KEY idx_user_id (user_id),
    KEY idx_module (module),
    KEY idx_create_time (create_time),
    KEY idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =============================================
-- 2. 物业管理模块
-- =============================================

-- 小区表
CREATE TABLE community (
    community_id    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '小区ID',
    name            VARCHAR(128)    NOT NULL                 COMMENT '小区名称',
    address         VARCHAR(255)    DEFAULT NULL             COMMENT '小区地址',
    map_lng         DECIMAL(10,6)   DEFAULT NULL             COMMENT '百度地图经度',
    map_lat         DECIMAL(10,6)   DEFAULT NULL             COMMENT '百度地图纬度',
    total_building  INT             NOT NULL DEFAULT 0       COMMENT '楼栋总数',
    total_house     INT             NOT NULL DEFAULT 0       COMMENT '房屋总数',
    description     VARCHAR(512)    DEFAULT NULL             COMMENT '小区描述',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-停用，1-启用',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (community_id),
    KEY idx_name (name),
    KEY idx_status (status),
    KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小区表';

-- 居民表
CREATE TABLE person (
    person_id       BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '居民ID',
    community_id    BIGINT          NOT NULL                 COMMENT '所属小区ID',
    user_name       VARCHAR(64)     NOT NULL                 COMMENT '姓名',
    mobile          VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    sex             TINYINT         DEFAULT 1                COMMENT '性别：1-男，2-女',
    house_no        VARCHAR(128)    DEFAULT NULL             COMMENT '门牌号',
    person_type     TINYINT         NOT NULL DEFAULT 1       COMMENT '人员类型：1-业主，2-租户，3-家属',
    face_url        VARCHAR(512)    DEFAULT NULL             COMMENT '人脸照片URL',
    face_id         VARCHAR(128)    DEFAULT NULL             COMMENT '腾讯AI人脸ID',
    state           TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-迁出，1-在住',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (person_id),
    KEY idx_community_id (community_id),
    KEY idx_user_name (user_name),
    KEY idx_mobile (mobile),
    KEY idx_person_type (person_type),
    KEY idx_state (state),
    KEY idx_deleted (deleted),
    KEY idx_community_state (community_id, state),
    FOREIGN KEY (community_id) REFERENCES community(community_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='居民表';

-- 摄像头表
CREATE TABLE camera (
    camera_id       BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '摄像头ID',
    name            VARCHAR(128)    NOT NULL                 COMMENT '摄像头名称',
    device_code     VARCHAR(128)    NOT NULL                 COMMENT '设备编码',
    ip_address      VARCHAR(64)     DEFAULT NULL             COMMENT 'IP地址',
    community_id    BIGINT          NOT NULL                 COMMENT '所属小区ID',
    location        VARCHAR(255)    DEFAULT NULL             COMMENT '安装位置',
    stream_url      VARCHAR(512)    DEFAULT NULL             COMMENT '视频流地址',
    device_type     TINYINT         NOT NULL DEFAULT 1       COMMENT '类型：1-门禁摄像头，2-监控摄像头',
    online_status   TINYINT         NOT NULL DEFAULT 0       COMMENT '在线状态：0-离线，1-在线',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-启用',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (camera_id),
    UNIQUE KEY uk_device_code (device_code),
    KEY idx_community_id (community_id),
    KEY idx_device_type (device_type),
    KEY idx_online_status (online_status),
    KEY idx_status (status),
    FOREIGN KEY (community_id) REFERENCES community(community_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄像头表';

-- 车辆表
CREATE TABLE vehicle (
    vehicle_id      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '车辆ID',
    person_id       BIGINT          NOT NULL                 COMMENT '车主ID',
    community_id    BIGINT          NOT NULL                 COMMENT '所属小区ID',
    plate_number    VARCHAR(32)     NOT NULL                 COMMENT '车牌号',
    vehicle_type    TINYINT         NOT NULL DEFAULT 4       COMMENT '车辆类型：1-摩托车，2-三轮车，3-电瓶车，4-家用车',
    has_parking_space TINYINT       NOT NULL DEFAULT 0       COMMENT '是否有车位：0-无，1-有',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    deleted         TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (vehicle_id),
    UNIQUE KEY uk_plate_number (plate_number),
    KEY idx_person_id (person_id),
    KEY idx_community_id (community_id),
    KEY idx_vehicle_type (vehicle_type),
    KEY idx_has_parking_space (has_parking_space),
    FOREIGN KEY (person_id) REFERENCES person(person_id) ON DELETE RESTRICT,
    FOREIGN KEY (community_id) REFERENCES community(community_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆表';

-- =============================================
-- 3. 门禁管理模块
-- =============================================

-- 出入记录表
CREATE TABLE in_out_record (
    record_id       BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    person_id       BIGINT          DEFAULT NULL             COMMENT '居民ID（居民出入关联）',
    person_name     VARCHAR(64)     NOT NULL                 COMMENT '人员姓名',
    community_id    BIGINT          NOT NULL                 COMMENT '所属小区ID',
    camera_id       BIGINT          DEFAULT NULL             COMMENT '摄像头ID',
    type            TINYINT         NOT NULL                 COMMENT '出入类型：1-进，2-出',
    time            DATETIME        NOT NULL                 COMMENT '出入时间',
    location        VARCHAR(255)    DEFAULT NULL             COMMENT '出入位置（门禁点）',
    photo_url       VARCHAR(512)    DEFAULT NULL             COMMENT '抓拍照片URL',
    verify_type     TINYINT         NOT NULL DEFAULT 1       COMMENT '验证方式：1-人脸识别，2-门禁钥匙，3-访客登记',
    verified        TINYINT         NOT NULL DEFAULT 0       COMMENT '是否验证通过：0-未通过，1-通过',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (record_id),
    KEY idx_person_id (person_id),
    KEY idx_community_id (community_id),
    KEY idx_time (time),
    KEY idx_type (type),
    KEY idx_verified (verified),
    KEY idx_person_name (person_name),
    KEY idx_community_time (community_id, time),
    FOREIGN KEY (community_id) REFERENCES community(community_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出入记录表';

-- 访客登记表
CREATE TABLE visitor (
    visitor_id      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '访客ID',
    name            VARCHAR(64)     NOT NULL                 COMMENT '访客姓名',
    mobile          VARCHAR(20)     NOT NULL                 COMMENT '手机号',
    id_card         VARCHAR(18)     DEFAULT NULL             COMMENT '身份证号',
    community_id    BIGINT          NOT NULL                 COMMENT '访问小区ID',
    house_no        VARCHAR(128)    DEFAULT NULL             COMMENT '访问门牌号',
    person_id       BIGINT          DEFAULT NULL             COMMENT '被访居民ID',
    face_url        VARCHAR(512)    DEFAULT NULL             COMMENT '访客人脸照片URL',
    visit_time      DATETIME        NOT NULL                 COMMENT '预计来访时间',
    leave_time      DATETIME        DEFAULT NULL             COMMENT '实际离开时间',
    reason          VARCHAR(255)    DEFAULT NULL             COMMENT '来访事由',
    plate_number    VARCHAR(32)     DEFAULT NULL             COMMENT '车牌号',
    visitor_count   INT             DEFAULT 1                COMMENT '来访人数',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：1-已预约，2-已到访，3-已离开，4-已取消',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (visitor_id),
    KEY idx_community_id (community_id),
    KEY idx_visit_time (visit_time),
    KEY idx_mobile (mobile),
    KEY idx_status (status),
    KEY idx_name (name),
    FOREIGN KEY (community_id) REFERENCES community(community_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访客登记表';

-- =============================================
-- 初始化数据
-- =============================================

-- 密码：admin123（BCrypt加密）
INSERT INTO user (user_id, username, password, real_name, mobile, status) VALUES
(1, 'admin', '$2a$10$XEs59PS2AwwVQVxWBlb7Cek4u4kB7u8dlhG4Lb4m/kVYkbrllekha', '系统管理员', '13800000000', 1);

INSERT INTO role (role_id, role_name, role_code, description, status) VALUES
(1, '超级管理员', 'admin', '系统超级管理员，拥有所有权限', 1),
(2, '物业管理员', 'property', '物业管理人员', 1),
(3, '保安', 'security', '保安人员，负责门禁管理', 1);

INSERT INTO user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO menu (menu_id, menu_name, parent_id, path, component, icon, sort, type, permission, visible, status) VALUES
-- 一级目录
(1, '系统管理', 0, '/system', NULL, 'Setting', 1, 1, NULL, 1, 1),
(2, '物业管理', 0, '/property', NULL, 'OfficeBuilding', 2, 1, NULL, 1, 1),
(3, '门禁管理', 0, '/access', NULL, 'Key', 3, 1, NULL, 1, 1),
(4, '数据统计', 0, '/statistics', NULL, 'DataAnalysis', 4, 1, NULL, 1, 1),
-- 系统管理子菜单
(11, '用户管理', 1, '/system/user', 'system/UserManage', 'User', 1, 2, 'system:user:list', 1, 1),
(12, '角色管理', 1, '/system/role', 'system/RoleManage', 'Avatar', 2, 2, 'system:role:list', 1, 1),
(13, '菜单管理', 1, '/system/menu', 'system/MenuManage', 'Menu', 3, 2, 'system:menu:list', 1, 1),
(14, '操作日志', 1, '/system/log', 'system/LogManage', 'Document', 4, 2, 'system:log:list', 1, 1),
-- 系统管理按钮
(15, '用户新增', 11, NULL, NULL, NULL, 1, 3, 'system:user:add', 1, 1),
(16, '用户编辑', 11, NULL, NULL, NULL, 2, 3, 'system:user:edit', 1, 1),
(17, '用户删除', 11, NULL, NULL, NULL, 3, 3, 'system:user:delete', 1, 1),
-- 物业管理子菜单
(21, '小区管理', 2, '/property/community', 'property/CommunityManage', 'HomeFilled', 1, 2, 'property:community:list', 1, 1),
(22, '居民管理', 2, '/property/person', 'property/PersonManage', 'Avatar', 2, 2, 'property:person:list', 1, 1),
(23, '摄像头管理', 2, '/property/camera', 'property/CameraManage', 'VideoCamera', 3, 2, 'property:camera:list', 1, 1),
(24, '地图管理', 2, '/property/map', 'property/MapManage', 'MapLocation', 4, 2, 'property:map:view', 1, 1),
(25, '车辆管理', 2, '/property/vehicle', 'property/VehicleManage', 'Bicycle', 5, 2, 'property:vehicle:list', 1, 1),
-- 门禁管理子菜单
(31, '出入记录', 3, '/access/record', 'access/RecordManage', 'List', 1, 2, 'access:record:list', 1, 1),
(32, '访客登记', 3, '/access/visitor', 'access/VisitorManage', 'UserFilled', 2, 2, 'access:visitor:list', 1, 1),
(33, '人脸识别', 3, '/access/face', 'access/FaceManage', 'View', 3, 2, 'access:face:manage', 1, 1);

-- 超级管理员拥有所有菜单权限
INSERT INTO role_menu (role_id, menu_id)
SELECT 1, menu_id FROM menu;

-- 物业管理员权限（物业管理 + 数据统计 + 门禁查看）
INSERT INTO role_menu (role_id, menu_id) VALUES
(2, 2), (2, 21), (2, 22), (2, 23), (2, 24), (2, 25),
(2, 3), (2, 31), (2, 32), (2, 33),
(2, 4);

-- 保安权限（门禁管理 + 居民查看）
INSERT INTO role_menu (role_id, menu_id) VALUES
(3, 3), (3, 31), (3, 32), (3, 33),
(3, 2), (3, 22);

-- 数据统计菜单添加权限标识（用于接口级别控制）
UPDATE menu SET permission = 'statistics:dashboard' WHERE menu_id = 4;
