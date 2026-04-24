CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'user id',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT 'login username',
    password_hash VARCHAR(128) NOT NULL COMMENT 'password hash',
    nickname VARCHAR(64) NOT NULL COMMENT 'nickname',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT 'MinIO relative avatar object key',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time'
) COMMENT='APP user table';
