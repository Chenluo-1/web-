# ecom P0 工程骨架 + 全量数据库 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 ecom 多模块 Maven 骨架（父 pom + ecom-common + ecom-system）+ Vue3 前端工程 + 30 张表全量数据库脚本并建入本地 MySQL，打通前后端连通（`/health`）。

**Architecture:** 后端 cn.ecom 包，common 放基础件（响应/异常/配置/工具），system 放启动类与将来的 controller/service；前端 Vue CLI 5 工程经 `/api` 代理到 8888；数据库唯一权威脚本 `sql/ecom.sql`（30 表：5 张 sys_ + 25 张 ec_）。

**Tech Stack:** JDK 1.8 / Spring Boot 2.5.0（BOM 导入）/ MyBatis-Plus 3.4.2 / Druid 1.2.4 / MySQL 8 / springfox 2.9.2 / fastjson 1.2.33 / Vue 3.2 + Vue CLI 5 + Element Plus + Pinia + Axios。

**Spec:** `docs/superpowers/specs/2026-09-15-ecom-foundation-design.md`（字段级设计在 4.2-4.8 节，实施时逐表对照）

## Global Constraints

- **构建命令必须显式指定 JDK 1.8**（本机 JAVA_HOME 默认 17，Boot 2.5 不支持）：Git Bash 下 `JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn ...`
- 版本锁定：Spring Boot 2.5.0、MyBatis-Plus 3.4.2、jjwt 0.9.0、springfox 2.9.2、fastjson 1.2.33、druid-spring-boot-starter 1.2.4、Java 1.8；**不擅自升级**
- 端口：后端 8888（接口路径**不带** `/api` 前缀，前端代理负责）、前端 8082、MySQL 3306(root/root)、Redis 6379
- 后端接口一律返回 `cn.ecom.utils.ResponseResult<T>`；错误码一律用 `AppHttpCodeEnum`，禁止硬编码数字
- 分页参数 `pageNum`/`pageSize`；RESTful 路径规范照 spec 2.1（`GET/POST/PUT/DELETE`）
- 数据库：表前缀 `sys_`/`ec_`，每表必备 `id`(bigint 自增)/`create_time`/`update_time`/`del_flag`，逻辑删除，金额 `decimal(12,2)`，utf8mb4；所有变更同步 `sql/ecom.sql`
- **禁用**：RabbitMQ、未在技术栈列出的第三方依赖（lombok 例外——seig 全面使用，属于代码风格基线）
- Git commit message 用中文，格式 `feat(模块名): 做了什么`
- 本机为中国大陆网络：Maven 已配阿里云镜像、npm 已配腾讯镜像，直接使用即可

---

### Task 1: 项目初始化（git + CLAUDE.md + .gitignore + 参考-seig 拷贝）

**Files:**
- Create: `D:\webxiangmu\ecom\CLAUDE.md`
- Create: `D:\webxiangmu\ecom\.gitignore`
- Copy: 桌面 seig 的 `seig-common`、`seig-system`、`seig-ui/src`、`pom.xml`、`sql` → `D:\webxiangmu\ecom\参考-seig\`
- 已存在：`docs/`（需求与设计文档，随本任务首次提交）

**Interfaces:**
- Produces: 项目根目录结构、git 仓库、参考项目只读副本（后续所有任务依赖）

- [ ] **Step 1: git init 并确认用户信息**

```bash
cd "D:/webxiangmu/ecom" && git init && git config user.name && git config user.email
```
Expected: 显示已配置的用户名/邮箱（若无输出，停下来问用户配置，不要擅自改 git config）

- [ ] **Step 2: 写 .gitignore**

内容：
```gitignore
# --- Java / Maven ---
target/
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar

# --- Node ---
node_modules/
dist/
npm-debug.log*

# --- IDE ---
.idea/
*.iml
.vscode/

# --- OS ---
.DS_Store
Thumbs.db

# --- 参考项目（本地只读参考，不纳入版本库）---
参考-seig/

# --- 日志与上传 ---
logs/
*.log
```

- [ ] **Step 3: 写 CLAUDE.md（项目宪法）**

内容（取自用户需求手册「一、CLAUDE.md 项目宪法」，已适配本机）：
```markdown
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
- MySQL 8（库名 ecom，字符集 utf8mb4）
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
- 本地 MySQL：localhost:3306，用户 root，密码 root
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
```

- [ ] **Step 4: 拷贝参考项目**

```bash
SEIG="C:/Users/奥利奥/Desktop/web实训/实验1/seig-master/seig-master/seig"
cd "D:/webxiangmu/ecom" && mkdir -p 参考-seig
cp -r "$SEIG/seig-common" 参考-seig/
cp -r "$SEIG/seig-system" 参考-seig/
cp -r "$SEIG/seig-ui/src" 参考-seig/ui-src
cp "$SEIG/pom.xml" 参考-seig/
cp -r "$SEIG/sql" 参考-seig/
# 清掉可能拷进来的 target（体积大且无意义）
find 参考-seig -name target -type d -prune -exec rm -rf {} + 2>/dev/null; true
```

- [ ] **Step 5: 验证拷贝结果**

```bash
ls 参考-seig && ls 参考-seig/sql && ls 参考-seig/ui-src | head -5
```
Expected: 看到 pom.xml、seig-common、seig-system、sql、ui-src；sql 下有 graduation.sql

- [ ] **Step 6: 首次提交**

```bash
cd "D:/webxiangmu/ecom" && git add -A && git status --short | head -20
git commit -m "feat(P0): 项目初始化（CLAUDE.md、gitignore、需求拆分与设计文档）"
```
Expected: commit 成功；`参考-seig/` 不在提交列表（被 .gitignore 排除）

---

### Task 2: 后端多模块骨架（3 个 pom）

**Files:**
- Create: `pom.xml`（父）
- Create: `ecom-common/pom.xml`
- Create: `ecom-system/pom.xml`

**Interfaces:**
- Produces: `cn.ecom:ecom:1.0-SNAPSHOT`（父）、模块 `ecom-common`、`ecom-system`；后续任务的类全部放进这两个模块

- [ ] **Step 1: 验证 JDK8 构建环境**

```bash
JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn -version
```
Expected: `Java version: 1.8.0_xxx`（若不是 1.8，先排查再继续）

- [ ] **Step 2: 写父 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>cn.ecom</groupId>
    <artifactId>ecom</artifactId>
    <packaging>pom</packaging>
    <version>1.0-SNAPSHOT</version>
    <modules>
        <module>ecom-common</module>
        <module>ecom-system</module>
    </modules>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>1.8</java.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- SpringBoot 依赖配置（BOM 方式，与 seig 一致） -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>2.5.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- fastjson -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>fastjson</artifactId>
                <version>1.2.33</version>
            </dependency>
            <!-- jwt -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt</artifactId>
                <version>0.9.0</version>
            </dependency>
            <!-- mybatisPlus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>3.4.2</version>
            </dependency>
            <!-- swagger -->
            <dependency>
                <groupId>io.springfox</groupId>
                <artifactId>springfox-swagger2</artifactId>
                <version>2.9.2</version>
            </dependency>
            <dependency>
                <groupId>io.springfox</groupId>
                <artifactId>springfox-swagger-ui</artifactId>
                <version>2.9.2</version>
            </dependency>
            <!-- Druid 数据源 -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-starter</artifactId>
                <version>1.2.4</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.1</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

说明：jjwt/spring-security-crypto 等版本由 Boot BOM 管理；jjwt 0.9.0 显式声明与 seig 一致。

- [ ] **Step 3: 写 ecom-common/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ecom</artifactId>
        <groupId>cn.ecom</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecom-common</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- lombok（与 seig 代码风格一致） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- junit -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <!-- fastjson -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
        </dependency>
        <!-- jwt（版本由父 pom 锁定 0.9.0） -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt</artifactId>
        </dependency>
        <!-- mybatisPlus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        <!-- mysql 驱动（版本由 Boot BOM 管理） -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        <!-- springfox -->
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger2</artifactId>
        </dependency>
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger-ui</artifactId>
        </dependency>
        <!-- Druid 数据源 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
        </dependency>
        <!-- BCrypt 密码加密（仅 crypto，不引入 starter-security 自动配置；P1 再上完整安全框架） -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
    </dependencies>
</project>
```

说明：Redis/JWT 拦截器/Security 全套在 P1 引入；MinIO 在 P2；easyexcel/hanlp/hutool/mail/RabbitMQ 一律不引入。

- [ ] **Step 4: 写 ecom-system/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ecom</artifactId>
        <groupId>cn.ecom</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecom-system</artifactId>

    <dependencies>
        <dependency>
            <groupId>cn.ecom</groupId>
            <artifactId>ecom-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

    <build>
        <finalName>ecom-system</finalName>
        <plugins>
            <!-- 可执行 jar（P8 部署要用） -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 5: 构建验证**

```bash
cd "D:/webxiangmu/ecom" && JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn clean install -q
```
Expected: `BUILD SUCCESS`（首次会下载依赖，耐心等待；若 maven-compiler-plugin 3.1 报错则升为 3.8.1 并在 CLAUDE.md 记录）

- [ ] **Step 6: 提交**

```bash
cd "D:/webxiangmu/ecom" && git add pom.xml ecom-common/pom.xml ecom-system/pom.xml
git commit -m "feat(P0): 多模块 Maven 骨架（父pom + ecom-common + ecom-system）"
```

---

### Task 3: ecom-common 基础件

**Files:**
- Create: `ecom-common/src/main/java/cn/ecom/utils/ResponseResult.java`
- Create: `ecom-common/src/main/java/cn/ecom/utils/AppHttpCodeEnum.java`
- Create: `ecom-common/src/main/java/cn/ecom/handler/exception/BusinessException.java`
- Create: `ecom-common/src/main/java/cn/ecom/handler/GlobalExceptionHandler.java`
- Create: `ecom-common/src/main/java/cn/ecom/config/MybatisPlusConfig.java`
- Create: `ecom-common/src/main/java/cn/ecom/config/WebConfig.java`
- Create: `ecom-common/src/main/java/cn/ecom/config/SwaggerConfig.java`

**Interfaces:**
- Produces（后续任务与所有阶段依赖）:
  - `ResponseResult<T>`：`okResult(T data)` / `errorResult(int code, String msg)` / `errorResult(AppHttpCodeEnum e)` / `errorResult(AppHttpCodeEnum e, String msg)`
  - `AppHttpCodeEnum`：`SUCCESS(200)` / `NEED_LOGIN(401)` / `NO_OPERATION_AUTHORITY(403)` / `SYSTEM_ERROR(500)` / `PARAMETER_ERROR(400)` 等，后续阶段在此追加
  - `BusinessException(Integer code, String message)` 供 service 层抛出业务错误
  - `MybatisPlusConfig`（含 `@MapperScan("cn.ecom.mapper")` 与分页插件）、`WebConfig`（跨域）、`SwaggerConfig`（扫描 cn.ecom.controller）

- [ ] **Step 1: 写 ResponseResult.java**

```java
package cn.ecom.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体：所有接口的返回类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;

    public ResponseResult(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static <T> ResponseResult<T> okResult() {
        return new ResponseResult<>(AppHttpCodeEnum.SUCCESS.getCode(), AppHttpCodeEnum.SUCCESS.getMsg());
    }

    public static <T> ResponseResult<T> okResult(T data) {
        ResponseResult<T> result = okResult();
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> errorResult(int code, String msg) {
        return new ResponseResult<>(code, msg);
    }

    public static <T> ResponseResult<T> errorResult(AppHttpCodeEnum httpCodeEnum) {
        return new ResponseResult<>(httpCodeEnum.getCode(), httpCodeEnum.getMsg());
    }

    public static <T> ResponseResult<T> errorResult(AppHttpCodeEnum httpCodeEnum, String msg) {
        return new ResponseResult<>(httpCodeEnum.getCode(), msg);
    }
}
```

- [ ] **Step 2: 写 AppHttpCodeEnum.java**

```java
package cn.ecom.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码。编号段：1xxx 系统/用户，2xxx 商品，3xxx 交易，4xxx 文件
 */
@Getter
@AllArgsConstructor
public enum AppHttpCodeEnum {

    SUCCESS(200, "操作成功"),
    PARAMETER_ERROR(400, "参数错误"),
    NEED_LOGIN(401, "需要登录后操作"),
    NO_OPERATION_AUTHORITY(403, "无操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    SYSTEM_ERROR(500, "系统异常"),

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USERNAME_EXIST(1002, "用户名已存在"),
    USER_NOT_EXIST(1003, "用户不存在"),
    ROLE_NOT_EXIST(1004, "角色不存在"),
    ACCOUNT_DISABLED(1005, "账号已停用"),
    OLD_PASSWORD_ERROR(1006, "原密码错误"),

    CATEGORY_LEVEL_LIMIT(2001, "类目最多三级"),
    CATEGORY_HAS_PRODUCT(2002, "类目下存在商品，不允许删除"),
    CATEGORY_HAS_CHILD(2003, "类目下存在子类目，不允许删除"),
    PRODUCT_NOT_EXIST(2004, "商品不存在"),
    PRODUCT_OFF_SHELF(2005, "商品已下架"),
    SKU_NOT_EXIST(2006, "规格不存在"),

    STOCK_NOT_ENOUGH(3001, "库存不足"),
    CART_EMPTY(3002, "购物车没有选中商品"),
    ORDER_NOT_EXIST(3003, "订单不存在"),
    ORDER_STATUS_ERROR(3004, "当前订单状态不允许该操作"),
    REPEAT_SUBMIT(3005, "请勿重复提交"),
    COUPON_INVALID(3006, "优惠券不可用"),
    COUPON_RECEIVED_LIMIT(3007, "已达到领取上限"),

    FILE_UPLOAD_ERROR(4001, "文件上传失败");

    private final Integer code;
    private final String msg;
}
```

- [ ] **Step 3: 写 BusinessException.java（handler/exception 包）**

```java
package cn.ecom.handler.exception;

import cn.ecom.utils.AppHttpCodeEnum;
import lombok.Getter;

/**
 * 业务异常：service 层对可预期的业务错误抛出，由全局异常处理器转成 ResponseResult
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(AppHttpCodeEnum httpCodeEnum) {
        super(httpCodeEnum.getMsg());
        this.code = httpCodeEnum.getCode();
    }

    public BusinessException(AppHttpCodeEnum httpCodeEnum, String message) {
        super(message);
        this.code = httpCodeEnum.getCode();
    }
}
```

- [ ] **Step 4: 写 GlobalExceptionHandler.java**

```java
package cn.ecom.handler;

import cn.ecom.handler.exception.BusinessException;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ResponseResult.errorResult(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_ERROR, e.getMessage());
    }
}
```

- [ ] **Step 5: 写 MybatisPlusConfig.java**

```java
package cn.ecom.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("cn.ecom.mapper")
public class MybatisPlusConfig {

    /** 分页插件 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 6: 写 WebConfig.java（跨域，开发期全放开）**

```java
package cn.ecom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 7: 写 SwaggerConfig.java**

```java
package cn.ecom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("cn.ecom.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("电商智能系统接口文档")
                .description("ecom API（Spring Boot 2.5 + MyBatis-Plus）")
                .version("1.0")
                .build();
    }
}
```

- [ ] **Step 8: 构建验证**

```bash
cd "D:/webxiangmu/ecom" && JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn clean install -q
```
Expected: `BUILD SUCCESS`

- [ ] **Step 9: 提交**

```bash
cd "D:/webxiangmu/ecom" && git add ecom-common/src
git commit -m "feat(P0): ecom-common 基础件（统一响应/错误码/异常处理/MP分页/跨域/Swagger）"
```

---

### Task 4: 数据库脚本 sql/ecom.sql（30 张表 + 初始数据）并建入本地 MySQL

**Files:**
- Create: `sql/ecom.sql`
- Create（临时，用完删除）: `ecom-common/src/test/java/cn/ecom/BCryptHashGenTest.java`

**Interfaces:**
- Consumes: 设计文档 4.2-4.8 节的 30 张表设计（字段级）
- Produces: 本地 MySQL 库 `ecom`（30 张表 + 初始数据），Task 5 的后端启动依赖此库

- [ ] **Step 1: 生成 admin 密码 BCrypt 哈希**

先写临时测试：
```java
package cn.ecom;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptHashGenTest {

    /** 一次性工具：生成 admin/admin123 的 BCrypt 哈希，写入 sql/ecom.sql 后删除本文件 */
    @Test
    void genAdminHash() {
        System.out.println("BCRYPT_HASH_START>" + new BCryptPasswordEncoder().encode("admin123") + "<BCRYPT_HASH_END");
    }
}
```
运行：
```bash
cd "D:/webxiangmu/ecom" && JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn -pl ecom-common test -Dtest=BCryptHashGenTest -q
```
Expected: 输出中包含 `BCRYPT_HASH_START>$2a$10$....<BCRYPT_HASH_END`，记下完整哈希（下一步写入 SQL，随后删除该测试文件）

- [ ] **Step 2: 写 sql/ecom.sql — 建库 + 30 张表 DDL**

要求（对照设计文档 4.2-4.8 逐表写全，风格参考 `参考-seig/sql/graduation.sql`：COMMENT + 索引 + 字符集）：

1. 头部：
```sql
-- ============================================================
-- 电商智能系统（ecom）全量数据库脚本（唯一权威，随开发持续维护）
-- MySQL 8 / utf8mb4；每表必备 id/create_time/update_time/del_flag
-- 表清单：sys_* 5 张（用户/角色/菜单/用户角色/角色菜单）
--        ec_* 25 张（商品5 交易6 售后3 供应链3 统计2 智能与预留6）
-- ============================================================
DROP DATABASE IF EXISTS `ecom`;   -- 仅初始化脚本允许；生产改为迁移脚本
CREATE DATABASE `ecom` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `ecom`;
```

2. 建表顺序（外键/依赖约束，先系统后业务）：`sys_user → sys_role → sys_menu → sys_user_role → sys_role_menu → ec_category → ec_product → ec_product_spec → ec_sku → ec_product_image → ec_cart → ec_address → ec_coupon → ec_user_coupon → ec_order → ec_order_item → ec_order_status_log → ec_logistics → ec_refund → ec_stock_alert → ec_sales_forecast → ec_sys_config → ec_stat_daily → ec_product_sales_daily → ec_knowledge → ec_chat_log → ec_search_synonym → ec_search_log → ec_review → ec_user_behavior`

3. 每张表的统一收尾字段（所有表都要有）：
```sql
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  -- ... 业务字段 ...
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'xxx表';
```

4. 关键表的样板（照此风格写其余表）。**ec_sku 示例**（含乐观锁 version 与库存，设计文档 D2）：

```sql
DROP TABLE IF EXISTS `ec_sku`;
CREATE TABLE `ec_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品SPU id',
  `specs` varchar(500) NOT NULL COMMENT '规格组合JSON，如 {"颜色":"红","尺码":"S"}',
  `sku_code` varchar(64) DEFAULT NULL COMMENT 'SKU编码',
  `price` decimal(12,2) NOT NULL COMMENT '售价',
  `stock` int NOT NULL DEFAULT 0 COMMENT '库存',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `image` varchar(500) DEFAULT NULL COMMENT 'SKU图',
  `sales` int NOT NULL DEFAULT 0 COMMENT '销量',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(0停用 1启用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品SKU表（含库存）';
```

**sys_user 示例**（单表双端用户，设计文档 D1）：
```sql
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录名',
  `nick_name` varchar(50) NOT NULL COMMENT '昵称/姓名',
  `password` varchar(255) NOT NULL COMMENT '密码(BCrypt)',
  `user_type` tinyint NOT NULL DEFAULT 0 COMMENT '用户类型(0运营 1消费者)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0正常 1停用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表（运营+消费者）';
```

其余 28 张表按设计文档字段清单写全，重点标注：
- `ec_order`：唯一键 `uk_user_idem (user_id, idempotency_key)`、唯一键 `uk_order_no (order_no)`、索引 `(status, create_time)`
- `ec_category`：`parent_id`(0 根) / `level`(1-3) / `sort`
- `ec_product`：`price_min`/`price_max` 冗余自 SKU；`status`(0下架 1上架)
- `ec_cart`：唯一键 `(user_id, sku_id)`
- `ec_stock_alert`：索引 `(sku_id, alert_type, status)`（防重复推送）
- `ec_sales_forecast`：唯一键 `(sku_id, forecast_date)`
- `ec_user_coupon`：`status`(0未使用 1已使用 2已过期)
- `ec_order_status_log` / `ec_chat_log` / `ec_search_log` / `ec_user_behavior` / `ec_stat_daily` / `ec_product_sales_daily`：追加型表，仍保留 del_flag 以符合规范

- [ ] **Step 3: 写初始数据段**

```sql
-- ============================================================
-- 初始数据
-- ============================================================
-- 1) 角色（5 个）
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `status`) VALUES
(1, '超级管理员', 'super_admin', 1, 0),
(2, '商品运营', 'product_op', 2, 0),
(3, '活动运营', 'market_op', 3, 0),
(4, '客服主管', 'cs_op', 4, 0),
(5, '消费者', 'member', 5, 0);

-- 2) 菜单树（P0/P1 范围：首页 + 系统管理；后续阶段随页面增量追加）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `url_path`, `component`, `permit`, `icon`, `order_num`, `visible`, `status`) VALUES
(1,  0,  '首页',     'M', '/home',        'home',       'home',                 'iconfont icon-home-full', 1, 0, 0),
(2,  0,  '系统管理', 'M', '/sys',         'sys',        'sys',                  'iconfont icon-xitong',    9, 0, 0),
(18, 2,  '用户管理', 'C', '/sys/user',    'sysUser',    'sys:user',             'iconfont icon-yonghu',    21, 0, 0),
(19, 2,  '角色管理', 'C', '/sys/role',    'sysRole',    'sys:role',             'iconfont icon-jiaoseguanli', 22, 0, 0),
-- 用户管理按钮
(20, 18, '搜索用户', 'F', '/sys/user/find',     'sysUserFind',   'sys:user:find',   NULL, 0, 1, 0),
(21, 18, '新增用户', 'F', '/sys/user/add',      'sysUserAdd',    'sys:user:add',    NULL, 0, 1, 0),
(22, 18, '修改用户', 'F', '/sys/user/edit',     'sysUserEdit',   'sys:user:edit',   NULL, 0, 1, 0),
(23, 18, '删除用户', 'F', '/sys/user/del',      'sysUserDel',    'sys:user:del',    NULL, 0, 1, 0),
(24, 18, '重置密码', 'F', '/sys/user/respwd',   'sysUserRespwd', 'sys:user:respwd', NULL, 0, 1, 0),
-- 角色管理按钮
(25, 19, '新增角色', 'F', '/sys/role/add',      'sysRoleAdd',    'sys:role:add',    NULL, 0, 1, 0),
(26, 19, '修改角色', 'F', '/sys/role/edit',     'sysRoleEdit',   'sys:role:edit',   NULL, 0, 1, 0),
(27, 19, '删除角色', 'F', '/sys/role/del',      'sysRoleDel',    'sys:role:del',    NULL, 0, 1, 0),
(28, 19, '获取权限', 'F', '/sys/role/getMenu',  'sysRoleGetMenu','sys:role:getMenu',NULL, 0, 1, 0),
(29, 19, '分配权限', 'F', '/sys/role/setMenu',  'sysRoleSetMenu','sys:role:setMenu',NULL, 0, 1, 0),
(30, 1,  '修改密码', 'F', '/home/password/change', 'homePasswordChange', 'home:password:change', NULL, 0, 1, 0);

-- 3) 超管账号（密码 admin123 的 BCrypt 哈希，替换为 Step 1 生成值）
INSERT INTO `sys_user` (`id`, `username`, `nick_name`, `password`, `user_type`, `status`) VALUES
(1, 'admin', '超级管理员', '$BCRYPT_HASH_HERE$', 0, 0);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 超管拥有全部菜单（P1 也可在代码层对 super_admin 直通，双保险）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1),(1, 2),(1, 18),(1, 19),(1, 20),(1, 21),(1, 22),(1, 23),(1, 24),(1, 25),(1, 26),(1, 27),(1, 28),(1, 29),(1, 30);

-- 4) 系统参数（规则全部配置化，禁止写死代码）
INSERT INTO `ec_sys_config` (`config_key`, `config_name`, `config_value`, `remark`) VALUES
('order_timeout_minutes',      '订单支付超时（分钟）',   '15',  '下单后未支付自动取消并释放库存'),
('stock_alert_low_default',    '缺货预警阈值（默认）',   '10',  '库存低于该值触发缺货预警'),
('stock_alert_high_default',   '积压预警阈值（默认）',   '1000','库存高于该值触发积压预警'),
('forecast_weight_recent',     '预测权重-近7天',        '0.5', '加权移动平均权重'),
('forecast_weight_mid',        '预测权重-8到30天',      '0.3', '加权移动平均权重'),
('forecast_weight_old',        '预测权重-31到90天',     '0.2', '加权移动平均权重'),
('chat_confidence_threshold',  '客服自动回复置信度',     '0.85','低于该值转人工'),
('refund_stock_rollback',      '退款是否回滚库存',       '1',   '1=回滚 0=不回滚');

-- 5) 演示优惠券（便于 P3 联调）
INSERT INTO `ec_coupon` (`name`, `type`, `threshold_amount`, `discount_amount`, `total_count`, `received_count`, `per_user_limit`, `start_time`, `end_time`, `status`) VALUES
('新客满100减10', 0, 100.00, 10.00, 1000, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 0),
('满200减30',     0, 200.00, 30.00, 1000, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 0);
```

注意：`$BCRYPT_HASH_HERE$` 必须替换为 Step 1 生成的真实哈希；完成后删除 `BCryptHashGenTest.java`。

- [ ] **Step 4: 定位 mysql 客户端并执行脚本**

```bash
which mysql || ls "C:/Program Files/MySQL/MySQL Server 8.0/bin/" | head
MYSQL="mysql"   # 若 which 无结果，改用 MYSQL="C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql"
"$MYSQL" -uroot -proot --default-character-set=utf8mb4 < "D:/webxiangmu/ecom/sql/ecom.sql"
```
Expected: 无报错（有 warning 可忽略）

- [ ] **Step 5: 验证建库结果**

```bash
"$MYSQL" -uroot -proot -e "USE ecom; SHOW TABLES;" | tail -n +2 | wc -l
"$MYSQL" -uroot -proot -e "USE ecom; SELECT id,role_key FROM sys_role; SELECT username,user_type FROM sys_user; SELECT COUNT(*) AS menu_cnt FROM sys_menu; SELECT config_key,config_value FROM ec_sys_config;"
```
Expected: 表数 = **30**；角色 5 行；admin 1 行；菜单 15 行；系统参数 8 行

- [ ] **Step 6: 删除临时测试文件并提交**

```bash
cd "D:/webxiangmu/ecom" && rm ecom-common/src/test/java/cn/ecom/BCryptHashGenTest.java
git add sql/ecom.sql ecom-common/src && git status --short
git commit -m "feat(P0): 全量数据库脚本（30表DDL+初始数据）并建入本地MySQL"
```

---

### Task 5: ecom-system 启动类 + application.yml + /health

**Files:**
- Create: `ecom-system/src/main/java/cn/ecom/EcomApplication.java`
- Create: `ecom-system/src/main/resources/application.yml`
- Create: `ecom-system/src/main/java/cn/ecom/controller/HealthController.java`

**Interfaces:**
- Consumes: Task 3 的 `ResponseResult`；Task 4 的 `ecom` 库
- Produces: 可启动的 `EcomApplication`（端口 8888）；`GET /health` → `{"code":200,"msg":"操作成功","data":"ok"}`

- [ ] **Step 1: 写 EcomApplication.java**

```java
package cn.ecom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EcomApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcomApplication.class, args);
    }
}
```

- [ ] **Step 2: 写 application.yml**

```yaml
server:
  port: 8888

spring:
  application:
    name: ecom
  datasource:
    druid:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/ecom?characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: root
      password: root
  mvc:
    # springfox 2.9.2 与 Boot 2.5 的兼容配置（不配会启动报错）
    pathmatch:
      matching-strategy: ant_path_matcher

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: cn.ecom.domain.entity
  configuration:
    map-underscore-to-camel-case: true
    # 开发期打印 SQL（P8 收尾时移除）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    banner: false
    db-config:
      id-type: auto
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
```

- [ ] **Step 3: 写 HealthController.java**

```java
package cn.ecom.controller;

import cn.ecom.utils.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /** 前后端连通验证用，P1 之后保留（运维健康检查） */
    @GetMapping("/health")
    public ResponseResult<String> health() {
        return ResponseResult.okResult("ok");
    }
}
```

- [ ] **Step 4: 打包并启动验证**

```bash
cd "D:/webxiangmu/ecom" && JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn clean install -q
cd "D:/webxiangmu/ecom/ecom-system/target" && JAVA_HOME="/c/Program Files/Java/jdk-1.8" "/c/Program Files/Java/jdk-1.8/bin/java" -jar ecom-system.jar
```
（用 run_in_background 方式启动，日志出现 `Started EcomApplication` 后执行下一步）

- [ ] **Step 5: curl 验证接口**

```bash
curl -s http://localhost:8888/health
```
Expected: `{"code":200,"msg":"操作成功","data":"ok"}`

同时验证 Swagger 启动无 NPE（说明 ant_path_matcher 生效）：
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/v2/api-docs
```
Expected: `200`

- [ ] **Step 6: 停止进程并提交**

```bash
cd "D:/webxiangmu/ecom" && git add ecom-system/src
git commit -m "feat(P0): ecom-system 启动类与配置（8888端口、Druid、MP、/health）"
```

---

### Task 6: 前端 ecom-ui 工程（Vue CLI 5 + 代理 + 连通页）

**Files:**
- Create（脚手架生成）: `ecom-ui/`（整个目录）
- Create: `ecom-ui/vue.config.js`、`ecom-ui/src/util/request.js`
- Modify: `ecom-ui/src/main.js`、`ecom-ui/src/App.vue`
- Delete: 脚手架默认的 `src/components/HelloWorld.vue` 等演示文件

**Interfaces:**
- Consumes: 后端 `/health`（经代理 `/api/health`）
- Produces: `src/util/request.js`（axios 实例，baseURL `/api`，响应拦截直接返回 `response.data`）——**P1 及所有阶段的请求层**

- [ ] **Step 1: 创建 Vue3 工程**

```bash
cd "D:/webxiangmu/ecom" && npx @vue/cli@5.0.8 create ecom-ui -d -n --packageManager npm
```
（`-d` 默认预设 Vue3+babel+eslint，`-n` 跳过 git init；若已生成 `ecom-ui/.git` 则 `rm -rf ecom-ui/.git`）
若 Node 24 下 CLI 报错无法创建：改为手工脚手架（写 package.json + 目录结构后 `npm install`），并在 CLAUDE.md 记录原因。

- [ ] **Step 2: 安装依赖**

```bash
cd "D:/webxiangmu/ecom/ecom-ui" && npm i element-plus pinia pinia-plugin-persistedstate axios vue-router@4 echarts nprogress && npm i -D sass sass-loader
```

- [ ] **Step 3: 写 vue.config.js**

```js
const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    port: 8082,
    proxy: {
      '/api': {
        target: 'http://localhost:8888',
        changeOrigin: true,
        pathRewrite: { '^/api': '' }
      }
    }
  }
})
```

- [ ] **Step 4: 写 src/util/request.js**

```js
import axios from 'axios'

// 统一请求实例：/api 由 devServer 代理到后端 8888（重写去掉 /api 前缀）
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export default request
```

- [ ] **Step 5: 重写 src/main.js**

```js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

createApp(App)
  .use(ElementPlus)
  .use(pinia)
  .mount('#app')
```

- [ ] **Step 6: 重写 src/App.vue（P0 连通验证页，P1 会被登录页/主框架替换）**

```vue
<template>
  <div class="health-page">
    <h1>电商智能系统（ecom）</h1>
    <p>P0 前后端连通验证</p>
    <el-button type="primary" @click="checkHealth">调用后端 /health</el-button>
    <p class="result">后端返回：{{ result }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from './util/request'

const result = ref('未调用')

const checkHealth = async () => {
  try {
    result.value = JSON.stringify(await request.get('/health'))
  } catch (e) {
    result.value = '请求失败：' + e.message
  }
}

onMounted(checkHealth)
</script>

<style>
.health-page { padding: 60px; text-align: center; }
.result { margin-top: 20px; color: #409eff; }
</style>
```

- [ ] **Step 7: 清理脚手架演示文件**

```bash
cd "D:/webxiangmu/ecom/ecom-ui" && rm -f src/components/HelloWorld.vue && ls src src/components 2>/dev/null; true
```

- [ ] **Step 8: 启动验证（需后端已在 8888 运行）**

```bash
cd "D:/webxiangmu/ecom/ecom-ui" && npm run serve
```
（run_in_background 启动；编译成功后：）
```bash
curl -s http://localhost:8082/ | grep -o "<title>[^<]*</title>"
curl -s http://localhost:8082/api/health
```
Expected: 返回 `<title>ecom-ui</title>`（或 App 标题）；`/api/health` 返回 `{"code":200,...,"data":"ok"}`（证明代理→后端链路通）

- [ ] **Step 9: 提交**

```bash
cd "D:/webxiangmu/ecom" && git add ecom-ui && git commit -m "feat(P0): ecom-ui 前端工程（Vue3+ElementPlus+Pinia，/api代理8082，连通验证页）"
```

---

### Task 7: 端到端验收 + 收尾

**Files:**
- Modify: `CLAUDE.md`（仅在验收中发现遗漏时补充）
- Modify: `docs/superpowers/specs/2026-09-15-ecom-foundation-design.md`（回填实际差异，如有）

**Interfaces:**
- Consumes: 前 6 个任务全部产物
- Produces: 通过 md 验收清单的 P0 交付；给用户的启动/验证说明

- [ ] **Step 1: 全量构建**

```bash
cd "D:/webxiangmu/ecom" && JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn clean install
```
Expected: `BUILD SUCCESS`

- [ ] **Step 2: 按 md P0 验收清单逐项验证**

| 验收项 | 命令 | Expected |
|---|---|---|
| 后端启动无报错 | 启动 EcomApplication，看日志 | `Started EcomApplication`，无 ERROR |
| /health 返回 JSON | `curl -s http://localhost:8888/health` | `{"code":200,"msg":"操作成功","data":"ok"}` |
| 前端启动 | `npm run serve` 后 `curl -s http://localhost:8082/` | 返回 HTML |
| 前端调到后端 | `curl -s http://localhost:8082/api/health` | 返回同上 JSON |
| 数据库 | `mysql -uroot -proot -e "USE ecom; SHOW TABLES;" \| wc` | 30 张表 |

- [ ] **Step 3: 请用户浏览器人工确认**

提示用户：打开 `http://localhost:8082`，应看到「电商智能系统（ecom）」页面与后端返回的 JSON（自动化只能验证到 curl 层）。

- [ ] **Step 4: 最终提交**

```bash
cd "D:/webxiangmu/ecom" && git add -A && git status --short
git commit -m "feat(P0): 端到端验收通过（后端8888/前端8082/数据库30表）"
```

- [ ] **Step 5: 向用户汇报**

汇报内容：启动顺序（MySQL→Redis→后端→前端）、验证方式、IDEA 需设 JDK1.8 的提醒、下一步（P1 鉴权与权限管理）。

---

## 计划自审记录

- **Spec 覆盖**：设计文档 §5 的 6 项交付物 → Task 1（CLAUDE.md/git/参考-seig）、Task 2-3（骨架+基础件）、Task 4（30 表 sql+建库）、Task 5（8888+/health）、Task 6（ecom-ui+代理）、Task 7（验收）。无遗漏。
- **占位符扫描**：无 TBD/TODO；`$BCRYPT_HASH_HERE$` 是 Task 4 Step 1 必须真实生成后替换的值（有明确生成步骤与验证）。28 张表 DDL 未逐行内联——由设计文档 4.2-4.8 字段清单 + 两张样板表 + seig 风格参考三段精确定义，执行者（同一会话）按此展开。
- **类型一致性**：`ResponseResult.okResult/errorResult`、`AppHttpCodeEnum`、`BusinessException` 的签名在 Task 3 定义、Task 5 使用，一致；前端 `request.get('/health')` 与后端 `/health`（代理去 `/api`）对应。
