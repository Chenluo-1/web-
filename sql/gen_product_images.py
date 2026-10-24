# -*- coding: utf-8 -*-
"""
商品配图生成器（Pillow 绘制，不依赖外部服务与 AI 密钥）

作用：为 ec_product 生成"与商品对应"的配图——按一级类目主题配色、按叶子类目
     绘制扁平插画、并叠加商品标题，替代原先无任何信息的纯色渐变块。

用法：
    python sql/gen_product_images.py --pilot            # 本地渲染 6 张样张到 sql/_pilot/，不上传
    python sql/gen_product_images.py --upload           # 渲染全部在售商品、上传 MinIO 并输出回填 SQL
    python sql/gen_product_images.py --upload --only 1001,1002

前置：后端 8888 + MinIO 9000 已启动（上传走 /file/upload，与 upload_images.py 同一通道）
"""
import argparse
import io
import json
import os
import subprocess
import urllib.request

from PIL import Image, ImageDraw, ImageFont

BASE = os.path.dirname(os.path.abspath(__file__))
BASE_URL = 'http://localhost:8888'
MYSQL = r'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'
SIZE = 800
FONT_REG = r'C:\Windows\Fonts\msyh.ttc'
FONT_BOLD = r'C:\Windows\Fonts\msyhbd.ttc'

# 一级类目主题：背景渐变起止色 / 主色 / 标签色
THEMES = {
    '服饰': {'bg': ((253, 236, 242), (245, 195, 214)), 'ink': (191, 84, 126), 'chip': (211, 112, 148)},
    '数码': {'bg': ((229, 240, 255), (188, 214, 253)), 'ink': (45, 93, 174), 'chip': (72, 124, 208)},
    '美食': {'bg': ((255, 243, 227), (255, 210, 170)), 'ink': (186, 104, 28), 'chip': (212, 134, 56)},
    '运动': {'bg': ((230, 248, 237), (189, 234, 211)), 'ink': (30, 128, 86), 'chip': (52, 150, 108)},
}
DEFAULT_THEME = {'bg': ((240, 242, 246), (212, 219, 230)), 'ink': (88, 102, 126), 'chip': (118, 132, 156)}

# 叶子类目 → 插画形状
SHAPES = {
    'T恤': 'tshirt', '卫衣': 'tshirt', '衬衫': 'tshirt', '运动服饰': 'tshirt',
    '连衣裙': 'dress', '女式外套': 'coat',
    '休闲裤': 'pants',
    'iPhone 17': 'phone', 'iPhone 16': 'phone', 'Mate 70': 'phone', '小米 15': 'phone',
    '手机壳': 'phonecover', '充电器': 'plug', '数据线': 'plug',
    '笔记本': 'laptop', '台式机': 'laptop', '平板电脑': 'laptop', '显示器': 'laptop',
    '耳机': 'headphone', '音箱': 'speaker',
    '智能手表': 'watch', '智能穿戴': 'watch',
    '智能家居': 'plug', '相机': 'camera',
    '坚果炒货': 'packet', '饼干糕点': 'packet', '肉干肉脯': 'packet', '糖果巧克力': 'packet',
    '大米面粉': 'packet', '调味酱料': 'packet', '蔬菜蛋品': 'packet',
    '水果': 'fruit', '海鲜水产': 'fish',
    '咖啡': 'bottle', '茶饮': 'bottle', '乳品': 'bottle', '食用油': 'bottle',
    '跑步鞋': 'shoe', '篮球鞋': 'shoe',
    '瑜伽用品': 'dumbbell', '力量器械': 'dumbbell', '跳绳': 'dumbbell',
    '露营装备': 'tent', '骑行装备': 'tent', '登山徒步': 'tent',
    '篮球': 'ball', '足球': 'ball', '羽毛球': 'shuttle', '乒乓球': 'ball',
}


def font(path, size):
    return ImageFont.truetype(path, size)


def gradient(top, bottom):
    """垂直渐变背景（逐行插值）"""
    img = Image.new('RGB', (SIZE, SIZE))
    d = ImageDraw.Draw(img)
    for y in range(SIZE):
        t = y / (SIZE - 1)
        d.line([(0, y), (SIZE, y)],
               fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img


def wrap(text, fnt, max_width, max_lines=2):
    """按像素宽度折行（中文逐字、英文数字逐字符）"""
    lines, cur = [], ''
    for ch in text:
        if fnt.getlength(cur + ch) <= max_width or not cur:
            cur += ch
        else:
            lines.append(cur)
            cur = ch
            if len(lines) == max_lines:
                break
    if cur and len(lines) < max_lines:
        lines.append(cur)
    if not lines:
        return ['']
    if fnt.getlength(lines[-1]) > max_width:
        while lines[-1] and fnt.getlength(lines[-1] + '…') > max_width:
            lines[-1] = lines[-1][:-1]
        lines[-1] += '…'
    return lines


def shade(color, f):
    """调亮/调暗（f>1 变亮，f<1 变暗）"""
    return tuple(max(0, min(255, int(c * f))) for c in color)


def draw_shape(d, kind, cx, cy, ink, white=(255, 255, 255)):
    """在 (cx, cy) 为中心绘制扁平插画，主体约 320x320"""
    i, w = ink, white

    def rr(x0, y0, x1, y1, r, fill):
        d.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=fill)

    if kind == 'tshirt':
        d.polygon([(cx - 66, cy - 66), (cx - 30, cy - 88), (cx + 30, cy - 88), (cx + 66, cy - 66),
                   (cx + 104, cy - 26), (cx + 62, cy - 2), (cx + 62, cy + 96),
                   (cx - 62, cy + 96), (cx - 62, cy - 2), (cx - 104, cy - 26)], fill=i)
        d.ellipse([cx - 30, cy - 100, cx + 30, cy - 62], fill=w)
    elif kind == 'dress':
        # 吊带
        d.line([(cx - 22, cy - 100), (cx - 31, cy - 128)], fill=i, width=9)
        d.line([(cx + 22, cy - 100), (cx + 31, cy - 128)], fill=i, width=9)
        # 上身
        d.polygon([(cx - 32, cy - 104), (cx + 32, cy - 104), (cx + 28, cy - 6), (cx - 28, cy - 6)], fill=i)
        # 裙摆
        d.polygon([(cx - 28, cy - 16), (cx + 28, cy - 16), (cx + 96, cy + 108), (cx - 96, cy + 108)], fill=i)
        # 腰带
        d.rectangle([cx - 30, cy - 20, cx + 30, cy - 2], fill=w)
    elif kind == 'pants':
        d.rounded_rectangle([cx - 68, cy - 104, cx + 68, cy - 82], radius=8, fill=w)
        d.polygon([(cx - 66, cy - 100), (cx + 66, cy - 100), (cx + 60, cy - 10),
                   (cx + 40, cy + 104), (cx + 6, cy + 104), (cx, cy - 4),
                   (cx - 6, cy + 104), (cx - 40, cy + 104), (cx - 60, cy - 10)], fill=i)
    elif kind == 'coat':
        d.polygon([(cx - 86, cy - 86), (cx - 38, cy - 110), (cx + 38, cy - 110), (cx + 86, cy - 86),
                   (cx + 102, cy - 18), (cx + 94, cy + 110), (cx - 94, cy + 110), (cx - 102, cy - 18)], fill=i)
        d.polygon([(cx - 38, cy - 110), (cx, cy - 62), (cx + 38, cy - 110)], fill=w)
        d.line([(cx, cy - 56), (cx, cy + 110)], fill=w, width=6)
        d.ellipse([cx - 11, cy + 4, cx + 11, cy + 26], fill=w)
        d.ellipse([cx - 11, cy + 56, cx + 11, cy + 78], fill=w)
    elif kind == 'phone':
        rr(cx - 74, cy - 130, cx + 74, cy + 130, 22, i)
        rr(cx - 62, cy - 112, cx + 62, cy + 100, 14, w)
        rr(cx - 20, cy - 123, cx + 20, cy - 114, 4, w)
        d.ellipse([cx - 12, cy + 108, cx + 12, cy + 120], fill=w)
    elif kind == 'phonecover':
        rr(cx - 76, cy - 130, cx + 76, cy + 130, 24, i)
        rr(cx - 58, cy - 112, cx + 58, cy + 112, 18, w)
        rr(cx - 28, cy - 112, cx + 28, cy - 56, 14, i)
    elif kind == 'laptop':
        rr(cx - 106, cy - 112, cx + 106, cy + 60, 12, i)
        rr(cx - 92, cy - 99, cx + 92, cy + 48, 7, w)
        d.polygon([(cx - 146, cy + 60), (cx + 146, cy + 60), (cx + 124, cy + 96), (cx - 124, cy + 96)], fill=i)
    elif kind == 'headphone':
        d.arc([cx - 116, cy - 100, cx + 116, cy + 96], start=190, end=350, fill=i, width=24)
        rr(cx - 138, cy - 30, cx - 98, cy + 78, 18, i)
        rr(cx + 98, cy - 30, cx + 138, cy + 78, 18, i)
    elif kind == 'speaker':
        rr(cx - 70, cy - 118, cx + 70, cy + 118, 16, i)
        d.ellipse([cx - 44, cy - 88, cx + 44, cy - 4], outline=w, width=9)
        d.ellipse([cx - 30, cy + 22, cx + 30, cy + 80], outline=w, width=9)
    elif kind == 'watch':
        rr(cx - 52, cy - 130, cx + 52, cy + 130, 16, i)
        rr(cx - 72, cy - 54, cx + 72, cy + 54, 24, i)
        rr(cx - 60, cy - 43, cx + 60, cy + 43, 17, w)
        d.line([(cx, cy - 28), (cx, cy + 2)], fill=i, width=8)
        d.line([(cx, cy + 2), (cx + 22, cy + 2)], fill=i, width=8)
    elif kind == 'plug':
        rr(cx - 62, cy - 46, cx + 62, cy + 104, 16, i)
        d.rectangle([cx - 24, cy - 100, cx - 10, cy - 44], fill=i)
        d.rectangle([cx + 10, cy - 100, cx + 24, cy - 44], fill=i)
        rr(cx - 38, cy + 4, cx + 38, cy + 76, 10, w)
    elif kind == 'camera':
        rr(cx - 116, cy - 62, cx + 116, cy + 92, 18, i)
        rr(cx - 46, cy - 96, cx + 46, cy - 58, 10, i)
        d.ellipse([cx - 62, cy - 34, cx + 62, cy + 74], fill=w)
        d.ellipse([cx - 40, cy - 12, cx + 40, cy + 52], fill=i)
        d.ellipse([cx + 68, cy - 40, cx + 92, cy - 16], fill=w)
    elif kind == 'packet':
        rr(cx - 96, cy - 84, cx + 96, cy + 100, 20, i)
        d.polygon([(cx - 96, cy - 84), (cx - 48, cy - 124), (cx, cy - 84),
                   (cx + 48, cy - 124), (cx + 96, cy - 84)], fill=i)
        rr(cx - 60, cy - 20, cx + 60, cy + 62, 12, w)
        d.line([(cx - 40, cy + 6), (cx + 40, cy + 6)], fill=i, width=8)
        d.line([(cx - 26, cy + 34), (cx + 26, cy + 34)], fill=i, width=8)
    elif kind == 'bottle':
        rr(cx - 60, cy - 24, cx + 60, cy + 116, 18, i)
        d.polygon([(cx - 32, cy - 24), (cx + 32, cy - 24), (cx + 18, cy - 96), (cx - 18, cy - 96)], fill=i)
        d.rectangle([cx - 24, cy - 122, cx + 24, cy - 94], fill=i)
        rr(cx - 38, cy + 20, cx + 38, cy + 92, 10, w)
    elif kind == 'fruit':
        d.ellipse([cx - 92, cy - 60, cx + 92, cy + 120], fill=i)
        d.polygon([(cx + 4, cy - 58), (cx + 16, cy - 116), (cx + 58, cy - 92)], fill=i)
        d.ellipse([cx - 40, cy - 20, cx - 4, cy + 16], fill=w)
    elif kind == 'fish':
        d.ellipse([cx - 74, cy - 58, cx + 74, cy + 58], fill=i)
        d.polygon([(cx - 62, cy), (cx - 126, cy - 58), (cx - 126, cy + 58)], fill=i)
        d.ellipse([cx + 30, cy - 24, cx + 50, cy - 4], fill=w)
        d.arc([cx - 20, cy - 16, cx + 44, cy + 40], start=100, end=250, fill=w, width=7)
    elif kind == 'shoe':
        d.polygon([(cx - 116, cy + 34), (cx - 108, cy - 32), (cx - 50, cy - 62),
                   (cx + 8, cy - 24), (cx + 50, cy - 8), (cx + 116, cy + 16),
                   (cx + 122, cy + 62), (cx - 122, cy + 62)], fill=i)
        d.line([(cx - 116, cy + 38), (cx + 118, cy + 38)], fill=w, width=8)
    elif kind == 'dumbbell':
        rr(cx - 38, cy - 16, cx + 38, cy + 16, 8, i)
        rr(cx - 108, cy - 62, cx - 44, cy + 62, 12, i)
        rr(cx - 132, cy - 42, cx - 102, cy + 42, 8, i)
        rr(cx + 44, cy - 62, cx + 108, cy + 62, 12, i)
        rr(cx + 102, cy - 42, cx + 132, cy + 42, 8, i)
    elif kind == 'tent':
        d.polygon([(cx, cy - 100), (cx + 124, cy + 100), (cx - 124, cy + 100)], fill=i)
        d.polygon([(cx, cy - 44), (cx + 52, cy + 100), (cx - 52, cy + 100)], fill=w)
        d.polygon([(cx, cy - 100), (cx + 18, cy + 100), (cx - 18, cy + 100)], fill=i)
    elif kind == 'ball':
        d.ellipse([cx - 108, cy - 108, cx + 108, cy + 108], fill=i)
        d.arc([cx - 108, cy - 108, cx + 108, cy + 108], start=90, end=270, fill=w, width=8)
        d.arc([cx - 108, cy - 108, cx + 108, cy + 108], start=-90, end=90, fill=w, width=8)
        d.line([(cx - 108, cy), (cx + 108, cy)], fill=w, width=8)
    elif kind == 'shuttle':
        d.polygon([(cx - 34, cy - 104), (cx + 34, cy - 104), (cx + 14, cy + 12), (cx - 14, cy + 12)], fill=w)
        d.polygon([(cx - 34, cy - 104), (cx - 14, cy + 12), (cx - 60, cy + 66), (cx - 76, cy - 40)], fill=i)
        d.polygon([(cx + 34, cy - 104), (cx + 14, cy + 12), (cx + 60, cy + 66), (cx + 76, cy - 40)], fill=i)
        d.ellipse([cx - 52, cy + 30, cx + 52, cy + 100], fill=i)
    else:
        rr(cx - 92, cy - 92, cx + 92, cy + 92, 20, i)
        d.ellipse([cx - 44, cy - 44, cx + 44, cy + 44], fill=w)


def render(title, domain, leaf, price_min=None, spec_label=None):
    theme = THEMES.get(domain, DEFAULT_THEME)
    img = gradient(*theme['bg'])
    d = ImageDraw.Draw(img, 'RGBA')

    # 背景装饰圆
    d.ellipse([-170, 520, 300, 990], fill=(255, 255, 255, 55))
    d.ellipse([540, -150, 990, 300], fill=(255, 255, 255, 65))

    # 主体圆形底板 + 底部投影
    d.ellipse([SIZE // 2 - 218, 108, SIZE // 2 + 218, 560], fill=shade(theme['bg'][1], 0.96))
    d.ellipse([SIZE // 2 - 206, 96, SIZE // 2 + 206, 536], fill=(255, 255, 255, 235))

    # 类目标签
    f_chip = font(FONT_BOLD, 25)
    chip = '%s · %s' % (domain, leaf)
    d.rounded_rectangle([40, 40, 40 + f_chip.getlength(chip) + 46, 94], radius=27, fill=theme['chip'])
    d.text((63, 55), chip, font=f_chip, fill=(255, 255, 255))

    # 插画
    draw_shape(d, SHAPES.get(leaf, 'box'), SIZE // 2, 320, theme['ink'])

    # 标题
    f_title = font(FONT_BOLD, 39)
    lines = wrap(title, f_title, SIZE - 140, max_lines=2)
    y = 596 if len(lines) > 1 else 626
    for line in lines:
        d.text(((SIZE - f_title.getlength(line)) / 2, y), line, font=f_title, fill=(46, 50, 58))
        y += 54

    # 规格标签（SKU 图）或价格（SPU 主图）
    if spec_label:
        f_spec = font(FONT_BOLD, 26)
        x0 = (SIZE - f_spec.getlength(spec_label)) / 2 - 22
        d.rounded_rectangle([x0, y + 14, x0 + f_spec.getlength(spec_label) + 44, y + 66],
                            radius=26, fill=theme['chip'])
        d.text(((SIZE - f_spec.getlength(spec_label)) / 2, y + 24), spec_label, font=f_spec, fill=(255, 255, 255))
    elif price_min is not None:
        f_price = font(FONT_REG, 29)
        text = '¥ %s 起' % ('%.2f' % price_min).rstrip('0').rstrip('.')
        d.text(((SIZE - f_price.getlength(text)) / 2, y + 10), text, font=f_price, fill=theme['chip'])

    return img


def login(username, password):
    req = urllib.request.Request(BASE_URL + '/login',
                                 data=json.dumps({'username': username, 'password': password}).encode(),
                                 method='POST')
    req.add_header('Content-Type', 'application/json')
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode('utf-8'))['data']['token']


def upload(token, filename, content):
    boundary = '----ecomimg'
    body = io.BytesIO()
    body.write(('--%s\r\n' % boundary).encode())
    body.write(('Content-Disposition: form-data; name="file"; filename="%s"\r\n' % filename).encode())
    body.write(b'Content-Type: image/png\r\n\r\n')
    body.write(content)
    body.write(('\r\n--%s--\r\n' % boundary).encode())
    req = urllib.request.Request(BASE_URL + '/file/upload', data=body.getvalue(), method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=%s' % boundary)
    req.add_header('authorization', token)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode('utf-8'))['data']['url']


def load_products(only=None):
    """读商品 + 叶子类目名，并沿类目树一路上溯到根节点来确定所属大域"""
    def query(sql):
        out = subprocess.run([MYSQL, '-uroot', '-p123456', '--default-character-set=utf8mb4',
                              '-N', '-B', '-e', sql], capture_output=True)
        return [l.split('\t') for l in out.stdout.decode('utf-8').splitlines() if '\t' in l]

    # 类目树：id -> (parent_id, name)
    cats = {}
    for row in query("SELECT id, parent_id, name FROM ecom.ec_category;"):
        cats[int(row[0])] = (int(row[1]), row[2])

    def root_name(cat_id):
        """沿 parent_id 上溯到根（parent_id = 0），返回根类目名作为大域"""
        seen = set()
        cur = cat_id
        while cur in cats and cur not in seen:
            seen.add(cur)
            pid, name = cats[cur]
            if pid == 0:
                return name
            cur = pid
        return ''

    rows = []
    sql = ("SELECT p.id, p.title, p.price_min, p.category_id, c.name "
           "FROM ecom.ec_product p JOIN ecom.ec_category c ON c.id = p.category_id "
           "WHERE p.del_flag = 0 ORDER BY p.id;")
    for pid, title, price_min, cat_id, leaf in query(sql):
        if only and int(pid) not in only:
            continue
        rows.append({'id': int(pid), 'title': title, 'price_min': float(price_min),
                     'leaf': leaf, 'domain': root_name(int(cat_id))})
    return rows


def load_skus():
    """读所有 SKU（规格组合用于生成规格图）"""
    def query(sql):
        out = subprocess.run([MYSQL, '-uroot', '-p123456', '--default-character-set=utf8mb4',
                              '-N', '-B', '-e', sql], capture_output=True)
        return [l.split('\t') for l in out.stdout.decode('utf-8').splitlines() if '\t' in l]

    rows = []
    sql = "SELECT id, product_id, specs, price FROM ecom.ec_sku WHERE del_flag = 0 ORDER BY product_id, id;"
    for sid, pid, specs, price in query(sql):
        try:
            label = ' · '.join(str(v) for v in json.loads(specs).values())
        except Exception:
            label = ''
        rows.append({'id': int(sid), 'product_id': int(pid), 'label': label, 'price': float(price)})
    return rows


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, format='PNG', optimize=True)
    return buf.getvalue()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pilot', action='store_true', help='只本地渲染样张，不上传')
    ap.add_argument('--upload', action='store_true', help='渲染并上传 MinIO，输出回填 SQL')
    ap.add_argument('--only', default='', help='只处理指定商品 id，逗号分隔')
    ap.add_argument('--limit', type=int, default=0, help='样张数量')
    args = ap.parse_args()

    only = set(int(x) for x in args.only.split(',') if x.strip()) if args.only else None
    products = load_products(only)
    print('商品数：%d' % len(products))

    if args.pilot:
        out_dir = os.path.join(BASE, '_pilot')
        os.makedirs(out_dir, exist_ok=True)
        picks = products if args.limit == 0 else products[:args.limit]
        for p in picks:
            img = render(p['title'], p['domain'], p['leaf'], p['price_min'])
            path = os.path.join(out_dir, '%d.png' % p['id'])
            img.save(path)
            print('样张 -> %s   [%s] %s' % (path, p['domain'], p['title']))
        return

    if not args.upload:
        ap.print_help()
        return

    token = login('admin', 'admin123')
    skus = load_skus()
    skus_by_product = {}
    for s in skus:
        skus_by_product.setdefault(s['product_id'], []).append(s)

    lines = ['-- 商品配图回填（由 sql/gen_product_images.py 生成）', 'SET NAMES utf8mb4;',
             '-- SPU 主图 + SKU 规格图 + 商品图集']
    gallery = []
    for p in products:
        img = render(p['title'], p['domain'], p['leaf'], p['price_min'])
        url = upload(token, 'p%d.png' % p['id'], png_bytes(img))
        lines.append("UPDATE ec_product SET main_image = '%s' WHERE id = %d;" % (url, p['id']))

        # 图集首图 = 主图
        gallery.append((p['id'], url, 0))
        sort = 1
        for s in skus_by_product.get(p['id'], []):
            simg = render(p['title'], p['domain'], p['leaf'], spec_label=s['label'])
            surl = upload(token, 's%d.png' % s['id'], png_bytes(simg))
            lines.append("UPDATE ec_sku SET image = '%s' WHERE id = %d;" % (surl, s['id']))
            gallery.append((p['id'], surl, sort))
            sort += 1
        print('uploaded product %d (main + %d sku imgs)' % (p['id'], sort - 1))

    # 商品图集：先清后插（表当前为空，重复执行也安全）
    lines.append('')
    lines.append('-- 商品图集（主图 + 各 SKU 规格图）')
    lines.append('DELETE FROM ec_product_image WHERE product_id IN (%s);'
                 % ','.join(str(p['id']) for p in products))
    for pid, url, sort in gallery:
        lines.append("INSERT INTO ec_product_image (product_id, url, sort, del_flag) VALUES (%d, '%s', %d, 0);"
                     % (pid, url, sort))

    path = os.path.join(BASE, '_images_backfill.sql')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines) + '\n')
    print('OK 已写出 %s（%d 商品 / %d SKU / %d 图集行）' % (path, len(products), len(skus), len(gallery)))


if __name__ == '__main__':
    main()
