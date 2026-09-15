# -*- coding: utf-8 -*-
"""
商品演示配图：python sql/upload_images.py
流程：按 data_manifest.json 生成渐变占位图 → 经后端 /file/upload 上传 MinIO
     → 汇总输出 sql/data_images.sql（回填 ec_product.main_image / ec_sku.image）
执行前提：后端 8888 + MinIO 9000 已启动，data.sql 已导入
用法：python sql/upload_images.py && mysql -uroot -p ecom < sql/data_images.sql
"""
import colorsys
import io
import json
import os
import struct
import urllib.request
import zlib

BASE = os.path.dirname(os.path.abspath(__file__))
BASE_URL = 'http://localhost:8888'
W = H = 400


def make_png(hue):
    """垂直渐变 PNG（无第三方库）：hue 0-360，从深到浅"""
    r0, g0, b0 = colorsys.hsv_to_rgb(hue / 360.0, 0.55, 0.85)
    r1, g1, b1 = colorsys.hsv_to_rgb(hue / 360.0, 0.25, 0.98)
    raw = bytearray()
    for y in range(H):
        t = y / (H - 1)
        raw.append(0)  # filter: none
        r = int((r0 + (r1 - r0) * t) * 255)
        g = int((g0 + (g1 - g0) * t) * 255)
        b = int((b0 + (b1 - b0) * t) * 255)
        raw.extend(bytes([r, g, b]) * W)

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    ihdr = struct.pack('>IIBBBBB', W, H, 8, 2, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr)
            + chunk(b'IDAT', zlib.compress(bytes(raw), 6)) + chunk(b'IEND', b''))


def login(username, password):
    req = urllib.request.Request(BASE_URL + '/login',
                                 data=json.dumps({'username': username, 'password': password}).encode(),
                                 method='POST')
    req.add_header('Content-Type', 'application/json')
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode('utf-8'))['data']['token']


def upload(token, filename, content):
    boundary = '----ecomdemo'
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


def main():
    with open(os.path.join(BASE, 'data_manifest.json'), encoding='utf-8') as f:
        manifest = json.load(f)
    token = login('admin', 'admin123')
    lines = ['-- 商品演示配图回填（由 sql/upload_images.py 生成）', 'SET NAMES utf8mb4;']
    for item in manifest:
        pid = item['id']
        url = upload(token, 'product-%d.png' % pid, make_png(item['hue']))
        lines.append("UPDATE ec_product SET main_image = '%s' WHERE id = %d;" % (url, pid))
        lines.append("UPDATE ec_sku SET image = '%s' WHERE product_id = %d;" % (url, pid))
        print('uploaded product %d -> %s' % (pid, url))
    with open(os.path.join(BASE, 'data_images.sql'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines) + '\n')
    print('OK 已生成 sql/data_images.sql（%d 个商品）' % len(manifest))


if __name__ == '__main__':
    main()
