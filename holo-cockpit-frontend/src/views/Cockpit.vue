<template>
  <div class="cockpit-container">
    <!-- ===== 背景层（数据层最上，背景均在 z-index 0/1） ===== -->
    <ParticleBackground />
    <MatrixRain />
    <ScanOverlay />
    <div class="bg-effects">
      <div class="grid-bg"></div>
      <div class="radial-bg"></div>
      <div class="vignette"></div>
    </div>

    <!-- 顶部标题栏 -->
    <HeaderBar @open-ai="aiOpen = true" />

    <!-- ===== 主体：三列布局 ===== -->
    <div class="main-content">
      <!-- 左列：4 统计卡（2×2）+ 30日销售趋势 -->
      <div class="col col-left">
        <div class="stat-grid" style="--i: 1">
          <StatCard
            v-for="card in statCards"
            :key="card.title"
            :title="card.title"
            :en="card.en"
            :value="card.value"
            :growth="card.growth"
            :suffix="card.suffix"
            :icon="card.icon"
            :color="card.color"
            :trend="card.trend"
            :tag="card.tag"
          />
        </div>

        <div class="panel panel-trend" style="--i: 2">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">01</span>
            销售趋势分析
            <span class="title-en">30-Day Trend</span>
            <router-link to="/trend" class="trend-detail-link">详情分析 →</router-link>
          </div>
          <SalesTrendChart :data="salesTrendData" />
        </div>
      </div>

      <!-- 中央：今日销售额主视觉 + 双模式地图 -->
      <div class="col col-center">
        <div class="hero-wrap" style="--i: 3">
          <SalesHero :sales="todaySales" :growth="todaySalesGrowth" />
        </div>

        <div class="panel panel-map" style="--i: 4">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">02</span>
            区域销售热力
            <span class="title-en">Regional Sales Matrix</span>
          </div>
          <DualMapChart :region-data="regionData" :city-data="cityData" :masked="maskedMode" />
        </div>
      </div>

      <!-- 右列：热销机型 / 流量来源 / 用户画像 -->
      <div class="col col-right">
        <div class="panel panel-hot" style="--i: 5">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">03</span>
            热销机型 TOP10
            <span class="title-en">Hot Models</span>
          </div>
          <HotProductRank :data="hotProducts" />
        </div>

        <div class="panel panel-sun" style="--i: 6">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">04</span>
            流量来源分析
            <span class="title-en">Traffic Sources</span>
          </div>
          <TrafficBarChart :data="trafficSources" />
        </div>

        <div class="panel panel-profile" style="--i: 7">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">05</span>
            用户画像分析
            <span class="title-en">User Profile</span>
          </div>
          <UserProfileChart :data="userProfiles" />
        </div>
      </div>
    </div>

    <!-- 底部：实时订单跑马灯 -->
    <div class="marquee-enter" style="--i: 8">
      <OrderMarquee :orders="realtimeOrders" />
    </div>

    <!-- AI 对话 + 智能预警（fixed 浮层） -->
    <AlertToast />
    <AIChat :open="aiOpen" @close="aiOpen = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { cockpitApi } from '@/api'
import { audioManager } from '@/audio/manager'

// 组件
import HeaderBar from '@/components/HeaderBar.vue'
import StatCard from '@/components/StatCard.vue'
import SalesHero from '@/components/SalesHero.vue'
import SalesTrendChart from '@/components/SalesTrendChart.vue'
import DualMapChart from '@/components/DualMapChart.vue'
import HotProductRank from '@/components/HotProductRank.vue'
import TrafficBarChart from '@/components/TrafficBarChart.vue'
import UserProfileChart from '@/components/UserProfileChart.vue'
import OrderMarquee from '@/components/OrderMarquee.vue'
import AIChat from '@/components/AIChat.vue'
import AlertToast from '@/components/AlertToast.vue'
import ParticleBackground from '@/components/ParticleBackground.vue'
import MatrixRain from '@/components/MatrixRain.vue'
import ScanOverlay from '@/components/ScanOverlay.vue'

/* ============================================================
   Mock 数据（后端不可用时降级）：陕西 55% 权重、西安 38% 领跑
   ============================================================ */
const mockOverview = {
  todaySales: 12586310,
  todayOrders: 3256,
  todayNewUsers: 1256,
  todayVisits: 128650,
  salesGrowth: 12.36,
  ordersGrowth: 8.42,
  usersGrowth: 15.68,
  visitsGrowth: 22.34,
  conversionRate: 3.26,
  totalSales: 128654320.5,
  totalUsers: 523456
}

/* 30 日销售趋势 */
function generateMockTrend() {
  const arr = []
  const today = new Date()
  for (let i = 29; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const wave = Math.sin((i / 30) * Math.PI * 3) * 1800000 // 周期波动
    const base = 9200000 + wave + Math.random() * 1600000
    const sales = Math.round(base / 100) * 100
    arr.push({
      statDate: d.toISOString().slice(0, 10),
      sales,
      orders: Math.round(sales / 3600),
      visits: Math.round(sales / 85),
      avgOrderValue: Math.round((sales / (sales / 3600)) * 100) / 100
    })
  }
  return arr
}

/* 全国 31 省销售（陕西占 55%） */
const PROVINCE_WEIGHTS = [
  ['陕西省', 55], ['广东省', 5.2], ['江苏省', 3.6], ['浙江省', 3.2], ['四川省', 3.0],
  ['山东省', 2.6], ['河南省', 2.4], ['湖北省', 2.0], ['湖南省', 1.8], ['福建省', 1.6],
  ['上海市', 1.5], ['北京市', 1.4], ['安徽省', 1.3], ['河北省', 1.2], ['江西省', 1.0],
  ['重庆市', 1.0], ['辽宁省', 0.9], ['广西壮族自治区', 0.8], ['云南省', 0.7], ['山西省', 0.7],
  ['贵州省', 0.6], ['黑龙江省', 0.5], ['吉林省', 0.5], ['天津市', 0.5], ['内蒙古自治区', 0.4],
  ['新疆维吾尔自治区', 0.4], ['甘肃省', 0.35], ['海南省', 0.3], ['宁夏回族自治区', 0.2],
  ['青海省', 0.15], ['西藏自治区', 0.1]
]
function generateMockRegion() {
  const total = 96540000
  const sum = PROVINCE_WEIGHTS.reduce((s, [, w]) => s + w, 0)
  return PROVINCE_WEIGHTS.map(([province, w]) => {
    const sales = Math.round((total * w / sum) * (0.92 + Math.random() * 0.16))
    return {
      province,
      sales,
      orders: Math.round(sales / 3800),
      users: Math.round(sales / 150)
    }
  })
}

/* 陕西 10 地市销售（西安 38% 领跑，名称与地图对齐） */
const CITY_WEIGHTS = [
  ['西安市', 38], ['咸阳市', 9], ['榆林市', 8.5], ['宝鸡市', 8], ['渭南市', 7.5],
  ['汉中市', 6.5], ['延安市', 6], ['安康市', 5.5], ['商洛市', 5], ['铜川市', 4]
]
function generateMockCity() {
  // 陕西总销售额 ≈ 全国 55%
  const total = 96540000 * 0.55
  const sum = CITY_WEIGHTS.reduce((s, [, w]) => s + w, 0)
  return CITY_WEIGHTS.map(([city, w]) => {
    const sales = Math.round((total * w / sum) * (0.9 + Math.random() * 0.2))
    return {
      city,
      sales,
      orders: Math.round(sales / 4200),
      users: Math.round(sales / 165)
    }
  })
}

/* 热销机型 TOP10（华为机型） */
const HOT_MODELS = [
  ['HUAWEI Mate 60 Pro+', 48260000, 6380, 28.6],
  ['HUAWEI Mate X5 典藏版', 42180000, 3820, 22.4],
  ['HUAWEI P60 Pro', 33520000, 7150, 15.8],
  ['HUAWEI nova 12 Ultra', 28760000, 9840, 18.2],
  ['HUAWEI Mate 60', 25140000, 5210, 12.6],
  ['HUAWEI nova 12', 21830000, 11420, 9.4],
  ['HUAWEI 畅享 70', 15680000, 18360, 6.8],
  ['HUAWEI WATCH GT 4', 12850000, 21530, 14.2],
  ['HUAWEI FreeBuds Pro 3', 9860000, 18240, -2.6],
  ['HUAWEI MatePad Pro 13.2', 8920000, 6480, 8.6]
]
const mockHotProducts = HOT_MODELS.map(([modelName, salesAmount, salesCount, growth], i) => ({
  rankNo: i + 1,
  modelName,
  salesAmount,
  salesCount,
  growth
}))

/* 流量来源（两级：category → sourceName） */
const mockTrafficSources = [
  { category: '搜索引擎', sourceName: '百度搜索', visits: 2865000, ratio: 22.4 },
  { category: '搜索引擎', sourceName: '360搜索', visits: 940000, ratio: 7.3 },
  { category: '搜索引擎', sourceName: '搜狗搜索', visits: 520000, ratio: 4.1 },
  { category: '社交媒体', sourceName: '微信小程序', visits: 2580000, ratio: 20.2 },
  { category: '社交媒体', sourceName: '抖音', visits: 1230000, ratio: 9.6 },
  { category: '社交媒体', sourceName: '微博', visits: 890000, ratio: 7.0 },
  { category: '广告投放', sourceName: '抖音信息流', visits: 760000, ratio: 5.9 },
  { category: '广告投放', sourceName: '浏览器开屏', visits: 430000, ratio: 3.4 },
  { category: '电商渠道', sourceName: '天猫旗舰店', visits: 1080000, ratio: 8.5 },
  { category: '电商渠道', sourceName: '京东自营', visits: 820000, ratio: 6.4 },
  { category: '电商渠道', sourceName: '华为商城', visits: 650000, ratio: 5.1 },
  { category: '直接访问', sourceName: '直接访问', visits: 1050000, ratio: 8.2 }
]

/* 用户画像（扁平数组，profileType 分组） */
const mockUserProfiles = [
  { profileType: '性别', profileName: '男性', userCount: 305000, ratio: 58.2 },
  { profileType: '性别', profileName: '女性', userCount: 219000, ratio: 41.8 },
  { profileType: '年龄', profileName: '18岁以下', userCount: 26000, ratio: 5.0 },
  { profileType: '年龄', profileName: '18-24岁', userCount: 84000, ratio: 16.0 },
  { profileType: '年龄', profileName: '25-34岁', userCount: 183000, ratio: 35.0 },
  { profileType: '年龄', profileName: '35-44岁', userCount: 131000, ratio: 25.0 },
  { profileType: '年龄', profileName: '45-54岁', userCount: 68000, ratio: 13.0 },
  { profileType: '年龄', profileName: '55岁以上', userCount: 31000, ratio: 6.0 }
]

/* 实时订单 */
const ORDER_MODELS = ['Mate 60 Pro+', 'Mate X5', 'P60 Pro', 'nova 12 Ultra', 'Mate 60', 'nova 12', '畅享 70', 'WATCH GT 4', 'FreeBuds Pro 3', 'MatePad Pro 13.2']
const ORDER_NAMES = ['张**', '李**', '王**', '赵**', '陈**', '刘**', '杨**', '黄**', '周**', '吴**']
const ORDER_CITIES = ['西安市', '宝鸡市', '咸阳市', '渭南市', '汉中市', '榆林市', '延安市', '安康市', '商洛市', '铜川市']
function generateMockOrders() {
  const arr = []
  for (let i = 0; i < 12; i++) {
    const d = new Date()
    d.setMinutes(d.getMinutes() - i * 3 - Math.floor(Math.random() * 3))
    arr.push({
      orderNo: 'HW' + Date.now().toString().slice(-8) + String(i).padStart(2, '0'),
      userName: ORDER_NAMES[Math.floor(Math.random() * ORDER_NAMES.length)],
      modelName: ORDER_MODELS[Math.floor(Math.random() * ORDER_MODELS.length)],
      amount: (Math.random() * 9500 + 399).toFixed(2),
      province: '陕西省',
      city: ORDER_CITIES[Math.floor(Math.random() * ORDER_CITIES.length)],
      status: Math.floor(Math.random() * 4) + 1,
      createTime: d.toISOString()
    })
  }
  return arr
}

/* ============================================================
   响应式状态
   ============================================================ */
const aiOpen = ref(false)
const maskedMode = ref(false) // 商家角色脱敏模式
const overview = ref({})
const salesTrendData = ref([])
const regionData = ref([])
const cityData = ref([])
const hotProducts = ref([])
const trafficSources = ref([])
const userProfiles = ref([])
const realtimeOrders = ref([])

/* 主视觉（脱敏模式下销售额为 null） */
const todaySales = computed(() => {
  if (maskedMode.value) return null
  return Number(overview.value.todaySales ?? mockOverview.todaySales)
})
const todaySalesGrowth = computed(() =>
  overview.value.salesGrowth !== undefined ? overview.value.salesGrowth : mockOverview.salesGrowth
)

/* 趋势迷你图 */
const trendOrders = computed(() => salesTrendData.value.map(d => d.orders))
const trendVisits = computed(() => salesTrendData.value.map(d => d.visits))

/* 4 张统计卡 */
const statCards = computed(() => {
  const ov = overview.value
  const pick = (key, digits = 0) => {
    const v = ov[key] !== undefined ? ov[key] : mockOverview[key]
    return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
  }
  return [
    {
      title: '今日订单数', en: 'TODAY ORDERS',
      value: pick('todayOrders'), growth: num(ov.ordersGrowth, mockOverview.ordersGrowth),
      suffix: '单', icon: 'orders', color: '#00ffa3', trend: trendOrders.value
    },
    {
      title: '新增用户', en: 'NEW USERS',
      value: pick('todayNewUsers'), growth: num(ov.usersGrowth, mockOverview.usersGrowth),
      suffix: '人', icon: 'users', color: '#ffcc00'
    },
    {
      title: '今日访问量', en: 'TODAY VISITS',
      value: pick('todayVisits'), growth: num(ov.visitsGrowth, mockOverview.visitsGrowth),
      suffix: '次', icon: 'visits', color: '#ff4d6a', trend: trendVisits.value
    },
    {
      title: '转化率', en: 'CONVERSION',
      value: pick('conversionRate', 2), growth: null,
      suffix: '%', icon: 'target', color: '#7b68ee', tag: 'REALTIME'
    }
  ]
})
function num(v, fallback) {
  return v !== undefined && v !== null ? Number(v) : fallback
}

/* ============================================================
   数据加载：30s 轮询，失败 mock 降级
   ============================================================ */
function fillOrKeep(refVal, incoming, mockFn) {
  if (Array.isArray(incoming) && incoming.length) {
    refVal.value = incoming
  } else if (!refVal.value.length) {
    refVal.value = mockFn() // 惰性生成 mock
  }
}

async function loadData() {
  try {
    const data = await cockpitApi.getAll()
    if (data) {
      maskedMode.value = data.masked === true // 商家脱敏模式
      overview.value = data.overview || mockOverview
      fillOrKeep(salesTrendData, data.salesTrend, generateMockTrend)
      fillOrKeep(regionData, data.regionSales, generateMockRegion)
      fillOrKeep(cityData, data.citySales, generateMockCity)
      fillOrKeep(hotProducts, data.hotProducts, () => mockHotProducts)
      fillOrKeep(trafficSources, data.trafficSources, () => mockTrafficSources)
      fillOrKeep(userProfiles, data.userProfiles, () => mockUserProfiles)
      fillOrKeep(realtimeOrders, data.realtimeOrders, generateMockOrders)
      audioManager.feedback() // 刷新成功提示音
    } else {
      useMock()
    }
  } catch (e) {
    console.warn('后端接口不可用，使用模拟数据:', e.message)
    useMock()
  }
}

function useMock() {
  overview.value = mockOverview
  if (!salesTrendData.value.length) salesTrendData.value = generateMockTrend()
  if (!regionData.value.length) regionData.value = generateMockRegion()
  if (!cityData.value.length) cityData.value = generateMockCity()
  if (!hotProducts.value.length) hotProducts.value = mockHotProducts
  if (!trafficSources.value.length) trafficSources.value = mockTrafficSources
  if (!userProfiles.value.length) userProfiles.value = mockUserProfiles
  if (!realtimeOrders.value.length) realtimeOrders.value = generateMockOrders()
}

/* ============================================================
   音频 + 交互音效（所有按钮点击 → click 音）
   ============================================================ */
function onGlobalClick(e) {
  if (e.target.closest('button, a')) audioManager.click()
}

/* ============================================================
   鼠标跟随粒子拖尾（节流 30ms，动画结束移除）
   ============================================================ */
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
let lastTrailTime = 0
function onMouseMove(e) {
  const now = performance.now()
  if (now - lastTrailTime < 30) return
  lastTrailTime = now
  const dot = document.createElement('div')
  dot.className = 'mouse-trail'
  dot.style.left = e.clientX + 'px'
  dot.style.top = e.clientY + 'px'
  document.body.appendChild(dot)
  const remove = () => dot.remove()
  dot.addEventListener('animationend', remove)
  setTimeout(remove, 800) // 兜底
}

let pollTimer = null

onMounted(() => {
  // 音频初始化（首次点击激活 AudioContext + BGM）
  audioManager.init()
  // 按钮点击音（事件委托）
  document.addEventListener('click', onGlobalClick, true)
  // 鼠标拖尾
  if (!reducedMotion) document.addEventListener('mousemove', onMouseMove)
  // 数据加载 + 30s 轮询
  loadData()
  pollTimer = setInterval(loadData, 30000)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onGlobalClick, true)
  document.removeEventListener('mousemove', onMouseMove)
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.panel-title .trend-detail-link {
  margin-left: auto; font-size: 11px; letter-spacing: 1px; color: rgba(0, 212, 255, 0.75); text-decoration: none; border: 1px solid rgba(0, 212, 255, 0.3); padding: 2px 10px; border-radius: 4px; transition: all 0.25s; }
.panel-title .trend-detail-link:hover { color: #00d4ff; border-color: rgba(0, 212, 255, 0.7); box-shadow: 0 0 10px rgba(0, 212, 255, 0.3); }
.cockpit-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  padding: 0 16px 16px;
}

/* ===== 背景特效（grid / radial / vignette，与 canvas 层同级） ===== */
.bg-effects {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}
.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 229, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 229, 255, 0.035) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(ellipse at center, #000 30%, transparent 80%);
          mask-image: radial-gradient(ellipse at center, #000 30%, transparent 80%);
}
.radial-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at top center, rgba(0, 165, 255, 0.16), transparent 60%),
    radial-gradient(ellipse at 18% 82%, rgba(123, 104, 238, 0.1), transparent 50%),
    radial-gradient(ellipse at 82% 82%, rgba(0, 255, 163, 0.06), transparent 50%);
}
.vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 55%, rgba(1, 4, 12, 0.55) 100%);
}

/* ===== 主体三列（数据层最上） ===== */
.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 24% 1fr 24%;
  gap: 14px;
  position: relative;
  z-index: 2;
  min-height: 0;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

/* 左列：2×2 统计卡 + 趋势占余 */
.col-left .stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.panel-trend { flex: 1; min-height: 0; }

/* 中央：主视觉 28% + 地图占余 */
.hero-wrap {
  height: 28%;
  min-height: 175px;
  animation: panel-enter 0.7s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 95ms + 0.15s);
}
.panel-map { flex: 1; min-height: 0; }

/* 右列：热销 40% / 旭日 30% / 画像 30% */
.panel-hot { flex: 4; min-height: 0; }
.panel-sun { flex: 3; min-height: 0; }
.panel-profile { flex: 3; min-height: 0; }

/* ===== 入场编排：面板按 --i 依次浮现 ===== */
.panel,
.stat-grid {
  animation: panel-enter 0.7s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 95ms + 0.15s);
}
@keyframes panel-enter {
  from { transform: translateY(26px) scale(0.985); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}

/* 底部跑马灯入场 */
.marquee-enter {
  margin-top: 14px;
  animation: panel-enter 0.7s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 95ms + 0.15s);
}

/* ===== 鼠标拖尾光点（全局 body 下） ===== */
:global(.mouse-trail) {
  position: fixed;
  width: 7px;
  height: 7px;
  margin: -3.5px 0 0 -3.5px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 229, 255, 0.9), rgba(0, 229, 255, 0));
  box-shadow: 0 0 10px rgba(0, 229, 255, 0.6);
  pointer-events: none;
  z-index: 997;
  animation: trail-fade 0.65s ease-out forwards;
}
@keyframes trail-fade {
  from { transform: scale(1); opacity: 0.9; }
  to { transform: scale(0.1) translateY(-6px); opacity: 0; }
}

@media (max-width: 1366px) {
  .main-content { grid-template-columns: 26% 1fr 26%; }
  .cockpit-container { padding: 0 10px 10px; }
  .main-content { gap: 10px; }
  .col { gap: 10px; }
}
</style>
