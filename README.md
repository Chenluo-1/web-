# 电商智能系统（ecom）

Web 应用项目实训课程设计（CW3104）。一个面向店铺运营的电商系统：完整交易闭环（浏览-下单-支付-发货-收货-评价-售后）+ 数据看板 + 规则版智能功能（智能补货建议、库存预警、智能客服、智能搜索、个性化推荐预留）。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | JDK 1.8 · Spring Boot 2.5.0 · Spring Security + JWT · MyBatis-Plus 3.4.2 · Druid |
| 存储 | MySQL 5.7（utf8mb4） · Redis（登录态/看板缓存） · MinIO（商品图片） |
| 前端 | Vue 3.2 · Vue CLI 5 · Element Plus · Pinia · Vue Router 4 · Axios · ECharts |
| 文档 | springfox-swagger2 2.9.2（/doc.html） |

## 目录结构

```
ecom/
├─ ecom-common/          通用层：domain / mapper / service / config / handler / utils
├─ ecom-system/          业务层：controller / 启动类（EcomApplication，端口 8888）
├─ ecom-ui/              前端 Vue 工程（开发端口 8082）
├─ sql/
│  ├─ ecom.sql           全量脚本：31 张表 + 初始数据（菜单/角色/类目树/知识库）
│  ├─ data_generator.py  演示数据生成器（可选）
│  ├─ data.sql           演示数据：50 商品 / 每商品 2-6 SKU / 200 用户 / 500 订单
│  └─ upload_images.py   演示配图上传（可选，生成 data_images.sql）
├─ deploy/nginx.conf     生产部署：静态资源 + /api 反向代理
└─ docs/                 开发进度 / 需求拆分 / 设计文档
```

## 环境要求

| 组件 | 版本/端口 | 说明 |
|---|---|---|
| JDK | 1.8 | 构建必须用 JDK8（本机 `C:\Program Files\Java\jdk-1.8`） |
| Maven | 3.6+ | 构建后端 |
| MySQL | 5.7，端口 3306 | 库名 `ecom`，用户 root / 123456（按实际修改 application.yml） |
| Redis | 端口 6379 | 登录态 `user:{id}`、看板缓存 |
| MinIO | 端口 9000（控制台 9001） | 商品图片存储；启动命令见下 |
| Node.js | 14+ | 前端构建（npm 已配腾讯镜像） |
| Nginx | 任意稳定版 | 仅生产部署需要 |

## 快速启动（开发模式）

**1. 初始化数据库**（全新环境按顺序执行）

```bash
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS ecom DEFAULT CHARSET utf8mb4;"
mysql -uroot -p123456 ecom < sql/ecom.sql        # 建表 + 基础数据
mysql -uroot -p123456 ecom < sql/data.sql        # 演示数据（50 商品 / 200 用户 / 500 订单）
# 可选：商品配图（先启动后端 + MinIO，再执行）
python sql/upload_images.py && mysql -uroot -p123456 ecom < sql/data_images.sql
```

**2. 启动中间件**

```bash
# MinIO（Windows，路径按实际调整）
minio.exe server D:\ecom\minio-data --address ":9000" --console-address ":9001"
# Redis / MySQL 按本机服务启动即可
```

**3. 启动后端**（端口 8888）

```bash
JAVA_HOME="/c/Program Files/Java/jdk-1.8" mvn clean install     # Git Bash
java -jar ecom-system/target/ecom-system.jar                     # 须用 JDK8 运行
# 验证：curl http://localhost:8888/health
```

**4. 启动前端**（端口 8082）

```bash
cd ecom-ui
npm install
npm run serve
# 浏览器打开 http://localhost:8082 ，/api 由 devServer 代理到 8888
```

## 生产部署（Nginx）

```bash
cd ecom-ui && npm run build          # 产出 ecom-ui/dist
# 将 deploy/nginx.conf 的 server 段合入 nginx 配置（root 指向 dist 实际路径）
nginx -s reload
# 访问 http://localhost/  （/api 由 Nginx 转发到后端 8888）
```

后端 jar 可用 `nohup java -jar ecom-system.jar &`（Linux）或 `start javaw -jar ecom-system.jar`（Windows）常驻。

## 默认账号

| 账号 | 密码 | 角色 | 用途 |
|---|---|---|---|
| admin | admin123 | 超级管理员 | 全部功能 |
| op1 | 123456 | 商品运营 | 商品/库存/供应链/看板（商品域） |
| cs1 | 123456 | 客服主管 | 知识库 + 会话记录 + 待补充问题 |
| member1 | 123456 | 消费者 | 商城购物全流程（已备各状态订单） |
| 演示数据买家 | 123456 | 消费者 | data.sql 生成 200 个模拟用户（用户名见数据库 sys_user） |

前台还支持**消费者自助注册**（登录页 → 消费者注册）。

## 常见问题（FAQ）

| 问题 | 处理 |
|---|---|
| 构建报 `JCImport ... qualid` | 用了高版本 JDK 编译，把 IDEA Project SDK / Maven Runner JRE 改为 1.8 |
| 启动报端口占用 / jar 无法覆盖 | `netstat -ano` 找到 8888 端口 PID 后执行 `taskkill /F /PID xxx` |
| 登录返回 401 或提示需要登录 | Redis 未启动（登录态存 Redis） |
| 商品图片不显示 | MinIO 未启动或图片未上传（执行 sql/upload_images.py 后导入 data_images.sql） |
| 看板趋势/排行无数据 | 先执行 data.sql（含 30 天汇总），或后端运行后调用「手动重算」 |
| 演示数据被改乱 | 重新执行 `mysql -uroot -p123456 ecom < sql/data.sql` 即可复位 |
| Windows 控制台中文乱码 | 终端编码问题，不影响数据（库/页面均为 utf8mb4） |

## 文档索引

- 开发进度与验收记录：`docs/开发进度.md`
- 需求拆分与模块分类：`docs/需求拆分与模块分类.md`
- 设计文档：`docs/superpowers/specs/2026-09-15-ecom-foundation-design.md`
- 接口文档：后端启动后访问 http://localhost:8888/doc.html
- 「功能-接口-数据表」对照表：`docs/功能-接口-数据表对照表.md`
- 工程检查与实现边界清单：`docs/工程检查与实现边界.md`
