/**
 * 华为手机全息数据驾驶舱 - 数据生成器
 * 生成 sql/init.sql：建库建表 + 陕西为主的业务数据（30天）
 * 运行：node tools/generate_sql.js
 */
const fs = require('fs');
const path = require('path');

/* ================= 基础配置 ================= */
const DAILY_TOTAL_SALES = 12_000_000;   // 全国日销售额 ¥1200万
const DAYS = 30;

/* 省份权重（陕西为主 55%） */
const PROVINCE_W = {
  '陕西': 0.55, '广东': 0.058, '江苏': 0.042, '浙江': 0.040, '山东': 0.030,
  '河南': 0.030, '四川': 0.028, '湖北': 0.026, '湖南': 0.022, '福建': 0.018,
  '上海': 0.022, '北京': 0.022, '安徽': 0.018, '河北': 0.022, '江西': 0.013,
  '辽宁': 0.017, '重庆': 0.017, '广西': 0.013, '云南': 0.012, '山西': 0.017,
  '贵州': 0.010, '黑龙江': 0.010, '吉林': 0.008, '新疆': 0.010, '甘肃': 0.010,
  '内蒙古': 0.013, '海南': 0.004, '天津': 0.010, '宁夏': 0.004, '青海': 0.003, '西藏': 0.002
};

/* 陕西10地市权重（GDP加权，西安领跑） */
const CITY_W = {
  '西安': 0.38, '榆林': 0.13, '咸阳': 0.09, '宝鸡': 0.08, '渭南': 0.07,
  '延安': 0.07, '汉中': 0.06, '安康': 0.05, '商洛': 0.04, '铜川': 0.03
};

/* 华为真实机型（2026在售，官方价） */
const PHONES = [
  { name: 'HUAWEI Mate 80 Pro+', series: 'Mate 80', price: 8999, rating: 4.9, hot: 0.9 },
  { name: 'HUAWEI Mate 80 Pro', series: 'Mate 80', price: 6999, rating: 4.9, hot: 1.0 },
  { name: 'HUAWEI Mate 80', series: 'Mate 80', price: 5499, rating: 4.8, hot: 0.85 },
  { name: 'HUAWEI Pura 80 Pro', series: 'Pura 80', price: 6499, rating: 4.8, hot: 0.8 },
  { name: 'HUAWEI Pura 80', series: 'Pura 80', price: 4999, rating: 4.7, hot: 0.7 },
  { name: 'HUAWEI Mate X7', series: 'Mate X7', price: 12999, rating: 4.9, hot: 0.5 },
  { name: 'HUAWEI Pocket 3', series: 'Pocket 3', price: 7999, rating: 4.8, hot: 0.45 },
  { name: 'HUAWEI nova 14 Pro', series: 'nova 14', price: 3999, rating: 4.7, hot: 0.75 },
  { name: 'HUAWEI nova 14', series: 'nova 14', price: 2999, rating: 4.6, hot: 0.65 },
  { name: 'HUAWEI 畅享 80X', series: '畅享 80', price: 1799, rating: 4.5, hot: 0.4 },
  { name: 'HUAWEI 畅享 80', series: '畅享 80', price: 1499, rating: 4.5, hot: 0.5 },
  { name: 'HUAWEI Mate 70 Pro', series: 'Mate 70', price: 6899, rating: 4.8, hot: 0.55 }
];

/* 流量来源（两级，支撑旭日图） */
const TRAFFIC = [
  { cat: '搜索引擎', subs: [['百度搜索', 14], ['360搜索', 5], ['搜狗搜索', 4], ['必应', 3]] },
  { cat: '社交媒体', subs: [['抖音', 13], ['微信视频号', 8], ['微博', 4], ['小红书', 4], ['B站', 2]] },
  { cat: '电商平台广告', subs: [['京东', 9], ['天猫', 7], ['拼多多', 6]] },
  { cat: '直接访问', subs: [['官网直达', 9], ['APP内访问', 6]] },
  { cat: '线下引流', subs: [['门店扫码', 3.5], ['体验店预约', 2.5]] }
];

/* ================= 工具 ================= */
let seed = 20260903;
function rnd() { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; }
const r = (min, max) => min + rnd() * (max - min);
const ri = (min, max) => Math.floor(r(min, max + 1));
const fmt2 = n => Number(n.toFixed(2));
function dateStr(daysAgo) {
  const d = new Date(); d.setDate(d.getDate() - daysAgo);
  return d.toISOString().slice(0, 10);
}
function dtStr(daysAgo, minutesAgo) {
  const d = new Date(); d.setDate(d.getDate() - daysAgo);
  d.setMinutes(d.getMinutes() - minutesAgo);
  return d.toISOString().slice(0, 19).replace('T', ' ');
}
const esc = s => String(s).replace(/'/g, "\\'");

/* ================= 生成逻辑 ================= */
const out = [];
const W = (line = '') => out.push(line);

W('/* 华为手机全息数据驾驶舱 - 数据库初始化脚本（自动生成） */');
W('/* 生成时间: ' + new Date().toISOString() + ' */');
W('SET NAMES utf8mb4;');
W('SET FOREIGN_KEY_CHECKS = 0;');
W('');
W('CREATE DATABASE IF NOT EXISTS `huawei_cockpit` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;');
W('USE `huawei_cockpit`;');
W('');

/* ---------- 建表 ---------- */
W('/* 1. 手机型号表（爬虫目标表） */');
W(`DROP TABLE IF EXISTS \`phone_model\`;
CREATE TABLE \`phone_model\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`model_name\` VARCHAR(100) NOT NULL COMMENT '机型全名',
  \`series\` VARCHAR(50) DEFAULT NULL COMMENT '系列',
  \`price\` DECIMAL(10,2) DEFAULT NULL COMMENT '官方价格',
  \`rating\` DECIMAL(3,1) DEFAULT NULL COMMENT '评分',
  \`image_url\` VARCHAR(500) DEFAULT NULL,
  \`create_time\` DATETIME DEFAULT CURRENT_TIMESTAMP,
  \`update_time\` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_model\` (\`model_name\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='华为手机型号（vmall爬虫写入）';`);

W('/* 2. 省份销售表 */');
W(`DROP TABLE IF EXISTS \`region_sales\`;
CREATE TABLE \`region_sales\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`province\` VARCHAR(20) NOT NULL,
  \`sales\` DECIMAL(14,2) DEFAULT 0,
  \`orders\` INT DEFAULT 0,
  \`users\` INT DEFAULT 0,
  \`stat_date\` DATE NOT NULL,
  PRIMARY KEY (\`id\`), KEY \`idx_date\` (\`stat_date\`), KEY \`idx_prov\` (\`province\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='省份日销售';`);

W('/* 3. 陕西地市销售表 */');
W(`DROP TABLE IF EXISTS \`city_sales\`;
CREATE TABLE \`city_sales\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`city\` VARCHAR(20) NOT NULL,
  \`sales\` DECIMAL(14,2) DEFAULT 0,
  \`orders\` INT DEFAULT 0,
  \`users\` INT DEFAULT 0,
  \`stat_date\` DATE NOT NULL,
  PRIMARY KEY (\`id\`), KEY \`idx_date\` (\`stat_date\`), KEY \`idx_city\` (\`city\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='陕西地市日销售';`);

W('/* 4. 实时订单表 */');
W(`DROP TABLE IF EXISTS \`realtime_order\`;
CREATE TABLE \`realtime_order\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`order_no\` VARCHAR(32) NOT NULL,
  \`user_name\` VARCHAR(20) DEFAULT NULL,
  \`model_name\` VARCHAR(100) DEFAULT NULL,
  \`amount\` DECIMAL(10,2) DEFAULT 0,
  \`province\` VARCHAR(20) DEFAULT NULL,
  \`city\` VARCHAR(20) DEFAULT NULL,
  \`status\` TINYINT DEFAULT 1 COMMENT '1待付款 2已付款 3已发货 4已完成',
  \`create_time\` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`), KEY \`idx_time\` (\`create_time\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实时订单';`);

W('/* 5. 热销机型表 */');
W(`DROP TABLE IF EXISTS \`hot_product\`;
CREATE TABLE \`hot_product\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`rank_no\` INT DEFAULT 0,
  \`model_name\` VARCHAR(100) DEFAULT NULL,
  \`sales_count\` INT DEFAULT 0 COMMENT '销量(台)',
  \`sales_amount\` DECIMAL(14,2) DEFAULT 0,
  \`growth\` DECIMAL(6,2) DEFAULT 0,
  PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='热销机型排行';`);

W('/* 6. 流量来源表（两级，支撑旭日图） */');
W(`DROP TABLE IF EXISTS \`traffic_source\`;
CREATE TABLE \`traffic_source\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`category\` VARCHAR(50) NOT NULL COMMENT '一级分类',
  \`source_name\` VARCHAR(50) NOT NULL COMMENT '二级来源',
  \`visits\` BIGINT DEFAULT 0,
  \`ratio\` DECIMAL(5,2) DEFAULT 0,
  PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流量来源(两级)';`);

W('/* 7. 用户画像表 */');
W(`DROP TABLE IF EXISTS \`user_profile\`;
CREATE TABLE \`user_profile\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`profile_type\` VARCHAR(10) NOT NULL COMMENT 'gender/age',
  \`profile_name\` VARCHAR(20) NOT NULL,
  \`user_count\` INT DEFAULT 0,
  \`ratio\` DECIMAL(5,2) DEFAULT 0,
  PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户画像';`);

W('/* 8. 销售趋势表 */');
W(`DROP TABLE IF EXISTS \`sales_trend\`;
CREATE TABLE \`sales_trend\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`stat_date\` DATE NOT NULL,
  \`sales\` DECIMAL(14,2) DEFAULT 0,
  \`orders\` INT DEFAULT 0,
  \`visits\` INT DEFAULT 0,
  \`avg_order_value\` DECIMAL(10,2) DEFAULT 0,
  PRIMARY KEY (\`id\`), UNIQUE KEY \`uk_date\` (\`stat_date\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='30日销售趋势';`);

W('/* 9. 总览统计表 */');
W(`DROP TABLE IF EXISTS \`overview_stats\`;
CREATE TABLE \`overview_stats\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`total_sales\` DECIMAL(14,2) DEFAULT 0,
  \`total_orders\` INT DEFAULT 0,
  \`total_users\` INT DEFAULT 0,
  \`today_sales\` DECIMAL(14,2) DEFAULT 0,
  \`today_orders\` INT DEFAULT 0,
  \`today_new_users\` INT DEFAULT 0,
  \`today_visits\` INT DEFAULT 0,
  \`sales_growth\` DECIMAL(6,2) DEFAULT 0,
  \`orders_growth\` DECIMAL(6,2) DEFAULT 0,
  \`users_growth\` DECIMAL(6,2) DEFAULT 0,
  \`visits_growth\` DECIMAL(6,2) DEFAULT 0,
  \`conversion_rate\` DECIMAL(5,2) DEFAULT 0,
  \`update_time\` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='总览统计';`);

W('/* 10. 系统用户表 */');
W(`DROP TABLE IF EXISTS \`sys_user\`;
CREATE TABLE \`sys_user\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`username\` VARCHAR(50) NOT NULL,
  \`password\` VARCHAR(64) NOT NULL COMMENT 'MD5',
  \`role\` VARCHAR(20) NOT NULL COMMENT 'ADMIN/MERCHANT',
  \`merchant_name\` VARCHAR(50) DEFAULT NULL,
  \`create_time\` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`), UNIQUE KEY \`uk_username\` (\`username\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户';`);

W('/* 11. AI对话日志表 */');
W(`DROP TABLE IF EXISTS \`ai_chat_log\`;
CREATE TABLE \`ai_chat_log\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`user_id\` BIGINT DEFAULT NULL,
  \`role\` VARCHAR(10) NOT NULL COMMENT 'user/assistant',
  \`content\` TEXT,
  \`create_time\` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话日志';`);

W('/* 12. 预警记录表 */');
W(`DROP TABLE IF EXISTS \`alert_record\`;
CREATE TABLE \`alert_record\` (
  \`id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`alert_type\` VARCHAR(20) DEFAULT NULL,
  \`title\` VARCHAR(100) DEFAULT NULL,
  \`content\` TEXT,
  \`ai_advice\` TEXT,
  \`level\` TINYINT DEFAULT 1 COMMENT '1提示 2警告 3严重',
  \`create_time\` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能预警记录';`);
W('');

/* ---------- phone_model ---------- */
W('/* ---- 手机型号 ---- */');
W('INSERT INTO `phone_model` (`model_name`, `series`, `price`, `rating`) VALUES');
W(PHONES.map(p =>
  `('${esc(p.name)}', '${esc(p.series)}', ${p.price}, ${p.rating})`
).join(',\n') + ';');
W('');

/* ---------- 30天销售趋势（带增长+周末效应） ---------- */
const trendRows = [];
for (let d = DAYS - 1; d >= 0; d--) {
  const date = dateStr(d);
  const dow = new Date(date).getDay();
  const weekend = (dow === 0 || dow === 6) ? 1.18 : 1;
  const growthCurve = 1 + (DAYS - 1 - d) * 0.006;          // 缓慢增长
  const noise = r(0.92, 1.08);
  const factor = weekend * growthCurve * noise;
  const sales = DAILY_TOTAL_SALES * factor;
  const orders = Math.round(5800 * factor * r(0.95, 1.05));
  const visits = Math.round(185000 * factor * r(0.95, 1.05));
  trendRows.push({ date, sales: fmt2(sales), orders, visits, aov: fmt2(sales / orders) });
}
W('/* ---- 销售趋势(30日) ---- */');
W('INSERT INTO `sales_trend` (`stat_date`, `sales`, `orders`, `visits`, `avg_order_value`) VALUES');
W(trendRows.map(t => `('${t.date}', ${t.sales}, ${t.orders}, ${t.visits}, ${t.aov})`).join(',\n') + ';');
W('');

/* ---------- 省份销售（31省×30天，陕西55%） ---------- */
W('/* ---- 省份销售 ---- */');
const provRows = [];
for (let d = DAYS - 1; d >= 0; d--) {
  const t = trendRows[DAYS - 1 - d];
  for (const [prov, w] of Object.entries(PROVINCE_W)) {
    const sales = t.sales * w * r(0.93, 1.07);
    const orders = Math.round(t.orders * w * r(0.93, 1.07));
    const users = Math.round(2100 * w * r(0.9, 1.1));
    provRows.push(`('${esc(prov)}', ${fmt2(sales)}, ${orders}, ${users}, '${t.date}')`);
  }
}
W('INSERT INTO `region_sales` (`province`, `sales`, `orders`, `users`, `stat_date`) VALUES');
// 分批 INSERT（每批 ~200 行）
for (let i = 0; i < provRows.length; i += 200) {
  W(provRows.slice(i, i + 200).join(',\n') + (i + 200 < provRows.length ? ',' : ';'));
}
W('');

/* ---------- 陕西地市销售（10市×30天） ---------- */
W('/* ---- 陕西地市销售 ---- */');
const cityRows = [];
for (let d = DAYS - 1; d >= 0; d--) {
  const t = trendRows[DAYS - 1 - d];
  const sxDaily = t.sales * PROVINCE_W['陕西'];
  for (const [city, w] of Object.entries(CITY_W)) {
    const sales = sxDaily * w * r(0.92, 1.08);
    const orders = Math.round(t.orders * PROVINCE_W['陕西'] * w * r(0.92, 1.08));
    const users = Math.round(2100 * PROVINCE_W['陕西'] * w * r(0.9, 1.1));
    cityRows.push(`('${esc(city)}', ${fmt2(sales)}, ${orders}, ${users}, '${t.date}')`);
  }
}
W('INSERT INTO `city_sales` (`city`, `sales`, `orders`, `users`, `stat_date`) VALUES');
for (let i = 0; i < cityRows.length; i += 200) {
  W(cityRows.slice(i, i + 200).join(',\n') + (i + 200 < cityRows.length ? ',' : ';'));
}
W('');

/* ---------- 实时订单（20条，陕西为主） ---------- */
W('/* ---- 实时订单 ---- */');
const surnames = ['张', '李', '王', '赵', '陈', '刘', '杨', '黄', '周', '吴', '郑', '孙', '马', '朱', '胡', '郭', '何', '高', '林', '罗'];
const cities = Object.keys(CITY_W);
const cityPool = [];
cities.forEach(c => { const n = Math.max(1, Math.round(CITY_W[c] * 20)); for (let i = 0; i < n; i++) cityPool.push(c); });
const orderRows = [];
for (let i = 0; i < 20; i++) {
  const phone = PHONES[Math.floor(r(0, PHONES.length))];
  const city = cityPool[Math.floor(r(0, cityPool.length))];
  orderRows.push({
    no: 'HW' + Date.now().toString().slice(-8) + String(i).padStart(2, '0'),
    user: surnames[ri(0, surnames.length - 1)] + '**',
    model: phone.name,
    amount: fmt2(phone.price * r(0.98, 1.02)),
    city,
    status: ri(1, 4),
    mins: i * 4 + ri(0, 3)
  });
}
W('INSERT INTO `realtime_order` (`order_no`, `user_name`, `model_name`, `amount`, `province`, `city`, `status`, `create_time`) VALUES');
W(orderRows.map(o =>
  `('${o.no}', '${o.user}', '${esc(o.model)}', ${o.amount}, '陕西', '${o.city}', ${o.status}, '${dtStr(0, o.mins)}')`
).join(',\n') + ';');
W('');

/* ---------- 热销机型（按 hot 权重算30日销量） ---------- */
W('/* ---- 热销机型TOP ---- */');
const totalOrders30 = trendRows.reduce((s, t) => s + t.orders, 0);
const hotList = PHONES.map(p => {
  const count = Math.round(totalOrders30 * p.hot / PHONES.reduce((s, x) => s + x.hot, 0) * r(0.9, 1.1));
  return { name: p.name, count, amount: fmt2(count * p.price * r(0.97, 1.0)), growth: fmt2(r(-5, 38)) };
}).sort((a, b) => b.amount - a.amount);
W('INSERT INTO `hot_product` (`rank_no`, `model_name`, `sales_count`, `sales_amount`, `growth`) VALUES');
W(hotList.map((h, i) => `(${i + 1}, '${esc(h.name)}', ${h.count}, ${h.amount}, ${h.growth})`).join(',\n') + ';');
W('');

/* ---------- 流量来源（两级） ---------- */
W('/* ---- 流量来源(两级) ---- */');
const todayVisits = trendRows[trendRows.length - 1].visits;
const trafficRows = [];
TRAFFIC.forEach(({ cat, subs }) => {
  subs.forEach(([name, ratio]) => {
    trafficRows.push(`('${esc(cat)}', '${esc(name)}', ${Math.round(todayVisits * ratio / 100)}, ${ratio})`);
  });
});
W('INSERT INTO `traffic_source` (`category`, `source_name`, `visits`, `ratio`) VALUES');
W(trafficRows.join(',\n') + ';');
W('');

/* ---------- 用户画像 ---------- */
W('/* ---- 用户画像 ---- */');
const totalUsers = 1_680_000;
W('INSERT INTO `user_profile` (`profile_type`, `profile_name`, `user_count`, `ratio`) VALUES');
W([
  `('gender', '男性', ${Math.round(totalUsers * 0.58)}, 58.00)`,
  `('gender', '女性', ${Math.round(totalUsers * 0.42)}, 42.00)`,
  `('age', '18岁以下', ${Math.round(totalUsers * 0.03)}, 3.00)`,
  `('age', '18-24岁', ${Math.round(totalUsers * 0.15)}, 15.00)`,
  `('age', '25-34岁', ${Math.round(totalUsers * 0.38)}, 38.00)`,
  `('age', '35-44岁', ${Math.round(totalUsers * 0.26)}, 26.00)`,
  `('age', '45-54岁', ${Math.round(totalUsers * 0.12)}, 12.00)`,
  `('age', '55岁以上', ${Math.round(totalUsers * 0.06)}, 6.00)`
].join(',\n') + ';');
W('');

/* ---------- 总览统计 ---------- */
W('/* ---- 总览统计 ---- */');
const today = trendRows[trendRows.length - 1];
const yesterday = trendRows[trendRows.length - 2];
const totalSales30 = trendRows.reduce((s, t) => s + t.sales, 0);
const totalOrdersAll = trendRows.reduce((s, t) => s + t.orders, 0);
W(`INSERT INTO \`overview_stats\` (\`total_sales\`, \`total_orders\`, \`total_users\`, \`today_sales\`, \`today_orders\`, \`today_new_users\`, \`today_visits\`, \`sales_growth\`, \`orders_growth\`, \`users_growth\`, \`visits_growth\`, \`conversion_rate\`) VALUES
(${fmt2(totalSales30 * 42)}, ${totalOrdersAll * 42}, ${totalUsers}, ${today.sales}, ${today.orders}, 2145, ${today.visits}, ${fmt2((today.sales / yesterday.sales - 1) * 100)}, ${fmt2((today.orders / yesterday.orders - 1) * 100)}, 16.83, ${fmt2((today.visits / yesterday.visits - 1) * 100)}, ${fmt2(today.orders / today.visits * 100)});`);
W('');

/* ---------- 系统用户（MD5） ---------- */
W('/* ---- 系统用户：admin/admin123, merchant/merchant123 ---- */');
const md5 = s => require('crypto').createHash('md5').update(s).digest('hex');
W(`INSERT INTO \`sys_user\` (\`username\`, \`password\`, \`role\`, \`merchant_name\`) VALUES
('admin', '${md5('admin123')}', 'ADMIN', NULL),
('merchant', '${md5('merchant123')}', 'MERCHANT', '华为西安旗舰店');`);
W('');
W('SET FOREIGN_KEY_CHECKS = 1;');

/* ================= 输出 ================= */
const outPath = path.join(__dirname, '..', 'sql', 'init.sql');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, out.join('\n'), 'utf8');
console.log('生成完成: ' + outPath);
console.log(`行数: ${out.length}, 省份行: ${provRows.length}, 地市行: ${cityRows.length}`);
