# HearBridge API 文档

## APP 登录

### 注册

`POST /app/auth/register`

```json
{
  "username": "test001",
  "password": "123456",
  "nickname": "测试用户"
}
```

响应：

```json
{
  "token": "token-value",
  "user": {
    "id": 1,
    "username": "test001",
    "nickname": "测试用户",
    "avatarUrl": null
  }
}
```

### 登录

`POST /app/auth/login`

```json
{
  "username": "test001",
  "password": "123456"
}
```

响应同注册。密码沿用旧项目方案，后端保存 `MD5(password)` 到 `app_user.password_hash`，不保存明文。

### 当前用户

`GET /app/auth/me`

请求头：

```text
token: token-value
```

响应：

```json
{
  "id": 1,
  "username": "test001",
  "nickname": "测试用户",
  "avatarUrl": "http://example.com/avatar.png"
}
```

### 退出登录

`POST /app/auth/logout`

请求头：

```text
token: token-value
```

退出时删除 Redis 登录态：`hearbridge:app:login:{token}`。

### 修改资料

`PUT /app/user/profile`

请求头：

```text
token: token-value
```

```json
{
  "nickname": "新昵称"
}
```

响应为更新后的用户资料。

### 上传头像

`POST /app/user/avatar`

请求头：

```text
token: token-value
Content-Type: image/jpeg
X-Filename: avatar.jpg
```

请求体直接传图片二进制，不使用 multipart。后端会上传到 MinIO，并把相对 object key 写入 `app_user.avatar_url`，例如：

```text
images/avatar/1/20260424-2a7f0b8f3f6f4a5cb5c0b4c4a9e0c111.jpg
```

响应返回拼接后的完整头像地址：

```json
{
  "id": 1,
  "username": "test001",
  "nickname": "测试用户",
  "avatarUrl": "http://192.168.43.6:9000/cwasa-static/images/avatar/1/20260424-2a7f0b8f3f6f4a5cb5c0b4c4a9e0c111.jpg"
}
```

## 用户表

```sql
CREATE TABLE app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'user id',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT 'login username',
    password_hash VARCHAR(128) NOT NULL COMMENT 'password hash',
    nickname VARCHAR(64) NOT NULL COMMENT 'nickname',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT 'MinIO relative avatar object key',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time'
) COMMENT='APP user table';
```

## 手语资源

### 分类列表

`GET /sign/categories`

### 分类详情

`GET /sign/categories/{code}`

### 资源列表

`GET /sign/resources`

支持按分类筛选：

`GET /sign/resources?categoryCode=phrase`

### 资源详情

`GET /sign/resources/{code}`

资源表保存 MinIO 相对路径，接口额外返回完整地址：

```json
{
  "id": 1,
  "code": "hello",
  "nameZh": "你好",
  "categoryCode": "phrase",
  "sigmlObjectKey": "sigml/official-bsl/hello.sigml",
  "coverObjectKey": "images/sign/official-bsl/hello.png",
  "sigmlUrl": "http://127.0.0.1:9000/cwasa-static/sigml/official-bsl/hello.sigml",
  "coverUrl": "http://127.0.0.1:9000/cwasa-static/images/sign/official-bsl/hello.png"
}
```

## 配置

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 26379
      database: 1

hearbridge:
  auth:
    token-prefix: hearbridge:app:login:
    token-expire-days: 7
  minio:
    endpoint: http://127.0.0.1:9000
    bucket: cwasa-static
```
