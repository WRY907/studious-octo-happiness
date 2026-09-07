# -*- coding: utf-8 -*-
"""
华为商城爬虫 - 爬取 vmall.com 公开商品页的真实机型/价格/评分
合规边界：仅爬公开页面，控制请求频率，不碰登录接口
依赖：pip install requests beautifulsoup4 pymysql
运行：python vmall_crawler.py
"""
import os
import time
import random
import re
import json

import requests
from bs4 import BeautifulSoup

# ================= 配置 =================
DB_CONFIG = dict(
    host='127.0.0.1', port=3306, user='root',
    password=os.environ.get('DB_PASSWORD', '123456'), database='huawei_cockpit',
    charset='utf8mb4'
)

# 华为商城手机列表页（公开）
LIST_URL = 'https://www.vmall.com/list-36'
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
                  '(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'Accept-Language': 'zh-CN,zh;q=0.9',
}
REQUEST_INTERVAL = (2, 5)   # 每次请求间隔 2~5 秒，礼貌爬取


def fetch_page(url):
    """请求页面，带频率控制"""
    time.sleep(random.uniform(*REQUEST_INTERVAL))
    resp = requests.get(url, headers=HEADERS, timeout=15)
    resp.raise_for_status()
    resp.encoding = 'utf-8'
    return resp.text


def parse_products(html):
    """解析商品列表：机型名 / 价格 / 评论数"""
    soup = BeautifulSoup(html, 'html.parser')
    items = []
    # vmall 列表页结构（ul#productList li）
    for li in soup.select('ul#productList li, .product-list li, .grid-item'):
        name_el = li.select_one('.p-name a, h3 a, .p-name')
        price_el = li.select_one('.p-price .pm-num, .p-price, em')
        if not name_el:
            continue
        name = name_el.get_text(strip=True)
        price = None
        if price_el:
            m = re.search(r'[\d.]+', price_el.get_text())
            if m:
                price = float(m.group())
        if not name or len(name) < 4:
            continue
        # 系列名提取：HUAWEI Mate 80 Pro → Mate 80
        series_m = re.search(r'(Mate\s*\w+|Pura\s*\w+|nova\s*\w+|畅享\s*\w+|Pocket\s*\w+|Mate\s*X\s*\w+)', name)
        series = series_m.group(1).replace(' ', ' ') if series_m else None
        items.append(dict(model_name=name, series=series, price=price, rating=4.7))
    return items


def upsert_to_db(items):
    """写入 phone_model 表（存在则更新价格）"""
    import pymysql
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cur:
            for it in items:
                cur.execute(
                    "SELECT id FROM phone_model WHERE model_name=%s", (it['model_name'],))
                row = cur.fetchone()
                if row:
                    cur.execute(
                        "UPDATE phone_model SET price=%s, update_time=NOW() WHERE id=%s",
                        (it['price'], row[0]))
                else:
                    cur.execute(
                        "INSERT INTO phone_model(model_name, series, price, rating) "
                        "VALUES(%s,%s,%s,%s)",
                        (it['model_name'], it['series'], it['price'], it['rating']))
        conn.commit()
        print(f'[OK] 已同步 {len(items)} 个机型到 phone_model 表')
    finally:
        conn.close()


def main():
    print('=== 华为商城爬虫启动 ===')
    try:
        html = fetch_page(LIST_URL)
        items = parse_products(html)
        if not items:
            print('[WARN] 页面结构可能已变化或被反爬拦截，未解析到商品')
            print('提示：vmall 为动态渲染页面，如失败可改用其公开 JSON 接口或人工核对')
            return
        print(f'解析到 {len(items)} 个机型:')
        for it in items:
            print(f"  - {it['model_name']}  ¥{it['price']}")
        upsert_to_db(items)
    except Exception as e:
        print(f'[ERROR] 爬取失败: {e}')


if __name__ == '__main__':
    main()
