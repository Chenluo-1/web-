# -*- coding: utf-8 -*-
"""为 bulk 商品补 ec_product_spec（规格维度）行：python sql/backfill_product_specs.py
产出 sql/_fix_specs.sql

背景：gen_bulk_data.py 早期只写了 ec_sku，没有写 ec_product_spec，
导致商品详情弹窗拿不到规格维度（specDims 为空），加购按钮一直置灰。
"""
import json
import os
import subprocess
from collections import OrderedDict

MYSQL = r"C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "_fix_specs.sql")


def query(sql):
    out = subprocess.check_output(
        [MYSQL, "-uroot", "-p123456", "-N", "-B", "--default-character-set=utf8mb4", "-e", sql, "ecom"],
        stderr=subprocess.DEVNULL)
    return [line.split("\t") for line in out.decode("utf-8").splitlines() if line]


def esc(v):
    return str(v).replace("\\", "\\\\").replace("'", "''")


products = [int(r[0]) for r in query(
    "SELECT id FROM ec_product WHERE del_flag=0 AND id BETWEEN 3001 AND 3150 ORDER BY id")]
have = {int(r[0]) for r in query(
    "SELECT DISTINCT product_id FROM ec_product_spec WHERE del_flag=0")}

rows_by_product = {}
for pid, specs in query(
        "SELECT product_id, specs FROM ec_sku WHERE del_flag=0 AND product_id BETWEEN 3001 AND 3150 "
        "ORDER BY product_id, id"):
    dims = rows_by_product.setdefault(int(pid), OrderedDict())
    for name, value in json.loads(specs).items():
        dims.setdefault(name, [])
        if value not in dims[name]:
            dims[name].append(value)

# 值排序：纯数字按数值（38/40/42/44），其余保持词库出现顺序
for dims in rows_by_product.values():
    for name, values in dims.items():
        if values and all(v.isdigit() for v in values):
            dims[name] = sorted(values, key=int)

lines = ["-- 由 sql/backfill_product_specs.py 生成：补 ec_product_spec（规格维度）", "SET NAMES utf8mb4;"]
# 全量重写（不只是补缺）：顺带修正历史写入的规格值顺序
targets = [pid for pid in products if pid in rows_by_product]
if targets:
    lines.append("DELETE FROM ec_product_spec WHERE product_id IN (%s);"
                 % ",".join(str(p) for p in targets))
for pid in targets:
    for sort, (name, values) in enumerate(rows_by_product[pid].items()):
        lines.append("INSERT INTO ec_product_spec (product_id, spec_name, spec_values, sort) VALUES "
                     "(%d, '%s', '%s', %d);" % (pid, esc(name), esc(json.dumps(values, ensure_ascii=False)), sort))

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(lines) + "\n")

print("商品 %d 个（重写 %d 个，原缺规格 %d 个），写出行数 %d"
      % (len(products), len(targets), len([p for p in products if p not in have]),
         sum(len(rows_by_product[p]) for p in targets)))
print("输出：%s" % OUT)
