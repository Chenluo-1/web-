# -*- coding: utf-8 -*-
"""
看板活跃数据补数：python sql/gen_live_data.py

【已被 sql/gen_bulk_data.py 取代，除非只要小批量数据，否则别跑这个】
两个脚本都会按时间窗清理 ec_user_behavior，跑完 bulk 再跑本脚本会把大批行为埋点删掉。

产出：sql/_live_data.sql（先清后插，可重复执行）

为什么需要它：
  原有演示数据（data.sql）的 500 笔订单集中在 2026-04 ~ 2026-09-15，
  看板的「今日实时 / 近30天趋势 / 近30天排行 / 近7天漏斗」窗口内全为空，
  页面看起来像没做。本脚本按「今天」向前补 400 天订单 + 近 30 天行为埋点，
  补完再调用 POST /dashboard/aggregate?days=400 重建 T+1 汇总表。

数据规模（seed 固定，结果可复现）：
  订单   ~400 天，近 30 天 15~25 单/天，更早 5~20 单/天，含已取消/已退款
  明细   每单 1~3 个商品，数量 1~2
  行为   近 30 天 expose/click/cart（漏斗比例约 600 : 140 : 35）
"""
import os
import random
import subprocess
from datetime import datetime, timedelta

random.seed(20261023)

MYSQL = r"C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
DB = "ecom"
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "_live_data.sql")

WINDOW_DAYS = 760          # 订单回溯天数（覆盖到去年同月，月趋势同比才有可比基数）
BEHAVIOR_DAYS = 30         # 行为埋点回溯天数
SEQ_START = 900001         # 订单号序号段（900001 起，避开 data.sql 的 000001~000999）

NOW = datetime.now().replace(microsecond=0)
TODAY = NOW.replace(hour=0, minute=0, second=0)


def query(sql):
    """执行查询并按制表符切分（-N 去表头，-B 批量模式）"""
    out = subprocess.check_output(
        [MYSQL, "-uroot", "-p123456", "-N", "-B", "--default-character-set=utf8mb4", "-e", sql, DB],
        stderr=subprocess.DEVNULL)
    return [line.split("\t") for line in out.decode("utf-8").splitlines() if line]


def esc(value):
    return str(value).replace("\\", "\\\\").replace("'", "''")


def fmt(d):
    return d.strftime("%Y-%m-%d %H:%M:%S")


def fmt_date(d):
    return d.strftime("%Y-%m-%d")


# ---------------- 读取现有主数据（避免硬编码商品/用户） ----------------
products = []
for pid, title, cid, image, sales in query(
        "SELECT id, title, category_id, IFNULL(main_image,''), IFNULL(sales,0) "
        "FROM ec_product WHERE del_flag=0 AND status=1"):
    products.append({"id": int(pid), "title": title, "cid": int(cid),
                     "image": image, "sales": int(sales)})

skus = {}
for sid, pid, specs, price, stock in query(
        "SELECT id, product_id, specs, price, stock FROM ec_sku "
        "WHERE del_flag=0 AND status=1 AND stock>0"):
    skus.setdefault(int(pid), []).append(
        {"id": int(sid), "specs": specs, "price": float(price)})

products = [p for p in products if p["id"] in skus]
if not products:
    raise SystemExit("没有可售商品，先执行 sql/ecom.sql + sql/data.sql")

users = [int(r[0]) for r in query(
    "SELECT u.id FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id "
    "JOIN sys_role r ON r.id=ur.role_id "
    "WHERE r.role_key='member' AND u.del_flag=0")]

addresses = {}
for uid, name, phone, prov, city, dist, detail in query(
        "SELECT user_id, receiver_name, phone, province, city, district, detail "
        "FROM ec_address WHERE del_flag=0"):
    addresses.setdefault(int(uid), []).append(
        (name, phone, "%s%s%s%s" % (prov, city, dist, detail)))

# 选品权重 = 销量热度 / 价格影响：高客单商品压低权重，
# 否则手机类目会独占 GMV，类目占比饼图变成一整块
for p in products:
    p["avg_price"] = sum(s["price"] for s in skus[p["id"]]) / len(skus[p["id"]])
# 0.015 的下限让高客单商品也能零星出单，避免「iPhone 30 天 0 销量」显得假
weights = [0.015 + (1.0 + p["sales"] / 25.0) / (max(p["avg_price"], 20.0) ** 0.7) for p in products]

# ---------------- 生成订单 ----------------
orders = []          # (order dict, [items])
product_qty = {}     # 商品累计销量，用于回写 ec_product.sales
sku_qty = {}
seq = SEQ_START

for i in range(WINDOW_DAYS):
    day = TODAY - timedelta(days=WINDOW_DAYS - 1 - i)
    base = 8 + 7.0 * (i / WINDOW_DAYS)                 # 缓慢增长 8 → 15
    if day.weekday() >= 5:
        base *= 1.35                                   # 周末
    if day.day in (1, 11, 15):
        base *= 1.5                                    # 小促日
    if (TODAY - day).days <= BEHAVIOR_DAYS:
        base *= 1.55                                   # 近 30 天更活跃
    n = max(3, int(round(base * random.uniform(0.8, 1.2))))

    day_end = NOW if day.date() == TODAY.date() else day + timedelta(days=1)
    for _ in range(n):
        created = day + timedelta(seconds=random.randint(0, max(60, int((day_end - day).total_seconds()) - 60)))
        # 支付超时窗口内的单还没支付（保持未支付态）
        if created > NOW - timedelta(minutes=20):
            continue
        user = random.choice(users)
        picked = random.choices(products, weights=weights, k=random.choice([1, 1, 2, 2, 3]))
        picked = list({p["id"]: p for p in picked}.values())

        items, total = [], 0.0
        for p in picked:
            sku = random.choice(skus[p["id"]])
            qty = random.choice([1, 1, 1, 2])
            subtotal = round(sku["price"] * qty, 2)
            total += subtotal
            items.append({"product": p, "sku": sku, "qty": qty, "subtotal": subtotal})
            product_qty[p["id"]] = product_qty.get(p["id"], 0) + qty
            sku_qty[sku["id"]] = sku_qty.get(sku["id"], 0) + qty

        age = (NOW - created).total_seconds() / 86400.0
        roll = random.random()
        if roll < 0.04:                       # 已取消（不计 GMV）
            status = 4
        elif age >= 7:
            status = 3 if roll < 0.92 else (6 if roll < 0.97 else 2)
        elif age >= 2:
            status = 3 if roll < 0.45 else (2 if roll < 0.88 else (1 if roll < 0.95 else 6))
        else:
            status = 1 if roll < 0.55 else (2 if roll < 0.9 else (3 if roll < 0.95 else 6))

        pay = created + timedelta(minutes=random.randint(1, 8)) if status != 4 else None
        ship = receive = None
        if status in (2, 3, 6) and pay:
            ship = pay + timedelta(hours=random.randint(4, 36))
            if status == 3:
                receive = ship + timedelta(hours=random.randint(24, 96))
        if ship and ship > NOW:
            ship, receive, status = None, None, 1
        if receive and receive > NOW:
            receive, status = None, 2

        addr = random.choice(addresses.get(user) or [("演示用户", "13800000000", "广东省深圳市南山区科技园 1 号")])
        seq += 1
        order_no = "EC%s%06d" % (created.strftime("%Y%m%d%H%M"), seq)
        orders.append({
            "order_no": order_no,
            "user_id": user,
            "total": round(total, 2),
            "status": status,
            "receiver": addr,
            "create": created,
            "pay": pay,
            "ship": ship,
            "receive": receive,
            "cancel": created + timedelta(minutes=15) if status == 4 else None,
            "items": items,
        })

# ---------------- 生成行为埋点（漏斗） ----------------
behaviors = []   # (user, type, productId, time)
for i in range(BEHAVIOR_DAYS):
    day = TODAY - timedelta(days=BEHAVIOR_DAYS - 1 - i)
    day_end = NOW if day.date() == TODAY.date() else day + timedelta(days=1)
    span = max(60, int((day_end - day).total_seconds()))
    expose = random.randint(520, 680)
    click = int(expose * random.uniform(0.20, 0.26))
    cart = int(click * random.uniform(0.22, 0.30))

    def at():
        return day + timedelta(seconds=random.randint(0, span - 1))

    for _ in range(expose):
        behaviors.append((random.choice(users), "expose", random.choices(products, weights=weights)[0]["id"], at()))
    for _ in range(click):
        behaviors.append((random.choice(users), "click", random.choices(products, weights=weights)[0]["id"], at()))
    for _ in range(cart):
        behaviors.append((random.choice(users), "cart", random.choices(products, weights=weights)[0]["id"], at()))

# ---------------- 输出 SQL ----------------
out = []
w = out.append
w("-- 由 sql/gen_live_data.py 生成，可重复执行（先清后插）")
w("-- 生成时间：%s ｜ 订单 %d 笔 ｜ 行为 %d 条" % (fmt(NOW), len(orders), len(behaviors)))
w("SET NAMES utf8mb4;")
w("DELETE FROM ec_order WHERE idempotency_key LIKE 'demo-live-%';")
w("DELETE FROM ec_order_item WHERE order_no NOT IN (SELECT order_no FROM ec_order);")
w("DELETE FROM ec_user_behavior WHERE create_time >= '%s';" % fmt(TODAY - timedelta(days=BEHAVIOR_DAYS + 1)))
w("")

cols = ("order_no,user_id,total_amount,discount_amount,pay_amount,status,"
        "receiver_name,receiver_phone,receiver_address,pay_type,pay_time,ship_time,"
        "receive_time,cancel_time,remark,idempotency_key,expire_time,create_time,update_time")
for start in range(0, len(orders), 200):
    w("INSERT INTO ec_order (%s) VALUES" % cols)
    rows = []
    for o in orders[start:start + 200]:
        pay = "'%s'" % fmt(o["pay"]) if o["pay"] else "NULL"
        ship = "'%s'" % fmt(o["ship"]) if o["ship"] else "NULL"
        receive = "'%s'" % fmt(o["receive"]) if o["receive"] else "NULL"
        cancel = "'%s'" % fmt(o["cancel"]) if o["cancel"] else "NULL"
        expire = "'%s'" % fmt(o["create"] + timedelta(minutes=15))
        rows.append("('%s',%d,%.2f,0,%.2f,%d,'%s','%s','%s',%d,%s,%s,%s,%s,'演示补数','demo-live-%s',%s,'%s','%s')" % (
            o["order_no"], o["user_id"], o["total"], o["total"], o["status"],
            esc(o["receiver"][0]), esc(o["receiver"][1]), esc(o["receiver"][2]),
            random.choice([1, 1, 2]),
            pay, ship, receive, cancel, o["order_no"], expire,
            fmt(o["create"]), fmt(o["create"])))
    w(",\n".join(rows) + ";")

w("")
icols = ("order_id,order_no,product_id,sku_id,product_name,product_image,specs,"
         "price,quantity,subtotal,create_time,update_time")
item_parts = []
for o in orders:
    for it in o["items"]:
        item_parts.append("SELECT '%s' AS order_no,%d AS product_id,%d AS sku_id,'%s' AS product_name,"
                          "'%s' AS product_image,'%s' AS specs,%.2f AS price,%d AS quantity,%.2f AS subtotal" % (
                              o["order_no"], it["product"]["id"], it["sku"]["id"],
                              esc(it["product"]["title"]), esc(it["product"]["image"]),
                              esc(it["sku"]["specs"]), it["sku"]["price"], it["qty"], it["subtotal"]))
# 分批 UNION ALL：单条语句过大（>4MB）会撞上 max_allowed_packet
for start in range(0, len(item_parts), 500):
    w("INSERT INTO ec_order_item (%s) SELECT o.id, v.order_no, v.product_id, v.sku_id, v.product_name, "
      "v.product_image, v.specs, v.price, v.quantity, v.subtotal, o.create_time, o.create_time "
      "FROM ec_order o JOIN (" % icols)
    w("\nUNION ALL\n".join(item_parts[start:start + 500]))
    w(") v ON v.order_no = o.order_no;")

w("")
for start in range(0, len(behaviors), 500):
    w("INSERT INTO ec_user_behavior (user_id,event_type,target_type,target_id,create_time,update_time) VALUES")
    rows = ["(%d,'%s','product',%d,'%s','%s')" % (u, t, pid, fmt(ts), fmt(ts))
            for u, t, pid, ts in behaviors[start:start + 500]]
    w(",\n".join(rows) + ";")

w("")
w("-- 累计销量按订单明细整表重算（可重复执行，不会像 +N 那样重复累加）")
w("UPDATE ec_product p SET p.sales = (SELECT IFNULL(SUM(i.quantity),0) FROM ec_order_item i "
  "JOIN ec_order o ON o.id = i.order_id WHERE i.product_id = p.id AND i.del_flag = 0 "
  "AND o.del_flag = 0 AND o.status IN (1,2,3,5));")
w("UPDATE ec_sku s SET s.sales = (SELECT IFNULL(SUM(i.quantity),0) FROM ec_order_item i "
  "JOIN ec_order o ON o.id = i.order_id WHERE i.sku_id = s.id AND i.del_flag = 0 "
  "AND o.del_flag = 0 AND o.status IN (1,2,3,5));")

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(out) + "\n")

paid = [o for o in orders if o["status"] in (1, 2, 3, 5)]
today_paid = [o for o in paid if o["create"].date() == TODAY.date()]
gmv = sum(o["total"] for o in paid)
print("输出：%s" % OUT)
print("订单 %d 笔（已支付口径 %d 笔）｜今日 %d 笔 / %.2f 元｜累计 GMV %.2f 元"
      % (len(orders), len(paid), len(today_paid), sum(o["total"] for o in today_paid), gmv))
print("行为 %d 条（近 %d 天）｜涉及商品 %d 个 / 用户 %d 个"
      % (len(behaviors), BEHAVIOR_DAYS, len(products), len(users)))
