# 电商智能系统（ecom）

## 项目背景
Web应用项目实训课程设计（CW3104）。一个面向店铺运营的电商系统，
核心卖点是用规则引擎替代重复性人工决策（智能补货建议、库存预警、
智能客服、个性化推荐）。

## 技术栈（必须严格对齐，不得擅自升级版本）
- JDK 1.8 / Maven 多模块
- Spring Boot 2.5.0
- Spring Security + JWT (jjwt 0.9.0) + Redis
- MyBatis-Plus 3.4.2（mapper XML 放 classpath*:/mapper/**/*.xml，主键 id-type: auto）
- Druid 连接池
- MySQL（库名 ecom，字符集 utf8mb4；本机为 5.7.28，DDL/查询语法必须兼容 5.7）
- MinIO（商品图片存储）
- springfox-swagger2 2.9.2
- 前端：Vue 3.2 + Element Plus + Pinia + Vue Router 4 + Axios + ECharts
- 前端 Vue CLI 5（非 Vite）

## 参考项目
`参考-seig/` 目录是本课程提供的参考实现（毕业设计管理系统），
代码风格、目录结构、鉴权方案全部以它为准。本目录不纳入 git。
实现任何功能前，先看 seig 里对应的实现再动手。

## 模块划分
- ecom-common   —— 通用层：domain(pojo/dto/vo)、mapper、config、handler、utils
- ecom-system   —— 业务层：controller、service、config、filter，含启动类
- ecom-ui       —— 前端 Vue 工程

## 后端强制约定
1. 所有接口返回 `cn.ecom.utils.ResponseResult<T>`，禁止直接返回裸对象
   - 成功：`ResponseResult.okResult(data)`
   - 失败：`ResponseResult.errorResult(AppHttpCodeEnum.XXX, "具体原因")`
2. 错误码统一在 `AppHttpCodeEnum` 枚举里定义，禁止硬编码数字
3. RESTful 路径规范（照 seig）：
   - `GET    /{domain}/{entity}`        分页查询（参数走 query）
   - `GET    /{domain}/{entity}/getAll` 查全部
   - `GET    /{domain}/{entity}/{id}`   按 id 查
   - `POST   /{domain}/{entity}`        新增
   - `PUT    /{domain}/{entity}`        修改
   - `DELETE /{domain}/{entity}/{id}`   删除
   - 分页参数统一用 `pageNum` / `pageSize`，返回 MyBatis-Plus 的 `Page` 对象
4. Service 层接口 + 实现分开，实现类放 `service.impl`
5. 接口鉴权用 `@PreAuthorize("@ps.hasPermission('xxx:yyy')")`，权限标识格式 `模块:操作`
6. 所有写操作（增删改）记录操作日志
7. 后端端口 8888，接口路径前缀不加 /api（由前端代理负责）

## 前端强制约定
1. 请求封装：每个业务域一个 `src/api/{domain}Http.js`，
   导出 default 对象，函数命名 `selectXxxByPage / selectAllXxx / addXxx / updateXxx / delXxx`
2. axios 直连相对路径 `/api/...`，由 `vue.config.js` 代理到 8888
3. 页面组件放 `src/components/`，命名用业务名（如 `productList.vue`），
   不用 PascalCase 目录结构
4. 全局状态用 Pinia，放 `src/store/`，开启 persistedstate
5. token 存 `sessionStorage` 的 `authorization` 字段
6. UI 统一用 Element Plus，表格分页用 `el-pagination`，不允许引第二套 UI 库
7. 前端端口 8082

## 数据库约定
- 表名前缀：系统表 `sys_`，业务表 `ec_`
- 每张表必备字段：`id`(bigint 自增)、`create_time`、`update_time`、`del_flag`(0正常/1删除)
- 逻辑删除，不做物理删除
- 金额字段统一 `decimal(12,2)`，禁止用 float/double
- 所有 SQL 变更必须同时更新 `sql/ecom.sql` 全量脚本

## 开发纪律
- 每完成一个功能模块，必须能通过 `mvn clean install` 和前端 `npm run serve`
- 不允许为了让代码跑通而注释掉鉴权、绕过权限校验
- 数据库改动必须写进 sql 脚本，不允许只改本地库
- 不引入未在技术栈中列出的第三方依赖，需要时先问我
- 不要用 RabbitMQ（本项目不需要，降低环境负担）

## 当前环境
- 本地 MySQL：localhost:3306（MySQL 5.7.28，服务名 MySQL57），用户 root，密码 123456
  - 客户端路径：C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe
- 默认账号：admin / admin123（超管）；新建用户不填密码时默认 123456
- 登录态：Redis key `user:{id}`，token 放请求头 `authorization`（前端存 sessionStorage 同名字段）
- 本地 Redis：localhost:6379
- 本地 MinIO：localhost:9000，minioadmin/minioadmin（P2 前无需启动）
- 文件上传目录：D:\ecom\

## 构建与启动（本机特有，重要）
- JDK 1.8 路径：C:\Program Files\Java\jdk-1.8（本机 JAVA_HOME 默认指向 17，
  构建**必须**显式指定 JDK 8）：
  - Git Bash：`JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn clean install`
  - IntelliJ IDEA：Project SDK 与 Maven Runner JRE 均设为 1.8
- 启动顺序：MySQL(3306) → Redis(6379) → 后端 EcomApplication(8888) → 前端 `npm run serve`(8082)
- 前端包管理走腾讯镜像（已配置）

## 文档
- 需求拆分与模块分类：`docs/需求拆分与模块分类.md`
- 设计文档：`docs/superpowers/specs/2026-09-15-ecom-foundation-design.md`
- 实施计划：`docs/superpowers/plans/2026-09-15-ecom-p0-foundation.md`
