-- 由 sql/fix_product_titles.py 生成：按类目词库重写商品标题
SET NAMES utf8mb4;
UPDATE ec_product SET title = '乒乓拍套装 横拍长柄 两拍三球' WHERE id = 3140;  -- 原：专业乒乓球拍 双面反胶 第 2 批
UPDATE ec_product SET title = '热粘合无缝足球 5 号' WHERE id = 3144;  -- 原：5 号成人比赛足球 第 2 批
UPDATE ec_product SET title = 'PU 手缝训练足球 4 号' WHERE id = 3146;  -- 原：4 号青少年训练足球 第 2 批
UPDATE ec_product SET title = '软皮室内训练篮球 7 号' WHERE id = 3148;  -- 原：室内外通用 7 号篮球 第 2 批

UPDATE ec_order_item i JOIN (
SELECT 3140 AS pid, '乒乓拍套装 横拍长柄 两拍三球' AS nm
UNION ALL
SELECT 3144 AS pid, '热粘合无缝足球 5 号' AS nm
UNION ALL
SELECT 3146 AS pid, 'PU 手缝训练足球 4 号' AS nm
UNION ALL
SELECT 3148 AS pid, '软皮室内训练篮球 7 号' AS nm
) v ON v.pid = i.product_id SET i.product_name = v.nm;
