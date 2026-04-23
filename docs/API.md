# HearBridge API 文档

## 1. 手语资源分类列表
GET `/sign/categories`

响应示例：

```json
[
  {
    "id": 1,
    "code": "greeting",
    "nameZh": "问候语",
    "coverObjectKey": "sign/category/greeting.png"
  },
  {
    "id": 2,
    "code": "question",
    "nameZh": "疑问句",
    "coverObjectKey": "sign/category/question.png"
  }
]
```

## 2. 根据分类编码查询单个分类
GET `/sign/categories/greeting`

响应示例：

```json
{
  "id": 1,
  "code": "greeting",
  "nameZh": "问候语",
  "coverObjectKey": "sign/category/greeting.png"
}
```

## 3. 手语资源列表
GET `/sign/resources`

支持按分类编码筛选：

GET `/sign/resources?categoryCode=greeting`

响应示例：

```json
[
  {
    "id": 1,
    "code": "hello",
    "nameZh": "你好",
    "categoryCode": "greeting",
    "sigmlObjectKey": "sign/sigml/hello.sigml",
    "coverObjectKey": "sign/cover/hello.png"
  }
]
```

## 4. 根据资源编码查询单个资源
GET `/sign/resources/hello`

响应示例：

```json
{
  "id": 1,
  "code": "hello",
  "nameZh": "你好",
  "categoryCode": "greeting",
  "sigmlObjectKey": "sign/sigml/hello.sigml",
  "coverObjectKey": "sign/cover/hello.png"
}
```

## 5. 数据库表结构

- `sign_category`
- `sign_resource`

字段含义：

- `sign_category.id`：主键 ID
- `sign_category.code`：分类编码
- `sign_category.name_zh`：分类中文名
- `sign_category.cover_object_key`：分类图片在 MinIO 中的相对路径
- `sign_resource.id`：主键 ID
- `sign_resource.code`：资源统一编码
- `sign_resource.name_zh`：资源中文名称
- `sign_resource.category_code`：分类编码
- `sign_resource.sigml_object_key`：SigML 在 MinIO 中的相对路径
- `sign_resource.cover_object_key`：资源图片在 MinIO 中的相对路径

## 6. 初始化 SQL

初始化脚本位置：

- `src/main/resources/sql/sign_schema.sql`
