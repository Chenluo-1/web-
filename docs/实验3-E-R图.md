# 实验3 E-R 图（ecom 电商智能系统）

> 依据 `sql/ecom.sql`（31 张表）绘制。仓库无物理外键，图中 `FK` 为逻辑外键。
> 公共字段（`create_time` / `update_time` / `del_flag`）不在各实体中重复列出。

## 图1 核心交易 E-R 图

```mermaid
erDiagram
    SYS_USER ||--o{ EC_ORDER : "下单 1:N"
    SYS_USER ||--o{ EC_ADDRESS : "维护 1:N"
    SYS_USER ||--o{ EC_CART : "加购 1:N"
    SYS_USER ||--o{ EC_USER_COUPON : "领取 1:N"
    SYS_USER ||--o{ EC_REFUND : "申请 1:N"

    EC_CATEGORY ||--o{ EC_PRODUCT : "归类 1:N"
    EC_CATEGORY ||--o{ EC_CATEGORY : "父子 1:N"

    EC_PRODUCT ||--|{ EC_SKU : "含规格 1:N"
    EC_SKU ||--o{ EC_CART : "被加购 1:N"
    EC_PRODUCT ||--o{ EC_ORDER_ITEM : "被购买 1:N"
    EC_SKU ||--o{ EC_ORDER_ITEM : "被选购 1:N"

    EC_ORDER ||--|{ EC_ORDER_ITEM : "包含 1:N"
    EC_ORDER ||--o{ EC_ORDER_STATUS_LOG : "流转 1:N"
    EC_ORDER ||--o| EC_LOGISTICS : "发货 1:1"
    EC_ORDER ||--o{ EC_REFUND : "退款 1:N"
    EC_USER_COUPON }o--o| EC_ORDER : "核销于 0..1"

    SYS_USER {
        bigint id PK
        varchar username UK "登录名"
        varchar nick_name "昵称"
        tinyint user_type "0运营 1消费者"
        tinyint status "0正常 1停用"
    }
    EC_CATEGORY {
        bigint id PK
        bigint parent_id FK "父类目,0为根"
        varchar name "类目名"
        tinyint level "层级"
    }
    EC_PRODUCT {
        bigint id PK
        bigint category_id FK
        varchar title "标题"
        decimal price_min "价格下限(冗余)"
        decimal price_max "价格上限(冗余)"
        tinyint status "0下架 1上架"
    }
    EC_SKU {
        bigint id PK
        bigint product_id FK
        varchar specs "规格组合JSON"
        decimal price "售价"
        int stock "库存"
        int version "乐观锁版本"
    }
    EC_CART {
        bigint id PK
        bigint user_id FK
        bigint sku_id FK
        int quantity "数量"
    }
    EC_ADDRESS {
        bigint id PK
        bigint user_id FK
        varchar receiver_name "收货人"
        varchar phone "电话"
        varchar detail "详细地址"
        tinyint is_default "默认地址"
    }
    EC_COUPON {
        bigint id PK
        varchar name "券名"
        tinyint type "0满减 1折扣"
        decimal threshold_amount "门槛"
        datetime start_time "生效"
        datetime end_time "失效"
    }
    EC_USER_COUPON {
        bigint id PK
        bigint user_id FK
        bigint coupon_id FK
        tinyint status "0未用 1已用 2过期"
        varchar order_no FK "核销订单号"
    }
    EC_ORDER {
        bigint id PK
        varchar order_no UK "订单号"
        bigint user_id FK
        bigint user_coupon_id FK
        decimal total_amount "商品总额"
        decimal pay_amount "实付"
        tinyint status "0待支付…6已退款"
        varchar receiver_name "收货人快照"
        varchar idempotency_key "幂等键"
        datetime expire_time "支付超时"
    }
    EC_ORDER_ITEM {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        bigint sku_id FK
        varchar product_name "商品名快照"
        decimal price "单价快照"
        int quantity "数量"
        decimal subtotal "小计"
    }
    EC_ORDER_STATUS_LOG {
        bigint id PK
        varchar order_no FK
        tinyint from_status "原状态"
        tinyint to_status "新状态"
        tinyint operator_type "1用户 2管理员 3系统"
    }
    EC_LOGISTICS {
        bigint id PK
        varchar order_no UK
        varchar company "物流公司"
        varchar tracking_no "运单号"
        tinyint status "0揽收…3签收"
    }
    EC_REFUND {
        bigint id PK
        varchar order_no FK
        bigint user_id FK
        decimal amount "退款金额"
        tinyint status "0待审 1通过 2拒绝"
        bigint audit_user_id FK
    }
```

## 图2 系统权限 E-R 图（RBAC）

```mermaid
erDiagram
    SYS_USER ||--o{ SYS_USER_ROLE : "拥有 1:N"
    SYS_ROLE ||--o{ SYS_USER_ROLE : "被授予 1:N"
    SYS_ROLE ||--o{ SYS_ROLE_MENU : "分配 1:N"
    SYS_MENU ||--o{ SYS_ROLE_MENU : "被分配 1:N"
    SYS_MENU ||--o{ SYS_MENU : "父子 1:N"
    SYS_USER ||--o{ SYS_OPER_LOG : "产生 1:N"

    SYS_USER {
        bigint id PK
        varchar username UK
        varchar password "BCrypt"
        tinyint user_type
    }
    SYS_USER_ROLE {
        bigint id PK
        bigint user_id FK
        bigint role_id FK
    }
    SYS_ROLE {
        bigint id PK
        varchar role_name "角色名"
        varchar role_key UK "权限字符"
        tinyint status
    }
    SYS_ROLE_MENU {
        bigint id PK
        bigint role_id FK
        bigint menu_id FK
    }
    SYS_MENU {
        bigint id PK
        bigint parent_id FK
        varchar name "菜单名"
        varchar permit "权限标识"
        tinyint menu_type
    }
    SYS_OPER_LOG {
        bigint id PK
        varchar title "操作模块"
        varchar oper_url "请求路径"
        bigint oper_user_id FK
    }
```

> 用户 ↔ 角色、角色 ↔ 菜单均为 M:N，已拆中间表 `sys_user_role`、`sys_role_menu`，并用唯一索引 `uk_user_role` / `uk_role_menu` 防重复授予。

## 图3 售后与智能运营 E-R 图

```mermaid
erDiagram
    EC_ORDER_ITEM ||--o| EC_REVIEW : "评价 1:1"
    EC_PRODUCT ||--o{ EC_REVIEW : "被评价 1:N"
    SYS_USER ||--o{ EC_REVIEW : "发表 1:N"

    EC_SKU ||--o{ EC_STOCK_ALERT : "触发预警 1:N"
    EC_SKU ||--o{ EC_SALES_FORECAST : "预测 1:N"
    EC_SKU ||--o{ EC_PRODUCT_SALES_DAILY : "日销 1:N"

    SYS_USER ||--o{ EC_CHAT_LOG : "咨询 1:N"
    SYS_USER ||--o{ EC_BEHAVIOR : "行为 1:N"
    SYS_USER ||--o{ EC_SEARCH_LOG : "搜索 1:N"

    EC_REVIEW {
        bigint id PK
        bigint order_item_id FK "一单一评"
        bigint user_id FK
        bigint product_id FK
        tinyint rating "1-5"
        varchar content "评价内容"
    }
    EC_STOCK_ALERT {
        bigint id PK
        bigint sku_id FK
        tinyint alert_type "0缺货 1积压"
        int current_stock "触发库存"
        tinyint status "0未处理 1已处理"
    }
    EC_SALES_FORECAST {
        bigint id PK
        bigint sku_id FK
        date forecast_date
        int forecast_qty "预测销量"
    }
    EC_PRODUCT_SALES_DAILY {
        bigint id PK
        date stat_date
        bigint sku_id FK
        int sales_qty "当日销量"
    }
    EC_CHAT_LOG {
        bigint id PK
        bigint user_id FK
        varchar question "用户问题"
        varchar answer "机器人回答"
        varchar source "rule/llm/transfer"
    }
    EC_BEHAVIOR {
        bigint id PK
        bigint user_id FK
        varchar event_type "expose/click/cart/order/pay"
        varchar target_type "product/sku/category"
        bigint target_id
    }
    EC_SEARCH_LOG {
        bigint id PK
        bigint user_id FK
        varchar keyword "关键词"
        int result_count "结果数"
    }
```

## 陈氏 E-R 图（核心实体）

矩形=实体，菱形=联系，椭圆=属性（属性见上文实体属性块，此处只画联系与基数）。

```mermaid
flowchart LR
    U[用户] ---|1| R1{下单}
    R1 ---|N| O[订单]

    O ---|1| R2{包含}
    R2 ---|N| I[订单明细]

    P[商品] ---|1| R3{拥有}
    R3 ---|N| S[SKU]

    P ---|1| R4{被购买}
    R4 ---|N| I

    S ---|1| R5{被选购}
    R5 ---|N| I

    U ---|1| R6{加购}
    R6 ---|N| S

    U ---|1| R7{维护}
    R7 ---|N| A[收货地址]

    O ---|1| R8{发货}
    R8 ---|1| L[物流单]
```

## 关系基数汇总

| 联系 | 类型 | 说明 |
|---|---|---|
| 用户—订单 | 1:N | 一名用户多笔订单，一笔订单只属于一名用户 |
| 订单—订单明细 | 1:N | 一笔订单至少一条明细；明细复合主键可用 (order_id, sku_id) |
| 商品—SKU | 1:N | SPU 与规格组合是典型主从 |
| 商品/SKU—订单明细 | 1:N | 商品可出现在多条明细中，使订单与商品构成 M:N |
| 订单—物流单 | 1:1 | 一笔订单对应一张运单，uk_order_no 保证唯一 |
| 订单明细—评价 | 1:1 | 一条明细最多一条评价，由 ec_review.order_item_id 控制 |
| 用户—角色 | M:N → sys_user_role | 中间表加唯一索引，防重复授权 |
| 角色—菜单 | M:N → sys_role_menu | 同上 |
| 用户—SKU（购物车） | M:N → ec_cart | uk_user_sku 保证同一 SKU 只占一行 |
| 用户—优惠券 | M:N → ec_user_coupon | 记录领取/核销状态与订单号 |

## 两点审查说明

1. **M:N 全部拆表**：user_role、role_menu、cart、user_coupon、order_item 五张中间表，对应实验要求的「M:N 拆中间表」。
2. **快照 ≠ 冗余**：ec_order 的 receiver_*、ec_order_item 的 product_name/price 是下单时快照（防止历史订单被后续修改污染）；ec_product.price_min/max 才是由 SKU 汇总、可重算的冗余（表注释已标「冗余自SKU」）。
