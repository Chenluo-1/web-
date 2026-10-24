# -*- coding: utf-8 -*-
"""
用 Unsplash 按商品品名搜索真实商品照片，替换商品配图。

要点：
  - 每个叶子类目一个英文搜索词（Unsplash 内容以英文为主，中文搜不准），
    个别商品（水果、糕点等）用商品级词覆盖，保证"橙子就是橙子"；
  - 一个搜索词只发一次请求（per_page=30），同类目多个商品从不同下标取图，
    既省额度又不会所有商品撞同一张；
  - 图片下载后统一转 800px JPEG 再上传 MinIO（离线演示也不会裂图）。

用法：
    python sql/fetch_unsplash_images.py --pilot 8      # 前 8 个商品，只下载预览
    python sql/fetch_unsplash_images.py --only 1036,1030
    python sql/fetch_unsplash_images.py --upload       # 全量替换并输出回填 SQL

Access Key 读取顺序：环境变量 UNSPLASH_ACCESS_KEY > sql/_unsplash_key.txt（已被 .gitignore 忽略）
前置：后端 8888 + MinIO 9000 已启动
"""
import argparse
import hashlib
import io
import json
import os
import ssl
import subprocess
import sys
import time
import urllib.parse
import urllib.request

from PIL import Image

BASE = os.path.dirname(os.path.abspath(__file__))
BASE_URL = 'http://localhost:8888'
MYSQL = r'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'
CACHE_DIR = os.path.join(BASE, '_imgcache')
KEY_FILE = os.path.join(BASE, '_unsplash_key.txt')
API = 'https://api.unsplash.com/search/photos'
IMAGES_PER_PRODUCT = 3
PER_PAGE = 30
MAX_SIDE = 800
UA = {'User-Agent': 'Mozilla/5.0'}

# 叶子类目 → 搜索词。同类目多个商品共用一次搜索、从不同下标取图，
# 把请求数压到 40 以内（Unsplash 免费版 50 次/小时）
LEAF_QUERY = {
    'T恤': 't-shirt clothing', '衬衫': 'shirt clothing', '卫衣': 'hoodie sweatshirt',
    '休闲裤': 'pants trousers', '连衣裙': 'woman dress fashion',
    '女式外套': 'woman jacket coat', '女装': 'pants trousers',
    'iPhone 17': 'iphone smartphone', 'iPhone 16': 'iphone smartphone',
    # 搜 android 会出来安卓机器人，不像手机；按品牌搜更准
    'Mate 70': 'huawei smartphone', '小米 15': 'smartphone android',
    '平板电脑': 'tablet computer', '笔记本': 'laptop computer',
    '显示器': 'computer monitor', '耳机': 'wireless earbuds',
    '音箱': 'bluetooth speaker', '充电器': 'usb charger adapter',
    '数据线': 'usb cable', '手机壳': 'smartphone case',
    '相机': 'mirrorless camera', '智能手表': 'smartwatch',
    '智能穿戴': 'fitness tracker band', '智能家居': 'smart speaker home',
    '坚果炒货': 'mixed nuts dried fruit', '饼干糕点': 'cookies pastry',
    '肉干肉脯': 'dried meat jerky', '糖果巧克力': 'dark chocolate',
    '水果': 'fresh fruit', '海鲜水产': 'shrimp seafood',
    '咖啡': 'coffee beans', '茶饮': 'green tea leaves',
    '乳品': 'milk bottle', '大米面粉': 'rice grains',
    '食用油': 'cooking oil bottle', '调味酱料': 'sauce bottle',
    '跑步鞋': 'running shoes', '篮球鞋': 'basketball shoes',
    '运动服饰': 'sport t-shirt', '瑜伽用品': 'yoga mat',
    '力量器械': 'dumbbell', '跳绳': 'jump rope skipping',
    '露营装备': 'camping gear', '骑行装备': 'cycling helmet',
    '篮球': 'basketball', '足球': 'soccer ball',
    '羽毛球': 'badminton racket', '乒乓球': 'table tennis paddle',
    # bulk 生成的新商品会用到的叶子类目（缺映射会退化成通用的 product 搜索，图就货不对板了）
    '登山徒步': 'hiking backpack trekking', '蔬菜蛋品': 'fresh vegetables eggs',
    '台式机': 'desktop computer setup',
}

# 商品级覆盖：这几个必须精确到具体品种，不能用品类词
PRODUCT_QUERY = {
    1036: 'navel orange', 1037: 'mango fruit', 1058: 'durian fruit',
    1062: 'sleeping bag camping',
    # 品类词合并后仍偏泛的，单独给精确词
    1004: 'cargo pants', 1005: 'knit dress woman', 1009: 'wide leg pants woman',
    1011: 'floral summer dress', 1012: 'men jeans denim', 1047: 'camping chair',
}


def ssl_ctx():
    c = ssl.create_default_context()
    c.check_hostname = False
    c.verify_mode = ssl.CERT_NONE
    return c


CTX = ssl_ctx()
SEARCH_CACHE = {}
REMAINING = {'n': None}
STOP_SEARCHING = {'v': False}
SEARCH_FILE = os.path.join(CACHE_DIR, '_search_results.json')


def load_search_cache():
    global SEARCH_CACHE
    if os.path.exists(SEARCH_FILE):
        try:
            with open(SEARCH_FILE, encoding='utf-8') as f:
                SEARCH_CACHE = json.load(f)
            print('已载入 %d 个搜索词的历史结果（不重复消耗额度）' % len(SEARCH_CACHE))
        except Exception:
            SEARCH_CACHE = {}


def save_search_cache():
    os.makedirs(CACHE_DIR, exist_ok=True)
    with open(SEARCH_FILE, 'w', encoding='utf-8') as f:
        json.dump(SEARCH_CACHE, f, ensure_ascii=False)


def access_key():
    k = os.environ.get('UNSPLASH_ACCESS_KEY', '').strip()
    if k:
        return k
    if os.path.exists(KEY_FILE):
        return open(KEY_FILE, encoding='utf-8').read().strip()
    sys.exit('缺少 Unsplash Access Key：设置环境变量 UNSPLASH_ACCESS_KEY 或写入 %s' % KEY_FILE)


def mysql_rows(sql):
    out = subprocess.run([MYSQL, '-uroot', '-p123456', '--default-character-set=utf8mb4',
                          '-N', '-B', '-e', sql], capture_output=True)
    return [l.split('\t') for l in out.stdout.decode('utf-8').splitlines() if '\t' in l]


def search(query):
    """按搜索词取结果（同一词只请求一次，节省额度）"""
    if query in SEARCH_CACHE:
        return SEARCH_CACHE[query]
    if STOP_SEARCHING['v']:
        return []
    u = '%s?query=%s&per_page=%d&client_id=%s' % (
        API, urllib.parse.quote(query), PER_PAGE, access_key())
    req = urllib.request.Request(u, headers=UA)
    try:
        resp = urllib.request.urlopen(req, timeout=30, context=CTX)
        rem = resp.headers.get('X-Ratelimit-Remaining')
        if rem is not None:
            REMAINING['n'] = int(rem)
        data = json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8', 'replace')[:200]
        if e.code == 403:
            # 额度用尽：停止后续搜索，已完成的部分照常出图；过一小时重跑即续上
            print('   !! Unsplash 额度用尽，本轮停止搜索（已完成的照常出图）')
            STOP_SEARCHING['v'] = True
            return []
        print('   搜索失败 [%s] %s %s' % (query, e.code, body))
        SEARCH_CACHE[query] = []
        return []
    rows = [r['urls']['regular'] for r in data.get('results', []) if r.get('urls')]
    SEARCH_CACHE[query] = rows
    save_search_cache()
    print('   搜索「%s」-> %d 张（剩余额度 %s）' % (query, len(rows), REMAINING['n']))
    time.sleep(0.3)
    return rows


def fetch_image(url, attempts=3):
    """下载并统一转成 800px JPEG（带本地缓存，可断点续跑）"""
    os.makedirs(CACHE_DIR, exist_ok=True)
    key = hashlib.md5(url.encode()).hexdigest()
    path = os.path.join(CACHE_DIR, key + '.jpg')
    if os.path.exists(path) and os.path.getsize(path) > 3000:
        with open(path, 'rb') as f:
            return f.read()
    for _ in range(attempts):
        try:
            raw = urllib.request.urlopen(urllib.request.Request(url, headers=UA),
                                         timeout=40, context=CTX).read()
            im = Image.open(io.BytesIO(raw))
            im.load()
            im = im.convert('RGB')
            im.thumbnail((MAX_SIDE, MAX_SIDE), Image.LANCZOS)
            buf = io.BytesIO()
            im.save(buf, format='JPEG', quality=88, optimize=True)
            data = buf.getvalue()
            with open(path, 'wb') as f:
                f.write(data)
            return data
        except Exception:
            time.sleep(1.5)
    return None


def login():
    req = urllib.request.Request(
        BASE_URL + '/login',
        data=json.dumps({'username': 'admin', 'password': 'admin123'}).encode(),
        method='POST')
    req.add_header('Content-Type', 'application/json')
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode('utf-8'))['data']['token']


def upload(token, filename, content, retry=True):
    boundary = '----ecomunsplash'
    body = io.BytesIO()
    body.write(('--%s\r\n' % boundary).encode())
    body.write(('Content-Disposition: form-data; name="file"; filename="%s"\r\n' % filename).encode())
    body.write(b'Content-Type: image/jpeg\r\n\r\n')
    body.write(content)
    body.write(('\r\n--%s--\r\n' % boundary).encode())
    req = urllib.request.Request(BASE_URL + '/file/upload', data=body.getvalue(), method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=%s' % boundary)
    req.add_header('authorization', token)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode('utf-8'))['data']['url']
    except urllib.error.HTTPError as e:
        detail = e.read().decode('utf-8', 'replace')[:160]
        if e.code == 401 and retry:
            return upload(login(), filename, content, retry=False)
        raise RuntimeError('上传失败 HTTP %s %s' % (e.code, detail))


def plan_images(products):
    """按搜索词分组：同类目内各商品从不同下标取图，避免所有商品撞同一张"""
    by_query = {}
    for p in products:
        q = PRODUCT_QUERY.get(p['id']) or LEAF_QUERY.get(p['leaf']) or 'product'
        by_query.setdefault(q, []).append(p)
    plan = {}
    for q, group in by_query.items():
        rows = search(q)
        for i, p in enumerate(group):
            if not rows:
                plan[p['id']] = []
                continue
            start = (i * IMAGES_PER_PRODUCT) % len(rows)
            picks = [rows[(start + k) % len(rows)]
                     for k in range(min(IMAGES_PER_PRODUCT, len(rows)))]
            dedup = []
            for u in picks:
                if u not in dedup:
                    dedup.append(u)
            plan[p['id']] = dedup
    return plan


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pilot', type=int, default=0, help='只处理前 N 个商品，保存预览')
    ap.add_argument('--only', default='')
    ap.add_argument('--upload', action='store_true')
    ap.add_argument('--plan', action='store_true', help='只打印搜索词计划与请求数，不调接口')
    args = ap.parse_args()

    rows = mysql_rows("SELECT p.id, p.title, c.name FROM ecom.ec_product p "
                      "JOIN ecom.ec_category c ON c.id=p.category_id "
                      "WHERE p.del_flag=0 ORDER BY p.id;")
    load_search_cache()
    products = [{'id': int(r[0]), 'title': r[1], 'leaf': r[2]} for r in rows]
    if args.only:
        keep = set(int(x) for x in args.only.split(',') if x.strip())
        products = [p for p in products if p['id'] in keep]
    if args.pilot:
        products = products[:args.pilot]
    print('待处理商品 %d 个' % len(products))

    if args.plan:
        groups = {}
        for p in products:
            q = PRODUCT_QUERY.get(p['id']) or LEAF_QUERY.get(p['leaf']) or 'product'
            groups.setdefault(q, []).append(p['id'])
        print('需要不同搜索词 %d 个（= %d 次 API 请求）' % (len(groups), len(groups)))
        for q, ids in sorted(groups.items(), key=lambda x: -len(x[1])):
            print('  %-30s -> %s' % (q, ids))
        print('Access Key：%s...' % access_key()[:12])
        return

    plan = plan_images(products)

    if args.pilot or not args.upload:
        out_dir = os.path.join(BASE, '_pilot_unsplash')
        os.makedirs(out_dir, exist_ok=True)
        for p in products:
            print('--- %s %s' % (p['id'], p['title']))
            for i, u in enumerate(plan.get(p['id'], []), 1):
                data = fetch_image(u)
                if not data:
                    print('    %d) 下载失败' % i)
                    continue
                im = Image.open(io.BytesIO(data))
                im.save(os.path.join(out_dir, '%d_%d.png' % (p['id'], i)))
                print('    %d) %dx%d  %d KB' % (i, im.width, im.height, len(data) // 1024))
        print('预览目录：%s' % out_dir)
        return

    token = login()
    map_file = os.path.join(CACHE_DIR, '_unsplash_uploaded.json')
    uploaded = {}
    if os.path.exists(map_file):
        with open(map_file, encoding='utf-8') as f:
            uploaded = json.load(f)

    lines = ['-- 商品配图回填（Unsplash 实拍图，由 sql/fetch_unsplash_images.py 生成）',
             'SET NAMES utf8mb4;']
    gallery = []
    skus = mysql_rows("SELECT id, product_id FROM ecom.ec_sku WHERE del_flag=0 ORDER BY product_id, id;")
    skus_by_product = {}
    for sid, pid in skus:
        skus_by_product.setdefault(int(pid), []).append(int(sid))

    done = 0
    updated_ids = []
    for p in products:
        urls = []
        for src in plan.get(p['id'], []):
            key = hashlib.md5(src.encode()).hexdigest()
            if key not in uploaded:
                data = fetch_image(src)
                if not data:
                    print('   !! 图片下载失败，跳过')
                    continue
                try:
                    uploaded[key] = upload(token, 'u%s.jpg' % key[:10], data)
                except Exception as e:
                    print('   !! 上传失败：%s' % str(e)[:70])
                    continue
                with open(map_file, 'w', encoding='utf-8') as f:
                    json.dump(uploaded, f, ensure_ascii=False)
            urls.append(uploaded[key])
        if not urls:
            print('product %d 跳过（无可用图）' % p['id'])
            continue
        updated_ids.append(p['id'])
        lines.append("UPDATE ec_product SET main_image = '%s' WHERE id = %d;" % (urls[0], p['id']))
        for i, sid in enumerate(skus_by_product.get(p['id'], [])):
            lines.append("UPDATE ec_sku SET image = '%s' WHERE id = %d;" % (urls[i % len(urls)], sid))
        for sort, u in enumerate(urls):
            gallery.append((p['id'], u, sort))
        done += 1
        print('product %d done (%d imgs): %s' % (p['id'], len(urls), p['title'][:26]))

    lines.append('')
    # 只重建本轮真正拿到图的商品，避免把跳过的商品的图集清空
    lines.append('DELETE FROM ec_product_image WHERE product_id IN (%s);'
                 % ','.join(str(i) for i in updated_ids))
    for pid, u, sort in gallery:
        lines.append("INSERT INTO ec_product_image (product_id, url, sort, del_flag) "
                     "VALUES (%d, '%s', %d, 0);" % (pid, u, sort))
    path = os.path.join(BASE, '_images_backfill.sql')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines) + '\n')
    print('完成 %d/%d，已写出 %s（%d 图集行）' % (done, len(products), path, len(gallery)))


if __name__ == '__main__':
    main()
