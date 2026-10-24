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
│  ├─ upload_images.py   演示配图上传（早期版：纯色渐变占位图）
│  ├─ add_new_products.py    新增商品数据（12 SPU / 44 SKU，可重复执行）
│  ├─ gen_product_images.py        配图生成（代码绘制：类目主题卡片，无外部依赖）
│  ├─ fetch_unsplash_images.py     配图抓取（Unsplash 按品名搜真实商品照）
│  ├─ gen_live_data.py     小批量补数（已被 gen_bulk_data.py 取代，勿同时使用）
│  └─ gen_bulk_data.py     全量演示数据：1千用户 / 150商品 / 6万订单 / 13万埋点 / 4千评价
├─ deploy/nginx.conf     生产部署：静态资源 + /api 反向代理
├─ deploy/start-minio.ps1  本地 MinIO 无窗口启动（被登录自启项调用）
├─ deploy/minio_service.py MinIO 的 Windows 服务宿主（pywin32，开机即启、崩溃自拉起）
├─ deploy/install-minio-service.ps1  注册服务的提权安装脚本
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

# 可选：新增商品（12 SPU / 44 SKU，先清后插、可重复执行）
python sql/add_new_products.py

# 商品配图（二选一，先启动后端 8888 + MinIO 9000）
# A. 真实商品照片（推荐）：Unsplash 按品名搜索 → 62 商品主图 + 190 SKU 图 + 商品图集
#    需先配置 Access Key（见下方 FAQ），免费版 50 次/小时，脚本按类目合并搜索词并缓存结果
python sql/fetch_unsplash_images.py --upload
mysql -uroot -p123456 ecom < sql/_images_backfill.sql

# B. 代码绘制卡片（无网络依赖，离线可用）
# python sql/gen_product_images.py --upload && mysql -uroot -p123456 ecom < sql/_images_backfill.sql

# 早期纯色渐变占位图方案（保留兼容，已被上面两个取代）
# python sql/upload_images.py && mysql -uroot -p123456 ecom < sql/data_images.sql

# 让数据看板动起来（推荐）：data.sql 的订单截止在 2026-09-15，
# 不补数的话「今日实时 / 近30天趋势 / 排行 / 近7天漏斗」全是 0
# 全量演示数据（推荐）：1000 用户 / 150 商品 / 6 万订单 / 13 万埋点 / 4 千评价
# 以「今天」为基准回溯 760 天，演示当天重跑即可刷新「今日」指标
python sql/gen_bulk_data.py                      # 生成 sql/_bulk_data.sql（约 50MB，先清后插）
mysql -uroot -p123456 ecom < sql/_bulk_data.sql
mysql -uroot -p123456 ecom < sql/_images_backfill.sql   # 回填新商品配图（图已存在 MinIO）
# 再调 /dashboard/aggregate?days=760 重建汇总（约 5 分钟）

# 小批量补数（只需 400 天订单时用，别和上面的 bulk 混用）
python sql/gen_live_data.py                      # 生成 sql/_live_data.sql（先清后插，可重复执行）
mysql -uroot -p123456 ecom < sql/_live_data.sql
# 再重建 T+1 汇总（需后端已启动；token 用 admin 登录拿）
curl -X POST -H "authorization: <token>" "http://localhost:8888/dashboard/aggregate?days=760"
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
| 每次开机都要手动开 MinIO | 已注册**登录自启**（启动目录 `ecom-minio.cmd` → `deploy/start-minio.ps1`，无窗口、重复执行不重复起进程）。要关掉：删掉启动目录里的 `ecom-minio.cmd` |
| 想注册成 Windows 服务（开机即启、不用登录） | 需要管理员权限，二选一：① 右键「Windows PowerShell」→ 以管理员身份运行，执行 `powershell -NoProfile -ExecutionPolicy Bypass -File "D:\webxiangmu\ecom\deploy\install-minio-service.ps1"`；② 管理员 CMD 执行 `"D:\Anaconda Prompt\Anaconda3\python.exe" D:\webxiangmu\ecom\deploy\minio_service.py --startup auto install` 再 `net start ecom-minio`。服务名 `ecom-minio`：自启动、子进程崩溃后 5 秒自动重启、输出写 `D:\ecom\minio\logs\minio.log`。卸载：`Stop-Service ecom-minio` + `python deploy/minio_service.py remove`（管理员） |
| 看板趋势/排行无数据 / 全是 0 | 演示订单截止在 2026-09-15，看板窗口（今日 / 近7天 / 近30天）内为空。执行 `python sql/gen_live_data.py` → 导入 `sql/_live_data.sql` → 调 `/dashboard/aggregate?days=760` 重建汇总（见「快速启动」第 1 步） |
| 演示数据被改乱 | 重新执行 `mysql -uroot -p123456 ecom < sql/data.sql` 即可复位 |
| 新增的 12 个商品（1051-1062）不见了 | `sql/data.sql` 是「先清后插」的完整复位脚本，会清空 `ec_product`。按顺序重跑 `python sql/add_new_products.py` → `python sql/gen_product_images.py --upload` → 导入 `sql/_images_backfill.sql` 即可恢复 |
| 商品图是纯色块 / 想换配图方案 | 两套脚本任选：`fetch_unsplash_images.py`（Unsplash 按商品名搜真实照片，推荐）或 `gen_product_images.py`（代码绘制卡片，离线可用）。原图 URL 备份在 `sql/_images_backup.sql`，可一键回滚 |
| 配图脚本要怎么配 Unsplash Key | 在 https://unsplash.com/developers 建应用后，把 **Access Key** 写入 `sql/_unsplash_key.txt`（已 gitignore），或设环境变量 `UNSPLASH_ACCESS_KEY`。注意仓库里不要提交该文件 |
| 抓图脚本报额度用尽 / 下载超时 | 免费版 50 次/小时。脚本已做「搜索词合并 + 结果缓存 + 图片缓存 + 上传记录」，**直接重跑即续传**，已完成的搜索与上传都会跳过，不会重复消耗额度 |
| 客服没有 AI 回答（直接转人工） | 大模型兜底需要 `application.yml` 里 `deepseek.api-key`；留空或调用失败时自动降级为「转人工」。也可用 `ec_sys_config.chat_llm_enabled`（0 关 1 开）一键切换规则版/大模型版 |
| Windows 控制台中文乱码 | 终端编码问题，不影响数据（库/页面均为 utf8mb4） |

## 文档索引

- 开发进度与验收记录：`docs/开发进度.md`
- 需求拆分与模块分类：`docs/需求拆分与模块分类.md`
- 设计文档：`docs/superpowers/specs/2026-09-15-ecom-foundation-design.md`
- 接口文档：后端启动后访问 http://localhost:8888/doc.html
- 「功能-接口-数据表」对照表：`docs/功能-接口-数据表对照表.md`
- 工程检查与实现边界清单：`docs/工程检查与实现边界.md`
