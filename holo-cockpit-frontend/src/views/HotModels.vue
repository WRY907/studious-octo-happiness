<template>
  <div class="hot-page">
    <!-- ===== 背景层 ===== -->
    <div class="bg-effects">
      <div class="grid-bg"></div>
      <div class="radial-bg"></div>
      <div class="vignette"></div>
    </div>

    <!-- ===== 顶部栏 ===== -->
    <header class="hot-header">
      <div class="header-left">
        <router-link to="/" class="back-link" title="返回数据大屏">🖥 返回大屏</router-link>
        <router-link v-if="!isAdmin" to="/admin/dashboard" class="back-link back-sub" title="返回管理后台">⬡ 后台</router-link>
        <router-link to="/trend" class="back-link back-sub" title="销售趋势分析">📈 趋势分析</router-link>
        <router-link to="/traffic" class="back-link back-sub" title="流量来源分析">🛰 流量分析</router-link>
        <router-link to="/profile" class="back-link back-sub" title="用户画像分析">👥 用户画像</router-link>
      </div>
      <div class="header-center">
        <h1 class="page-title">
          <span class="title-zh">热销机型分析</span>
          <span class="title-en">HOT&nbsp;MODELS&nbsp;ANALYSIS</span>
        </h1>
      </div>
      <div class="header-right">
        <span class="role-badge" :class="isAdmin ? 'role-admin' : 'role-merchant'">
          {{ isAdmin ? '👑 管理员视图' : '🏪 商家视图' }}
        </span>
        <span class="header-time">{{ currentTime }}</span>
      </div>
    </header>

    <!-- ===== 加载 / 错误态 ===== -->
    <div v-if="loading" class="state-box">
      <div class="loading-dots"></div>
      <div class="state-text">机型数据加载中…</div>
    </div>
    <div v-else-if="failed" class="state-box">
      <div class="state-icon">⚠️</div>
      <div class="state-text">数据加载失败：{{ errorMsg }}</div>
      <button class="retry-btn" @click="loadData">重新加载</button>
    </div>
    <div v-else-if="ready && !rows.length" class="state-box">
      <div class="state-icon">📭</div>
      <div class="state-text">暂无热销机型数据</div>
      <div class="state-sub">可由管理员在「数据导入」页面上传 hot_product 表 Excel</div>
    </div>

    <!-- ===== 主体 ===== -->
    <main v-else-if="ready" class="hot-body">
      <!-- 信息条 -->
      <div class="info-bar">
        <span class="info-chip">
          <span class="chip-icon">📱</span>
          热销机型 <b>{{ rows.length }}</b> 款 · {{ isAdmin ? '按销售额排名' : '按官方榜单排名' }}
          <span class="chip-sep">·</span>
          数据来源：hot_product 榜单
        </span>
        <span v-if="!isAdmin" class="info-chip chip-lock">
          <span class="chip-icon">🔒</span>
          商家视图：销售额等金额指标不可见，以下为销量与增长维度分析
        </span>
      </div>

      <!-- KPI 指标卡 -->
      <div class="kpi-grid">
        <div v-for="k in kpis" :key="k.label" class="kpi-card" :style="{ '--c': k.color }">
          <div class="kpi-icon">{{ k.icon }}</div>
          <div class="kpi-info">
            <div class="kpi-label">{{ k.label }}</div>
            <div class="kpi-value">{{ k.value }}<span v-if="k.unit" class="kpi-unit">{{ k.unit }}</span></div>
            <div v-if="k.sub" class="kpi-sub">{{ k.sub }}</div>
          </div>
        </div>
      </div>

      <!-- 主区：TOP3 领奖台（2/3）+ 市场份额环（1/3） -->
      <div class="grid-main">
        <section class="panel panel-podium">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">01</span>
            销量三甲 · 产品墙
            <span class="title-en">Top 3 Podium</span>
          </div>
          <div class="podium-wrap">
            <!-- 亚军 -->
            <div v-if="podium[1]" class="podium-item p2">
              <div class="medal medal-silver">2</div>
              <div class="img-beam beam-silver">
                <div class="product-box"><img :src="podium[1].imageUrl" :alt="podium[1].modelName" @error="onImgError"></div>
              </div>
              <div class="podium-name">{{ podium[1].modelName }}</div>
              <div class="podium-tag">{{ podium[1].series }}</div>
              <div class="podium-value" v-html="podiumValue(podium[1])"></div>
            </div>
            <!-- 冠军 -->
            <div v-if="podium[0]" class="podium-item p1">
              <div class="medal medal-gold">1</div>
              <div class="crown">👑</div>
              <div class="img-beam beam-gold">
                <div class="product-box"><img :src="podium[0].imageUrl" :alt="podium[0].modelName" @error="onImgError"></div>
              </div>
              <div class="podium-name">{{ podium[0].modelName }}</div>
              <div class="podium-tag">{{ podium[0].series }}</div>
              <div class="podium-value" v-html="podiumValue(podium[0])"></div>
            </div>
            <!-- 季军 -->
            <div v-if="podium[2]" class="podium-item p3">
              <div class="medal medal-bronze">3</div>
              <div class="img-beam beam-bronze">
                <div class="product-box"><img :src="podium[2].imageUrl" :alt="podium[2].modelName" @error="onImgError"></div>
              </div>
              <div class="podium-name">{{ podium[2].modelName }}</div>
              <div class="podium-tag">{{ podium[2].series }}</div>
              <div class="podium-value" v-html="podiumValue(podium[2])"></div>
            </div>
          </div>
        </section>

        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">02</span>
            {{ isAdmin ? '销售额份额' : '销量份额' }}
            <span class="title-en">Market Share</span>
          </div>
          <div ref="shareChartEl" class="chart chart-share"></div>
        </section>
      </div>

      <!-- 底区：完整排行榜（2/3）+ 增长率排行（1/3） -->
      <div class="grid-bottom">
        <section class="panel panel-rank">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">03</span>
            完整榜单 · 第 4-12 名
            <span class="title-en">Full Ranking</span>
          </div>
          <div class="rank-list">
            <div v-for="r in restRows" :key="r.rankNo" class="rank-row" :title="r.modelName">
              <span class="rank-no">{{ r.rankNo }}</span>
              <div class="rank-img"><img :src="r.imageUrl" :alt="r.modelName" @error="onImgError"></div>
              <div class="rank-info">
                <div class="rank-name-line">
                  <span class="rank-name">{{ r.modelName }}</span>
                  <span class="rank-series">{{ r.series }}</span>
                  <span class="rank-rating">⭐ {{ r.rating ?? '—' }}</span>
                </div>
                <div class="rank-bar-track">
                  <div class="rank-bar" :style="{ width: r.barPct + '%' }"></div>
                </div>
              </div>
              <div class="rank-value">
                <div class="rv-main">{{ isAdmin ? '¥ ' + fmtWan(r.salesAmount) + ' 万' : fmtNum(r.salesCount) + ' 台' }}</div>
                <div class="rv-sub">
                  <span v-if="isAdmin">{{ fmtNum(r.salesCount) }} 台</span>
                  <span class="growth" :class="r.growth >= 0 ? 'up' : 'down'">{{ r.growth >= 0 ? '▲' : '▼' }} {{ Math.abs(r.growth).toFixed(1) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">04</span>
            增长率排行
            <span class="title-en">Growth Rate</span>
          </div>
          <div ref="growthChartEl" class="chart chart-growth"></div>
        </section>
      </div>
    </main>

    <!-- 底部版权条 -->
    <footer class="hot-footer">
      HUAWEI DATA COCKPIT · HOT MODELS · 数据仅供参考
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'

/* ===== 登录态与角色 ===== */
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 页面状态 ===== */
const loading = ref(false)
const failed = ref(false)
const errorMsg = ref('')
const ready = ref(false)
const rows = ref([])

/* ===== 顶栏时间 ===== */
const currentTime = ref('')
let timer = null

function pad(n) { return n < 10 ? '0' + n : '' + n }

function updateTime() {
  const d = new Date()
  currentTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  failed.value = false
  try {
    const data = await adminApi.getHotModels()
    rows.value = (data || []).map(r => ({
      rankNo: Number(r.rankNo || 0),
      modelName: r.modelName || '',
      series: r.series || '',
      rating: r.rating ?? null,
      imageUrl: r.imageUrl || '',
      price: r.price === null || r.price === undefined ? null : Number(r.price),
      salesCount: Number(r.salesCount || 0),
      salesAmount: r.salesAmount === null || r.salesAmount === undefined ? null : Number(r.salesAmount),
      growth: Number(r.growth || 0)
    }))
  } catch (e) {
    failed.value = true
    errorMsg.value = e?.response?.data?.message || e?.message || '服务异常'
    rows.value = []
  } finally {
    // 必须先解除 loading，v-if 链才会渲染出主内容与图表容器
    loading.value = false
  }
  if (failed.value) return
  ready.value = true
  await nextTick()
  renderAll()
}

/* ===== 图片加载失败兜底 ===== */
function onImgError(e) {
  e.target.style.display = 'none'
  if (e.target.parentElement && !e.target.parentElement.querySelector('.img-fallback')) {
    const fb = document.createElement('div')
    fb.className = 'img-fallback'
    fb.textContent = '📱'
    e.target.parentElement.appendChild(fb)
  }
}

/* ===== 通用格式化 ===== */
function fmtWan(v) { // 元 → 万（保留 1 位）
  return (Number(v || 0) / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}
function fmtNum(v) {
  return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

/* ===== 三甲与榜四至末位 ===== */
const podium = computed(() => rows.value.slice(0, 3))
const restRows = computed(() => {
  const rest = rows.value.slice(3)
  const key = isAdmin.value ? 'salesAmount' : 'salesCount'
  const maxV = Math.max(...rest.map(r => r[key] || 0), 1)
  return rest.map(r => ({ ...r, barPct: Math.max(4, Math.round((r[key] || 0) / maxV * 100)) }))
})

/* ===== 领奖台数值行（管理员：金额+销量；商家：销量+增长） ===== */
function podiumValue(r) {
  const growth = `<span class="growth ${r.growth >= 0 ? 'up' : 'down'}">${r.growth >= 0 ? '▲' : '▼'} ${Math.abs(r.growth).toFixed(1)}%</span>`
  if (isAdmin.value) {
    return `<b>¥ ${fmtWan(r.salesAmount)} 万</b><span class="pv-sep">·</span>${fmtNum(r.salesCount)} 台 ${growth}`
  }
  return `<b>${fmtNum(r.salesCount)} 台</b><span class="pv-sep">·</span>${growth}`
}

/* ===== KPI ===== */
const kpis = computed(() => {
  if (!rows.value.length) return []
  const total = rows.value.reduce((a, b) => a + (b.salesCount || 0), 0)
  const growths = rows.value.map(r => r.growth || 0)
  const avgGrowth = growths.reduce((a, b) => a + b, 0) / growths.length
  const ratings = rows.value.map(r => r.rating).filter(v => v > 0)
  const avgRating = ratings.length ? (ratings.reduce((a, b) => a + b, 0) / ratings.length).toFixed(2) : '—'
  const topByCount = [...rows.value].sort((a, b) => b.salesCount - a.salesCount)[0]
  const topByGrowth = [...rows.value].sort((a, b) => b.growth - a.growth)[0]
  if (isAdmin.value) {
    const totalAmount = rows.value.reduce((a, b) => a + (b.salesAmount || 0), 0)
    return [
      { icon: '💰', color: '#00d4ff', label: '热销总销售额', value: '¥ ' + fmtWan(totalAmount), unit: '万', sub: `${rows.value.length} 款机型合计` },
      { icon: '📦', color: '#00ffa3', label: '热销总销量', value: fmtNum(total), unit: '台', sub: '榜单周期内累计' },
      { icon: '🚀', color: '#ffcc00', label: '平均增长率', value: avgGrowth.toFixed(1), unit: '%', sub: `增长冠军：${topByGrowth.modelName}` },
      { icon: '⭐', color: '#7b68ee', label: '平均口碑评分', value: avgRating, sub: '满分 5.0' }
    ]
  }
  return [
    { icon: '📦', color: '#00ffa3', label: '热销总销量', value: fmtNum(total), unit: '台', sub: `${rows.value.length} 款机型合计` },
    { icon: '🏆', color: '#00d4ff', label: '销量榜首', value: fmtNum(topByCount.salesCount), unit: '台', sub: topByCount.modelName },
    { icon: '🚀', color: '#ffcc00', label: '平均增长率', value: avgGrowth.toFixed(1), unit: '%', sub: `增长冠军：${topByGrowth.modelName}` },
    { icon: '⭐', color: '#7b68ee', label: '平均口碑评分', value: avgRating, sub: '满分 5.0' }
  ]
})

/* ===== 图表实例 ===== */
const shareChartEl = ref(null)
const growthChartEl = ref(null)
let charts = {}

const tooltipStyle = {
  backgroundColor: 'rgba(3, 11, 34, 0.92)',
  borderColor: 'rgba(0, 212, 255, 0.4)',
  textStyle: { color: '#c9e1ff', fontSize: 12 }
}

/* ===== 份额环图 ===== */
function shareOption() {
  const key = isAdmin.value ? 'salesAmount' : 'salesCount'
  const sorted = [...rows.value].sort((a, b) => (b[key] || 0) - (a[key] || 0))
  const top6 = sorted.slice(0, 6)
  const others = sorted.slice(6)
  const othersVal = others.reduce((a, r) => a + (r[key] || 0), 0)
  const names = top6.map(r => shortName(r.modelName))
  const values = top6.map(r => +(r[key] || 0).toFixed(isAdmin.value ? 0 : 0))
  if (othersVal > 0) { names.push('其他 ' + others.length + ' 款'); values.push(+othersVal.toFixed(0)) }
  const colors = ['#00d4ff', '#00ffa3', '#ffcc00', '#7b68ee', '#ff4d6b', '#38bdf8', 'rgba(122, 151, 197, 0.45)']
  return {
    color: colors,
    tooltip: { ...tooltipStyle, trigger: 'item', formatter: p => `${p.name}<br/><b>${p.value}</b>（${p.percent}%）` },
    legend: {
      bottom: 0, left: 'center',
      textStyle: { color: '#7a97c5', fontSize: 10 },
      itemWidth: 10, itemHeight: 8, icon: 'circle'
    },
    series: [{
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#060f2a', borderWidth: 2, borderRadius: 4 },
      label: {
        show: true, position: 'center',
        formatter: () => isAdmin.value ? '销售额\n份额' : '销量\n份额',
        color: '#7a97c5', fontSize: 12, lineHeight: 16
      },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 700, color: '#e8f6ff' },
        itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0, 212, 255, 0.4)' }
      },
      labelLine: { show: false },
      data: names.map((n, i) => ({ name: n, value: values[i] }))
    }]
  }
}

/* 机型短名（去掉 HUAWEI 前缀） */
function shortName(n) { return (n || '').replace('HUAWEI ', '') }

/* ===== 增长率排行条形图 ===== */
function growthOption() {
  const sorted = [...rows.value].sort((a, b) => a.growth - b.growth)
  const names = sorted.map(r => shortName(r.modelName))
  const values = sorted.map(r => +(r.growth || 0).toFixed(1))
  const maxV = Math.max(...values)
  return {
    grid: { left: 86, right: 40, top: 8, bottom: 8 },
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => `${p[0].name}<br/>增长率：<b>${p[0].value}%</b>` },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category', data: names,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#7a97c5', fontSize: 10, width: 80, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: values.map(v => ({
        value: v,
        itemStyle: {
          borderRadius: [0, 3, 3, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: v >= maxV * 0.9 ? 'rgba(255, 204, 0, 0.35)' : 'rgba(0, 212, 255, 0.25)' },
            { offset: 1, color: v >= maxV * 0.9 ? '#ffcc00' : '#00d4ff' }
          ])
        }
      })),
      barWidth: '52%',
      label: { show: true, position: 'right', color: '#a8c4e8', fontSize: 10, formatter: '{c}%' }
    }]
  }
}

/* ===== 初始化图表实例（需在 ready 渲染出容器后调用） ===== */
function initCharts() {
  if (charts.share || !shareChartEl.value) return
  charts.share = echarts.init(shareChartEl.value)
  charts.growth = echarts.init(growthChartEl.value)
}

/* ===== 渲染全部图表 ===== */
function renderAll() {
  if (!rows.value.length) return
  initCharts()
  if (!charts.share) return
  charts.share.setOption(shareOption(), true)
  charts.growth.setOption(growthOption(), true)
}

const resizeHandler = () => Object.values(charts).forEach(c => c && c.resize())

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  window.addEventListener('resize', resizeHandler)
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  if (timer) clearInterval(timer)
  Object.values(charts).forEach(c => { if (c) c.dispose() })
  charts = {}
})
</script>

<style scoped>
/* ===== 页面容器：桌面端定高一屏 ===== */
.hot-page {
  position: relative;
  height: 100vh;
  background: #030b22;
  color: #c9e1ff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
}

/* ===== 背景 ===== */
.bg-effects { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.grid-bg {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(0, 212, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.04) 1px, transparent 1px);
  background-size: 44px 44px;
}
.radial-bg {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse 60% 45% at 50% 0%, rgba(0, 90, 181, 0.22), transparent 70%),
              radial-gradient(ellipse 40% 35% at 85% 90%, rgba(123, 104, 238, 0.08), transparent 70%);
}
.vignette {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse 90% 80% at 50% 50%, transparent 60%, rgba(1, 5, 16, 0.75) 100%);
}

/* ===== 顶部栏 ===== */
.hot-header {
  position: relative; z-index: 2;
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 28px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.15);
  background: rgba(3, 11, 34, 0.75);
  backdrop-filter: blur(8px);
}
.header-left { display: flex; gap: 12px; align-items: center; min-width: 220px; }
.back-link {
  color: #7a97c5; text-decoration: none; font-size: 13px;
  padding: 6px 12px; border: 1px solid rgba(122, 151, 197, 0.25); border-radius: 6px;
  transition: all 0.25s;
}
.back-link:hover { color: #00d4ff; border-color: rgba(0, 212, 255, 0.5); box-shadow: 0 0 12px rgba(0, 212, 255, 0.2); }
.header-center { text-align: center; }
.page-title { display: flex; flex-direction: column; align-items: center; gap: 2px; margin: 0; }
.title-zh {
  font-size: 22px; font-weight: 700; letter-spacing: 6px;
  background: linear-gradient(180deg, #e8f6ff 30%, #00d4ff 100%);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
  text-shadow: 0 0 24px rgba(0, 212, 255, 0.35);
}
.title-en { font-size: 10px; letter-spacing: 5px; color: rgba(122, 151, 197, 0.7); }
.header-right { display: flex; gap: 14px; align-items: center; min-width: 220px; justify-content: flex-end; }
.role-badge { font-size: 12px; padding: 5px 12px; border-radius: 999px; white-space: nowrap; }
.role-admin { color: #ffcc00; background: rgba(255, 204, 0, 0.1); border: 1px solid rgba(255, 204, 0, 0.35); }
.role-merchant { color: #00ffa3; background: rgba(0, 255, 163, 0.08); border: 1px solid rgba(0, 255, 163, 0.3); }
.header-time { font-size: 12px; color: #7a97c5; font-variant-numeric: tabular-nums; white-space: nowrap; }

/* ===== 信息条 ===== */
.info-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.info-chip {
  display: inline-flex; align-items: center; gap: 7px;
  font-size: 12px; color: #7a97c5; white-space: nowrap;
  padding: 6px 14px; border-radius: 999px;
  background: rgba(0, 212, 255, 0.04);
  border: 1px solid rgba(0, 212, 255, 0.14);
}
.info-chip b { color: #c9e1ff; font-weight: 600; }
.chip-icon { font-size: 13px; }
.chip-sep { color: rgba(122, 151, 197, 0.5); }
.chip-lock { color: #00ffa3; background: rgba(0, 255, 163, 0.05); border-color: rgba(0, 255, 163, 0.22); }

/* ===== 加载 / 错误 / 空态 ===== */
.state-box {
  position: relative; z-index: 2;
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px;
  color: #7a97c5; font-size: 14px;
}
.state-icon { font-size: 42px; }
.state-sub { font-size: 12px; color: rgba(122, 151, 197, 0.7); }
.loading-dots {
  width: 44px; height: 44px; border-radius: 50%;
  border: 3px solid rgba(0, 212, 255, 0.15); border-top-color: #00d4ff;
  animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.retry-btn {
  margin-top: 4px; padding: 8px 22px; font-size: 13px; cursor: pointer;
  color: #00d4ff; background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.45); border-radius: 6px;
  transition: all 0.25s;
}
.retry-btn:hover { background: rgba(0, 212, 255, 0.18); box-shadow: 0 0 14px rgba(0, 212, 255, 0.3); }

/* ===== 主体：弹性网格行 ===== */
.hot-body {
  position: relative; z-index: 2;
  flex: 1;
  min-height: 0;
  padding: 12px 28px 14px;
  display: grid;
  grid-template-rows: auto auto minmax(300px, 1.6fr) minmax(230px, 1fr);
  gap: 14px;
  width: 100%;
  box-sizing: border-box;
}

/* KPI 卡 */
.kpi-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.kpi-card {
  --c: #00d4ff;
  position: relative;
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px;
  background: linear-gradient(135deg, rgba(10, 25, 55, 0.85), rgba(6, 15, 38, 0.9));
  border: 1px solid rgba(0, 212, 255, 0.16);
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.25s, box-shadow 0.25s;
}
.kpi-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px;
  background: var(--c); box-shadow: 0 0 12px var(--c);
}
.kpi-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35), 0 0 16px rgba(0, 212, 255, 0.08); }
.kpi-icon {
  flex-shrink: 0;
  width: 46px; height: 46px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
  background: color-mix(in srgb, var(--c) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 26%, transparent);
  box-shadow: inset 0 0 12px color-mix(in srgb, var(--c) 12%, transparent);
}
.kpi-info { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.kpi-label { font-size: 12px; color: #7a97c5; letter-spacing: 1px; white-space: nowrap; }
.kpi-value {
  font-size: 22px; font-weight: 700; color: #e8f6ff;
  font-variant-numeric: tabular-nums; white-space: nowrap;
  text-shadow: 0 0 18px color-mix(in srgb, var(--c) 45%, transparent);
}
.kpi-unit { font-size: 12px; color: #7a97c5; margin-left: 3px; font-weight: 400; }
.kpi-sub { font-size: 11px; color: rgba(122, 151, 197, 0.75); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ===== 面板 ===== */
.panel {
  position: relative;
  background: linear-gradient(160deg, rgba(9, 22, 50, 0.88), rgba(5, 13, 34, 0.92));
  border: 1px solid rgba(0, 212, 255, 0.18);
  border-radius: 10px;
  padding: 14px 16px 10px;
  min-height: 0;
}
.corner-tl, .corner-tr, .corner-bl, .corner-br {
  position: absolute; width: 12px; height: 12px;
  border: 2px solid rgba(0, 212, 255, 0.6); pointer-events: none;
}
.corner-tl { top: -1px; left: -1px; border-right: none; border-bottom: none; border-radius: 10px 0 0 0; }
.corner-tr { top: -1px; right: -1px; border-left: none; border-bottom: none; border-radius: 0 10px 0 0; }
.corner-bl { bottom: -1px; left: -1px; border-right: none; border-top: none; border-radius: 0 0 0 10px; }
.corner-br { bottom: -1px; right: -1px; border-left: none; border-top: none; border-radius: 0 0 10px 0; }

.panel-title {
  display: flex; align-items: baseline; gap: 8px;
  font-size: 14px; font-weight: 600; color: #c9e1ff; letter-spacing: 2px;
  margin-bottom: 6px;
}
.panel-no { font-size: 11px; color: #00d4ff; font-weight: 700; text-shadow: 0 0 8px rgba(0, 212, 255, 0.5); }
.title-en { font-size: 10px; color: rgba(122, 151, 197, 0.65); letter-spacing: 1px; }

/* ===== 主区 / 底区网格 ===== */
.grid-main {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
  min-height: 0;
}
.grid-bottom {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 14px;
  min-height: 0;
}
.panel-podium, .panel-rank { display: flex; flex-direction: column; }
.chart { width: 100%; min-width: 0; }
.chart-share { flex: 1; min-height: 200px; }
.chart-growth { flex: 1; min-height: 180px; }
.panel .chart-growth, .panel .chart-share { flex: 1; }
.grid-main .panel:not(.panel-podium), .grid-bottom .panel:not(.panel-rank) { display: flex; flex-direction: column; }

/* ===== 领奖台 ===== */
.podium-wrap {
  flex: 1; min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1.25fr 1fr;
  gap: 14px;
  align-items: end;
  padding: 4px 6px 8px;
}
.podium-item {
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  min-width: 0; position: relative;
  animation: rise 0.6s ease both;
}
.podium-item.p1 { animation-delay: 0.15s; }
.podium-item.p2 { animation-delay: 0s; }
.podium-item.p3 { animation-delay: 0.3s; }
@keyframes rise { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: none; } }
.medal {
  position: absolute; top: -4px; z-index: 3;
  width: 26px; height: 26px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 800; color: #030b22;
}
.medal-gold { background: linear-gradient(135deg, #ffe27a, #ffcc00); box-shadow: 0 0 14px rgba(255, 204, 0, 0.7); left: 6px; }
.medal-silver { background: linear-gradient(135deg, #e8eef6, #aab8cc); box-shadow: 0 0 12px rgba(170, 184, 204, 0.6); left: 4px; }
.medal-bronze { background: linear-gradient(135deg, #f0b48c, #cd7f32); box-shadow: 0 0 12px rgba(205, 127, 50, 0.6); left: 4px; }
.crown {
  position: absolute; top: -22px; left: 50%; transform: translateX(-50%);
  font-size: 20px; z-index: 3;
  filter: drop-shadow(0 0 8px rgba(255, 204, 0, 0.8));
  animation: float 2.4s ease-in-out infinite;
}
@keyframes float { 0%, 100% { transform: translateX(-50%) translateY(0); } 50% { transform: translateX(-50%) translateY(-4px); } }
.img-beam {
  position: relative;
  width: 100%; max-width: 150px;
  border-radius: 10px;
  padding: 4px;
}
.podium-item.p1 .img-beam { max-width: 178px; }
.beam-gold { background: linear-gradient(180deg, rgba(255, 204, 0, 0.55), rgba(255, 204, 0, 0.04)); box-shadow: 0 0 22px rgba(255, 204, 0, 0.25); }
.beam-silver { background: linear-gradient(180deg, rgba(170, 184, 204, 0.5), rgba(170, 184, 204, 0.03)); }
.beam-bronze { background: linear-gradient(180deg, rgba(205, 127, 50, 0.5), rgba(205, 127, 50, 0.03)); }
.img-beam::after {
  content: ''; position: absolute; left: 8%; right: 8%; bottom: -10px; height: 10px;
  border-radius: 0 0 50% 50% / 0 0 100% 100%;
  background: radial-gradient(ellipse at center, rgba(0, 212, 255, 0.28), transparent 70%);
}
.product-box {
  position: relative;
  width: 100%; aspect-ratio: 1 / 1;
  background: linear-gradient(160deg, #f4f8fd, #dde7f2);
  border-radius: 8px;
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
}
.product-box img { width: 88%; height: 88%; object-fit: contain; }
.img-fallback { font-size: 34px; }
.podium-name {
  font-size: 13px; font-weight: 700; color: #e8f6ff; letter-spacing: 0.5px;
  max-width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.podium-tag {
  font-size: 10px; color: #00d4ff;
  padding: 1px 10px; border-radius: 999px;
  border: 1px solid rgba(0, 212, 255, 0.3); background: rgba(0, 212, 255, 0.06);
}
.podium-value {
  font-size: 12px; color: #a8c4e8; font-variant-numeric: tabular-nums;
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap; justify-content: center;
}
.podium-value b { color: #00d4ff; font-size: 14px; }
.p1 .podium-value b { color: #ffcc00; text-shadow: 0 0 12px rgba(255, 204, 0, 0.5); }
.pv-sep { color: rgba(122, 151, 197, 0.5); }

/* ===== 增长箭头通用 ===== */
.growth { font-size: 11px; font-weight: 600; white-space: nowrap; }
.growth.up { color: #00ffa3; }
.growth.down { color: #ff4d6b; }

/* ===== 完整排行榜 ===== */
.rank-list {
  flex: 1; min-height: 0;
  display: flex; flex-direction: column; justify-content: space-evenly;
  gap: 4px; overflow: hidden;
}
.rank-row {
  display: flex; align-items: center; gap: 10px;
  padding: 3px 6px;
  border-radius: 6px;
  transition: background 0.2s;
  min-width: 0;
}
.rank-row:hover { background: rgba(0, 212, 255, 0.05); }
.rank-no {
  flex-shrink: 0; width: 20px; text-align: center;
  font-size: 12px; font-weight: 700; color: #7a97c5;
  font-variant-numeric: tabular-nums;
}
.rank-img {
  flex-shrink: 0; width: 34px; height: 34px; border-radius: 6px;
  background: linear-gradient(160deg, #f4f8fd, #dde7f2);
  overflow: hidden; display: flex; align-items: center; justify-content: center;
}
.rank-img img { width: 86%; height: 86%; object-fit: contain; }
.rank-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.rank-name-line { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.rank-name {
  font-size: 12px; font-weight: 600; color: #c9e1ff;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.rank-series { font-size: 10px; color: rgba(122, 151, 197, 0.8); white-space: nowrap; }
.rank-rating { font-size: 10px; color: #ffcc00; white-space: nowrap; margin-left: auto; }
.rank-bar-track {
  height: 5px; border-radius: 3px;
  background: rgba(122, 151, 197, 0.1); overflow: hidden;
}
.rank-bar {
  height: 100%; border-radius: 3px;
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.9), rgba(0, 212, 255, 0.3));
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.35);
}
.rank-value { flex-shrink: 0; text-align: right; display: flex; flex-direction: column; gap: 1px; }
.rv-main { font-size: 13px; font-weight: 700; color: #e8f6ff; font-variant-numeric: tabular-nums; white-space: nowrap; }
.rv-sub { font-size: 10px; color: rgba(122, 151, 197, 0.85); display: flex; gap: 8px; justify-content: flex-end; align-items: center; white-space: nowrap; }

/* ===== 底部 ===== */
.hot-footer {
  position: relative; z-index: 2;
  text-align: center; padding: 12px;
  font-size: 10px; letter-spacing: 3px; color: rgba(122, 151, 197, 0.5);
  border-top: 1px solid rgba(0, 212, 255, 0.1);
}

/* ===== 响应式 ===== */
/* 矮屏压缩 */
@media (max-height: 900px) {
  .hot-header { padding: 8px 20px; }
  .hot-body { padding: 8px 20px 10px; gap: 10px; grid-template-rows: auto auto minmax(270px, 1.6fr) minmax(200px, 1fr); }
  .kpi-grid { gap: 10px; }
  .kpi-card { padding: 10px 14px; }
  .kpi-icon { width: 38px; height: 38px; font-size: 18px; }
  .kpi-value { font-size: 20px; }
  .grid-main, .grid-bottom { gap: 10px; }
  .chart-share { min-height: 180px; }
  .chart-growth { min-height: 150px; }
  .rank-img { width: 30px; height: 30px; }
  .rv-main { font-size: 12px; }
  .hot-footer { padding: 8px; }
}
/* 窄屏或极矮屏：退化为可滚动 */
@media (max-width: 1024px), (max-height: 720px) {
  .hot-page { height: auto; min-height: 100vh; }
  .hot-body { grid-template-rows: auto; }
  .grid-main, .grid-bottom { grid-template-columns: 1fr; }
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-share { min-height: 260px; }
  .chart-growth { min-height: 320px; }
  .rank-list { overflow: visible; }
}
@media (max-width: 620px) {
  .kpi-grid { grid-template-columns: 1fr; }
  .podium-wrap { grid-template-columns: 1fr; align-items: start; }
  .hot-header { flex-wrap: wrap; gap: 8px; }
  .header-left, .header-right { min-width: auto; }
  .hot-body { padding: 10px 12px 14px; }
}
</style>
