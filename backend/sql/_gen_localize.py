# -*- coding: utf-8 -*-
"""RR-5 存量数据本地化辅助脚本：
1) 将 index.sql 中 product 的种子 product_intro 由外部图片 URL 改为文字描述；
2) 将 product_imgs 由外部 text_to_image API URL 改为本地 /uploads/products/... 约定；
3) 在 backend/src/main/resources/seed-images/products/ 下生成对应的 SVG 占位图（自包含、无外部依赖）。
"""
import os
import re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INDEX = os.path.join(BASE, "index.sql")
SEED_DIR = os.path.join(BASE, "src", "main", "resources", "seed-images", "products")

# id -> (商品介绍文字, 占位图背景色, 简称)
META = {
    "p001": ("经典纯白低帮板鞋，头层牛皮鞋面，Air 气垫缓震，百搭耐穿。", "#f0f0f0", "AF1 白"),
    "p002": ("Primeknit 针织鞋面搭配 Boost 中底，黑白配色，轻盈回弹。", "#2b2b2b", "Yeezy 黑白"),
    "p003": ("美产复古跑鞋，ENCAP 缓震中底，灰色麂皮，舒适支撑。", "#9e9e9e", "NB 990 灰"),
    "p004": ("经典高帮帆布鞋，加厚鞋头与鞋底，黑色简约百搭。", "#1a1a1a", "Chuck 70 黑"),
    "p005": ("经典黑白滑板鞋，麂皮拼接鞋面，华夫格大底，街头风格。", "#eeeeee", "Old Skool"),
    "p006": ("经典蓝色麂皮板鞋，橡胶大底，复古运动风范。", "#1f4e79", "Suede 蓝"),
    "p007": ("复古网球鞋，白绿配色，软皮鞋面，日常通勤舒适之选。", "#e8f5e9", "Club C 85"),
    "p008": ("高帮篮球鞋，黑红经典配色，真皮鞋面，潮流标志性单品。", "#b71c1c", "AJ1 黑红"),
}

line_re = re.compile(
    r"^(INSERT INTO `product` \([^)]*\) VALUES \("
    r"'(p00\d)', '(.*?)', '(.*?)', ([\d.]+), (\d+), )"
    r"'(.*?)', '(.*?)', '(.*?)', '(.*?)'\);$"
)


def svg(name, color):
    text_color = "#222" if color in ("#f0f0f0", "#eeeeee", "#e8f5e9", "#9e9e9e") else "#fff"
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 600 600">'
        f'<rect width="600" height="600" fill="{color}"/>'
        f'<text x="300" y="280" font-family="PingFang SC,Microsoft YaHei,sans-serif" '
        f'font-size="48" fill="{text_color}" text-anchor="middle">{name}</text>'
        f'<text x="300" y="340" font-family="PingFang SC,Microsoft YaHei,sans-serif" '
        f'font-size="28" fill="{text_color}" text-anchor="middle" opacity="0.8">青禾商城</text>'
        f'</svg>'
    )


def main():
    with open(INDEX, encoding="utf-8") as f:
        lines = f.read().split("\n")

    changed = 0
    for i, line in enumerate(lines):
        m = line_re.match(line)
        if not m:
            continue
        gid = m.group(2)
        if gid not in META:
            continue
        _prefix, gid, name, brand, price, pn, _intro, _imgs, d1, d2 = m.groups()
        new_intro, _color, _short = META[gid]
        new_imgs = ";".join(f"/uploads/products/{gid}-{n}.svg" for n in (1, 2, 3))
        lines[i] = (
            f"INSERT INTO `product` (id, name, brand, price, purchase_num, "
            f"product_intro, product_imgs, gmt_created, gmt_modified) VALUES "
            f"('{gid}', '{name}', '{brand}', {price}, {pn}, '{new_intro}', "
            f"'{new_imgs}', '{d1}', '{d2}');"
        )
        changed += 1

    with open(INDEX, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    os.makedirs(SEED_DIR, exist_ok=True)
    for gid, (_intro, color, short) in META.items():
        for n in (1, 2, 3):
            with open(os.path.join(SEED_DIR, f"{gid}-{n}.svg"), "w", encoding="utf-8") as f:
                f.write(svg(f"{short} {n}", color))

    print(f"index.sql 改写商品行数: {changed}")
    print(f"生成 SVG 占位图: {len(META) * 3} 张 -> {SEED_DIR}")


if __name__ == "__main__":
    main()
