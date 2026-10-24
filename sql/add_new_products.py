# -*- coding: utf-8 -*-
"""
新增商品数据（12 个 SPU / 44 个 SKU），风格与 data.sql 的演示数据一致。

【注意】这 12 个商品（1051-1062）与 sql/gen_bulk_data.py 生成的 3001-3150 概念重复
（联想小新、Mate 70、瑜伽垫等各出现两次），已在库中逻辑删除（del_flag=1）。
**不要随便重跑本脚本**，否则它们会重新上架；确需使用请先确认没有重复商品。

固定 id 段（避免与既有数据冲突）：
    商品 1051-1062（现有最大 1050）
    规格 3071+     （现有最大 3070）
    SKU  5147+     （现有最大 5146）

用法：
    python sql/add_new_products.py            # 生成 sql/_new_products.sql 并执行
    python sql/add_new_products.py --dry-run  # 只生成 SQL，不执行

可重复执行：按固定 id 先删后插（订单/购物车若已引用这些新商品，重跑会先清掉引用行）。
"""
import argparse
import json
import os
import subprocess
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
MYSQL = r'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'

# (标题, 叶子类目, 描述, 规格:[(规格名,[值...])], 价格矩阵[(规格组合 dict, 价格, 库存, 销量)])
PRODUCTS = [
    # ---------------- 服饰 ----------------
    ('羊毛混纺针织开衫女宽松慵懒风', '女式外套', '羊毛混纺纱线，落肩宽松版型，秋季外搭保暖不臃肿。',
     [('颜色', ['米白', '燕麦灰']), ('尺码', ['S', 'M'])],
     [({'颜色': '米白', '尺码': 'S'}, 199.00, 180, 6),
      ({'颜色': '米白', '尺码': 'M'}, 209.00, 240, 9),
      ({'颜色': '燕麦灰', '尺码': 'S'}, 219.00, 150, 4),
      ({'颜色': '燕麦灰', '尺码': 'M'}, 229.00, 96, 7)]),

    ('重磅纯棉oversize短袖T恤男', 'T恤', '260g 重磅纯棉，落肩 oversize 版型，水洗不易变形。',
     [('颜色', ['黑色', '白色']), ('尺码', ['M', 'L', 'XL'])],
     [({'颜色': '黑色', '尺码': 'M'}, 89.00, 320, 12),
      ({'颜色': '黑色', '尺码': 'L'}, 92.00, 410, 18),
      ({'颜色': '黑色', '尺码': 'XL'}, 96.00, 180, 7),
      ({'颜色': '白色', '尺码': 'M'}, 89.00, 260, 15),
      ({'颜色': '白色', '尺码': 'L'}, 92.00, 380, 21),
      ({'颜色': '白色', '尺码': 'XL'}, 109.00, 120, 5)]),

    ('高腰牛仔阔腿裤女显瘦垂感', '休闲裤', '高腰阔腿版型，垂感挺括面料，遮肉显瘦四季可穿。',
     [('颜色', ['浅蓝', '深蓝']), ('尺码', ['26', '27', '28'])],
     [({'颜色': '浅蓝', '尺码': '26'}, 159.00, 140, 8),
      ({'颜色': '浅蓝', '尺码': '27'}, 165.00, 210, 11),
      ({'颜色': '浅蓝', '尺码': '28'}, 172.00, 130, 6),
      ({'颜色': '深蓝', '尺码': '26'}, 169.00, 160, 9),
      ({'颜色': '深蓝', '尺码': '27'}, 175.00, 240, 14),
      ({'颜色': '深蓝', '尺码': '28'}, 189.00, 88, 3)]),

    # ---------------- 数码 ----------------
    ('华为 MateBook 14 轻薄笔记本电脑', '笔记本', '14 英寸 2.8K 触控屏，轻薄机身，多屏协同办公。',
     [('颜色', ['深空灰', '皓月银']), ('配置', ['16G+512G', '32G+1T'])],
     [({'颜色': '深空灰', '配置': '16G+512G'}, 4999.00, 60, 3),
      ({'颜色': '深空灰', '配置': '32G+1T'}, 6299.00, 32, 1),
      ({'颜色': '皓月银', '配置': '16G+512G'}, 5099.00, 48, 2),
      ({'颜色': '皓月银', '配置': '32G+1T'}, 6399.00, 20, 1)]),

    ('Air Pro 主动降噪无线蓝牙耳机', '耳机', '42dB 主动降噪，通透模式一键切换，续航 36 小时。',
     [('颜色', ['皓月白', '曜石黑'])],
     [({'颜色': '皓月白'}, 399.00, 420, 26),
      ({'颜色': '曜石黑'}, 449.00, 330, 19)]),

    ('小米手环 10 NFC版 智能手表', '智能穿戴', '1.72 英寸大屏，全天心率与睡眠监测，支持 NFC 门禁。',
     [('颜色', ['曜石黑', '云杉绿'])],
     [({'颜色': '曜石黑'}, 249.00, 560, 44),
      ({'颜色': '云杉绿'}, 279.00, 380, 27)]),

    # ---------------- 美食 ----------------
    ('云南小粒咖啡豆 中深烘焙 500g', '咖啡', '云南高海拔小粒种，中深烘焙，坚果与焦糖风味明显。',
     [('规格', ['整豆', '现磨'])],
     [({'规格': '整豆'}, 78.00, 300, 22),
      ({'规格': '现磨'}, 88.00, 210, 16)]),

    ('泰国金枕头榴莲干 冻干锁鲜', '水果', '泰国金枕头榴莲，冻干工艺锁住果香，酥脆不粘手。',
     [('规格', ['100g', '200g'])],
     [({'规格': '100g'}, 45.00, 480, 38),
      ({'规格': '200g'}, 79.00, 260, 21)]),

    ('手工黄油曲奇礼盒 混合口味', '饼干糕点', '动物黄油手工烘焙，四种口味混装，送礼体面。',
     [('规格', ['400g', '800g'])],
     [({'规格': '400g'}, 69.00, 350, 25),
      ({'规格': '800g'}, 118.00, 180, 12)]),

    # ---------------- 运动 ----------------
    ('专业缓震跑步鞋 男女同款', '跑步鞋', '全掌回弹中底，透气飞织鞋面，长距离跑步缓震护膝。',
     [('颜色', ['黑白', '氟绿']), ('尺码', ['40', '41', '42', '43'])],
     [({'颜色': '黑白', '尺码': '40'}, 359.00, 120, 8),
      ({'颜色': '黑白', '尺码': '41'}, 369.00, 200, 15),
      ({'颜色': '黑白', '尺码': '42'}, 379.00, 260, 22),
      ({'颜色': '黑白', '尺码': '43'}, 389.00, 90, 5),
      ({'颜色': '氟绿', '尺码': '40'}, 399.00, 80, 4),
      ({'颜色': '氟绿', '尺码': '41'}, 409.00, 150, 10),
      ({'颜色': '氟绿', '尺码': '42'}, 419.00, 180, 13),
      ({'颜色': '氟绿', '尺码': '43'}, 419.00, 70, 3)]),

    ('加厚防滑瑜伽垫 183x61cm 家用', '瑜伽用品', 'TPE 双面防滑纹理，10mm 加厚回弹，护膝护腕。',
     [('颜色', ['雾霾紫', '森林绿']), ('厚度', ['8mm', '10mm'])],
     [({'颜色': '雾霾紫', '厚度': '8mm'}, 129.00, 220, 17),
      ({'颜色': '雾霾紫', '厚度': '10mm'}, 159.00, 160, 11),
      ({'颜色': '森林绿', '厚度': '8mm'}, 139.00, 200, 14),
      ({'颜色': '森林绿', '厚度': '10mm'}, 169.00, 130, 8)]),

    ('户外轻量化羽绒睡袋 秋冬加厚', '露营装备', '90 白鸭绒填充，可拼接双人使用，适合秋冬露营。',
     [('款式', ['信封式', '木乃伊式'])],
     [({'款式': '信封式'}, 549.00, 90, 6),
      ({'款式': '木乃伊式'}, 689.00, 55, 3)]),
]

PRODUCT_ID_BASE = 1051
SPEC_ID_BASE = 3071
SKU_ID_BASE = 5147


def find_category(leaf):
    """按叶子类目名取 category_id（重名取 id 最小的）"""
    sql = ("SELECT id FROM ecom.ec_category WHERE name = '%s' AND del_flag = 0 ORDER BY id LIMIT 1;" % leaf)
    out = subprocess.run([MYSQL, '-uroot', '-p123456', '--default-character-set=utf8mb4', '-N', '-B', '-e', sql],
                         capture_output=True)
    for line in out.stdout.decode('utf-8').splitlines():
        line = line.strip()
        if line.isdigit():
            return int(line)
    raise SystemExit('类目不存在：%s' % leaf)


def build_sql():
    lines = [
        '-- ============================================================',
        '-- 新增商品数据（12 SPU / 44 SKU），由 sql/add_new_products.py 生成',
        '-- 固定 id 段：商品 1051+ / 规格 3071+ / SKU 5147+',
        '-- 可重复执行：先按 id 段清理，再插入',
        '-- ============================================================',
        'SET NAMES utf8mb4;',
        '',
        '-- ---------- 清理本脚本管辖的 id 段（含引用行，保证可重复执行） ----------',
        '-- 说明：不清理订单/订单明细——ec_order_item 存的是商品快照（名称/图片/规格），',
        '-- 删商品不影响历史订单展示；若已被下单引用，重跑只影响新商品的在售数据。',
        'DELETE FROM ec_cart WHERE sku_id BETWEEN %d AND %d;' % (SKU_ID_BASE, SKU_ID_BASE + 200),
        'DELETE FROM ec_product_image WHERE product_id BETWEEN %d AND %d;'
        % (PRODUCT_ID_BASE, PRODUCT_ID_BASE + len(PRODUCTS) - 1),
        'DELETE FROM ec_sku WHERE product_id BETWEEN %d AND %d;'
        % (PRODUCT_ID_BASE, PRODUCT_ID_BASE + len(PRODUCTS) - 1),
        'DELETE FROM ec_product_spec WHERE product_id BETWEEN %d AND %d;'
        % (PRODUCT_ID_BASE, PRODUCT_ID_BASE + len(PRODUCTS) - 1),
        'DELETE FROM ec_product WHERE id BETWEEN %d AND %d;'
        % (PRODUCT_ID_BASE, PRODUCT_ID_BASE + len(PRODUCTS) - 1),
        '',
    ]

    pid = PRODUCT_ID_BASE
    sid = SPEC_ID_BASE
    kid = SKU_ID_BASE
    for title, leaf, desc, specs, skus in PRODUCTS:
        cat_id = find_category(leaf)
        prices = [s[1] for s in skus]
        price_min = min(prices)
        price_max = max(prices)
        total_sales = sum(s[3] for s in skus)

        lines.append('-- ---------- %s ----------' % title)
        lines.append(
            "INSERT INTO ec_product (id, title, category_id, description, status, price_min, price_max, sales, del_flag) "
            "VALUES (%d, '%s', %d, '%s', 1, %.2f, %.2f, %d, 0);"
            % (pid, title, cat_id, desc, price_min, price_max, total_sales))

        for sort, (spec_name, values) in enumerate(specs):
            lines.append(
                "INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) "
                "VALUES (%d, %d, '%s', '%s', %d, 0);"
                % (sid, pid, spec_name, json.dumps(values, ensure_ascii=False), sort))
            sid += 1

        for combo, price, stock, sales in skus:
            lines.append(
                "INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, sales, status, del_flag) "
                "VALUES (%d, %d, '%s', 'SKU%d', %.2f, %d, 0, %d, 1, 0);"
                % (kid, pid, json.dumps(combo, ensure_ascii=False), kid, price, stock, sales))
            kid += 1
        lines.append('')
        pid += 1

    lines.append('-- 统计：商品 %d 个 / 规格 %d 条 / SKU %d 个'
                 % (pid - PRODUCT_ID_BASE, sid - SPEC_ID_BASE, kid - SKU_ID_BASE))
    return '\n'.join(lines) + '\n'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true', help='只生成 SQL，不执行')
    args = ap.parse_args()

    sql = build_sql()
    path = os.path.join(BASE, '_new_products.sql')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(sql)
    print('SQL 已生成：%s' % path)

    if args.dry_run:
        return
    with open(path, 'rb') as f:
        out = subprocess.run([MYSQL, '-uroot', '-p123456', '--default-character-set=utf8mb4', 'ecom'],
                             stdin=f, capture_output=True)
    if out.returncode != 0:
        print('执行失败：')
        print(out.stderr.decode('utf-8', 'replace'))
        sys.exit(1)
    print('已导入数据库。商品 %d 个 / SKU %d 个'
          % (len(PRODUCTS), sum(len(p[4]) for p in PRODUCTS)))


if __name__ == '__main__':
    main()
