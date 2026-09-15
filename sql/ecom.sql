-- ============================================================
-- 电商智能系统（ecom）全量数据库脚本（唯一权威，随开发持续维护）
-- MySQL 8 / utf8mb4；每表必备 id/create_time/update_time/del_flag
-- 表清单：sys_* 6 张（用户/角色/菜单/用户角色/角色菜单/操作日志）
--        ec_* 25 张（商品5 交易6 售后3 供应链3 统计2 智能与预留6）
-- 初始账号：admin / admin123（BCrypt 加密）
-- ============================================================
DROP DATABASE IF EXISTS `ecom`;
CREATE DATABASE `ecom` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `ecom`;

-- ============================================================
-- 一、系统域（5 张）
-- ============================================================

-- ----------------------------
-- 1. 用户表（运营+消费者单表，user_type 区分）
-- ----------------------------
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

-- ----------------------------
-- 2. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_key` varchar(50) NOT NULL COMMENT '角色权限字符串(如 super_admin)',
  `role_sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0正常 1停用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';

-- ----------------------------
-- 3. 菜单表（M目录 C菜单 F按钮）
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父菜单id(0为根)',
  `name` varchar(50) NOT NULL COMMENT '菜单名',
  `menu_type` char(1) NOT NULL DEFAULT 'C' COMMENT '菜单类型(M目录 C菜单 F按钮)',
  `url_path` varchar(200) DEFAULT NULL COMMENT '路由路径',
  `component` varchar(100) DEFAULT NULL COMMENT '前端组件名(对应 components/*.vue)',
  `permit` varchar(100) DEFAULT NULL COMMENT '权限标识(模块:操作)',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `order_num` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `visible` tinyint NOT NULL DEFAULT 0 COMMENT '显示状态(0显示 1隐藏)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '菜单状态(0正常 1停用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单/权限表';

-- ----------------------------
-- 4. 用户-角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `role_id` bigint NOT NULL COMMENT '角色id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表';

-- ----------------------------
-- 5. 角色-菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色id',
  `menu_id` bigint NOT NULL COMMENT '菜单id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关联表';

-- ----------------------------
-- 6. 操作日志表（写操作审计，P1 新增）
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `module` varchar(50) NOT NULL COMMENT '模块名',
  `oper_type` varchar(20) NOT NULL COMMENT '操作类型(新增/修改/删除等)',
  `method` varchar(200) DEFAULT NULL COMMENT '方法名',
  `params` text COMMENT '请求参数(密码已脱敏)',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人id',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人登录名',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0成功 1失败)',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表';

-- ============================================================
-- 二、商品域（5 张）
-- ============================================================

-- ----------------------------
-- 6. 类目表（三级树）
-- ----------------------------
DROP TABLE IF EXISTS `ec_category`;
CREATE TABLE `ec_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父类目id(0为根)',
  `name` varchar(50) NOT NULL COMMENT '类目名称',
  `level` tinyint NOT NULL DEFAULT 1 COMMENT '层级(1起，不限深度)',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `icon` varchar(255) DEFAULT NULL COMMENT '类目图标',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0正常 1停用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品类目表（三级树）';

-- ----------------------------
-- 7. 商品SPU表
-- ----------------------------
DROP TABLE IF EXISTS `ec_product`;
CREATE TABLE `ec_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(200) NOT NULL COMMENT '商品标题',
  `category_id` bigint NOT NULL COMMENT '类目id(商品所属类目)',
  `main_image` varchar(500) DEFAULT NULL COMMENT '主图URL',
  `description` text COMMENT '商品详情(富文本)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '上下架(0下架 1上架)',
  `price_min` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '价格区间下限(冗余自SKU)',
  `price_max` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '价格区间上限(冗余自SKU)',
  `sales` int NOT NULL DEFAULT 0 COMMENT '总销量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品SPU表';

-- ----------------------------
-- 8. 规格模板表（如"颜色"及其可选值）
-- ----------------------------
DROP TABLE IF EXISTS `ec_product_spec`;
CREATE TABLE `ec_product_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品SPU id',
  `spec_name` varchar(50) NOT NULL COMMENT '规格名(如 颜色/尺码)',
  `spec_values` varchar(500) NOT NULL COMMENT '可选值JSON数组(如 ["红","蓝"])',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品规格模板表';

-- ----------------------------
-- 9. SKU表（含库存，规格组合笛卡尔积）
-- ----------------------------
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

-- ----------------------------
-- 10. 商品图集
-- ----------------------------
DROP TABLE IF EXISTS `ec_product_image`;
CREATE TABLE `ec_product_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品SPU id',
  `url` varchar(500) NOT NULL COMMENT '图片URL',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品图集表';

-- ============================================================
-- 三、交易域（6 张）
-- ============================================================

-- ----------------------------
-- 11. 购物车
-- ----------------------------
DROP TABLE IF EXISTS `ec_cart`;
CREATE TABLE `ec_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `checked` tinyint NOT NULL DEFAULT 1 COMMENT '选中状态(0未选中 1选中)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '购物车表';

-- ----------------------------
-- 12. 收货地址簿
-- ----------------------------
DROP TABLE IF EXISTS `ec_address`;
CREATE TABLE `ec_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `receiver_name` varchar(50) NOT NULL COMMENT '收货人姓名',
  `phone` varchar(20) NOT NULL COMMENT '收货人电话',
  `province` varchar(50) NOT NULL COMMENT '省',
  `city` varchar(50) NOT NULL COMMENT '市',
  `district` varchar(50) NOT NULL COMMENT '区/县',
  `detail` varchar(255) NOT NULL COMMENT '详细地址',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '默认地址(0否 1是)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '收货地址表';

-- ----------------------------
-- 13. 优惠券模板
-- ----------------------------
DROP TABLE IF EXISTS `ec_coupon`;
CREATE TABLE `ec_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '优惠券名称',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '类型(0满减 1折扣)',
  `threshold_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
  `discount_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '满减金额(满减用)',
  `discount_rate` decimal(4,2) DEFAULT NULL COMMENT '折扣率(折扣用，如 0.85)',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '发放总量(0不限)',
  `received_count` int NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `per_user_limit` int NOT NULL DEFAULT 1 COMMENT '每人限领',
  `start_time` datetime NOT NULL COMMENT '生效时间',
  `end_time` datetime NOT NULL COMMENT '失效时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0正常 1停用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '优惠券模板表';

-- ----------------------------
-- 14. 用户持券
-- ----------------------------
DROP TABLE IF EXISTS `ec_user_coupon`;
CREATE TABLE `ec_user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `coupon_id` bigint NOT NULL COMMENT '优惠券id',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0未使用 1已使用 2已过期)',
  `receive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `order_no` varchar(32) DEFAULT NULL COMMENT '使用的订单号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户优惠券表';

-- ----------------------------
-- 15. 订单主表
-- ----------------------------
DROP TABLE IF EXISTS `ec_order`;
CREATE TABLE `ec_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `total_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '商品总额',
  `discount_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `user_coupon_id` bigint DEFAULT NULL COMMENT '使用的用户券id',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0待支付 1待发货 2运输中 3已完成 4已取消 5退款中 6已退款)',
  `receiver_name` varchar(50) NOT NULL COMMENT '收货人(快照)',
  `receiver_phone` varchar(20) NOT NULL COMMENT '收货电话(快照)',
  `receiver_address` varchar(500) NOT NULL COMMENT '收货地址(快照)',
  `pay_type` tinyint DEFAULT NULL COMMENT '支付方式(0模拟支付)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '收货时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '订单备注',
  `idempotency_key` varchar(64) DEFAULT NULL COMMENT '幂等键(防重复提交)',
  `expire_time` datetime DEFAULT NULL COMMENT '支付超时时间(下单+15分钟)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_user_idem` (`user_id`, `idempotency_key`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单主表';

-- ----------------------------
-- 16. 订单明细（商品快照）
-- ----------------------------
DROP TABLE IF EXISTS `ec_order_item`;
CREATE TABLE `ec_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `product_id` bigint NOT NULL COMMENT '商品SPU id',
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `product_name` varchar(200) NOT NULL COMMENT '商品名(快照)',
  `product_image` varchar(500) DEFAULT NULL COMMENT '商品图(快照)',
  `specs` varchar(500) DEFAULT NULL COMMENT '规格组合(快照)',
  `price` decimal(12,2) NOT NULL COMMENT '单价(快照)',
  `quantity` int NOT NULL COMMENT '数量',
  `subtotal` decimal(12,2) NOT NULL COMMENT '小计',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单明细表';

-- ============================================================
-- 四、订单售后域（3 张）
-- ============================================================

-- ----------------------------
-- 17. 订单状态流转记录
-- ----------------------------
DROP TABLE IF EXISTS `ec_order_status_log`;
CREATE TABLE `ec_order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `from_status` tinyint NOT NULL COMMENT '原状态',
  `to_status` tinyint NOT NULL COMMENT '新状态',
  `operator_type` tinyint NOT NULL DEFAULT 3 COMMENT '操作者类型(1用户 2管理员 3系统)',
  `operator_id` bigint DEFAULT NULL COMMENT '操作者id',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单状态流转记录表';

-- ----------------------------
-- 18. 物流单
-- ----------------------------
DROP TABLE IF EXISTS `ec_logistics`;
CREATE TABLE `ec_logistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `company` varchar(50) NOT NULL COMMENT '物流公司',
  `tracking_no` varchar(64) NOT NULL COMMENT '运单号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '物流状态(0已揽收 1运输中 2派送中 3已签收)',
  `trace_json` text COMMENT '物流轨迹JSON（模拟推进）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物流表';

-- ----------------------------
-- 19. 退款单
-- ----------------------------
DROP TABLE IF EXISTS `ec_refund`;
CREATE TABLE `ec_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '申请用户id',
  `amount` decimal(12,2) NOT NULL COMMENT '退款金额',
  `reason` varchar(255) NOT NULL COMMENT '退款原因',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0待审核 1已通过 2已拒绝)',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核人id',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '退款单表';

-- ============================================================
-- 五、供应链智能域（3 张）
-- ============================================================

-- ----------------------------
-- 20. 库存预警
-- ----------------------------
DROP TABLE IF EXISTS `ec_stock_alert`;
CREATE TABLE `ec_stock_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `product_id` bigint NOT NULL COMMENT '商品id(冗余便于展示)',
  `alert_type` tinyint NOT NULL COMMENT '预警类型(0缺货 1积压)',
  `current_stock` int NOT NULL COMMENT '触发时库存值',
  `threshold_value` int NOT NULL COMMENT '触发阈值',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态(0未处理 1已处理)',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_user_id` bigint DEFAULT NULL COMMENT '处理人id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_sku_type_status` (`sku_id`, `alert_type`, `status`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '库存预警表';

-- ----------------------------
-- 21. 销量预测
-- ----------------------------
DROP TABLE IF EXISTS `ec_sales_forecast`;
CREATE TABLE `ec_sales_forecast` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `forecast_date` date NOT NULL COMMENT '预测日期',
  `forecast_qty` int NOT NULL DEFAULT 0 COMMENT '预测销量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_date` (`sku_id`, `forecast_date`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '销量预测表';

-- ----------------------------
-- 22. 系统参数（所有规则阈值配置化）
-- ----------------------------
DROP TABLE IF EXISTS `ec_sys_config`;
CREATE TABLE `ec_sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '参数键',
  `config_name` varchar(100) NOT NULL COMMENT '参数名称',
  `config_value` varchar(500) NOT NULL COMMENT '参数值',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统参数表';

-- ============================================================
-- 六、数据统计域（2 张，T+1 预计算）
-- ============================================================

-- ----------------------------
-- 23. 日汇总统计
-- ----------------------------
DROP TABLE IF EXISTS `ec_stat_daily`;
CREATE TABLE `ec_stat_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `gmv` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT 'GMV(支付成功口径)',
  `order_count` int NOT NULL DEFAULT 0 COMMENT '订单数',
  `paid_count` int NOT NULL DEFAULT 0 COMMENT '支付订单数',
  `refund_count` int NOT NULL DEFAULT 0 COMMENT '退款订单数',
  `visitor_count` int NOT NULL DEFAULT 0 COMMENT '访客数(uv)',
  `pv` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `cart_count` int NOT NULL DEFAULT 0 COMMENT '加购数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '日汇总统计表';

-- ----------------------------
-- 24. 商品日销量统计
-- ----------------------------
DROP TABLE IF EXISTS `ec_product_sales_daily`;
CREATE TABLE `ec_product_sales_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `sales_qty` int NOT NULL DEFAULT 0 COMMENT '销售数量',
  `sales_amount` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '销售金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_sku` (`stat_date`, `sku_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品日销量统计表';

-- ============================================================
-- 七、智能与预留域（6 张）
-- ============================================================

-- ----------------------------
-- 25. 客服知识库
-- ----------------------------
DROP TABLE IF EXISTS `ec_knowledge`;
CREATE TABLE `ec_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question` varchar(255) NOT NULL COMMENT '标准问法',
  `similar_questions` varchar(1000) DEFAULT NULL COMMENT '相似问法(JSON数组)',
  `answer` text NOT NULL COMMENT '标准答案',
  `category` varchar(50) DEFAULT NULL COMMENT '知识分类',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '生效状态(0停用 1生效)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '客服知识库表';

-- ----------------------------
-- 26. 客服会话记录
-- ----------------------------
DROP TABLE IF EXISTS `ec_chat_log`;
CREATE TABLE `ec_chat_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `question` varchar(500) NOT NULL COMMENT '用户问题',
  `answer` varchar(1000) DEFAULT NULL COMMENT '机器人回答',
  `confidence` decimal(5,2) DEFAULT NULL COMMENT '匹配置信度(0-1)',
  `is_transfer` tinyint NOT NULL DEFAULT 0 COMMENT '是否转人工(0否 1是)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '客服会话记录表';

-- ----------------------------
-- 27. 搜索同义词
-- ----------------------------
DROP TABLE IF EXISTS `ec_search_synonym`;
CREATE TABLE `ec_search_synonym` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` varchar(50) NOT NULL COMMENT '主词',
  `synonyms` varchar(500) NOT NULL COMMENT '同义词(逗号分隔)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '搜索同义词表';

-- ----------------------------
-- 28. 搜索日志
-- ----------------------------
DROP TABLE IF EXISTS `ec_search_log`;
CREATE TABLE `ec_search_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户id(游客为空)',
  `keyword` varchar(100) NOT NULL COMMENT '搜索关键词',
  `result_count` int NOT NULL DEFAULT 0 COMMENT '结果数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_keyword` (`keyword`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '搜索日志表';

-- ----------------------------
-- 29. 商品评价（预留，后期简化实现）
-- ----------------------------
DROP TABLE IF EXISTS `ec_review`;
CREATE TABLE `ec_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `order_item_id` bigint NOT NULL COMMENT '订单明细id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `rating` tinyint NOT NULL DEFAULT 5 COMMENT '评分(1-5)',
  `content` varchar(1000) DEFAULT NULL COMMENT '评价内容',
  `images` varchar(1000) DEFAULT NULL COMMENT '评价图片(JSON数组)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(0隐藏 1显示)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品评价表（预留）';

-- ----------------------------
-- 30. 用户行为采集（预留，供漏斗与推荐）
-- ----------------------------
DROP TABLE IF EXISTS `ec_user_behavior`;
CREATE TABLE `ec_user_behavior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户id(游客为空)',
  `event_type` varchar(20) NOT NULL COMMENT '事件类型(expose曝光 click点击 cart加购 order下单 pay支付)',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型(product商品 sku规格 category类目)',
  `target_id` bigint NOT NULL COMMENT '目标id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_event_time` (`event_type`, `create_time`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户行为表（预留）';

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
(1,  0,  '首页',     'M', '/home',     'home',    'home',    'iconfont icon-home-full',      1,  0, 0),
(2,  0,  '系统管理', 'M', '/sys',      'sys',     'sys',     'iconfont icon-xitong',         9,  0, 0),
(18, 2,  '用户管理', 'C', '/sys/user', 'sysUser', 'sys:user','iconfont icon-yonghu',         21, 0, 0),
(19, 2,  '角色管理', 'C', '/sys/role', 'sysRole', 'sys:role','iconfont icon-jiaoseguanli',   22, 0, 0),
(20, 18, '搜索用户', 'F', '/sys/user/find',     'sysUserFind',    'sys:user:find',    NULL, 0, 1, 0),
(21, 18, '新增用户', 'F', '/sys/user/add',      'sysUserAdd',     'sys:user:add',     NULL, 0, 1, 0),
(22, 18, '修改用户', 'F', '/sys/user/edit',     'sysUserEdit',    'sys:user:edit',    NULL, 0, 1, 0),
(23, 18, '删除用户', 'F', '/sys/user/del',      'sysUserDel',     'sys:user:del',     NULL, 0, 1, 0),
(24, 18, '重置密码', 'F', '/sys/user/respwd',   'sysUserRespwd',  'sys:user:respwd',  NULL, 0, 1, 0),
(25, 19, '新增角色', 'F', '/sys/role/add',      'sysRoleAdd',     'sys:role:add',     NULL, 0, 1, 0),
(26, 19, '修改角色', 'F', '/sys/role/edit',     'sysRoleEdit',    'sys:role:edit',    NULL, 0, 1, 0),
(27, 19, '删除角色', 'F', '/sys/role/del',      'sysRoleDel',     'sys:role:del',     NULL, 0, 1, 0),
(28, 19, '获取权限', 'F', '/sys/role/getMenu',  'sysRoleGetMenu', 'sys:role:getMenu', NULL, 0, 1, 0),
(29, 19, '分配权限', 'F', '/sys/role/setMenu',  'sysRoleSetMenu', 'sys:role:setMenu', NULL, 0, 1, 0),
(30, 1,  '修改密码', 'F', '/home/password/change', 'homePasswordChange', 'home:password:change', NULL, 0, 1, 0);

-- 2.2) 商品管理菜单（P2 增量：M/C/F）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `url_path`, `component`, `permit`, `icon`, `order_num`, `visible`, `status`) VALUES
(3,  0,  '商品管理', 'M', '/product',          'product',         'product',          'iconfont icon-shangpin', 3, 0, 0),
(31, 3,  '类目管理', 'C', '/product/category', 'productCategory', 'product:category', NULL, 10, 0, 0),
(32, 3,  '商品列表', 'C', '/product/list',     'productList',     'product:item',     NULL, 11, 0, 0),
(33, 31, '搜索类目', 'F', '/product/category/find', 'productCategoryFind', 'product:category:find', NULL, 0, 1, 0),
(34, 31, '新增类目', 'F', '/product/category/add',  'productCategoryAdd',  'product:category:add',  NULL, 0, 1, 0),
(35, 31, '修改类目', 'F', '/product/category/edit', 'productCategoryEdit', 'product:category:edit', NULL, 0, 1, 0),
(36, 31, '删除类目', 'F', '/product/category/del',  'productCategoryDel',  'product:category:del',  NULL, 0, 1, 0),
(37, 32, '搜索商品', 'F', '/product/item/find',   'productItemFind',   'product:item:find',   NULL, 0, 1, 0),
(38, 32, '新增商品', 'F', '/product/item/add',    'productItemAdd',    'product:item:add',    NULL, 0, 1, 0),
(39, 32, '修改商品', 'F', '/product/item/edit',   'productItemEdit',   'product:item:edit',   NULL, 0, 1, 0),
(40, 32, '删除商品', 'F', '/product/item/del',    'productItemDel',    'product:item:del',    NULL, 0, 1, 0),
(41, 32, '上下架',   'F', '/product/item/status', 'productItemStatus', 'product:item:status', NULL, 0, 1, 0),
(42, 32, '上传图片', 'F', '/product/item/upload', 'productItemUpload', 'product:item:upload', NULL, 0, 1, 0);

-- 商品管理菜单分配给：超管(1) 与 商品运营(2)；商品运营同时拥有首页
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 3),(1, 31),(1, 32),(1, 33),(1, 34),(1, 35),(1, 36),(1, 37),(1, 38),(1, 39),(1, 40),(1, 41),(1, 42),
(2, 1),(2, 3),(2, 31),(2, 32),(2, 33),(2, 34),(2, 35),(2, 36),(2, 37),(2, 38),(2, 39),(2, 40),(2, 41),(2, 42);

-- 3) 超管账号（密码 admin123 的 BCrypt 哈希）
INSERT INTO `sys_user` (`id`, `username`, `nick_name`, `password`, `user_type`, `status`) VALUES
(1, 'admin', '超级管理员', '$2a$10$wS4a99L9y.o1BkbE8MENB.fCG/Ke8YWfFViI6t3RhqvE1mUE5aBSK', 0, 0);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 超管拥有全部菜单（P1 代码层对 super_admin 直通，双保险）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1),(1, 2),(1, 18),(1, 19),(1, 20),(1, 21),(1, 22),(1, 23),(1, 24),(1, 25),(1, 26),(1, 27),(1, 28),(1, 29),(1, 30);

-- 4) 系统参数（规则全部配置化，禁止写死代码）
INSERT INTO `ec_sys_config` (`config_key`, `config_name`, `config_value`, `remark`) VALUES
('order_timeout_minutes',     '订单支付超时（分钟）', '15',  '下单后未支付自动取消并释放库存'),
('stock_alert_low_default',   '缺货预警阈值（默认）', '10',  '库存低于该值触发缺货预警'),
('stock_alert_high_default',  '积压预警阈值（默认）', '1000','库存高于该值触发积压预警'),
('forecast_weight_recent',    '预测权重-近7天',      '0.5', '加权移动平均权重'),
('forecast_weight_mid',       '预测权重-8到30天',    '0.3', '加权移动平均权重'),
('forecast_weight_old',       '预测权重-31到90天',   '0.2', '加权移动平均权重'),
('chat_confidence_threshold', '客服自动回复置信度',   '0.85','低于该值转人工'),
('refund_stock_rollback',     '退款是否回滚库存',     '1',   '1=回滚 0=不回滚');

-- 5) 演示优惠券（便于 P3 联调）
INSERT INTO `ec_coupon` (`name`, `type`, `threshold_amount`, `discount_amount`, `total_count`, `received_count`, `per_user_limit`, `start_time`, `end_time`, `status`) VALUES
('新客满100减10', 0, 100.00, 10.00, 1000, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 0),
('满200减30',     0, 200.00, 30.00, 1000, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 0);

-- ============================================================
-- 演示类目数据（淘宝风格多级树，2026-09-15 增量；固定 id 便于与本地库一致）
-- 一级：服饰 / 数码 / 美食 / 运动；数码最深 5 级（手机>苹果>iPhone 17）
-- ============================================================
INSERT IGNORE INTO `ec_category` (`id`,`parent_id`,`name`,`level`,`sort`,`status`) VALUES
-- 服饰
(1,  0,  '服饰',     1,  1, 0),
(2,  1,  '男装',     2,  1, 0),
(3,  2,  'T恤',      3,  1, 0),
-- 数码
(100,0,  '数码',     1, 10, 0),
(110,100,'手机通讯', 2,  1, 0),
(111,110,'手机',     3,  1, 0),
(112,111,'苹果',     4,  1, 0),
(113,111,'华为',     4,  2, 0),
(114,111,'小米',     4,  3, 0),
(115,112,'iPhone 17',5,  1, 0),
(116,112,'iPhone 16',5,  2, 0),
(117,113,'Mate 70',  5,  1, 0),
(118,114,'小米 15',  5,  1, 0),
(119,110,'手机配件', 3,  2, 0),
(120,119,'充电器',   4,  1, 0),
(121,119,'数据线',   4,  2, 0),
(122,119,'手机壳',   4,  3, 0),
(130,100,'电脑办公', 2,  2, 0),
(131,130,'笔记本',   3,  1, 0),
(132,130,'台式机',   3,  2, 0),
(133,130,'平板电脑', 3,  3, 0),
(134,130,'显示器',   3,  4, 0),
(140,100,'影音娱乐', 2,  3, 0),
(141,140,'耳机',     3,  1, 0),
(142,140,'音箱',     3,  2, 0),
(143,140,'相机',     3,  3, 0),
(150,100,'智能设备', 2,  4, 0),
(151,150,'智能手表', 3,  1, 0),
(152,150,'智能家居', 3,  2, 0),
(153,150,'智能穿戴', 3,  3, 0),
-- 美食
(200,0,  '美食',     1, 20, 0),
(210,200,'零食',     2,  1, 0),
(211,210,'坚果炒货', 3,  1, 0),
(212,210,'饼干糕点', 3,  2, 0),
(213,210,'肉干肉脯', 3,  3, 0),
(214,210,'糖果巧克力',3, 4, 0),
(220,200,'生鲜',     2,  2, 0),
(221,220,'水果',     3,  1, 0),
(222,220,'海鲜水产', 3,  2, 0),
(223,220,'蔬菜蛋品', 3,  3, 0),
(230,200,'饮料冲调', 2,  3, 0),
(231,230,'咖啡',     3,  1, 0),
(232,230,'茶饮',     3,  2, 0),
(233,230,'乳品',     3,  3, 0),
(240,200,'粮油调味', 2,  4, 0),
(241,240,'大米面粉', 3,  1, 0),
(242,240,'食用油',   3,  2, 0),
(243,240,'调味酱料', 3,  3, 0),
-- 运动
(300,0,  '运动',     1, 30, 0),
(310,300,'运动鞋服', 2,  1, 0),
(311,310,'跑步鞋',   3,  1, 0),
(312,310,'篮球鞋',   3,  2, 0),
(313,310,'运动服饰', 3,  3, 0),
(320,300,'健身训练', 2,  2, 0),
(321,320,'瑜伽用品', 3,  1, 0),
(322,320,'力量器械', 3,  2, 0),
(323,320,'跳绳',     3,  3, 0),
(330,300,'户外运动', 2,  3, 0),
(331,330,'露营装备', 3,  1, 0),
(332,330,'骑行装备', 3,  2, 0),
(333,330,'登山徒步', 3,  3, 0),
(340,300,'球类运动', 2,  4, 0),
(341,340,'篮球',     3,  1, 0),
(342,340,'足球',     3,  2, 0),
(343,340,'羽毛球',   3,  3, 0),
(344,340,'乒乓球',   3,  4, 0);

-- ============================================================
-- P3 交易菜单（增量）：商品浏览/购物车/我的订单（消费者）+ 营销中心>优惠券管理（活动运营）
-- ============================================================
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `url_path`, `component`, `permit`, `icon`, `order_num`, `visible`, `status`) VALUES
(400, 0,   '商品浏览',   'M', '/shop',          'shop',         'shop',          'iconfont icon-shangpin',   2, 0, 0),
(401, 0,   '购物车',     'M', '/cart',          'cart',         'member:cart',   'iconfont icon-gouwuche',   5, 0, 0),
(402, 0,   '我的订单',   'M', '/order/list',    'orderList',    'member:order',  'iconfont icon-dingdan',    6, 0, 0),
(500, 0,   '营销中心',   'M', '/marketing',     'marketing',    'marketing',     'iconfont icon-yingxiao',   7, 0, 0),
(501, 500, '优惠券管理', 'C', '/coupon/manage', 'couponManage', 'coupon:manage', NULL, 1, 0, 0),
(502, 501, '新增优惠券', 'F', '/coupon/add',    'couponAdd',    'coupon:add',    NULL, 0, 1, 0),
(503, 501, '修改优惠券', 'F', '/coupon/edit',   'couponEdit',   'coupon:edit',   NULL, 0, 1, 0),
(504, 501, '删除优惠券', 'F', '/coupon/del',    'couponDel',    'coupon:del',    NULL, 0, 1, 0);

-- 超管：全部新菜单；消费者(member)：浏览/购物车/订单；活动运营(market_op)：首页 + 营销中心
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 400),(1, 401),(1, 402),(1, 500),(1, 501),(1, 502),(1, 503),(1, 504),
(5, 400),(5, 401),(5, 402),
(3, 1),(3, 500),(3, 501),(3, 502),(3, 503),(3, 504);

-- ============================================================
-- P4 售后菜单（增量）：订单管理（运营）+ 发货/审核退款按钮
-- ============================================================
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `url_path`, `component`, `permit`, `icon`, `order_num`, `visible`, `status`) VALUES
(600, 0,   '订单管理', 'M', '/order/manage',       'orderManage',     'order:manage', 'iconfont icon-dingdan', 4, 0, 0),
(601, 600, '发货',     'F', '/order/ship',         'orderShip',       'order:ship',   NULL, 0, 1, 0),
(602, 600, '审核退款', 'F', '/order/refund/audit', 'orderRefundAudit','order:refund', NULL, 0, 1, 0);

-- 超管 + 商品运营（订单处理）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 600),(1, 601),(1, 602),
(2, 600),(2, 601),(2, 602);
