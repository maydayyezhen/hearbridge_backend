DROP TABLE IF EXISTS sign_resource;
DROP TABLE IF EXISTS sign_category;
DROP TABLE IF EXISTS gesture_item;
DROP TABLE IF EXISTS gesture_category;

CREATE TABLE sign_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '分类编码，如 greeting/question/time',
    name_zh VARCHAR(128) NOT NULL COMMENT '分类中文名',
    cover_object_key VARCHAR(512) DEFAULT NULL COMMENT '分类图片在MinIO中的相对路径'
) COMMENT='手语资源分类表';

CREATE TABLE sign_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(128) NOT NULL UNIQUE COMMENT '统一编码，建议使用去后缀后的文件名',
    name_zh VARCHAR(128) DEFAULT NULL COMMENT '中文名称',
    category_code VARCHAR(64) NOT NULL COMMENT '分类编码',
    sigml_object_key VARCHAR(512) NOT NULL COMMENT 'sigml在MinIO中的相对路径',
    cover_object_key VARCHAR(512) DEFAULT NULL COMMENT '资源图片在MinIO中的相对路径'
) COMMENT='手语资源表';

CREATE INDEX idx_sign_resource_category_code ON sign_resource (category_code);
