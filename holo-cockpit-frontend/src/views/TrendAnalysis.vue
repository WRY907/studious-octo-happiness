<template>
  <div class="trend-page">
    <!-- ===== 背景层 ===== -->
    <div class="bg-effects">
      <div class="grid-bg"></div>
      <div class="radial-bg"></div>
      <div class="vignette"></div>
    </div>

    <!-- ===== 顶部栏 ===== -->
    <header class="trend-header">
      <div class="header-left">
        <router-link to="/" class="back-link" title="返回数据大屏">🖥 返回大屏</router-link>
        <router-link v-if="!isAdmin" to="/admin/dashboard" class="back-link back-sub" title="返回管理后台">⬡ 后台</router-link>
      </div>
      <div class="header-center">
        <h1 class="page-title">
          <span class="title-zh">{{ isAdmin ? '销售趋势分析' : '订单趋势分析' }}</span>
          <span class="title-en">SALES&nbsp;TREND&nbsp;ANALYSIS</span>
        </h1>
      </div>
      <div class="header-right">
        <span class="role-badge" :class="isAdmin ? 'role-admin' : 'role-merchant'">
          {{ isAdmin ? '👑 管理员视图' : '🏪 商家视图' }}
        </span>
        <span class="header-time">{{ currentTime }}</span>
      </div>
    </header>

    <!-- ===== 商家权限提示 ===== -->
    <div v-if="!isAdmin && ready" class="role-notice">
      <span class="notice-icon">🔒</span>
      <span>商家视图：按权限设置，销售额等金额指标不可见，以下为订单与流量维度分析</span>
    </div>

    <!-- ===== 加载 / 错误态 ===== -->
    <div v-if="loading" class="state-box">
      <div class="loading-dots"></div>
      <div class="state-text">趋势数据加载中…</div>
    </div>
    <div v-else-if="failed" class="state-box">
      <div class="state-icon">⚠️</div>
      <div class="state-text">数据加载失败：{{ errorMsg }}</div>
      <button class="retry-btn" @click="loadData">重新加载</button>
    </div>
    <div v-else-if="ready && !rows.length" class="state-box">
      <div class="state-icon">📭</div>
      <div class="state-text">暂无趋势数据</div>
      <div class="state-sub">可由管理员在「数据导入」页面上传 trend 表 Excel</div>
    </div>

    <!-- ===== 主体 ===== -->
    <main v-else-if="ready" class="trend-body">
      <!-- 数据范围条 -->
      <div class="range-bar">
        <span class="range-icon">📅</span>
        <span>统计范围：<b>{{ dateRange }}</b>（共 {{ rows.length }} 天）</span>
        <span class="range-dot"></span>
        <span>数据粒度：按天</span>
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

      <!-- 主图：30 日趋势 -->
      <section class="panel panel-main">
        <span class="corner-tl"></span><span class="corner-tr"></span>
        <span class="corner-bl"></span><span class="corner-br"></span>
        <div class="panel-title">
          <span class="panel-no">01</span>
          {{ isAdmin ? '30 日销售趋势' : '30 日订单趋势' }}
          <span class="title-en">30-Day Trend</span>
        </div>
        <div ref="mainChartEl" class="chart chart-main"></div>
      </section>

      <!-- 第二行：环比分析 + 周内效应 -->
      <div class="row-2col">
        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">02</span>
            周环比分析
            <span class="title-en">Week over Week</span>
          </div>
          <div class="wow-wrap">
            <div v-for="m in wowMetrics" :key="m.label" class="wow-item">
              <div class="wow-label">{{ m.label }}</div>
              <div class="wow-values">
                <div class="wow-col">
                  <div class="wow-col-label">本周</div>
                  <div class="wow-col-value now">{{ m.now }}</div>
                </div>
                <div class="wow-arrow" :class="m.pct >= 0 ? 'up' : 'down'">
                  {{ m.pct >= 0 ? '▲' : '▼' }} {{ Math.abs(m.pct).toFixed(1) }}%
                </div>
                <div class="wow-col">
                  <div class="wow-col-label">上周</div>
                  <div class="wow-col-value prev">{{ m.prev }}</div>
                </div>
              </div>
              <div class="wow-bar-track">
                <div class="wow-bar now-bar" :style="{ width: m.nowBar + '%' }"></div>
                <div class="wow-bar prev-bar" :style="{ width: m.prevBar + '%' }"></div>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">03</span>
            {{ isAdmin ? '周内销售效应' : '周内订单效应' }}
            <span class="title-en">Weekday Effect</span>
          </div>
          <div ref="weekdayChartEl" class="chart chart-half"></div>
        </section>
      </div>

      <!-- 第三行：每日环比增长率 + 客单价/转化率 -->
      <div class="row-2col">
        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">04</span>
            每日环比增长率
            <span class="title-en">Daily Growth</span>
          </div>
          <div ref="growthChartEl" class="chart chart-half"></div>
        </section>

        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">05</span>
            {{ isAdmin ? '客单价趋势' : '访问 - 转化趋势' }}
            <span class="title-en">{{ isAdmin ? 'Avg Order Value' : 'Conversion' }}</span>
          </div>
          <div ref="auxChartEl" class="chart chart-half"></div>
        </section>
      </div>
    </main>

    <!-- 底部版权条 -->
    <footer class="trend-footer">
      HUAWEI DATA COCKPIT · TREND ANALYSIS · 数据仅供参考
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
    const data = await adminApi.getTrend()
    rows.value = (data || []).map(r => ({
      date: (r.statDate || '').slice(0, 10),
      sales: r.sales === null || r.sales === undefined ? null : Number(r.sales),
      orders: Number(r.orders || 0),
      visits: Number(r.visits || 0),
      avgOrderValue: r.avgOrderValue === null || r.avgOrderValue === undefined ? null : Number(r.avgOrderValue)
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

/* ===== 通用格式化 ===== */
function fmtWan(v) { // 元 → 万（保留 1 位）
  return (v / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}
function fmtNum(v) {
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}
function shortDate(d) { return d ? d.slice(5) : '' }

/* ===== 统计范围 ===== */
const dateRange = computed(() => {
  if (!rows.value.length) return '—'
  return `${rows.value[0].date} ~ ${rows.value[rows.value.length - 1].date}`
})

/* ===== KPI（管理员：金额维度；商家：订单/流量维度） ===== */
const kpis = computed(() => {
  if (!rows.value.length) return []
  if (isAdmin.value) {
    const sales = rows.value.map(r => r.sales || 0)
    const total = sales.reduce((a, b) => a + b, 0)
    const peak = Math.max(...sales)
    const peakRow = rows.value[sales.indexOf(peak)]
    const aov = rows.value.map(r => r.avgOrderValue || (r.orders ? (r.sales || 0) / r.orders : 0)).filter(v => v > 0)
    return [
      { icon: '💰', color: '#00d4ff', label: '累计销售额', value: '¥ ' + fmtWan(total), unit: '万', sub: `${rows.value.length} 天合计` },
      { icon: '📊', color: '#00ffa3', label: '日均销售额', value: '¥ ' + fmtWan(total / rows.value.length), unit: '万', sub: '按自然日平均' },
      { icon: '🚀', color: '#ffcc00', label: '峰值单日销售额', value: '¥ ' + fmtWan(peak), unit: '万', sub: `出现于 ${shortDate(peakRow?.date)}` },
      { icon: '🧮', color: '#7b68ee', label: '平均客单价', value: '¥ ' + (aov.length ? (aov.reduce((a, b) => a + b, 0) / aov.length).toFixed(0) : '—'), sub: '元 / 单' }
    ]
  }
  const orders = rows.value.map(r => r.orders)
  const visits = rows.value.map(r => r.visits)
  const totalO = orders.reduce((a, b) => a + b, 0)
  const peak = Math.max(...orders)
  const peakRow = rows.value[orders.indexOf(peak)]
  const totalV = visits.reduce((a, b) => a + b, 0)
  return [
    { icon: '📦', color: '#00ffa3', label: '订单总量', value: fmtNum(totalO), sub: `${rows.value.length} 天合计` },
    { icon: '📊', color: '#00d4ff', label: '日均订单', value: fmtNum(totalO / rows.value.length), sub: '按自然日平均' },
    { icon: '🚀', color: '#ffcc00', label: '峰值单日订单', value: fmtNum(peak), sub: `出现于 ${shortDate(peakRow?.date)}` },
    { icon: '👀', color: '#7b68ee', label: '累计访问量', value: fmtNum(totalV), sub: '页面访问 PV' }
  ]
})

/* ===== 周环比（近 7 天 vs 前 7 天） ===== */
const wowMetrics = computed(() => {
  if (rows.value.length < 8) return []
  const last14 = rows.value.slice(-14)
  const thisWeek = last14.slice(-7)
  const prevWeek = last14.slice(0, 7)
  const sum = (arr, key) => arr.reduce((a, r) => a + (r[key] || 0), 0)
  const pct = (now, prev) => prev > 0 ? (now - prev) / prev * 100 : 0

  if (isAdmin.value) {
    const defs = [
      { key: 'sales', label: '销售额(万)', fmt: v => fmtWan(v) },
      { key: 'orders', label: '订单数', fmt: v => fmtNum(v) },
      { key: 'visits', label: '访问量', fmt: v => fmtNum(v) }
    ]
    const met = defs.map(d => {
      const now = sum(thisWeek, d.key), prev = sum(prevWeek, d.key)
      return { label: d.label, now: d.fmt(now), prev: d.fmt(prev), pct: pct(now, prev), nowBar: 0, prevBar: 0 }
    })
    const maxV = Math.max(...met.map(m => parseFloat(m.now) || 0), ...met.map(m => parseFloat(m.prev) || 0), 1)
    met.forEach(m => { m.nowBar = Math.min(100, (parseFloat(m.now) || 0) / maxV * 100); m.prevBar = Math.min(100, (parseFloat(m.prev) || 0) / maxV * 100) })
    return met
  }
  const defs = [
    { key: 'orders', label: '订单数', fmt: v => fmtNum(v) },
    { key: 'visits', label: '访问量', fmt: v => fmtNum(v) },
    { key: 'convert', label: '转化率(%)', fmt: v => v.toFixed(2) }
  ]
  const conv = arr => sum(arr, 'orders') / Math.max(1, sum(arr, 'visits')) * 100
  const met = defs.map(d => {
    const now = d.key === 'convert' ? conv(thisWeek) : sum(thisWeek, d.key)
    const prev = d.key === 'convert' ? conv(prevWeek) : sum(prevWeek, d.key)
    return { label: d.label, now: d.fmt(now), prev: d.fmt(prev), pct: pct(now, prev), nowBar: 0, prevBar: 0 }
  })
  const maxV = Math.max(...met.map(m => parseFloat(m.now) || 0), ...met.map(m => parseFloat(m.prev) || 0), 1)
  met.forEach(m => { m.nowBar = Math.min(100, (parseFloat(m.now) || 0) / maxV * 100); m.prevBar = Math.min(100, (parseFloat(m.prev) || 0) / maxV * 100) })
  return met
})

/* ===== 图表实例 ===== */
const mainChartEl = ref(null)
const weekdayChartEl = ref(null)
const growthChartEl = ref(null)
const auxChartEl = ref(null)
let charts = {}

const axisStyle = {
  axisLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.25)' } },
  axisLabel: { color: '#7a97c5', fontSize: 10 },
  axisTick: { show: false }
}
const tooltipStyle = {
  backgroundColor: 'rgba(3, 11, 34, 0.92)',
  borderColor: 'rgba(0, 212, 255, 0.4)',
  textStyle: { color: '#c9e1ff', fontSize: 12 },
  axisPointer: { lineStyle: { color: 'rgba(0, 212, 255, 0.5)' } }
}

/* ===== 主图：30 日趋势 ===== */
function mainOption() {
  const dates = rows.value.map(r => shortDate(r.date))
  const series = []
  const legend = []
  if (isAdmin.value) {
    legend.push('销售额(万)')
    series.push({
      name: '销售额(万)', type: 'bar', data: rows.value.map(r => r.sales ? +(r.sales / 10000).toFixed(1) : 0),
      barWidth: '42%',
      itemStyle: {
        borderRadius: [3, 3, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 212, 255, 0.9)' },
          { offset: 1, color: 'rgba(0, 212, 255, 0.15)' }
        ])
      }
    })
  }
  legend.push('订单数')
  series.push({
    name: '订单数', type: 'line', smooth: true, showSymbol: false,
    data: rows.value.map(r => r.orders),
    lineStyle: { width: 2.5, color: '#00ffa3', shadowColor: 'rgba(0,255,163,0.4)', shadowBlur: 8 },
    itemStyle: { color: '#00ffa3' }
  })
  legend.push('访问量(千)')
  series.push({
    name: '访问量(千)', type: 'line', smooth: true, showSymbol: false,
    data: rows.value.map(r => +(r.visits / 1000).toFixed(1)),
    lineStyle: { width: 2, color: '#ffcc00', type: 'dashed' },
    itemStyle: { color: '#ffcc00' }
  })
  return {
    grid: { left: 56, right: 56, top: 42, bottom: 32 },
    legend: { top: 4, right: 10, textStyle: { color: '#a8c4e8', fontSize: 11 }, itemWidth: 12, itemHeight: 8, data: legend },
    tooltip: { trigger: 'axis', ...tooltipStyle },
    xAxis: { type: 'category', data: dates, boundaryGap: true, ...axisStyle, axisLabel: { ...axisStyle.axisLabel, interval: 2 } },
    yAxis: [
      { type: 'value', name: isAdmin.value ? '销售额(万)' : '订单数', nameTextStyle: { color: '#7a97c5', fontSize: 10 }, axisLine: { show: false }, axisLabel: { color: '#7a97c5', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.08)' } } },
      { type: 'value', name: '访问量(千)', nameTextStyle: { color: '#7a97c5', fontSize: 10 }, axisLine: { show: false }, axisLabel: { color: '#7a97c5', fontSize: 10 }, splitLine: { show: false } }
    ],
    series: series.map((s, i) => (s.name === '访问量(千)' ? { ...s, yAxisIndex: 1 } : s))
  }
}

/* ===== 周内效应 ===== */
function weekdayOption() {
  const names = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const buckets = Array.from({ length: 7 }, () => ({ sum: 0, n: 0 }))
  rows.value.forEach(r => {
    const day = new Date(r.date).getDay()
    buckets[day].sum += isAdmin.value ? (r.sales || 0) / 10000 : r.orders
    buckets[day].n++
  })
  // 顺序：周一 ~ 周日
  const order = [1, 2, 3, 4, 5, 6, 0]
  const labels = order.map(i => names[i])
  const values = order.map(i => buckets[i].n ? +(buckets[i].sum / buckets[i].n).toFixed(isAdmin.value ? 1 : 0) : 0)
  const maxV = Math.max(...values, 1)
  return {
    grid: { left: 50, right: 20, top: 36, bottom: 28 },
    tooltip: { trigger: 'axis', ...tooltipStyle, formatter: p => `${p[0].name}<br/>平均${isAdmin.value ? '销售额' : '订单数'}：<b>${p[0].value}</b>${isAdmin.value ? ' 万' : ' 单'}` },
    xAxis: { type: 'category', data: labels, ...axisStyle },
    yAxis: { type: 'value', axisLine: { show: false }, axisLabel: { color: '#7a97c5', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.08)' } } },
    series: [{
      type: 'bar', data: values.map(v => ({
        value: v,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: v >= maxV * 0.85 ? '#ffcc00' : 'rgba(0, 212, 255, 0.85)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.12)' }
          ])
        }
      })),
      barWidth: '46%',
      label: { show: true, position: 'top', color: '#a8c4e8', fontSize: 10 }
    }]
  }
}

/* ===== 每日环比增长率 ===== */
function growthOption() {
  const key = isAdmin.value ? 'sales' : 'orders'
  const out = []
  rows.value.forEach((r, i) => {
    if (i === 0) return
    const prev = rows.value[i - 1][key] || 0
    const cur = r[key] || 0
    out.push({
      date: shortDate(r.date),
      pct: prev > 0 ? +(((cur - prev) / prev) * 100).toFixed(2) : 0
    })
  })
  return {
    grid: { left: 50, right: 20, top: 36, bottom: 28 },
    tooltip: { trigger: 'axis', ...tooltipStyle, formatter: p => `${p[0].name}<br/>环比增长：<b style="color:${p[0].value >= 0 ? '#00ffa3' : '#ff4d6b'}">${p[0].value}%</b>` },
    xAxis: { type: 'category', data: out.map(o => o.date), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, interval: 2 } },
    yAxis: { type: 'value', axisLine: { show: false }, axisLabel: { color: '#7a97c5', fontSize: 10, formatter: '{value}%' }, splitLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.08)' } } },
    series: [{
      type: 'bar',
      data: out.map(o => ({
        value: o.pct,
        itemStyle: {
          borderRadius: o.pct >= 0 ? [3, 3, 0, 0] : [0, 0, 3, 3],
          color: o.pct >= 0 ? 'rgba(0, 255, 163, 0.75)' : 'rgba(255, 77, 107, 0.75)'
        }
      })),
      barWidth: '46%',
      markLine: {
        symbol: 'none', silent: true,
        lineStyle: { color: 'rgba(122, 151, 197, 0.4)', type: 'dashed' },
        data: [{ yAxis: 0 }]
      }
    }]
  }
}

/* ===== 辅助图：管理员=客单价 / 商家=访问-转化 ===== */
function auxOption() {
  const dates = rows.value.map(r => shortDate(r.date))
  if (isAdmin.value) {
    const aov = rows.value.map(r => r.avgOrderValue ? +Number(r.avgOrderValue).toFixed(0) : (r.orders ? +((r.sales || 0) / r.orders).toFixed(0) : 0))
    return {
      grid: { left: 50, right: 20, top: 36, bottom: 28 },
      legend: { top: 4, right: 10, textStyle: { color: '#a8c4e8', fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
      tooltip: { trigger: 'axis', ...tooltipStyle, formatter: p => `${p[0].name}<br/>客单价：<b>¥ ${p[0].value}</b>` },
      xAxis: { type: 'category', data: dates, ...axisStyle, axisLabel: { ...axisStyle.axisLabel, interval: 2 } },
      yAxis: { type: 'value', axisLine: { show: false }, axisLabel: { color: '#7a97c5', fontSize: 10, formatter: '¥{value}' }, splitLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.08)' } } },
      series: [{
        name: '客单价(元)', type: 'line', smooth: true, data: aov, showSymbol: false,
        lineStyle: { width: 2.5, color: '#7b68ee', shadowColor: 'rgba(123,104,238,0.4)', shadowBlur: 8 },
        itemStyle: { color: '#7b68ee' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(123, 104, 238, 0.35)' },
            { offset: 1, color: 'rgba(123, 104, 238, 0.02)' }
          ])
        }
      }]
    }
  }
  const visits = rows.value.map(r => +(r.visits / 1000).toFixed(1))
  const conv = rows.value.map(r => +(r.visits ? (r.orders / r.visits * 100).toFixed(2) : 0))
  return {
    grid: { left: 50, right: 50, top: 42, bottom: 28 },
    legend: { top: 4, right: 10, textStyle: { color: '#a8c4e8', fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
    tooltip: { trigger: 'axis', ...tooltipStyle },
    xAxis: { type: 'category', data: dates, boundaryGap: false, ...axisStyle, axisLabel: { ...axisStyle.axisLabel, interval: 2 } },
    yAxis: [
      { type: 'value', name: '访问量(千)', nameTextStyle: { color: '#7a97c5', fontSize: 10 }, axisLine: { show: false }, axisLabel: { color: '#7a97c5', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.08)' } } },
      { type: 'value', name: '转化率(%)', nameTextStyle: { color: '#7a97c5', fontSize: 10 }, axisLine: { show: false }, axisLabel: { color: '#7a97c5', fontSize: 10, formatter: '{value}%' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '访问量(千)', type: 'bar', data: visits, barWidth: '42%',
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.8)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.12)' }
          ])
        }
      },
      {
        name: '转化率(%)', type: 'line', smooth: true, data: conv, yAxisIndex: 1, showSymbol: false,
        lineStyle: { width: 2.5, color: '#ff6b8a', shadowColor: 'rgba(255,107,138,0.4)', shadowBlur: 8 },
        itemStyle: { color: '#ff6b8a' }
      }
    ]
  }
}

/* ===== 初始化图表实例（需在 ready 渲染出容器后调用） ===== */
function initCharts() {
  if (charts.main || !mainChartEl.value) return
  charts.main = echarts.init(mainChartEl.value)
  charts.weekday = echarts.init(weekdayChartEl.value)
  charts.growth = echarts.init(growthChartEl.value)
  charts.aux = echarts.init(auxChartEl.value)
}

/* ===== 渲染全部图表 ===== */
function renderAll() {
  if (!rows.value.length) return
  initCharts()
  if (!charts.main) return
  charts.main.setOption(mainOption(), true)
  charts.weekday.setOption(weekdayOption(), true)
  charts.growth.setOption(growthOption(), true)
  charts.aux.setOption(auxOption(), true)
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
/* ===== 页面容器 ===== */
.trend-page {
  position: relative;
  min-height: 100vh;
  background: #030b22;
  color: #c9e1ff;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
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
.trend-header {
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
.role-badge {
  font-size: 12px; padding: 5px 12px; border-radius: 999px; white-space: nowrap;
}
.role-admin { color: #ffcc00; background: rgba(255, 204, 0, 0.1); border: 1px solid rgba(255, 204, 0, 0.35); }
.role-merchant { color: #00ffa3; background: rgba(0, 255, 163, 0.08); border: 1px solid rgba(0, 255, 163, 0.3); }
.header-time { font-size: 12px; color: #7a97c5; font-variant-numeric: tabular-nums; white-space: nowrap; }

/* ===== 商家提示条 ===== */
.role-notice {
  position: relative; z-index: 2;
  margin: 12px 28px 0;
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px; font-size: 13px;
  color: #00ffa3;
  background: rgba(0, 255, 163, 0.06);
  border: 1px solid rgba(0, 255, 163, 0.25);
  border-radius: 8px;
}
.notice-icon { font-size: 15px; }

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

/* ===== 主体 ===== */
.trend-body {
  position: relative; z-index: 2;
  flex: 1;
  padding: 16px 28px 24px;
  display: flex; flex-direction: column; gap: 16px;
  max-width: 1680px; width: 100%; margin: 0 auto; box-sizing: border-box;
}

/* 数据范围条 */
.range-bar {
  display: flex; align-items: center; gap: 10px;
  font-size: 12px; color: #7a97c5;
  padding: 8px 14px;
  background: rgba(0, 212, 255, 0.04);
  border: 1px solid rgba(0, 212, 255, 0.14);
  border-radius: 8px;
}
.range-bar b { color: #c9e1ff; font-weight: 600; }
.range-dot { width: 4px; height: 4px; border-radius: 50%; background: rgba(0, 212, 255, 0.5); }

/* KPI 卡 */
.kpi-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px;
}
.kpi-card {
  --c: #00d4ff;
  position: relative;
  display: flex; align-items: center; gap: 14px;
  padding: 16px 18px;
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
.kpi-icon { font-size: 26px; filter: drop-shadow(0 0 8px var(--c)); }
.kpi-info { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.kpi-label { font-size: 12px; color: #7a97c5; letter-spacing: 1px; }
.kpi-value {
  font-size: 24px; font-weight: 700; color: #e8f6ff;
  font-variant-numeric: tabular-nums; white-space: nowrap;
  text-shadow: 0 0 18px color-mix(in srgb, var(--c) 45%, transparent);
}
.kpi-unit { font-size: 12px; color: #7a97c5; margin-left: 3px; font-weight: 400; }
.kpi-sub { font-size: 11px; color: rgba(122, 151, 197, 0.75); }

/* ===== 面板 ===== */
.panel {
  position: relative;
  background: linear-gradient(160deg, rgba(9, 22, 50, 0.88), rgba(5, 13, 34, 0.92));
  border: 1px solid rgba(0, 212, 255, 0.18);
  border-radius: 10px;
  padding: 14px 16px 10px;
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
.panel-no {
  font-size: 11px; color: #00d4ff; font-weight: 700;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
}
.title-en { font-size: 10px; color: rgba(122, 151, 197, 0.65); letter-spacing: 1px; }

/* 主图高度 */
.panel-main .chart-main { height: 380px; }
.chart { width: 100%; }

/* 双列行 */
.row-2col {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
}
.chart-half { height: 260px; }

/* ===== 周环比面板 ===== */
.wow-wrap { display: flex; flex-direction: column; gap: 16px; padding: 8px 6px 12px; }
.wow-item { display: flex; flex-direction: column; gap: 8px; }
.wow-label { font-size: 12px; color: #7a97c5; letter-spacing: 1px; }
.wow-values { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.wow-col { display: flex; flex-direction: column; gap: 2px; }
.wow-col-label { font-size: 10px; color: rgba(122, 151, 197, 0.7); }
.wow-col-value { font-size: 17px; font-weight: 700; font-variant-numeric: tabular-nums; }
.wow-col-value.now { color: #00d4ff; text-shadow: 0 0 12px rgba(0, 212, 255, 0.4); }
.wow-col-value.prev { color: rgba(122, 151, 197, 0.85); }
.wow-arrow { font-size: 12px; font-weight: 600; white-space: nowrap; }
.wow-arrow.up { color: #00ffa3; }
.wow-arrow.down { color: #ff4d6b; }
.wow-bar-track {
  position: relative; height: 6px; border-radius: 3px;
  background: rgba(122, 151, 197, 0.1); overflow: hidden;
}
.wow-bar { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 3px; }
.now-bar { background: linear-gradient(90deg, rgba(0, 212, 255, 0.85), rgba(0, 212, 255, 0.35)); z-index: 2; }
.prev-bar { background: rgba(122, 151, 197, 0.3); z-index: 1; }

/* ===== 底部 ===== */
.trend-footer {
  position: relative; z-index: 2;
  text-align: center; padding: 12px;
  font-size: 10px; letter-spacing: 3px; color: rgba(122, 151, 197, 0.5);
  border-top: 1px solid rgba(0, 212, 255, 0.1);
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .row-2col { grid-template-columns: 1fr; }
  .panel-main .chart-main { height: 300px; }
}
@media (max-width: 640px) {
  .kpi-grid { grid-template-columns: 1fr; }
  .trend-header { flex-wrap: wrap; gap: 8px; }
  .header-left, .header-right { min-width: auto; }
  .trend-body { padding: 12px 14px 20px; }
  .role-notice { margin: 10px 14px 0; }
}
</style>
