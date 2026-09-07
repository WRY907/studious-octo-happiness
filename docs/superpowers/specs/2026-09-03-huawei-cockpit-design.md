# 华为手机全息数据驾驶舱 · 设计文档

日期：2026-09-03
状态：已批准

## 1. 项目定位
华为品牌手机业务数据可视化大屏 + 两层权限管理后台 + AI 智能分析（火山方舟·豆包）。业务数据以陕西省为主。

## 2. 数据层
- MySQL 库：`huawei_cockpit`
- 爬虫（Python）：vmall.com 公开页爬真实机型/价格 → `phone_model` 表
- 生成脚本：陕西为主（55%）省份权重、陕西10地市 GDP 权重（西安38%领跑）、业务数据按天生成30天
- 表：phone_model, region_sales, city_sales, realtime_order, hot_product, traffic_source(两级支撑旭日图), user_profile, sales_trend, overview_stats, sys_user, ai_chat_log, alert_record

## 3. 大屏布局
- 顶部：标题栏 + 时钟 + 音频控制 + 管理后台入口 + AI按钮
- 左列：4指标卡（今日订单数/新增用户/访问量/转化率）+ 销售趋势30日折线
- 中央：销售额超大主视觉（翻牌数字+环比+光晕）+ 双地图切换（默认陕西地市热力，可切全国）+ 涟漪/飞线
- 右列：热销机型TOP10 + 流量来源旭日图 + 用户画像
- 底部：实时订单横向跑马灯
- 删除3D地球板块

## 4. 动效（全套拉满）
粒子星空背景、网格涟漪、矩阵雨（低透明度边角）、全息扫描线、主标题故障字、鼠标跟随粒子拖尾、面板霓虹边框流光、玻璃拟态、数字翻牌、图表过渡动画。
约束：只动 transform/opacity；prefers-reduced-motion 降级；数据层永远最上层清晰可读。

## 5. 音频（Web Audio 程序化合成）
- 背景音乐：科技氛围电子（Pad+琶音）
- 按键音/交互反馈音：短促提示音
- Ducking：交互音触发时背景音压低60%后渐恢复
- 首次交互后启动；音量滑杆+静音

## 6. AI（火山方舟，模型名直调 doubao-seed-1.6-flash）
- 重点1：AI对话助手——注入实时大屏数据上下文，流式打字机输出
- 重点2：自然语言查询——LLM生成SQL → 白名单校验（仅SELECT、仅允许表）→ 执行 → 返回结果
- 简化1：智能预警——规则引擎（环比跌>25%）→ 弹窗+警示音 → AI生成解读
- 简化2：分析报告——模板+AI填充 → 下载HTML

## 7. 管理后台（两层权限）
- JWT 登录：sys_user（admin / merchant）
- 管理员👑：全量数据、增删改、成本/毛利、完整手机号、Excel导入
- 商家🏪：脱敏（总额/成本隐藏）、型号只读、仅本店订单、手机号打码、禁删除、禁导入
- 页面：登录、手机型号、订单、用户管理 + 角色路由守卫

## 7.5 Excel 数据导入（2026-09-04 新增）
- 后端：EasyExcel 3.3.4 解析，ImportController（JWT + 仅 ADMIN）
- 接口：POST /admin/import/{table}（上传导入）、GET /admin/import/{table}/template（模板下载 xlsx）、GET /admin/import/meta（8表元信息）
- 支持表：phone(手机型号)/region(省份销售)/city(城市销售)/order(实时订单)/hot(热销机型)/traffic(流量来源)/profile(用户画像)/trend(销售趋势)
- 逐行校验（必填/数字/日期/枚举），错误行跳过并记录行号+原因，返回 {total, success, fail, errors}
- 中文表头映射；phone 机型名重复则更新价格；trend 日期重复则更新
- 前端：DataImport.vue 页面（8表tab切换、列定义展示、模板下载、拖拽上传、结果统计+错误明细）

## 8. 技术栈
Spring Boot 2.7 + MyBatis-Plus + JWT / Vue3 + ECharts + Vite / Python 爬虫 / 火山方舟 OpenAI 兼容接口
