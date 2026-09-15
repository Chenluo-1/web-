# -*- coding: utf-8 -*-
"""
演示数据生成器：python sql/data_generator.py
产出：
  sql/data.sql            —— 演示数据（可重复执行，先清后插）
  sql/data_manifest.json  —— 商品配图清单（供 upload_images.py 上传占位图后回填 URL）
数据规模：50 商品(4 大域) / 每商品 2-6 SKU / 200 用户 / 500 订单(7 种状态) / 评价 / 搜索热词 / 30 天看板汇总
"""
import json
import os
import random
from datetime import datetime, timedelta

random.seed(20260915)

BASE = os.path.dirname(os.path.abspath(__file__))
OUT_SQL = os.path.join(BASE, 'data.sql')
OUT_MANIFEST = os.path.join(BASE, 'data_manifest.json')

NOW = datetime(2026, 9, 15, 20, 0, 0)


def esc(s):
    return s.replace('\\', '\\\\').replace("'", "''") if isinstance(s, str) else s


def fmt_dt(d):
    return d.strftime('%Y-%m-%d %H:%M:%S')


def fmt_m(v):
    return '%.2f' % v


L = []


def w(line=''):
    L.append(line)


# ============================================================
# 0. 商品定义（类目 id 见 ecom.sql 固定种子）
# ============================================================
# 需要补充的服饰子类目（INSERT IGNORE，可重复执行）
NEW_CATEGORIES = [
    (401, 1, '女装', 2), (402, 401, '连衣裙', 1), (403, 2, '卫衣', 2),
    (404, 2, '休闲裤', 3), (405, 401, '女式外套', 2), (406, 2, '衬衫', 4),
]

# spec 模板
SPEC_COLOR4 = ['黑色', '白色', '藏青', '灰色']
SPEC_SIZE = ['S', 'M', 'L', 'XL']
SPEC_PHONE_COLOR = ['曜石黑', '雪域白', '远山青']
SPEC_STORAGE = ['128G', '256G', '512G']
SPEC_VER = ['标准版', '高配版']
SPEC_FOOD_WEIGHT = ['500g', '1kg']

# (类目id, 标题, 价格区间, spec模板key, 卖点描述)
PRODUCTS = [
    # ---- 服饰 12 ----
    (3, '纯棉圆领短袖T恤男夏季宽松打底衫', 69, 119, 'T', '260g 重磅纯棉，不透不闷汗，水洗不易变形，基础百搭款。'),
    (406, '亚麻长袖衬衫男商务休闲薄款', 129, 189, 'S', '亚麻混纺面料，亲肤透气，通勤度假两相宜。'),
    (403, '加绒连帽卫衣男宽松情侣款', 139, 199, 'S', '加厚抓绒内里，秋冬保暖锁温，落肩版型显瘦。'),
    (404, '工装休闲裤男直筒宽松束脚两穿', 129, 179, 'S', '弹力斜纹面料，直筒/束脚两种穿法，多口袋实用。'),
    (402, '法式针织连衣裙女秋季收腰显瘦', 169, 259, 'S', '柔软针织面料，收腰 A 字裙摆，温柔显气质。'),
    (405, '小香风短款外套女春秋粗花呢', 199, 299, 'S', '粗花呢纹理，金扣点缀，短款设计显腿长。'),
    (3, '纯棉情侣装短袖T恤夏季新款', 79, 129, 'T', '情侣同款，多色可选，透气纯棉，日常休闲必备。'),
    (406, '商务翻领polo衫男免烫短袖', 99, 159, 'T', '珠地网眼面料，挺括免烫，商务休闲皆适宜。'),
    (401, '高腰阔腿裤女垂感直筒西装裤', 149, 219, 'S', '垂坠感面料，高腰显腿长，通勤百搭。'),
    (403, '摇粒绒卫衣男冬季保暖加厚', 119, 169, 'S', '双面摇粒绒，蓬松保暖，重量轻不压身。'),
    (402, '吊带连衣裙女度假风碎花长裙', 139, 199, 'S', '雪纺碎花，可调节吊带，海边度假拍照首选。'),
    (404, '水洗牛仔裤男修身直筒弹力', 159, 229, 'S', '微弹水洗牛仔，修身直筒版型，四季耐穿。'),
    # ---- 数码 16 ----
    (115, 'Apple iPhone 17 全网通5G手机', 5999, 8999, 'P', 'A19 芯片性能强悍，超视网膜 XDR 屏，长续航快充。'),
    (116, 'Apple iPhone 16 双卡双待手机', 4999, 7299, 'P', '经典机型现货，A18 芯片流畅耐用，支持以旧换新。'),
    (117, '华为 Mate 70 旗舰手机 鸿蒙系统', 5499, 7499, 'P', '麒麟芯片 + 鸿蒙系统，卫星通信，影像旗舰。'),
    (118, '小米 15 徕卡光学镜头手机', 3999, 5999, 'P', '徕卡三摄，骁龙旗舰芯片，2K 屏幕细腻护眼。'),
    (120, '65W 氮化镓快充充电器 双口输出', 89, 149, 'D1', '氮化镓黑科技，小巧便携，手机笔记本通用。'),
    (121, 'Type-C 编织数据线 快充传输二合一', 29, 59, 'D2', '尼龙编织抗弯折，支持 100W 快充与高速传输。'),
    (122, '磁吸透明手机壳 防摔全包镜头', 39, 79, 'D3', '强磁吸力兼容无线充，军工级防摔，久用不发黄。'),
    (131, '联想小新 Pro16 轻薄笔记本电脑', 4599, 6299, 'C1', '16 英寸 2.5K 高刷屏，标压处理器，办公创作流畅。'),
    (133, '华为 MatePad 11.5 平板电脑', 1999, 3199, 'C2', '11.5 英寸柔光屏，多屏协同，学习办公两不误。'),
    (134, '27英寸 2K 高刷电竞显示器', 899, 1599, 'C3', '2K 分辨率 165Hz 高刷，广色域低蓝光，护眼不闪屏。'),
    (141, '主动降噪蓝牙耳机 半入耳长续航', 299, 499, 'D1', '40dB 主动降噪，单次续航 8 小时，通话清晰。'),
    (142, '便携蓝牙音箱 重低音户外防水', 159, 299, 'D1', '360° 环绕立体声，IPX7 防水，续航 20 小时。'),
    (143, '入门微单相机 4K视频 触控翻转屏', 3199, 4599, 'C1', '2420 万像素，4K 视频，轻量机身适合入门。'),
    (151, '智能手表 血氧心率监测 长续航', 499, 899, 'D1', '全天候健康监测，百种运动模式，两周续航。'),
    (152, '智能音箱 语音控制智能家居', 199, 349, 'D1', '远场语音唤醒，控制家电，海量有声内容。'),
    (153, '运动手环 50米防水 睡眠监测', 169, 269, 'D1', '轻薄佩戴无感，心率睡眠监测，续航 14 天。'),
    # ---- 美食 12 ----
    (211, '新疆若羌灰枣 特级免洗 500g', 29, 49, 'F1', '果肉厚实核小，自然晾晒无添加，煲汤泡茶皆宜。'),
    (211, '每日坚果混合果仁 30袋装', 59, 99, 'F2', '科学配比 6 种坚果果干，独立小包新鲜锁鲜。'),
    (212, '手工曲奇饼干礼盒 800g', 49, 79, 'F2', '黄油含量足，酥松不腻，下午茶伴手礼首选。'),
    (212, '云南鲜花饼 现烤直发 10枚装', 39, 59, 'F2', '玫瑰花瓣馅，酥皮层层掉渣，云南老字号。'),
    (213, '靖江猪肉脯 原味 300g', 45, 69, 'F3', '整片猪后腿肉，炭火烘烤，咸甜适中越嚼越香。'),
    (213, '内蒙古风干牛肉干 250g', 69, 109, 'F3', '草原黄牛肉，超干工艺，高蛋白低脂零食。'),
    (214, '比利时黑巧克力礼盒 72%可可', 79, 129, 'F3', '72% 可可脂，微苦回甘，独立包装送人体面。'),
    (221, '赣南脐橙 新鲜现摘 5斤装', 39, 59, 'F1', '皮薄多汁化渣，甜度 13° 以上，产地直发。'),
    (221, '海南贵妃芒 树上熟 3斤装', 45, 69, 'F1', '果香浓郁纤维少，皮薄核薄，入口即化。'),
    (222, '大连冷冻虾仁 去壳去虾线 500g', 59, 89, 'F1', '个大饱满，-38℃ 急冻锁鲜，Q 弹爽口。'),
    (231, '云南小粒咖啡豆 中深烘焙 454g', 59, 89, 'F3', '阿拉比卡豆，坚果巧克力风味，现烘直发。'),
    (232, '明前龙井绿茶 罐装 250g', 129, 259, 'F3', '明前手工采摘，豆香明显，回甘持久。'),
    # ---- 运动 10 ----
    (311, '轻量缓震跑步鞋 男女同款透气', 219, 359, 'S', '回弹中底 + 透气网面，长跑不累脚。'),
    (312, '高帮实战篮球鞋 防侧翻水泥地', 269, 429, 'S', '高帮护踝，耐磨大底，实战缓震抗扭。'),
    (313, '速干运动T恤男健身跑步短袖', 59, 99, 'T', '冰感速干面料，吸湿排汗，健身跑步清爽。'),
    (321, '加厚防滑瑜伽垫 10mm 初学者', 69, 129, 'D2', '10mm 加厚回弹，防滑纹理，附绑带背包。'),
    (322, '可调节哑铃 20kg 一对 家用健身', 199, 399, 'C1', '快速调节重量，包胶静音，一物多用。'),
    (323, '智能计数跳绳 无绳负重两用', 39, 79, 'D1', '电子计数，无绳/有绳切换，室内不扰邻。'),
    (331, '户外折叠露营椅 便携承重300斤', 89, 159, 'D1', '加粗钢管承重强，一秒折叠，附收纳袋。'),
    (331, '自动充气防潮垫 加厚野餐垫', 109, 189, 'D2', '自动充气 3 秒回弹，防潮隔热，露营必备。'),
    (332, '山地车骑行头盔 一体成型带尾灯', 129, 229, 'D1', '一体成型抗冲击，磁吸尾灯，骑行安全。'),
    (343, '碳素羽毛球拍 双拍套装含球', 159, 299, 'C2', '全碳素拍杆轻量高弹，附 3 球 + 手胶 + 拍包。'),
]

# spec 模板定义：key -> [(维度名, 取值列表, 该维度最多保留几个)]
SPEC_TEMPLATES = {
    'T': [('颜色', SPEC_COLOR4, 2), ('尺码', SPEC_SIZE, 3)],
    'S': [('颜色', SPEC_COLOR4, 2), ('尺码', SPEC_SIZE, 2)],
    'P': [('颜色', SPEC_PHONE_COLOR, 2), ('存储', SPEC_STORAGE, 2)],
    'C1': [('版本', SPEC_VER, 2)],
    'C2': [('颜色', ['深空灰', '海岛蓝', '雪域白'], 2)],
    'C3': [('尺寸', ['27英寸', '32英寸'], 2)],
    'D1': [('颜色', ['黑色', '白色'], 2)],
    'D2': [('规格', ['标准款', '升级款'], 2)],
    'D3': [('颜色', ['透明', '磨砂黑', '浅蓝'], 2)],
    'F1': [('规格', SPEC_FOOD_WEIGHT, 2)],
    'F2': [('口味', ['原味', '奶香', '抹茶'], 2)],
    'F3': [('规格', SPEC_FOOD_WEIGHT, 2)],
}

# ============================================================
# 1. 用户 / 地址素材
# ============================================================
SURNAMES = ['zhang', 'wang', 'li', 'zhao', 'liu', 'chen', 'yang', 'huang', 'zhou', 'wu',
            'xu', 'sun', 'hu', 'zhu', 'gao', 'lin', 'he', 'guo', 'ma', 'luo',
            'liang', 'song', 'zheng', 'xie', 'han', 'tang', 'feng', 'deng', 'cao', 'peng']
GIVENS = ['wei', 'min', 'jing', 'lei', 'na', 'qiang', 'yan', 'tao', 'chao', 'hui',
          'juan', 'peng', 'yu', 'hong', 'gang', 'xin', 'rui', 'jia', 'hao', 'qian',
          'bin', 'ning', 'fei', 'lu', 'ting', 'yong', 'kun', 'mei', 'dong', 'fang']
NICK_PRE = ['爱吃火锅的', '爱运动的', '数码控', '追剧的', '健身的', '文艺的', '热心的', '低调的', '快乐的',
            '慢生活的', '爱喝咖啡的', '熬夜的', '爱旅行的', '会省钱的', '爱做饭的', '养猫的', '养狗的',
            '爱读书的', '听歌的', '爱拍照的']
NICK_CORE = ['小林', '阿伟', '橙子', '山茶', '海风', '柚子', '糖豆', '阿宁', '栗子', '半夏',
             '阿哲', '小满', '青柠', '晚风', '晴天', '阿树', '小鹿', '南星', '子墨', '一诺']

CITY_POOL = [
    ('广东省', '深圳市', '南山区', '科技园路'), ('广东省', '广州市', '天河区', '体育西路'),
    ('北京市', '北京市', '朝阳区', '望京街'), ('上海市', '上海市', '浦东新区', '张江路'),
    ('浙江省', '杭州市', '西湖区', '文一西路'), ('江苏省', '南京市', '鼓楼区', '中山北路'),
    ('四川省', '成都市', '武侯区', '天府大道'), ('湖北省', '武汉市', '洪山区', '珞喻路'),
    ('陕西省', '西安市', '雁塔区', '高新路'), ('山东省', '青岛市', '市南区', '香港中路'),
    ('福建省', '厦门市', '思明区', '软件园二期'), ('湖南省', '长沙市', '岳麓区', '麓谷大道'),
]
RECEIVER_NAMES = ['张伟', '王芳', '李娜', '刘洋', '陈静', '杨帆', '赵磊', '黄敏', '周涛', '吴倩',
                  '徐强', '孙丽', '胡军', '朱婷', '高峰', '林杰', '何平', '郭涛', '马超', '罗琳',
                  '梁静', '宋佳', '郑爽', '谢军', '韩雪', '唐磊', '冯刚', '邓超', '曹颖', '彭勇']

# 评价内容池（按域）
REVIEW_TEXT = {
    'cloth': ['面料很舒服，尺码标准，洗了不掉色', '版型好看，穿上显瘦，物流也快', '质量比想象中好，会回购',
              '颜色和图片一致，做工细致没有线头', '夏天穿很透气，性价比高', '给家里人也买了一件，都很满意',
              '收到就试穿了，合身，好评', '面料厚实，不是那种薄的，很满意'],
    'digital': ['开机流畅，做工精致，包装完好', '性能很强，玩游戏不卡，发热控制不错', '性价比很高，用了一周很稳定',
                '拍照效果出色，续航给力', '音质超出预期，降噪明显', '发货速度快，正品没毛病',
                '屏幕清晰，手感很好，值得购买', '连接稳定，延迟低，体验不错'],
    'food': ['味道很正宗，包装严实没有破损', '分量足，口感好，家里人都爱吃', '很新鲜，日期也是新的',
             '甜度适中不腻，独立包装方便', '回购第三次了，一如既往地好', '下饭神器，拌面拌饭都好吃',
             '果肉很饱满，水分足，比超市便宜', '口感不错，就是有点小贵，做活动划算'],
    'sports': ['弹性很好，回弹明显，跑步舒服', '穿着透气，运动不闷汗', '做工扎实，用料足，值得入手',
               '防滑效果不错，收纳也方便', '重量合适，家用足够', '颜值很高，使用体验好，推荐',
               '支撑性不错，保护到位', '性价比高，新手够用了'],
}
DOMAIN_OF_CAT = {}


def domain_of(cat_id):
    if 1 <= cat_id < 100:
        return 'cloth'
    if 100 <= cat_id < 200:
        return 'digital'
    if 200 <= cat_id < 300:
        return 'food'
    return 'sports'


# ============================================================
# 2. 生成段
# ============================================================
w('-- ============================================================')
w('-- 电商智能系统 · 演示数据（由 sql/data_generator.py 生成，seed=20260915）')
w('-- 执行顺序：1) ecom.sql 建库 2) data.sql 演示数据 3) data_images.sql 商品配图（可选）')
w('-- 内容：50 商品(服饰/数码/美食/运动) / 每商品 2-6 SKU / 200 用户 / 500 订单(7 状态)')
w('--       / 评价 / 搜索热词 / 客服会话 / 30 天看板汇总；可重复执行（先清后插）')
w('-- ============================================================')
w('SET NAMES utf8mb4;')
w()

w('-- ---------- 0. 清理旧业务数据（保留 sys_* / 类目 / 知识库 / 配置） ----------')
for t in ['ec_review', 'ec_refund', 'ec_logistics', 'ec_order_status_log', 'ec_order_item', 'ec_order',
          'ec_cart', 'ec_user_coupon', 'ec_address', 'ec_product_image', 'ec_product_spec', 'ec_sku',
          'ec_product', 'ec_search_log', 'ec_chat_log', 'ec_user_behavior', 'ec_stock_alert',
          'ec_sales_forecast', 'ec_product_sales_daily', 'ec_stat_daily']:
    w('DELETE FROM %s;' % t)
w("DELETE ur FROM sys_user_role ur JOIN sys_user u ON ur.user_id = u.id WHERE u.username IN ('buyer99', 'buyer100') OR u.id BETWEEN 1001 AND 1200;")
w("DELETE FROM sys_user WHERE username IN ('buyer99', 'buyer100') OR id BETWEEN 1001 AND 1200;")
w("DELETE FROM ec_coupon;")
w('ALTER TABLE ec_coupon AUTO_INCREMENT = 1;')
w('-- 物理清理早期试建的已删测试类目（4-11），避免与演示类目 id 段冲突')
w('DELETE FROM ec_category WHERE id BETWEEN 4 AND 11;')
w()

w('-- ---------- 1. 补充服饰子类目（INSERT IGNORE 保证重复执行安全） ----------')
for cid, pid, name, sort in NEW_CATEGORIES:
    w("INSERT IGNORE INTO ec_category (id, parent_id, name, sort, del_flag) VALUES (%d, %d, '%s', %d, 0);"
      % (cid, pid, name, sort))
w()

# ---------- 2. 商品 / 规格 / SKU ----------
products = []  # (pid, title, cat, price_min, price_max, skus:[(sid, specs_json, price, stock, sales)], domain)
spec_rows = []
sku_rows = []
pid_base, spec_id, sku_id = 1001, 3001, 5001
used_sku_combo = set()

for i, (cat, title, low, high, tkey, desc) in enumerate(PRODUCTS):
    pid = pid_base + i
    dims = SPEC_TEMPLATES[tkey]
    chosen = []
    for dname, values, keep in dims:
        vals = values[:] if len(values) <= keep else random.sample(values, keep)
        chosen.append((dname, vals))
        spec_rows.append((spec_id, pid, dname, json.dumps(vals, ensure_ascii=False), len(chosen) - 1))
        spec_id += 1
    combos = [[]]
    for dname, vals in chosen:
        combos = [c + [(dname, v)] for c in combos for v in vals]
    max_sku = min(6, len(combos))
    if max_sku < 2:
        max_sku = len(combos)
    combos = random.sample(combos, max_sku) if len(combos) > max_sku else combos
    skus = []
    for j, combo in enumerate(combos):
        specs_map = {k: v for k, v in combo}
        base = random.uniform(low, high)
        if j > 0:
            base = min(high, base + j * random.uniform(2, 15))
        price = round(base, 2)
        stock = random.randint(30, 500)
        skus.append((sku_id, json.dumps(specs_map, ensure_ascii=False), price, stock))
        sku_rows.append((sku_id, pid, json.dumps(specs_map, ensure_ascii=False), price, stock))
        sku_id += 1
    products.append({
        'id': pid, 'title': title, 'cat': cat, 'desc': desc,
        'price_min': min(s[2] for s in skus), 'price_max': max(s[2] for s in skus),
        'skus': skus, 'domain': domain_of(cat), 'sales': 0,
    })

w('-- ---------- 2. 商品（50 个，覆盖服饰/数码/美食/运动） ----------')
for p in products:
    w("INSERT INTO ec_product (id, title, category_id, main_image, description, status, price_min, price_max, sales, create_time, update_time, del_flag) "
      "VALUES (%d, '%s', %d, NULL, '%s', 1, %s, %s, 0, '%s', '%s', 0);"
      % (p['id'], esc(p['title']), p['cat'], esc(p['desc']),
         fmt_m(p['price_min']), fmt_m(p['price_max']),
         fmt_dt(NOW - timedelta(days=random.randint(60, 200))), fmt_dt(NOW - timedelta(days=random.randint(1, 30)))))

w()
w('-- ---------- 3. 商品规格维度 ----------')
for sid, pid, name, vals, sort in spec_rows:
    w("INSERT INTO ec_product_spec (id, product_id, spec_name, spec_values, sort, del_flag) VALUES "
      "(%d, %d, '%s', '%s', %d, 0);" % (sid, pid, esc(name), esc(vals), sort))

w()
w('-- ---------- 4. SKU（每商品 %d-%d 个） ----------' % (min(len(p['skus']) for p in products), max(len(p['skus']) for p in products)))
for sid, pid, specs, price, stock in sku_rows:
    w("INSERT INTO ec_sku (id, product_id, specs, sku_code, price, stock, version, image, sales, status, del_flag) VALUES "
      "(%d, %d, '%s', 'SKU%d', %s, %d, 0, NULL, 0, 1, 0);"
      % (sid, pid, esc(specs), sid, fmt_m(price), stock))

# ---------- 5. 用户 / 地址 ----------
w()
w('-- ---------- 5. 用户 200 个（user_type=1 消费者，密码 123456）+ 每人 1 个收货地址 ----------')
# 复用 member1 的 bcrypt 密码哈希（明文 123456，BCrypt 同一明文每次哈希不同但校验等价）
PWD_HASH = '$2a$10$YlL.lbclPz4/ZDn/28kY/u/PDdv.ug0Eef8mG6bp.XdjgLvA7YUGy'
usernames = set()
users = []
uid = 1001
while len(users) < 200:
    u = random.choice(SURNAMES) + random.choice(GIVENS) + str(random.randint(1, 99))
    if u in usernames:
        continue
    usernames.add(u)
    nick = random.choice(NICK_PRE) + random.choice(NICK_CORE)
    users.append({'id': uid, 'username': u, 'nick': nick})
    uid += 1

for u in users:
    c = random.choice(CITY_POOL)
    phone = random.choice(['138', '139', '150', '158', '176', '186', '199']) + ''.join(str(random.randint(0, 9)) for _ in range(8))
    u['name'] = random.choice(RECEIVER_NAMES)
    u['phone'] = phone
    u['addr'] = '%s%s%s%s号' % (c[0], c[1], c[2], c[3] + str(random.randint(1, 999)) + '号')

for u in users:
    w("INSERT INTO sys_user (id, username, nick_name, password, user_type, phone, status, del_flag) VALUES "
      "(%d, '%s', '%s', '%s', 1, '%s', 0, 0);" % (u['id'], u['username'], esc(u['nick']), PWD_HASH, u['phone']))
for u in users:
    w("INSERT INTO sys_user_role (user_id, role_id) VALUES (%d, 5);" % u['id'])

aid = 2001
for u in users + [{'id': 3, 'name': '张三', 'phone': '13800000000', 'addr': '广东省广州市天河区体育西路 1 号'}]:
    w("INSERT INTO ec_address (id, user_id, receiver_name, phone, province, city, district, detail, is_default, del_flag) "
      "VALUES (%d, %d, '%s', '%s', '省', '市', '区', '%s', 1, 0);" % (aid, u['id'], esc(u['name']), u['phone'], esc(u['addr'])))
    aid += 1

# ---------- 6. 优惠券 ----------
w()
w('-- ---------- 6. 优惠券 4 张 + 演示用户领券 ----------')
coupons = [
    (1, '新客立减券', 0, 100, 10, 0, 1000, 1, ['2026-09-15 00:00:00', '2026-12-31 23:59:59']),
    (2, '满 200 减 30', 0, 200, 30, 0, 1000, 1, ['2026-09-15 00:00:00', '2026-12-31 23:59:59']),
    (3, '满 150 减 20', 0, 150, 20, 0, 800, 1, ['2026-09-15 00:00:00', '2026-11-30 23:59:59']),
    (4, '满 300 减 50', 0, 300, 50, 0, 500, 1, ['2026-09-15 00:00:00', '2026-11-30 23:59:59']),
]
for cid, name, ctype, thr, dis, rate, total, limit_n, times in coupons:
    w("INSERT INTO ec_coupon (id, name, type, threshold_amount, discount_amount, discount_rate, total_count, received_count, per_user_limit, start_time, end_time, status, del_flag) "
      "VALUES (%d, '%s', %d, %s, %s, NULL, %d, 0, %d, '%s', '%s', 0, 0);"
      % (cid, esc(name), ctype, fmt_m(thr), fmt_m(dis), total, limit_n, times[0], times[1]))

# member1 未使用的券（演示下单可用）
w("INSERT INTO ec_user_coupon (id, user_id, coupon_id, status, receive_time, del_flag) VALUES (1, 3, 1, 0, '%s', 0);" % fmt_dt(NOW - timedelta(days=2)))
w("INSERT INTO ec_user_coupon (id, user_id, coupon_id, status, receive_time, del_flag) VALUES (2, 3, 2, 0, '%s', 0);" % fmt_dt(NOW - timedelta(days=1)))

# ---------- 7. 订单 ----------
w()
w('-- ---------- 7. 订单 500 笔（已完成300 / 待支付60 / 待发货60 / 运输中40 / 已取消20 / 退款中10 / 已退款10） ----------')
STATUS_PLAN = [(3, 300), (0, 60), (1, 60), (2, 40), (4, 20), (5, 10), (6, 10)]
COMPANIES = [('顺丰速运', 'SF'), ('中通快递', 'ZT'), ('圆通速递', 'YT'), ('京东物流', 'JD')]
REFUND_REASONS = ['不想要了', '拍错了，重新买', '商品降价了', '发货太慢了', '尺码不合适', '临时不需要了']
AUDIT_OK = ['同意退款，已原路退回', '核实后同意退款', '同意，款项 1-3 个工作日到账']
CANCEL_REASON_USER = '用户主动取消'
CANCEL_REASON_SYS = '超时未支付，系统自动取消'

orders = []
oid, item_id, log_id, lg_id, rf_id = 2001, 5001, 8001, 9001, 1
uc_id = 100
order_seq = 0

sales_by_sku = {}
sales_by_product = {}

# 时间分布：已完成订单散布 180 天；未完结订单集中在近 7 天
def rand_create_time(final_status):
    if final_status == 3:
        return NOW - timedelta(days=random.randint(3, 180), hours=random.randint(0, 23), minutes=random.randint(0, 59))
    if final_status == 0:
        return NOW - timedelta(minutes=random.randint(1, 45))
    return NOW - timedelta(days=random.randint(0, 6), hours=random.randint(0, 23), minutes=random.randint(0, 59))


plan = []
for status, n in STATUS_PLAN:
    plan += [status] * n
random.shuffle(plan)

# 演示主账号 member1（id=3）每个状态各安排 3 笔，保证「我的订单」各标签有内容
MEMBER1 = {'id': 3, 'name': '张三', 'phone': '13800000000',
           'addr': '广东省广州市天河区体育西路 1 号', 'username': 'member1', 'nick': '会员一号'}
m1_quota = {}
m1_completed_seq = 0

for final_status in plan:
    oid += 1
    order_seq += 1
    if m1_quota.get(final_status, 0) < 3:
        user = MEMBER1
        m1_quota[final_status] = m1_quota.get(final_status, 0) + 1
    else:
        user = random.choice(users)
    ct = rand_create_time(final_status)
    order_no = 'EC' + ct.strftime('%Y%m%d%H%M%S') + '%04d' % order_seq
    n_items = random.choices([1, 2, 3], weights=[50, 35, 15])[0]
    picked = random.sample(products, n_items)
    items = []
    total = 0.0
    for p in picked:
        sku = random.choice(p['skus'])
        qty = random.choices([1, 2, 3], weights=[70, 22, 8])[0]
        subtotal = sku[2] * qty
        total += subtotal
        items.append({'p': p, 'sku': sku, 'qty': qty, 'subtotal': subtotal})
    total = round(total, 2)

    # 优惠券（仅已支付类订单 25% 概率）
    coupon_id = None
    discount = 0.0
    if final_status in (1, 2, 3, 5, 6) and random.random() < 0.25:
        avail = [c for c in coupons if total >= c[3]]
        if avail:
            c = random.choice(avail)
            coupon_id = c[0]
            discount = float(c[4])
    pay_amount = round(total - discount, 2)

    # 各状态时间线
    pay_time = ship_time = receive_time = cancel_time = None
    expire_time = ct + timedelta(minutes=15)
    if final_status == 0:
        expire_time = NOW + timedelta(days=30)  # 演示用：避免被超时任务清理
    if final_status in (1, 2, 3, 5, 6):
        pay_time = min(ct + timedelta(minutes=random.randint(1, 12)), NOW)
        if final_status in (2, 3):
            ship_time = min(pay_time + timedelta(hours=random.randint(2, 30)), NOW)
            if final_status == 3:
                receive_time = min(ship_time + timedelta(days=random.randint(1, 5), hours=random.randint(0, 12)), NOW)
    if final_status == 4:
        if random.random() < 0.6:
            cancel_time = ct + timedelta(minutes=random.randint(3, 25))
        else:
            cancel_time = ct + timedelta(minutes=15)

    # 订单主表
    w("INSERT INTO ec_order (id, order_no, user_id, total_amount, discount_amount, pay_amount, user_coupon_id, status, receiver_name, receiver_phone, receiver_address, pay_type, pay_time, ship_time, receive_time, cancel_time, remark, idempotency_key, expire_time, create_time, del_flag) VALUES "
      "(%d, '%s', %d, %s, %s, %s, %s, %d, '%s', '%s', '%s', %s, %s, %s, %s, %s, NULL, 'seed-%d', '%s', '%s', 0);"
      % (oid, order_no, user['id'], fmt_m(total), fmt_m(discount), fmt_m(pay_amount),
         str(coupon_id) if coupon_id else 'NULL', final_status, esc(user['name']), user['phone'], esc(user['addr']),
         '0' if final_status != 0 else 'NULL',
         "'%s'" % fmt_dt(pay_time) if pay_time else 'NULL',
         "'%s'" % fmt_dt(ship_time) if ship_time else 'NULL',
         "'%s'" % fmt_dt(receive_time) if receive_time else 'NULL',
         "'%s'" % fmt_dt(cancel_time) if cancel_time else 'NULL',
         oid, fmt_dt(expire_time), fmt_dt(ct)))

    # 明细
    for it in items:
        p, sku = it['p'], it['sku']
        w("INSERT INTO ec_order_item (id, order_id, order_no, product_id, sku_id, product_name, product_image, specs, price, quantity, subtotal, create_time, del_flag) VALUES "
          "(%d, %d, '%s', %d, %d, '%s', NULL, '%s', %s, %d, %s, '%s', 0);"
          % (item_id, oid, order_no, p['id'], sku[0], esc(p['title']), esc(sku[1]), fmt_m(sku[2]), it['qty'], fmt_m(it['subtotal']), fmt_dt(ct)))
        it['item_id'] = item_id
        item_id += 1
        if final_status == 3:
            sales_by_sku[sku[0]] = sales_by_sku.get(sku[0], 0) + it['qty']
            sales_by_product[p['id']] = sales_by_product.get(p['id'], 0) + it['qty']

    # 状态日志
    w("INSERT INTO ec_order_status_log (id, order_no, from_status, to_status, operator_type, operator_id, remark, create_time, del_flag) VALUES "
      "(%d, '%s', -1, 0, 1, %d, '创建订单', '%s', 0);" % (log_id, order_no, user['id'], fmt_dt(ct)))
    log_id += 1
    if pay_time:
        w("INSERT INTO ec_order_status_log (id, order_no, from_status, to_status, operator_type, operator_id, remark, create_time, del_flag) VALUES "
          "(%d, '%s', 0, 1, 1, %d, '模拟支付成功', '%s', 0);" % (log_id, order_no, user['id'], fmt_dt(pay_time)))
        log_id += 1
    if ship_time:
        comp = random.choice(COMPANIES)
        w("INSERT INTO ec_order_status_log (id, order_no, from_status, to_status, operator_type, operator_id, remark, create_time, del_flag) VALUES "
          "(%d, '%s', 1, 2, 2, 1, '商家发货：%s', '%s', 0);" % (log_id, order_no, comp[0], fmt_dt(ship_time)))
        log_id += 1
    if receive_time:
        w("INSERT INTO ec_order_status_log (id, order_no, from_status, to_status, operator_type, operator_id, remark, create_time, del_flag) VALUES "
          "(%d, '%s', 2, 3, 1, %d, '确认收货', '%s', 0);" % (log_id, order_no, user['id'], fmt_dt(receive_time)))
        log_id += 1
    if cancel_time:
        sys_cancel = random.random() < 0.4
        w("INSERT INTO ec_order_status_log (id, order_no, from_status, to_status, operator_type, operator_id, remark, create_time, del_flag) VALUES "
          "(%d, '%s', 0, 4, %d, %s, '%s', '%s', 0);"
          % (log_id, order_no, 3 if sys_cancel else 1, 'NULL' if sys_cancel else str(user['id']),
             CANCEL_REASON_SYS if sys_cancel else CANCEL_REASON_USER, fmt_dt(cancel_time)))
        log_id += 1
    if final_status in (5, 6):
        reason = random.choice(REFUND_REASONS)
        w("INSERT INTO ec_order_status_log (id, order_no, from_status, to_status, operator_type, operator_id, remark, create_time, del_flag) VALUES "
          "(%d, '%s', 1, 5, 1, %d, '申请退款：%s', '%s', 0);"
          % (log_id, order_no, user['id'], reason, fmt_dt(pay_time + timedelta(hours=random.randint(1, 20)))))
        log_id += 1
    if final_status == 6:
        audit_t = pay_time + timedelta(hours=random.randint(20, 40))
        w("INSERT INTO ec_order_status_log (id, order_no, from_status, to_status, operator_type, operator_id, remark, create_time, del_flag) VALUES "
          "(%d, '%s', 5, 6, 2, 1, '退款完成', '%s', 0);" % (log_id, order_no, fmt_dt(audit_t)))
        log_id += 1

    # 物流（仅运输中/已完成，退款单按状态机不经过发货）
    if final_status in (2, 3) and ship_time:
        comp = random.choice(COMPANIES)
        tracking = comp[1] + ''.join(str(random.randint(0, 9)) for _ in range(11))
        stages = [
            (ship_time, '%s：包裹已揽收' % comp[0]),
            (ship_time + timedelta(hours=8), '%s：包裹运输中' % comp[0]),
            (ship_time + timedelta(days=1, hours=4), '%s：包裹派送中' % comp[0]),
            (ship_time + timedelta(days=1, hours=10), '包裹已签收，感谢使用'),
        ]
        if final_status == 2:  # 运输中：轨迹只到当前时间
            stages = [s for s in stages if s[0] <= NOW] or stages[:1]
        trace = json.dumps([{'time': fmt_dt(t), 'text': txt} for t, txt in stages], ensure_ascii=False)
        lg_status = 4 if final_status == 3 else 2
        w("INSERT INTO ec_logistics (id, order_no, company, tracking_no, status, trace_json, create_time, del_flag) VALUES "
          "(%d, '%s', '%s', '%s', %d, '%s', '%s', 0);"
          % (lg_id, order_no, comp[0], tracking, lg_status, esc(trace), fmt_dt(ship_time)))
        lg_id += 1

    # 退款单
    if final_status in (5, 6):
        apply_t = pay_time + timedelta(hours=random.randint(1, 20))
        if final_status == 5:
            w("INSERT INTO ec_refund (id, order_no, user_id, amount, reason, status, create_time, del_flag) VALUES "
              "(%d, '%s', %d, %s, '%s', 0, '%s', 0);"
              % (rf_id, order_no, user['id'], fmt_m(pay_amount), random.choice(REFUND_REASONS), fmt_dt(apply_t)))
        else:
            audit_t = apply_t + timedelta(hours=random.randint(2, 20))
            w("INSERT INTO ec_refund (id, order_no, user_id, amount, reason, status, audit_user_id, audit_time, audit_remark, create_time, del_flag) VALUES "
              "(%d, '%s', %d, %s, '%s', 1, 1, '%s', '%s', '%s', 0);"
              % (rf_id, order_no, user['id'], fmt_m(pay_amount), random.choice(REFUND_REASONS),
                 fmt_dt(audit_t), random.choice(AUDIT_OK), fmt_dt(apply_t)))
        rf_id += 1

    # 已使用的 user_coupon
    if coupon_id:
        uc_id += 1
        w("INSERT INTO ec_user_coupon (id, user_id, coupon_id, status, receive_time, use_time, order_no, del_flag) VALUES "
          "(%d, %d, %d, 1, '%s', '%s', '%s', 0);"
          % (uc_id, user['id'], coupon_id, fmt_dt(ct - timedelta(days=1)), fmt_dt(ct), order_no))

    skip_review = False
    if user is MEMBER1 and final_status == 3:
        m1_completed_seq += 1
        # member1 的前 2 笔已完成订单不预置评价，供演示「提交评价」闭环
        skip_review = m1_completed_seq <= 2
    orders.append({'no': order_no, 'status': final_status, 'items': items,
                   'receive_time': receive_time, 'user': user, 'skip_review': skip_review})

# 回填商品/SKU 销量（已完成订单口径）
w()
w('-- ---------- 8. 回填商品与 SKU 销量（口径：已完成订单） ----------')
for p in products:
    s = sales_by_product.get(p['id'], 0)
    if s:
        w('UPDATE ec_product SET sales = %d WHERE id = %d;' % (s, p['id']))
for sid, v in sales_by_sku.items():
    w('UPDATE ec_sku SET sales = %d WHERE id = %d;' % (v, sid))
# 增加 SKU 库存消耗观感（已完成+进行中订单的扣减已体现在 stock？演示数据不做严格对账，SKU 初始库存已随机）

# ---------- 9. 评价 ----------
w()
w('-- ---------- 9. 商品评价（已完成订单约 70% 有评价） ----------')
rid = 6001
review_count = 0
for o in orders:
    if o['status'] != 3 or o.get('skip_review') or random.random() > 0.7:
        continue
    for it in o['items']:
        p = it['p']
        rating = random.choices([5, 4, 3, 2, 1], weights=[70, 20, 6, 3, 1])[0]
        content = random.choice(REVIEW_TEXT[p['domain']])
        rt = o['receive_time'] + timedelta(hours=random.randint(2, 72))
        if rt > NOW:
            rt = NOW - timedelta(hours=random.randint(1, 24))
        w("INSERT INTO ec_review (id, order_no, order_item_id, user_id, product_id, sku_id, rating, content, images, status, create_time, del_flag) VALUES "
          "(%d, '%s', %d, %d, %d, %d, %d, '%s', NULL, 1, '%s', 0);"
          % (rid, o['no'], it['item_id'], o['user']['id'], p['id'], it['sku'][0], rating, esc(content), fmt_dt(rt)))
        rid += 1
        review_count += 1

# ---------- 10. 搜索日志（热词榜/联想） ----------
w()
w('-- ---------- 10. 搜索日志（热词榜 + 前缀联想数据源） ----------')
HOT_WORDS = [('手机壳', 40), ('T恤', 36), ('蓝牙耳机', 32), ('零食', 28), ('跑步鞋', 26), ('连衣裙', 24),
             ('iPhone', 22), ('充电器', 20), ('咖啡', 18), ('瑜伽垫', 16), ('坚果', 15), ('篮球鞋', 14),
             ('数据线', 13), ('巧克力', 12), ('手表', 11), ('洗发水', 10), ('牛肉干', 10), ('瑜伽', 9),
             ('显示器', 8), ('羽绒服', 8), ('保温杯', 7), ('机械键盘', 6)]
sl_id = 1
for word, weight in HOT_WORDS:
    for _ in range(weight):
        t = NOW - timedelta(days=random.randint(0, 29), hours=random.randint(0, 23))
        uid_v = random.choice(users)['id'] if random.random() < 0.6 else 'NULL'
        w("INSERT INTO ec_search_log (id, user_id, keyword, result_count, create_time, del_flag) VALUES "
          "(%d, %s, '%s', %d, '%s', 0);" % (sl_id, uid_v, word, random.randint(0, 12), fmt_dt(t)))
        sl_id += 1

# ---------- 11. 客服会话日志 ----------
w()
w('-- ---------- 11. 客服会话日志（含少量转人工，构成「待补充问题」演示数据） ----------')
CHAT_SEED = [
    ('如何申请退款', '您好，待发货状态的订单可在「我的订单-订单详情」点击"申请退款"…', 0.99, 0),
    ('多久发货呀', '亲，付款后 48 小时内发货，节假日顺延…', 0.96, 0),
    ('怎么用优惠券', '在结算页选择可用优惠券即可自动抵扣…', 0.95, 0),
    ('运费怎么算', '单笔订单满 99 元包邮，未满收 8 元运费…', 0.97, 0),
    ('尺码怎么选', '建议参考商品详情页尺码表，正常体型选日常穿的尺码即可…', 0.94, 0),
    ('怎么修改收货地址', '未发货前可在订单详情联系客服修改收货地址…', 0.93, 0),
    ('发票怎么开', '您好，这个问题我先帮您转接人工客服～', 0.21, 1),
    ('可以货到付款吗', '您好，这个问题我先帮您转接人工客服～', 0.18, 1),
]
cl_id = 1
for q, a, conf, transfer in CHAT_SEED:
    for _ in range(random.randint(1, 4)):
        t = NOW - timedelta(days=random.randint(0, 14), hours=random.randint(0, 23))
        uid_v = random.choice(users)['id']
        w("INSERT INTO ec_chat_log (id, user_id, question, answer, confidence, is_transfer, create_time, del_flag) VALUES "
          "(%d, %d, '%s', '%s', %s, %d, '%s', 0);" % (cl_id, uid_v, esc(q), esc(a), conf, transfer, fmt_dt(t)))
        cl_id += 1

# ---------- 12. 购物车（member1 演示） ----------
w()
w('-- ---------- 12. 演示账号购物车（member1） ----------')
demo_skus = [(products[0]['skus'][0], 1), (products[15]['skus'][0], 2), (products[30]['skus'][0], 1)]
ci_id = 1
for sku, qty in demo_skus:
    w("INSERT INTO ec_cart (id, user_id, sku_id, quantity, checked, del_flag) VALUES (%d, 3, %d, %d, 1, 0);"
      % (ci_id, sku[0], qty))
    ci_id += 1

# ---------- 13. 看板 30 天汇总 ----------
w()
w('-- ---------- 13. 看板汇总：ec_stat_daily 30 天 + ec_product_sales_daily ----------')
# 按订单号内嵌时间戳反推每日指标
daily = {}
for o in orders:
    ct = datetime.strptime(o['no'][2:16], '%Y%m%d%H%M%S')
    key = ct.date()
    if key not in daily:
        daily[key] = {'gmv': 0.0, 'order': 0, 'paid': 0, 'refund': 0}
    daily[key]['order'] += 1
    if o['status'] in (1, 2, 3, 5, 6):
        daily[key]['paid'] += 1
        daily[key]['gmv'] += sum(it['subtotal'] for it in o['items'])
    if o['status'] in (5, 6):
        daily[key]['refund'] += 1

sd_id = 1
for i in range(29, -1, -1):
    d = (NOW - timedelta(days=i)).date()
    rec = daily.get(d, {'gmv': 0.0, 'order': 0, 'paid': 0, 'refund': 0})
    weekend = d.weekday() >= 5
    visitor = random.randint(500, 1100) * (1.35 if weekend else 1.0)
    w("INSERT INTO ec_stat_daily (id, stat_date, gmv, order_count, paid_count, refund_count, visitor_count, pv, cart_count, del_flag) VALUES "
      "(%d, '%s', %s, %d, %d, %d, %d, %d, %d, 0);"
      % (sd_id, d.strftime('%Y-%m-%d'), fmt_m(round(rec['gmv'], 2)), rec['order'], rec['paid'], rec['refund'],
         int(visitor), int(visitor * random.uniform(3.2, 5.8)), int(visitor * random.uniform(0.25, 0.6))))
    sd_id += 1

# product_sales_daily：近 30 天，按 SKU 汇总
daily_sku = {}
for o in orders:
    ct = datetime.strptime(o['no'][2:16], '%Y%m%d%H%M%S')
    if (NOW.date() - ct.date()).days > 29 or o['status'] not in (1, 2, 3, 5, 6):
        continue
    for it in o['items']:
        key = (ct.date(), it['p']['id'], it['sku'][0])
        agg = daily_sku.setdefault(key, {'qty': 0, 'amt': 0.0})
        agg['qty'] += it['qty']
        agg['amt'] += it['subtotal']

psd_id = 1
for (d, pid, sid), agg in sorted(daily_sku.items()):
    w("INSERT INTO ec_product_sales_daily (id, stat_date, product_id, sku_id, sales_qty, sales_amount, del_flag) VALUES "
      "(%d, '%s', %d, %d, %d, %s, 0);" % (psd_id, d.strftime('%Y-%m-%d'), pid, sid, agg['qty'], fmt_m(round(agg['amt'], 2))))
    psd_id += 1

w()
w('-- 完：data.sql（如需商品配图，执行 python sql/upload_images.py 后跑 data_images.sql）')

# ---------- 写文件 ----------
with open(OUT_SQL, 'w', encoding='utf-8', newline='\n') as f:
    f.write('\n'.join(L) + '\n')

manifest = [{'id': p['id'], 'title': p['title'], 'hue': (p['id'] * 47) % 360} for p in products]
with open(OUT_MANIFEST, 'w', encoding='utf-8') as f:
    json.dump(manifest, f, ensure_ascii=False, indent=2)

print('OK data.sql: %d lines, products=%d, users=%d, orders=%d, reviews=%d, search=%d, skuDaily=%d'
      % (len(L), len(products), len(users), len(orders), review_count, sl_id - 1, psd_id - 1))
