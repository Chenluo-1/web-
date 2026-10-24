# -*- coding: utf-8 -*-
"""
全量演示数据生成器：python sql/gen_bulk_data.py
产出：sql/_bulk_data.sql（先清后插，可重复执行）

覆盖：用户 / 地址 / 商品 / SKU / 订单 / 订单明细 / 行为埋点 / 评价 / 优惠券
规模（seed 固定，可复现）：
  新增用户    1000（id 2001-3000，密码同会员 123456）
  新增商品    150（id 3001-3150，每个 2-4 个 SKU）
  订单        ~760 天，近 30 天 ~90-140 单/天，更早 20-90 单/天
  行为        近 30 天曝光/点击/加购，约 8 万条
  评价        完成订单抽样约 4 千条
  优惠券      4 个新模板 + 覆盖部分用户的已领记录

数据量说明：生成的 SQL 约 30-40MB，导入需要 1-2 分钟；导入后再跑
  1) python sql/gen_product_images.py --upload --only <新商品id>   补商品图
  2) POST /dashboard/aggregate?days=760                            重建看板汇总
"""
import os
import random
import subprocess
from datetime import datetime, timedelta

from product_titles import title_for

random.seed(20261024)

MYSQL = r"C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
DB = "ecom"
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "_bulk_data.sql")

WINDOW_DAYS = 760
BEHAVIOR_DAYS = 30

USER_START, USER_COUNT = 2001, 1000
PRODUCT_START, PRODUCT_COUNT = 3001, 150
SKU_START = 90001
ADDR_START = 90001
REVIEW_START = 100001
COUPON_START = 101
ORDER_KEY = "demo-bulk-"

NOW = datetime.now().replace(microsecond=0)
TODAY = NOW.replace(hour=0, minute=0, second=0)


def query(sql):
    out = subprocess.check_output(
        [MYSQL, "-uroot", "-p123456", "-N", "-B", "--default-character-set=utf8mb4", "-e", sql, DB],
        stderr=subprocess.DEVNULL)
    return [line.split("\t") for line in out.decode("utf-8").splitlines() if line]


def esc(value):
    return str(value).replace("\\", "\\\\").replace("'", "''")


def fmt(d):
    return d.strftime("%Y-%m-%d %H:%M:%S")


# ---------------- 读取现有主数据 ----------------
cat_rows = query("SELECT id, parent_id, name FROM ec_category WHERE del_flag=0")
parents = {int(r[0]): int(r[1]) for r in cat_rows}
cnames = {int(r[0]): r[2] for r in cat_rows}
has_child = {p for p in parents.values() if p}
leaves = [(cid, cnames[cid]) for cid in cnames if cid not in has_child]


def root_of(cid):
    cur, guard = cid, 0
    while cur in parents and parents[cur] != 0 and guard < 10:
        cur = parents[cur]
        guard += 1
    return cnames.get(cur, "")


member_ids = [int(r[0]) for r in query(
    "SELECT u.id FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id "
    "JOIN sys_role r ON r.id=ur.role_id WHERE r.role_key='member' AND u.del_flag=0")]
pwd_hash = query("SELECT password FROM sys_user WHERE username='member1'")[0][0]

# ---------------- 用户 + 地址 ----------------
SURNAME = list("赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦章")
GIVEN = ["子涵", "梓萱", "一鸣", "思远", "书瑶", "嘉懿", "若彤", "天佑", "雨桐", "俊杰",
         "亦航", "晚晴", "知远", "清和", "沐辰", "以宁", "novel", "有容", "怀瑾", "锦程"] * 3
PROV = [("广东省", "深圳市", "南山区", "科技园路"), ("浙江省", "杭州市", "西湖区", "文三路"),
        ("江苏省", "南京市", "鼓楼区", "中山北路"), ("四川省", "成都市", "武侯区", "天府大道"),
        ("北京市", "北京市", "朝阳区", "建国路"), ("上海市", "上海市", "浦东新区", "张江路"),
        ("湖北省", "武汉市", "洪山区", "珞喻路"), ("陕西省", "西安市", "雁塔区", "科技路")]

users = []
for i in range(USER_COUNT):
    uid = USER_START + i
    nick = random.choice(SURNAME) + random.choice(GIVEN)
    users.append({"id": uid, "username": "u%d" % uid, "nick": nick})

addresses = []
for i, u in enumerate(users):
    prov, city, dist, road = random.choice(PROV)
    addresses.append((ADDR_START + i, u["id"], u["nick"], "1%02d%08d" % (i % 100, random.randint(0, 99999999)),
                      prov, city, dist, "%s %d 号" % (road, random.randint(1, 999))))

# ---------------- 商品 + SKU ----------------
THEME = {
    "服饰": {"adj": ["纯棉", "宽松", "修身", "加厚", "冰丝", "复古", "简约", "日系", "高街", "基础款", "薄款", "工装"],
             "suffix": ["男款", "女款", "情侣款", "夏季新款", "春秋款", "2026 新款"],
             "price": (59, 329)},
    "数码": {"brand": ["小米", "华为", "联想", "绿联", "倍思", "罗技", "索尼", "Anker", "雷蛇", "荣耀"],
             "suffix": ["标准版", "Pro", "旗舰版", "2026 款", "套装"],
             "price": (79, 5599)},
    "美食": {"origin": ["云南", "新疆", "内蒙古", "泰国", "东北", "海南", "福建", "潮汕"],
             "suffix": ["500g", "1kg", "礼盒装", "家庭装", "现摘", "独立小包"],
             "price": (19, 269)},
    "运动": {"adj": ["专业", "轻量", "加厚", "防滑", "便携", "户外", "速干", "缓震"],
             "suffix": ["训练款", "男女同款", "升级款", "户外款"],
             "price": (39, 899)},
}
DEFAULT_THEME = {"adj": ["精选", "经典", "升级"], "suffix": ["热销款", "同款"], "price": (29, 399)}

DIMS = {
    "服饰": [("颜色", ["黑色", "白色", "藏青", "雾灰", "卡其", "酒红", "浅蓝", "墨绿"]),
             ("尺码", ["S", "M", "L", "XL", "XXL"])],
    "运动": [("颜色", ["黑色", "白色", "荧光绿", "宝蓝", "碳灰"]),
             ("尺码", ["36", "38", "40", "42", "44"])],
    "数码": [("颜色", ["深空灰", "银色", "曜石黑", "雅川青"]),
             ("版本", ["标准版", "128G", "256G", "512G"])],
    "美食": [("规格", ["250g", "500g", "1kg"])],
}


title_seq = {}


def compose_title(root, leaf):
    # 所有类目都走成型标题库（sql/product_titles.py）：
    # 早先「品牌 + 类目 + 包装词」拼接会拼出「罗技 Mate 70 Pro」「缓震跳绳」这类不搭的标题
    idx = title_seq.get(leaf, 0)
    title_seq[leaf] = idx + 1
    title = title_for(leaf, idx)
    if title:
        return title
    t = THEME.get(root, DEFAULT_THEME)
    if "brand" in t:
        return "%s %s %s" % (random.choice(t["brand"]), leaf, random.choice(t["suffix"]))
    if "origin" in t:
        return "%s%s %s" % (random.choice(t["origin"]), leaf, random.choice(t["suffix"]))
    return "%s%s %s" % (random.choice(t["adj"]), leaf, random.choice(t["suffix"]))


products, skus = [], []
for i in range(PRODUCT_COUNT):
    pid = PRODUCT_START + i
    cid, leaf = random.choice(leaves)
    root = root_of(cid)
    t = THEME.get(root, DEFAULT_THEME)
    lo, hi = t["price"]
    base = round(random.uniform(lo, hi), 2)
    products.append({"id": pid, "title": compose_title(root, leaf), "cid": cid, "root": root, "base": base,
                     "desc": "%s｜%s 类目精选，支持 7 天无理由退换。" % (leaf, root)})

    dims = DIMS.get(root, [("规格", ["标准"])])
    combos = [{}]
    for name, values in dims:
        nxt = []
        for combo in combos:
            for v in random.sample(values, min(len(values), random.choice([2, 2, 3]))):
                item = dict(combo)
                item[name] = v
                nxt.append(item)
        combos = nxt
    combos = combos[:4] if len(combos) > 4 else combos
    for combo in combos:
        skus.append({"id": SKU_START + len(skus), "pid": pid,
                     "specs": "{%s}" % ", ".join('"%s": "%s"' % (k, v) for k, v in combo.items()),
                     "price": round(base * random.uniform(0.92, 1.16), 2),
                     "stock": random.randint(20, 500)})

sku_by_product = {}
for s in skus:
    sku_by_product.setdefault(s["pid"], []).append(s)

print("生成：用户 %d ｜ 商品 %d ｜ SKU %d" % (len(users), len(products), len(skus)))

# ---------------- 订单 ----------------
all_products = []
for pid, title, image, sales in query(
        "SELECT id, title, IFNULL(main_image,''), IFNULL(sales,0) FROM ec_product "
        "WHERE del_flag=0 AND status=1 AND id < %d" % PRODUCT_START):
    all_products.append({"id": int(pid), "title": title, "image": image, "sales": int(sales)})
all_products += [{"id": p["id"], "title": p["title"], "image": "", "sales": 0} for p in products]

all_skus = {}
for pid in [p["id"] for p in all_products]:
    if pid in sku_by_product:
        all_skus[pid] = sku_by_product[pid]
for sid, pid, specs, price in query(
        "SELECT id, product_id, specs, price FROM ec_sku WHERE del_flag=0 AND status=1 AND stock>0 AND product_id<%d" % PRODUCT_START):
    all_skus.setdefault(int(pid), []).append({"id": int(sid), "specs": specs, "price": float(price)})

all_products = [p for p in all_products if all_skus.get(p["id"])]
price_map = {p["id"]: sum(s["price"] for s in all_skus[p["id"]]) / len(all_skus[p["id"]]) for p in all_products}
weights = [0.02 + (1.0 + p["sales"] / 30.0) / (max(price_map[p["id"]], 20.0) ** 0.7) for p in all_products]

buyers = member_ids + [u["id"] for u in users]

# 买家地址：老用户从库里读，新用户用本脚本生成的地址
addr_map = {}
for uid, name, phone, prov, city, dist, detail in query(
        "SELECT user_id, receiver_name, phone, province, city, district, detail FROM ec_address WHERE del_flag=0"):
    addr_map.setdefault(int(uid), (name, phone, "%s%s%s%s" % (prov, city, dist, detail)))
for a in addresses:
    addr_map.setdefault(a[1], (a[2], a[3], "%s%s%s%s" % (a[4], a[5], a[6], a[7])))
DEFAULT_ADDR = ("演示用户", "13800000000", "广东省深圳市南山区科技园 1 号")
seq = 500000
orders = []
for i in range(WINDOW_DAYS):
    day = TODAY - timedelta(days=WINDOW_DAYS - 1 - i)
    base = 18 + 80.0 * (i / WINDOW_DAYS)
    if day.weekday() >= 5:
        base *= 1.35
    if day.day in (1, 11, 15, 18):
        base *= 1.45
    if (TODAY - day).days <= BEHAVIOR_DAYS:
        base *= 1.35
    n = max(4, int(round(base * random.uniform(0.82, 1.18))))

    day_end = NOW if day.date() == TODAY.date() else day + timedelta(days=1)
    span = max(60, int((day_end - day).total_seconds()) - 60)
    for _ in range(n):
        created = day + timedelta(seconds=random.randint(0, span))
        if created > NOW - timedelta(minutes=20):
            continue
        user = random.choice(buyers)
        picked = random.choices(all_products, weights=weights, k=random.choice([1, 1, 1, 2, 2, 3]))
        picked = list({p["id"]: p for p in picked}.values())

        items, total = [], 0.0
        for p in picked:
            sku = random.choice(all_skus[p["id"]])
            qty = random.choice([1, 1, 1, 2, 2, 3])
            subtotal = round(sku["price"] * qty, 2)
            total += subtotal
            items.append({"p": p, "sku": sku, "qty": qty, "subtotal": subtotal})

        age = (NOW - created).total_seconds() / 86400.0
        roll = random.random()
        if roll < 0.045:
            status = 4
        elif age >= 7:
            status = 3 if roll < 0.93 else (6 if roll < 0.975 else 2)
        elif age >= 2:
            status = 3 if roll < 0.5 else (2 if roll < 0.9 else (1 if roll < 0.96 else 6))
        else:
            status = 1 if roll < 0.55 else (2 if roll < 0.92 else (3 if roll < 0.96 else 6))

        pay = created + timedelta(minutes=random.randint(1, 9)) if status != 4 else None
        ship = receive = None
        if status in (2, 3, 6) and pay:
            ship = pay + timedelta(hours=random.randint(4, 40))
            if status == 3:
                receive = ship + timedelta(hours=random.randint(24, 96))
        if ship and ship > NOW:
            ship, receive, status = None, None, 1
        if receive and receive > NOW:
            receive, status = None, 2

        seq += 1
        orders.append({
            "no": "EC%s%06d" % (created.strftime("%Y%m%d%H%M"), seq),
            "user": user, "total": round(total, 2), "status": status,
            "create": created, "pay": pay, "ship": ship, "receive": receive,
            "cancel": created + timedelta(minutes=15) if status == 4 else None,
            "addr": addr_map.get(user) or DEFAULT_ADDR,
            "items": items,
        })

print("生成：订单 %d 笔（已支付 %d）" % (len(orders), sum(1 for o in orders if o["status"] in (1, 2, 3, 5))))

# ---------------- 行为埋点 ----------------
behaviors = []
for i in range(BEHAVIOR_DAYS):
    day = TODAY - timedelta(days=BEHAVIOR_DAYS - 1 - i)
    day_end = NOW if day.date() == TODAY.date() else day + timedelta(days=1)
    span = max(60, int((day_end - day).total_seconds()))
    day_orders = sum(1 for o in orders if o["create"].date() == day.date())
    expose = int(day_orders * 20) + random.randint(300, 600)
    click = int(expose * random.uniform(0.20, 0.27))
    cart = int(click * random.uniform(0.22, 0.32))

    for _ in range(expose):
        behaviors.append((random.choice(buyers), "expose", random.choices(all_products, weights=weights)[0]["id"],
                          day + timedelta(seconds=random.randint(0, span - 1))))
    for _ in range(click):
        behaviors.append((random.choice(buyers), "click", random.choices(all_products, weights=weights)[0]["id"],
                          day + timedelta(seconds=random.randint(0, span - 1))))
    for _ in range(cart):
        behaviors.append((random.choice(buyers), "cart", random.choices(all_products, weights=weights)[0]["id"],
                          day + timedelta(seconds=random.randint(0, span - 1))))

print("生成：行为 %d 条" % len(behaviors))

# ---------------- 评价（完成订单抽样） ----------------
GOOD = ["质量超出预期，做工细致，物流也快。", "包装很仔细，实物和图片一致，五星。",
        "用了一周，没有任何问题，性价比高。", "尺码合适，面料舒服，会回购。",
        "客服回复很快，发货也及时，满意。", "第二次买了，家里人很喜欢。"]
MID = ["东西还行，就是发货慢了两天。", "整体不错，包装有点简陋。", "价格合适，质量一般般。",
       "和描述基本一致，颜色略有色差。"]
BAD = ["收到时外包装压坏了，客服处理了一般。", "尺码偏小，建议买大一码。", "用了几次就出问题，有点失望。"]

reviews = []
rid = REVIEW_START
for o in orders:
    if o["status"] != 3 or rid >= REVIEW_START + 4200:
        continue
    if random.random() > 0.16:
        continue
    item = random.choice(o["items"])
    roll = random.random()
    if roll < 0.72:
        rating, content = 5, random.choice(GOOD)
    elif roll < 0.93:
        rating, content = 4, random.choice(MID)
    else:
        rating, content = 3, random.choice(BAD)
    reviews.append({"id": rid, "no": o["no"], "pid": item["p"]["id"], "sku": item["sku"]["id"],
                    "rating": rating, "content": content,
                    "time": o["receive"] + timedelta(hours=random.randint(2, 72))})
    rid += 1

print("生成：评价 %d 条" % len(reviews))

# ---------------- 优惠券模板 + 领取记录 ----------------
COUPONS = [
    (101, "满 500 减 80", 500, 80, 5000, 3),
    (102, "满 1000 减 150", 1000, 150, 3000, 2),
    (103, "满 99 减 10（回头客）", 99, 10, 8000, 5),
    (104, "满 300 减 40", 300, 40, 4000, 2),
]
grants = []
for cid, _, _, _, _, limit in COUPONS:
    for u in random.sample(buyers, int(len(buyers) * 0.22)):
        for _ in range(random.randint(1, limit)):
            grants.append((u, cid, TODAY - timedelta(days=random.randint(0, 60))))

print("生成：优惠券领取 %d 条" % len(grants))

# ---------------- 输出 SQL ----------------
out = []
w = out.append
w("-- 由 sql/gen_bulk_data.py 生成，可重复执行（先清后插）")
w("-- %s ｜ 用户 %d ｜ 商品 %d ｜ SKU %d ｜ 订单 %d ｜ 行为 %d ｜ 评价 %d"
  % (fmt(NOW), len(users), len(products), len(skus), len(orders), len(behaviors), len(reviews)))
w("SET NAMES utf8mb4;")
w("")
w("-- 1) 清理上一批生成的数据")
w("DELETE FROM ec_order WHERE idempotency_key LIKE '%s%%';" % ORDER_KEY)
w("DELETE FROM ec_order_item WHERE order_no NOT IN (SELECT order_no FROM ec_order);")
w("DELETE FROM ec_user_behavior WHERE create_time >= '%s';" % fmt(TODAY - timedelta(days=BEHAVIOR_DAYS + 1)))
w("DELETE FROM ec_review WHERE id >= %d;" % REVIEW_START)
w("DELETE FROM ec_user_coupon WHERE coupon_id BETWEEN %d AND %d;" % (COUPON_START, COUPON_START + 9))
w("DELETE FROM ec_coupon WHERE id BETWEEN %d AND %d;" % (COUPON_START, COUPON_START + 9))
w("DELETE FROM sys_user_role WHERE user_id BETWEEN %d AND %d;" % (USER_START, USER_START + USER_COUNT))
w("DELETE FROM ec_address WHERE user_id BETWEEN %d AND %d;" % (USER_START, USER_START + USER_COUNT))
w("DELETE FROM sys_user WHERE id BETWEEN %d AND %d;" % (USER_START, USER_START + USER_COUNT))
w("DELETE FROM ec_product_image WHERE product_id BETWEEN %d AND %d;" % (PRODUCT_START, PRODUCT_START + PRODUCT_COUNT))
w("DELETE FROM ec_sku WHERE product_id BETWEEN %d AND %d;" % (PRODUCT_START, PRODUCT_START + PRODUCT_COUNT))
w("DELETE FROM ec_product WHERE id BETWEEN %d AND %d;" % (PRODUCT_START, PRODUCT_START + PRODUCT_COUNT))
w("")

w("-- 2) 用户（密码与会员账号一致，123456）")
for start in range(0, len(users), 200):
    w("INSERT INTO sys_user (id,username,nick_name,password,user_type,status,create_time,update_time,del_flag) VALUES")
    rows = ["(%d,'%s','%s','%s',0,0,'%s','%s',0)" % (
        u["id"], u["username"], esc(u["nick"]), pwd_hash,
        fmt(TODAY - timedelta(days=random.randint(1, 760))), fmt(NOW)) for u in users[start:start + 200]]
    w(",\n".join(rows) + ";")

w("")
w("INSERT INTO sys_user_role (user_id, role_id, create_time, update_time) VALUES")
w(",\n".join("(%d,5,'%s','%s')" % (u["id"], fmt(NOW), fmt(NOW)) for u in users) + ";")

w("")
w("-- 3) 收货地址")
for start in range(0, len(addresses), 200):
    w("INSERT INTO ec_address (id,user_id,receiver_name,phone,province,city,district,detail,is_default,create_time,update_time,del_flag) VALUES")
    rows = ["(%d,%d,'%s','%s','%s','%s','%s','%s',%d,'%s','%s',0)" % (
        a[0], a[1], esc(a[2]), a[3], a[4], a[5], a[6], esc(a[7]),
        1 if i % 5 == 0 else 0, fmt(NOW), fmt(NOW)) for i, a in enumerate(addresses[start:start + 200])]
    w(",\n".join(rows) + ";")

w("")
w("-- 4) 商品 + SKU（主图稍后由 gen_product_images.py 补）")
for start in range(0, len(products), 100):
    w("INSERT INTO ec_product (id,title,category_id,main_image,description,status,price_min,price_max,sales,create_time,update_time,del_flag) VALUES")
    rows = []
    for p in products[start:start + 100]:
        prices = [s["price"] for s in sku_by_product[p["id"]]]
        rows.append("(%d,'%s',%d,NULL,'%s',1,%.2f,%.2f,0,'%s','%s',0)" % (
            p["id"], esc(p["title"]), p["cid"], esc(p["desc"]),
            min(prices), max(prices), fmt(TODAY - timedelta(days=random.randint(30, 700))), fmt(NOW)))
    w(",\n".join(rows) + ";")

w("")
for start in range(0, len(skus), 300):
    w("INSERT INTO ec_sku (id,product_id,specs,sku_code,price,stock,version,image,sales,status,create_time,update_time,del_flag) VALUES")
    rows = ["(%d,%d,'%s','SKU%d',%.2f,%d,0,NULL,0,1,'%s','%s',0)" % (
        s["id"], s["pid"], esc(s["specs"]), s["id"], s["price"], s["stock"], fmt(NOW), fmt(NOW))
        for s in skus[start:start + 300]]
    w(",\n".join(rows) + ";")

w("")
w("-- 5) 订单 + 明细")
for start in range(0, len(orders), 200):
    rows = []
    for o in orders[start:start + 200]:
        addr = o["addr"]
        pay = "'%s'" % fmt(o["pay"]) if o["pay"] else "NULL"
        ship = "'%s'" % fmt(o["ship"]) if o["ship"] else "NULL"
        recv = "'%s'" % fmt(o["receive"]) if o["receive"] else "NULL"
        cancel = "'%s'" % fmt(o["cancel"]) if o["cancel"] else "NULL"
        rows.append("('%s',%d,%.2f,0,%.2f,%d,'%s','%s','%s',%d,%s,%s,%s,%s,'演示补数',"
                    "'%s%s', '%s','%s','%s')" % (
                        o["no"], o["user"], o["total"], o["total"], o["status"],
                        esc(addr[0]), addr[1], esc(addr[2]),
                        random.choice([1, 1, 2]), pay, ship, recv, cancel,
                        ORDER_KEY, o["no"], fmt(o["create"] + timedelta(minutes=15)), fmt(o["create"]), fmt(o["create"])))
    body = ",\n".join(rows)
    w(("INSERT INTO ec_order (order_no,user_id,total_amount,discount_amount,pay_amount,status,receiver_name,"
       "receiver_phone,receiver_address,pay_type,pay_time,ship_time,receive_time,cancel_time,remark,"
       "idempotency_key,expire_time,create_time,update_time) VALUES\n") + body + ";")

w("")
item_sql = []
for o in orders:
    for it in o["items"]:
        item_sql.append("SELECT '%s' AS order_no,%d AS product_id,%d AS sku_id,'%s' AS product_name,"
                        "'%s' AS product_image,'%s' AS specs,%.2f AS price,%d AS quantity,%.2f AS subtotal" % (
                            o["no"], it["p"]["id"], it["sku"]["id"], esc(it["p"]["title"]),
                            esc(it["p"]["image"]), esc(it["sku"]["specs"]), it["sku"]["price"], it["qty"], it["subtotal"]))
for start in range(0, len(item_sql), 400):
    w("INSERT INTO ec_order_item (order_id,order_no,product_id,sku_id,product_name,product_image,specs,"
      "price,quantity,subtotal,create_time,update_time) SELECT o.id, v.order_no, v.product_id, v.sku_id, "
      "v.product_name, v.product_image, v.specs, v.price, v.quantity, v.subtotal, o.create_time, o.create_time "
      "FROM ec_order o JOIN (")
    w("\nUNION ALL\n".join(item_sql[start:start + 400]))
    w(") v ON v.order_no = o.order_no;")

w("")
w("-- 6) 行为埋点")
for start in range(0, len(behaviors), 500):
    w("INSERT INTO ec_user_behavior (user_id,event_type,target_type,target_id,create_time,update_time) VALUES")
    rows = ["(%d,'%s','product',%d,'%s','%s')" % (u, t, pid, fmt(ts), fmt(ts))
            for u, t, pid, ts in behaviors[start:start + 500]]
    w(",\n".join(rows) + ";")

w("")
w("-- 7) 评价")
for start in range(0, len(reviews), 300):
    w("INSERT INTO ec_review (id,order_no,order_item_id,user_id,product_id,sku_id,rating,content,images,"
      "status,create_time,update_time,del_flag) SELECT v.rid, v.order_no, i.id, o.user_id, v.product_id, "
      "v.sku_id, v.rating, v.content, '', 1, v.ctime, v.ctime, 0 FROM (")
    parts = []
    for r in reviews[start:start + 300]:
        parts.append("SELECT %d AS rid,'%s' AS order_no,%d AS product_id,%d AS sku_id,%d AS rating,'%s' AS content,'%s' AS ctime" % (
            r["id"], r["no"], r["pid"], r["sku"], r["rating"], esc(r["content"]), fmt(r["time"])))
    w("\nUNION ALL\n".join(parts))
    w(") v JOIN ec_order_item i ON i.order_no = v.order_no AND i.product_id = v.product_id AND i.sku_id = v.sku_id "
      "JOIN ec_order o ON o.order_no = v.order_no;")

w("")
w("-- 8) 优惠券模板 + 领取记录")
w("INSERT INTO ec_coupon (id,name,type,threshold_amount,discount_amount,total_count,received_count,per_user_limit,"
  "start_time,end_time,status,create_time,update_time,del_flag) VALUES")
w(",\n".join("(%d,'%s',0,%d,%d,%d,0,%d,'%s','%s',0,'%s','%s',0)" % (
    cid, esc(name), th, amt, total, limit,
    fmt(TODAY - timedelta(days=30)), fmt(TODAY + timedelta(days=120)), fmt(NOW), fmt(NOW))
    for cid, name, th, amt, total, limit in COUPONS) + ";")
w("")
for start in range(0, len(grants), 400):
    w("INSERT INTO ec_user_coupon (user_id,coupon_id,status,receive_time,create_time,update_time,del_flag) VALUES")
    w(",\n".join("(%d,%d,0,'%s','%s','%s',0)" % (u, c, fmt(ts), fmt(ts), fmt(ts))
                 for u, c, ts in grants[start:start + 400]) + ";")

w("")
# 累计销量重算：用 GROUP BY 派生表一次聚合再关联更新。
# 不要写成相关子查询（UPDATE p SET sales=(SELECT SUM.. WHERE product_id=p.id)），
# 十万级订单明细下每行都要全表扫一遍，实测能卡好几分钟。
w("-- 9) 累计销量按订单明细重算（先 GROUP BY 再 JOIN，避免相关子查询全表扫）")
w("UPDATE ec_product p JOIN (SELECT i.product_id AS pid, SUM(i.quantity) AS qty FROM ec_order_item i "
  "JOIN ec_order o ON o.id = i.order_id WHERE i.del_flag = 0 AND o.del_flag = 0 "
  "AND o.status IN (1,2,3,5) GROUP BY i.product_id) t ON t.pid = p.id SET p.sales = t.qty;")
w("UPDATE ec_sku s JOIN (SELECT i.sku_id AS sid, SUM(i.quantity) AS qty FROM ec_order_item i "
  "JOIN ec_order o ON o.id = i.order_id WHERE i.del_flag = 0 AND o.del_flag = 0 "
  "AND o.status IN (1,2,3,5) GROUP BY i.sku_id) t ON t.sid = s.id SET s.sales = t.qty;")

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(out) + "\n")

size = os.path.getsize(OUT) / 1024.0 / 1024.0
print("输出：%s（%.1f MB）" % (OUT, size))
print("新商品 id：%d-%d（补图：python sql/gen_product_images.py --upload --only %s）"
      % (PRODUCT_START, PRODUCT_START + PRODUCT_COUNT - 1,
         ",".join(str(PRODUCT_START + i) for i in range(0, PRODUCT_COUNT))))
