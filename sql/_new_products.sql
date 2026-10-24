-- ============================================================
-- 新增商品数据（12 SPU / 44 SKU），由 sql/add_new_products.py 生成
-- 固定 id 段：商品 1051+ / 规格 3071+ / SKU 5147+
-- 可重复执行：先按 id 段清理，再插入
-- ============================================================
SET NAMES utf8mb4;

-- ---------- 清理本脚本管辖的 id 段（含引用行，保证可重复执行） ----------
-- 说明：不清理订单/订单明细——ec_order_item 存的是商品快照（名称/图片/规格），
-- 删商品不影响历史订单展示；若已被下单引用，重跑只影响新商品的在售数据。
DELETE FROM ec_cart WHERE sku_id BETWEEN 5147 AND 5347;
DELETE FROM ec_product_image WHERE product_id BETWEEN 1051 AND 1062;
DELETE FROM ec_sku WHERE product_id BETWEEN 1051 AND 1062;
DELETE FROM ec_product_spec WHERE product_id BETWEEN 1051 AND 1062;
DELETE FROM ec_product WHERE id BETWEEN 1051 AND 1062;

-- ---------- 羊毛混纺针织开衫女宽松慵懒风 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1051, '羊毛混纺针织开衫女宽松慵懒风', 405, '羊毛混纺纱线，落肩宽松版型，秋季外搭保暖不臃肿。', 1, 199.00, 229.00, 26, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3071, 1051, '颜色', '["米白", "燕麦灰"]', 0, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3072, 1051, '尺码', '["S", "M"]', 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5147, 1051, '{"颜色": "米白", "尺码": "S"}', 'SKU5147', 199.00, 180, 0, 6, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5148, 1051, '{"颜色": "米白", "尺码": "M"}', 'SKU5148', 209.00, 240, 0, 9, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5149, 1051, '{"颜色": "燕麦灰", "尺码": "S"}', 'SKU5149', 219.00, 150, 0, 4, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5150, 1051, '{"颜色": "燕麦灰", "尺码": "M"}', 'SKU5150', 229.00, 96, 0, 7, 1, 0);

-- ---------- 重磅纯棉oversize短袖T恤男 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1052, '重磅纯棉oversize短袖T恤男', 3, '260g 重磅纯棉，落肩 oversize 版型，水洗不易变形。', 1, 89.00, 109.00, 78, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3073, 1052, '颜色', '["黑色", "白色"]', 0, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3074, 1052, '尺码', '["M", "L", "XL"]', 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5151, 1052, '{"颜色": "黑色", "尺码": "M"}', 'SKU5151', 89.00, 320, 0, 12, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5152, 1052, '{"颜色": "黑色", "尺码": "L"}', 'SKU5152', 92.00, 410, 0, 18, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5153, 1052, '{"颜色": "黑色", "尺码": "XL"}', 'SKU5153', 96.00, 180, 0, 7, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5154, 1052, '{"颜色": "白色", "尺码": "M"}', 'SKU5154', 89.00, 260, 0, 15, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5155, 1052, '{"颜色": "白色", "尺码": "L"}', 'SKU5155', 92.00, 380, 0, 21, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5156, 1052, '{"颜色": "白色", "尺码": "XL"}', 'SKU5156', 109.00, 120, 0, 5, 1, 0);

-- ---------- 高腰牛仔阔腿裤女显瘦垂感 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1053, '高腰牛仔阔腿裤女显瘦垂感', 404, '高腰阔腿版型，垂感挺括面料，遮肉显瘦四季可穿。', 1, 159.00, 189.00, 51, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3075, 1053, '颜色', '["浅蓝", "深蓝"]', 0, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3076, 1053, '尺码', '["26", "27", "28"]', 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5157, 1053, '{"颜色": "浅蓝", "尺码": "26"}', 'SKU5157', 159.00, 140, 0, 8, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5158, 1053, '{"颜色": "浅蓝", "尺码": "27"}', 'SKU5158', 165.00, 210, 0, 11, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5159, 1053, '{"颜色": "浅蓝", "尺码": "28"}', 'SKU5159', 172.00, 130, 0, 6, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5160, 1053, '{"颜色": "深蓝", "尺码": "26"}', 'SKU5160', 169.00, 160, 0, 9, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5161, 1053, '{"颜色": "深蓝", "尺码": "27"}', 'SKU5161', 175.00, 240, 0, 14, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5162, 1053, '{"颜色": "深蓝", "尺码": "28"}', 'SKU5162', 189.00, 88, 0, 3, 1, 0);

-- ---------- 华为 MateBook 14 轻薄笔记本电脑 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1054, '华为 MateBook 14 轻薄笔记本电脑', 131, '14 英寸 2.8K 触控屏，轻薄机身，多屏协同办公。', 1, 4999.00, 6399.00, 7, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3077, 1054, '颜色', '["深空灰", "皓月银"]', 0, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3078, 1054, '配置', '["16G+512G", "32G+1T"]', 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5163, 1054, '{"颜色": "深空灰", "配置": "16G+512G"}', 'SKU5163', 4999.00, 60, 0, 3, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5164, 1054, '{"颜色": "深空灰", "配置": "32G+1T"}', 'SKU5164', 6299.00, 32, 0, 1, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5165, 1054, '{"颜色": "皓月银", "配置": "16G+512G"}', 'SKU5165', 5099.00, 48, 0, 2, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5166, 1054, '{"颜色": "皓月银", "配置": "32G+1T"}', 'SKU5166', 6399.00, 20, 0, 1, 1, 0);

-- ---------- Air Pro 主动降噪无线蓝牙耳机 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1055, 'Air Pro 主动降噪无线蓝牙耳机', 141, '42dB 主动降噪，通透模式一键切换，续航 36 小时。', 1, 399.00, 449.00, 45, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3079, 1055, '颜色', '["皓月白", "曜石黑"]', 0, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5167, 1055, '{"颜色": "皓月白"}', 'SKU5167', 399.00, 420, 0, 26, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5168, 1055, '{"颜色": "曜石黑"}', 'SKU5168', 449.00, 330, 0, 19, 1, 0);

-- ---------- 小米手环 10 NFC版 智能手表 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1056, '小米手环 10 NFC版 智能手表', 153, '1.72 英寸大屏，全天心率与睡眠监测，支持 NFC 门禁。', 1, 249.00, 279.00, 71, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3080, 1056, '颜色', '["曜石黑", "云杉绿"]', 0, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5169, 1056, '{"颜色": "曜石黑"}', 'SKU5169', 249.00, 560, 0, 44, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5170, 1056, '{"颜色": "云杉绿"}', 'SKU5170', 279.00, 380, 0, 27, 1, 0);

-- ---------- 云南小粒咖啡豆 中深烘焙 500g ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1057, '云南小粒咖啡豆 中深烘焙 500g', 231, '云南高海拔小粒种，中深烘焙，坚果与焦糖风味明显。', 1, 78.00, 88.00, 38, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3081, 1057, '规格', '["整豆", "现磨"]', 0, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5171, 1057, '{"规格": "整豆"}', 'SKU5171', 78.00, 300, 0, 22, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5172, 1057, '{"规格": "现磨"}', 'SKU5172', 88.00, 210, 0, 16, 1, 0);

-- ---------- 泰国金枕头榴莲干 冻干锁鲜 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1058, '泰国金枕头榴莲干 冻干锁鲜', 221, '泰国金枕头榴莲，冻干工艺锁住果香，酥脆不粘手。', 1, 45.00, 79.00, 59, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3082, 1058, '规格', '["100g", "200g"]', 0, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5173, 1058, '{"规格": "100g"}', 'SKU5173', 45.00, 480, 0, 38, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5174, 1058, '{"规格": "200g"}', 'SKU5174', 79.00, 260, 0, 21, 1, 0);

-- ---------- 手工黄油曲奇礼盒 混合口味 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1059, '手工黄油曲奇礼盒 混合口味', 212, '动物黄油手工烘焙，四种口味混装，送礼体面。', 1, 69.00, 118.00, 37, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3083, 1059, '规格', '["400g", "800g"]', 0, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5175, 1059, '{"规格": "400g"}', 'SKU5175', 69.00, 350, 0, 25, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5176, 1059, '{"规格": "800g"}', 'SKU5176', 118.00, 180, 0, 12, 1, 0);

-- ---------- 专业缓震跑步鞋 男女同款 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1060, '专业缓震跑步鞋 男女同款', 311, '全掌回弹中底，透气飞织鞋面，长距离跑步缓震护膝。', 1, 359.00, 419.00, 80, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3084, 1060, '颜色', '["黑白", "氟绿"]', 0, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3085, 1060, '尺码', '["40", "41", "42", "43"]', 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5177, 1060, '{"颜色": "黑白", "尺码": "40"}', 'SKU5177', 359.00, 120, 0, 8, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5178, 1060, '{"颜色": "黑白", "尺码": "41"}', 'SKU5178', 369.00, 200, 0, 15, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5179, 1060, '{"颜色": "黑白", "尺码": "42"}', 'SKU5179', 379.00, 260, 0, 22, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5180, 1060, '{"颜色": "黑白", "尺码": "43"}', 'SKU5180', 389.00, 90, 0, 5, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5181, 1060, '{"颜色": "氟绿", "尺码": "40"}', 'SKU5181', 399.00, 80, 0, 4, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5182, 1060, '{"颜色": "氟绿", "尺码": "41"}', 'SKU5182', 409.00, 150, 0, 10, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5183, 1060, '{"颜色": "氟绿", "尺码": "42"}', 'SKU5183', 419.00, 180, 0, 13, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5184, 1060, '{"颜色": "氟绿", "尺码": "43"}', 'SKU5184', 419.00, 70, 0, 3, 1, 0);

-- ---------- 加厚防滑瑜伽垫 183x61cm 家用 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1061, '加厚防滑瑜伽垫 183x61cm 家用', 321, 'TPE 双面防滑纹理，10mm 加厚回弹，护膝护腕。', 1, 129.00, 169.00, 50, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3086, 1061, '颜色', '["雾霾紫", "森林绿"]', 0, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3087, 1061, '厚度', '["8mm", "10mm"]', 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5185, 1061, '{"颜色": "雾霾紫", "厚度": "8mm"}', 'SKU5185', 129.00, 220, 0, 17, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5186, 1061, '{"颜色": "雾霾紫", "厚度": "10mm"}', 'SKU5186', 159.00, 160, 0, 11, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5187, 1061, '{"颜色": "森林绿", "厚度": "8mm"}', 'SKU5187', 139.00, 200, 0, 14, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5188, 1061, '{"颜色": "森林绿", "厚度": "10mm"}', 'SKU5188', 169.00, 130, 0, 8, 1, 0);

-- ---------- 户外轻量化羽绒睡袋 秋冬加厚 ----------
INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) VALUES (1062, '户外轻量化羽绒睡袋 秋冬加厚', 331, '90 白鸭绒填充，可拼接双人使用，适合秋冬露营。', 1, 549.00, 689.00, 9, 0);
INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES (3088, 1062, '款式', '["信封式", "木乃伊式"]', 0, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5189, 1062, '{"款式": "信封式"}', 'SKU5189', 549.00, 90, 0, 6, 1, 0);
INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) VALUES (5190, 1062, '{"款式": "木乃伊式"}', 'SKU5190', 689.00, 55, 0, 3, 1, 0);

-- 统计：商品 12 个 / 规格 18 条 / SKU 44 个
