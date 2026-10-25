# -*- coding: utf-8 -*-
"""给评价过少的商品补评价：python sql/backfill_reviews.py
产出 sql/_backfill_reviews.sql

背景：gen_bulk_data.py 的评价是从「已完成订单」里按 16% 抽样生成的，
订单集中在热销品上，导致 200 个在售商品里有 81 个一条评价都没有、62 个只有 1-2 条，
商品详情页看起来像「没人买过」。

做法：对每个评价数 < 5 的在售商品，从它自己的**已完成订单明细**里取还没评价过的，
补到 5 条（订单不够就有多少补多少），评分分布 5:70% / 4:22% / 3:8%，
评价时间落在收货之后 2-72 小时。全部可通过 order_item_id 追溯，不是凭空造的。
"""
import os
import random
import subprocess

random.seed(20261025)

MYSQL = r"C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "_backfill_reviews.sql")
ID_BASE = 300001
TARGET = 5

GOOD = ["质量超出预期，做工细致，物流也快。", "包装很仔细，实物和图片一致，五星。",
        "用了一周，没有任何问题，性价比高。", "和描述一致，家里人都说不错，会回购。",
        "客服回复很快，发货也及时，满意。", "第二次买了，一如既往地稳。",
        "这个价位买到这个品质，很划算。", "细节做得到位，没有异味。"]
MID = ["东西还行，就是发货慢了两天。", "整体不错，包装有点简陋。",
       "价格合适，质量一般般。", "和描述基本一致，颜色略有色差。"]
BAD = ["收到时外包装压坏了，客服处理一般。", "用了几次就出问题，有点失望。"]


def query(sql):
    out = subprocess.check_output(
        [MYSQL, "-uroot", "-p123456", "-N", "-B", "--default-character-set=utf8mb4", "-e", sql, "ecom"],
        stderr=subprocess.DEVNULL)
    return [line.split("\t") for line in out.decode("utf-8").splitlines() if line]


def esc(v):
    return str(v).replace("\\", "\\\\").replace("'", "''")


targets = query(
    "SELECT p.id, p.title, (SELECT COUNT(*) FROM ec_review r WHERE r.product_id = p.id AND r.del_flag = 0) AS c "
    "FROM ec_product p WHERE p.del_flag = 0 AND p.status = 1 HAVING c < %d ORDER BY p.id" % TARGET)

rows = []
rid = ID_BASE
for pid, title, cnt in targets:
    need = TARGET - int(cnt)
    items = query(
        "SELECT i.id, i.order_no, i.sku_id, o.user_id, IFNULL(o.receive_time, o.create_time) "
        "FROM ec_order_item i JOIN ec_order o ON o.id = i.order_id "
        "LEFT JOIN ec_review r ON r.order_item_id = i.id AND r.del_flag = 0 "
        "WHERE i.product_id = %s AND i.del_flag = 0 AND o.del_flag = 0 AND o.status = 3 AND r.id IS NULL "
        "ORDER BY o.create_time DESC LIMIT %d" % (pid, need))
    for item_id, order_no, sku_id, user_id, base_time in items:
        roll = random.random()
        if roll < 0.70:
            rating, content = 5, random.choice(GOOD)
        elif roll < 0.92:
            rating, content = 4, random.choice(MID)
        else:
            rating, content = 3, random.choice(BAD)
        rows.append((rid, order_no, int(item_id), int(user_id), int(pid), int(sku_id), rating, content, base_time))
        rid += 1

lines = ["-- 由 sql/backfill_reviews.py 生成：为评价过少的商品补齐评价", "SET NAMES utf8mb4;",
         "DELETE FROM ec_review WHERE id >= %d;" % ID_BASE]
for start in range(0, len(rows), 200):
    lines.append("INSERT INTO ec_review (id, order_no, order_item_id, user_id, product_id, sku_id, rating, "
                 "content, images, status, create_time, update_time, del_flag) VALUES")
    chunk = []
    for (r_id, order_no, item_id, user_id, pid, sku_id, rating, content, base_time) in rows[start:start + 200]:
        # 评价时间 = 收货时间 + 2~72 小时，落在库里现存时间范围内
        chunk.append("(%d,'%s',%d,%d,%d,%d,%d,'%s','',1, DATE_ADD('%s', INTERVAL %d HOUR), DATE_ADD('%s', INTERVAL %d HOUR), 0)"
                     % (r_id, esc(order_no), item_id, user_id, pid, sku_id, rating, esc(content),
                        base_time, random.randint(2, 72), base_time, 0))
    lines.append(",\n".join([c.replace(", 0)", ", 0)") for c in chunk]) + ";")

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(lines) + "\n")

print("待补商品 %d 个，生成评价 %d 条" % (len(targets), len(rows)))
print("输出：%s" % OUT)
