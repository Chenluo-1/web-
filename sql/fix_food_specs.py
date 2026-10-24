# -*- coding: utf-8 -*-
"""按叶子类目重写美食商品的规格维度：python sql/fix_food_specs.py
产出 sql/_fix_food_specs.sql

做三件事（都按 sku_id 关联，不改 SKU 主键，历史订单不丢）：
  1. 改写 ec_sku.specs（例如鸡蛋从 250g/1kg 变成 30 枚/3 斤）
  2. 重建 ec_product_spec（详情页规格单选的数据源）
  3. 同步 ec_order_item.specs 的规格快照
"""
import json
import os
import subprocess

from product_titles import food_spec

MYSQL = r"C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "_fix_food_specs.sql")


def query(sql):
    out = subprocess.check_output(
        [MYSQL, "-uroot", "-p123456", "-N", "-B", "--default-character-set=utf8mb4", "-e", sql, "ecom"],
        stderr=subprocess.DEVNULL)
    return [line.split("\t") for line in out.decode("utf-8").splitlines() if line]


def esc(v):
    return str(v).replace("\\", "\\\\").replace("'", "''")


cats = {int(r[0]): (int(r[1]), r[2]) for r in query("SELECT id, parent_id, name FROM ec_category")}


def root_of(cid):
    cur, guard = cid, 0
    while cur in cats and cats[cur][0] != 0 and guard < 10:
        cur = cats[cur][0]
        guard += 1
    return cats.get(cur, (0, ""))[1]


food = []
for pid, cid, title in query(
        "SELECT id, category_id, title FROM ec_product WHERE del_flag=0 AND id BETWEEN 3001 AND 3150"):
    leaf = cats[int(cid)][1]
    if root_of(int(cid)) == "美食" and food_spec(leaf, title):
        food.append((int(pid), leaf, title))

sku_rows = query("SELECT id, product_id, specs FROM ec_sku WHERE del_flag=0 AND product_id BETWEEN 3001 AND 3150 "
                 "ORDER BY product_id, id")
by_product = {}
for sid, pid, specs in sku_rows:
    by_product.setdefault(int(pid), []).append((int(sid), specs))

lines = ["-- 由 sql/fix_food_specs.py 生成：美食类规格维度按叶子类目重写", "SET NAMES utf8mb4;"]
spec_delete, spec_values = [], []
changed = 0
for pid, leaf, title in food:
    dim, pool = food_spec(leaf, title)
    skus = by_product.get(pid, [])
    if not skus:
        continue
    values = pool[:len(skus)]
    for (sid, old), value in zip(skus, values):
        new = json.dumps({dim: value}, ensure_ascii=False)
        if old.strip() != new:
            changed += 1
        lines.append("UPDATE ec_sku SET specs = '%s' WHERE id = %d;" % (esc(new), sid))
    spec_delete.append(pid)
    spec_values.append((pid, dim, values))

lines.append("")
lines.append("DELETE FROM ec_product_spec WHERE product_id IN (%s);" % ",".join(str(p) for p in spec_delete))
for pid, dim, values in spec_values:
    lines.append("INSERT INTO ec_product_spec (product_id, spec_name, spec_values, sort) VALUES "
                 "(%d, '%s', '%s', 0);" % (pid, esc(dim), esc(json.dumps(values, ensure_ascii=False))))

lines.append("")
lines.append("-- 订单明细的规格快照同步（按 sku_id 关联，历史订单不丢）")
lines.append("UPDATE ec_order_item i JOIN ec_sku s ON s.id = i.sku_id SET i.specs = s.specs "
             "WHERE s.product_id IN (%s);" % ",".join(str(p) for p in spec_delete))

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(lines) + "\n")

print("美食商品 %d 个，SKU 改规格 %d 个" % (len(food), changed))
for pid, dim, values in spec_values[:6]:
    print("  %d  %s: %s" % (pid, dim, " / ".join(values)))
print("输出：%s" % OUT)
