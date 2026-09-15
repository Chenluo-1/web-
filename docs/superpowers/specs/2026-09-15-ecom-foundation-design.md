# 电商智能系统（ecom）· 基础设计文档

> 日期：2026-09-15 ｜ 范围：P0 工程骨架 + 全量数据库设计（P1-P8 表结构一次设计到位）
> 状态：已获用户确认，进入实施

## 1. 范围与目标

**本期交付**：

1. 需求拆分与模块分类文档（已交付：`docs/需求拆分与模块分类.md`）。
2. P0 工程骨架：多模块 Maven（父 pom + ecom-common + ecom-system）+ Vue3 前端 ecom-ui + `/health` 连通验证。
3. 全量数据库设计：30 张表 DDL + 初始数据，写入 `sql/ecom.sql` 并建入本地 MySQL（库名 `ecom`）。

**不在本期范围**：任何业务功能（P1 及以后）、MinIO 接入（P2 才需要）、演示数据（P8 的 `sql/data.sql`）。

## 2. 总体架构

### 2.1 目录结构

```
D:\webxiangmu\ecom\
├── CLAUDE.md                  # 项目宪法（技术栈/目录规范/代码约定）
├── pom.xml                    # 父 POM（dependencyManagement + 编译插件）
├── ecom-common\               # 通用层（jar）
│   └── src/main/java/cn/ecom\
│       ├── domain\{entity,dto,vo}\
│       ├── mapper\
│       ├── config\            # MybatisPlusConfig 等
│       ├── handler\           # GlobalExceptionHandler
│       └── utils\             # ResponseResult / AppHttpCodeEnum
├── ecom-system\               # 业务层（可执行 jar，含启动类）
│   └── src/main\
│       ├── java/cn/ecom\{controller,service\impl,config,filter}\
│       │   └── EcomApplication.java
│       └── resources\{application.yml,mapper\}
├── ecom-ui\                   # 前端 Vue3（Vue CLI 5，非 Maven 模块）
├── sql\ecom.sql               # 全量建库建表 + 初始数据（唯一权威脚本）
├── 参考-seig\                 # seig 参考项目副本（只读，不参与构建）
└── docs\                      # 需求/设计文档
```

### 2.2 技术栈（严格对齐，不擅自升级）

| 层 | 技术 | 版本 |
|---|---|---|
| 构建 | JDK / Maven 多模块 | 1.8 / 3.9.x |
| 框架 | Spring Boot（BOM 导入方式，非 parent） | 2.5.0 |
| 安全 | Spring Security + JWT (jjwt) + Redis | jjwt 0.9.0 |
| ORM | MyBatis-Plus（mapper xml 在 `classpath*:/mapper/**/*.xml`，主键 `id-type: auto`） | 3.4.2 |
| 连接池 | Druid | 对齐 seig |
| 数据库 | MySQL（`ecom`，utf8mb4） | 本机 5.7.28，DDL 兼容 5.x/8.x |
| 文件存储 | MinIO（P2 接入） | — |
| 接口文档 | springfox-swagger2 / swagger-ui | 2.9.2 |
| 前端 | Vue + Element Plus + Pinia + Router4 + Axios + ECharts + nprogress + sass | Vue 3.2 / CLI 5 |

### 2.3 端口与环境

| 项 | 值 |
|---|---|
| 后端 | 8888（接口无 `/api` 前缀，由前端代理负责） |
| 前端 dev | 8082（`vue.config.js` 将 `/api` 代理到 8888） |
| MySQL | localhost:3306，root/123456（MySQL 5.7.28，客户端在 `C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe`） |
| Redis | localhost:6379 |
| MinIO | localhost:9000（P2 前无需启动），minioadmin/minioadmin |
| 上传目录（备用） | D:\ecom\ |

## 3. 关键技术决策

| # | 决策 | 方案 | 理由 |
|---|------|------|------|
| D1 | 用户体系 | **单表 `sys_user` + `user_type`**（0 运营 / 1 消费者），消费者注册自动赋 `member` 角色；登录/JWT/权限全套复用 | 复用 P1 全部基建；运营/消费者在用户管理按 type 筛选。备选（双表 sys_user + ec_member）放弃：登录态与权限需两套，课设收益为负 |
| D2 | 库存扣减 | **下单即原子扣减**：`UPDATE ec_sku SET stock=stock-#{n}, version=version+1 WHERE id=#{id} AND stock>=#{n}`；超时/取消/退款回补；支付回调只改订单状态 | 天然满足全部验收点：超时释放、回调幂等（不碰库存）、不超卖；与 md 中「合并 SKU 与库存表 + 乐观锁版本号」的决策一致 |
| D3 | 订单幂等 | `ec_order.idempotency_key` + `(user_id, idempotency_key)` 唯一索引；前端提交时生成 UUID | 快速双击/网络重试仅落一笔，数据库唯一约束兜底 |
| D4 | 订单快照 | 收货信息、商品名/图/规格/单价全部快照进订单与明细表，不依赖外键实时查 | 商品改价/改址不影响历史订单 |
| D5 | 参考项目 | 拷贝 seig 的 common/system/ui-src/pom/sql 至 `参考-seig/`（排除 node_modules、target），只读参考 | md 指定；P1 需参考 `graduation.sql` 与 SecurityConfig 等实现 |
| D6 | 构建 JDK | 本机 `JAVA_HOME` 为 JDK 17，构建/运行统一显式指定 `C:\Program Files\Java\jdk-1.8` | Boot 2.5 不支持 17；IDEA 中需另配 Project SDK 1.8 |
| D7 | springfox 兼容 | Boot 2.5 需配置 `spring.mvc.pathmatch.matching-strategy: ant_path_matcher` | 已知启动报错（documentationPluginsBootstrapper NPE），照抄 seig |
| D8 | 缓存与统计 | Redis 缓存看板实时数据（5min）；销售趋势走 T+1 定时任务预计算入 `ec_stat_daily` / `ec_product_sales_daily` | P6 要求不实时扫大表 |
| D9 | 定时任务 | Spring `@Scheduled`（单机，够用），不使用 xxl-job/Quartz 集群 | 课程项目降低环境负担；禁用 RabbitMQ（CLAUDE.md 已注明） |
| D10 | 数据权限 | 一期只做 RBAC；P6 看板的「角色数据范围」在接口层做简单域过滤（非通用数据权限框架） | 对齐 md「数据权限延后」决策 |

## 4. 数据库设计（30 张表）

### 4.1 通用规范

- 库名 `ecom`，字符集 utf8mb4；系统表 `sys_` 前缀，业务表 `ec_` 前缀。
- 每表必备：`id`（bigint 自增）、`create_time`、`update_time`、`del_flag`（0 正常 / 1 删除）。
- 逻辑删除，不做物理删除（MyBatis-Plus 全局逻辑删除配置）。
- 金额统一 `decimal(12,2)`；数量/库存 `int`；状态用 `tinyint`。
- 字符串默认 utf8mb4；时间 `datetime`。
- 所有变更必须同步维护 `sql/ecom.sql`。

### 4.2 系统域（5 张，参考 seig 结构 + 本规范字段）

| 表 | 用途 | 关键字段与设计点 |
|---|---|---|
| `sys_user` | 用户（运营+消费者） | `username`(登录名,唯一) / `nick_name` / `password`(BCrypt) / `user_type`(0运营 1消费者) / `phone` / `email` / `avatar` / `status`；不含 role_id（走关联表） |
| `sys_role` | 角色 | `role_name` / `role_key`(唯一，如 super_admin) / `role_sort` / `status` |
| `sys_menu` | 菜单/权限 | `parent_id`(0 为根) / `name` / `menu_type`(M 目录 C 菜单 F 按钮) / `url_path` / `component`(前端组件名) / `permit`(权限标识 模块:操作) / `icon` / `order_num` / `visible` / `status` |
| `sys_user_role` | 用户-角色 | `user_id` + `role_id`，唯一索引；支持一人多角色 |
| `sys_role_menu` | 角色-菜单 | `role_id` + `menu_id`，唯一索引 |

### 4.3 商品域（5 张）

| 表 | 用途 | 关键字段与设计点 |
|---|---|---|
| `ec_category` | 三级类目树 | `parent_id` / `level`(1/2/3) / `name` / `sort` / `status`；最多三级，由服务层校验 |
| `ec_product` | 商品 SPU | `title` / `category_id` / `main_image` / `description` / `status`(0下架 1上架) / `price_min` / `price_max`（由 SKU 汇总）/ `sales`(总销量) |
| `ec_product_spec` | 规格维度 | `product_id` / `spec_name`(如"颜色") / `spec_values`(JSON 数组) / `sort` |
| `ec_sku` | 规格组合 SKU（**含库存**） | `product_id` / `specs`(JSON，如 {"颜色":"红","尺码":"S"}) / `price` / `stock` / `version`(乐观锁) / `sales` / `status`；规格组合唯一 |
| `ec_product_image` | 商品图集 | `product_id` / `url` / `sort` |

### 4.4 交易域（6 张）

| 表 | 用途 | 关键字段与设计点 |
|---|---|---|
| `ec_cart` | 购物车 | `user_id` + `sku_id` 唯一（同 SKU 合并数量）/ `quantity` / `checked`(选中) |
| `ec_address` | 收货地址簿 | `user_id` / `receiver_name` / `phone` / `province` / `city` / `district` / `detail` / `is_default` |
| `ec_order` | 订单主表 | `order_no`(唯一) / `user_id` / `total_amount` / `discount_amount` / `pay_amount` / `user_coupon_id` / `status`(0待支付 1待发货 2运输中 3已完成 4已取消 5退款中 6已退款) / 收货信息快照(3 字段) / `idempotency_key` / `expire_time`(下单+15min) / `pay_time` `ship_time` `receive_time`；唯一索引 `(user_id, idempotency_key)`；索引 `(status, create_time)` |
| `ec_order_item` | 订单明细（快照） | `order_id` / `order_no` / `product_id` / `sku_id` / `product_name` / `product_image` / `specs` / `price` / `quantity` / `subtotal` |
| `ec_coupon` | 优惠券模板 | `type`(0满减 1折扣) / `threshold_amount`(门槛) / `discount_amount` / `discount_rate` / `total_count` / `received_count` / `per_user_limit` / `start_time` `end_time` / `status` |
| `ec_user_coupon` | 用户持券 | `user_id` / `coupon_id` / `status`(0未使用 1已使用 2已过期) / `receive_time` / `use_time` / `order_no` |

### 4.5 订单售后域（3 张）

| 表 | 用途 | 关键字段与设计点 |
|---|---|---|
| `ec_order_status_log` | 状态流转留痕 | `order_no` / `from_status` / `to_status` / `operator`(用户/管理员/系统) / `remark`；每次状态变更必写 |
| `ec_logistics` | 物流单 | `order_no`(唯一) / `company` / `tracking_no` / `status`(0已揽收 1运输中 2派送中 3已签收) / `trace_json`(轨迹数组，模拟推进) |
| `ec_refund` | 退款单 | `order_no` / `user_id` / `amount` / `reason` / `status`(0待审核 1已通过 2已拒绝) / `audit_user_id` / `audit_time` / `audit_remark` |

### 4.6 供应链智能域（3 张）

| 表 | 用途 | 关键字段与设计点 |
|---|---|---|
| `ec_stock_alert` | 库存预警 | `sku_id` / `product_id`(冗余便于展示) / `alert_type`(0缺货 1积压) / `current_stock` / `threshold_value` / `status`(0未处理 1已处理) / `handle_time`；索引 `(sku_id, alert_type, status)` 支撑「连续触发只推首次」判断 |
| `ec_sales_forecast` | 销量预测 | `sku_id` / `forecast_date` / `forecast_qty`；唯一索引 `(sku_id, forecast_date)`，重跑覆盖 |
| `ec_sys_config` | 系统参数 | `config_key`(唯一) / `config_value` / `remark`；初始含：预警上下限比例、预测权重(0.5/0.3/0.2)、客服置信度 0.85、退款是否回滚库存、订单超时分钟数(15) —— **全部规则参数配置化，禁止写死代码** |

### 4.7 数据统计域（2 张，T+1 预计算）

| 表 | 用途 | 关键字段与设计点 |
|---|---|---|
| `ec_stat_daily` | 日汇总 | `stat_date`(唯一) / `gmv`(支付成功口径) / `order_count` / `paid_count` / `refund_count` / `visitor_count` / `pv` / `cart_count`；供实时卡片与趋势 |
| `ec_product_sales_daily` | 商品日销量 | `stat_date` + `sku_id` 唯一 / `product_id` / `sales_qty` / `sales_amount`；供热销/滞销 TOP10、类目占比 |

### 4.8 智能与预留域（6 张）

| 表 | 用途 | 关键字段与设计点 |
|---|---|---|
| `ec_knowledge` | 客服知识库 | `question`(标准问法) / `similar_questions`(相似问法 JSON) / `answer` / `category` / `status`(生效) |
| `ec_chat_log` | 会话记录 | `user_id` / `question` / `answer` / `confidence`(0-1) / `is_transfer`(是否转人工)；未命中问题标记后进「待补充」列表 |
| `ec_search_synonym` | 搜索同义词 | `word` / `synonyms`(逗号分隔)，如 手机=移动电话=cellphone |
| `ec_search_log` | 搜索日志 | `user_id`(可空，游客) / `keyword` / `result_count`；供热门搜索榜 |
| `ec_review` | 商品评价（预留） | `order_no` / `order_item_id` / `user_id` / `product_id` / `sku_id` / `rating`(1-5) / `content` / `images` / `status` |
| `ec_user_behavior` | 用户行为（预留） | `user_id`(可空，游客) / `event_type`(expose 曝光/click 点击/cart 加购/order 下单/pay 支付) / `target_type` / `target_id`；支撑 P6 漏斗与推荐 |

### 4.9 初始数据（随 ecom.sql 写入）

1. **5 个角色**：super_admin / product_op / market_op / cs_op / member。
2. **全量菜单树**：P1-P7 规划的全部页面（M/C 级）+ 按钮权限（F 级，权限标识格式 `模块:操作`），一次性设计到位，后续阶段只做增量维护。
3. **admin 账号**：`admin` / BCrypt 加密密码（构建后通过工具类生成真实 hash 写入脚本）。
4. **系统参数**：见 4.6 的 `ec_sys_config` 初始键值。
5. **演示优惠券模板** 2-3 张（便于 P3 联调）。

## 5. P0 交付物与验收

| # | 交付物 | 验收标准 |
|---|--------|---------|
| 1 | 父 pom + ecom-common + ecom-system | `mvn clean install`（JDK8）全绿 |
| 2 | 基础件：ResponseResult / AppHttpCodeEnum / MybatisPlusConfig / GlobalExceptionHandler / WebConfig(CORS) / SwaggerConfig | 编译通过，`/health` 返回 `ResponseResult.okResult("ok")` |
| 3 | 后端 EcomApplication | `http://localhost:8888/health` 返回 JSON |
| 4 | ecom-ui（Vue CLI 5 + 全套依赖 + /api 代理 + 8082） | `npm run serve` 启动，页面访问 `http://localhost:8082`，页面内调 `/api/health` 拿到后端返回 |
| 5 | `sql/ecom.sql` 30 张表 + 初始数据 | 本地 MySQL 建库成功，表数/初始角色/菜单/admin 账号核对无误 |
| 6 | CLAUDE.md / 参考-seig / git 仓库 | 文件就位，`git init` + 首次提交 |

## 6. 后续阶段路线（摘要）

P1 鉴权权限 → P2 商品 → P3 交易闭环 → P4 订单售后 → P5 供应链智能 → P6 看板 → P7 客服搜索 → P8 收尾。
详见 `docs/需求拆分与模块分类.md` 第三节。每阶段结束执行：`mvn clean install` + `npm run serve` + 接口 curl 验证 + 验收清单自查。

## 7. 风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 本机 JAVA_HOME=17，Boot 2.5 不支持 | 编译/启动失败 | 构建与运行显式指定 JDK 1.8；README/CLAUDE.md 记录 IDEA 配置步骤 |
| Node 24 与 Vue CLI 5 兼容性 | 前端启动异常 | 优先直连；若报错则排查 loader/OpenSSL 问题，必要时降级 Node LTS（记录方案） |
| springfox 2.9.2 与 Boot 2.5 | 启动 NPE | `ant_path_matcher` 配置（D7） |
| MinIO 未启动 | P2 上传功能不可用 | 本期不依赖；P2 前启动 MinIO 服务 |
| 中文路径（参考-seig 拷贝源在桌面中文目录） | 构建工具异常 | 拷贝目标为纯英文路径 `D:\webxiangmu\ecom`；seig 仅作只读参考不参与构建 |
