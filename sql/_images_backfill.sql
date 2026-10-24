-- 商品配图回填（Unsplash 实拍图，由 sql/fetch_unsplash_images.py 生成）
SET NAMES utf8mb4;
UPDATE ec_product SET main_image = 'http://localhost:9000/ecom/20261024/de984a59bdb54a6fb70a5422833daede.jpg' WHERE id = 1015;
UPDATE ec_sku SET image = 'http://localhost:9000/ecom/20261024/de984a59bdb54a6fb70a5422833daede.jpg' WHERE id = 5063;
UPDATE ec_sku SET image = 'http://localhost:9000/ecom/20261024/76b2a8ae6b684cbaa45f21017c7810f4.jpg' WHERE id = 5064;
UPDATE ec_sku SET image = 'http://localhost:9000/ecom/20261024/ac6296b737634689b4df8c53967fa1a7.jpg' WHERE id = 5065;
UPDATE ec_sku SET image = 'http://localhost:9000/ecom/20261024/de984a59bdb54a6fb70a5422833daede.jpg' WHERE id = 5066;

DELETE FROM ec_product_image WHERE product_id IN (1015);
INSERT INTO ec_product_image (product_id, url, sort, del_flag) VALUES (1015, 'http://localhost:9000/ecom/20261024/de984a59bdb54a6fb70a5422833daede.jpg', 0, 0);
INSERT INTO ec_product_image (product_id, url, sort, del_flag) VALUES (1015, 'http://localhost:9000/ecom/20261024/76b2a8ae6b684cbaa45f21017c7810f4.jpg', 1, 0);
INSERT INTO ec_product_image (product_id, url, sort, del_flag) VALUES (1015, 'http://localhost:9000/ecom/20261024/ac6296b737634689b4df8c53967fa1a7.jpg', 2, 0);
