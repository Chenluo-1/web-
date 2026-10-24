# -*- coding: utf-8 -*-
"""
按类目词库重写批量生成商品（id 3001-3150）的标题：python sql/fix_product_titles.py
产出 sql/_fix_product_titles.sql

同时更新 ec_order_item.product_name 的历史快照，避免订单详情里还是旧标题。
title_for() 见 sql/product_titles.py。
"""
import os
import subprocess

from product_titles import title_for

MYSQL = r"C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "_fix_product_titles.sql")


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


rows = query("SELECT id, title, category_id FROM ec_product WHERE del_flag=0 AND id BETWEEN 3001 AND 3150")

seen, plan = {}, []
for pid, old, cid in sorted(rows, key=lambda r: int(r[0])):
    leaf = cats[int(cid)][1]
    idx = seen.get(leaf, 0)
    seen[leaf] = idx + 1
    new = title_for(leaf, idx)
    if new and new != old:
        plan.append((int(pid), old, new, leaf))

print("商品 %d 个，需改标题 %d 个（未覆盖类目 %d 个）"
      % (len(rows), len(plan), len(rows) - len(plan)))

lines = ["-- 由 sql/fix_product_titles.py 生成：按类目词库重写商品标题", "SET NAMES utf8mb4;"]
for pid, old, new, _ in plan:
    lines.append("UPDATE ec_product SET title = '%s' WHERE id = %d;  -- 原：%s" % (esc(new), pid, esc(old)))

if plan:
    parts = ["SELECT %d AS pid, '%s' AS nm" % (pid, esc(new)) for pid, _, new, _ in plan]
    lines.append("")
    lines.append("UPDATE ec_order_item i JOIN (")
    lines.append("\nUNION ALL\n".join(parts))
    lines.append(") v ON v.pid = i.product_id SET i.product_name = v.nm;")

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(lines) + "\n")

by_leaf = {}
for _, _, new, leaf in plan:
    by_leaf.setdefault(leaf, []).append(new)
for leaf in sorted(by_leaf):
    print("  %s(%d): %s" % (leaf, len(by_leaf[leaf]), " / ".join(by_leaf[leaf][:2])))
print("输出：%s" % OUT)
