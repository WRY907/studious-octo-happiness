<template>
  <div class="dual-map">
    <!-- 陕西省 / 全国 切换 -->
    <div class="map-toggle">
      <button
        class="toggle-btn font-data"
        :class="{ active: mode === 'shaanxi' }"
        @click="setMode('shaanxi')"
      >陕西省</button>
      <span class="toggle-swap">⇄</span>
      <button
        class="toggle-btn font-data"
        :class="{ active: mode === 'china' }"
        @click="setMode('china')"
      >全国</button>
    </div>

    <!-- v-if(:key) + transition 切换淡入 -->
    <div class="map-stage">
      <transition name="map-fade" mode="out-in" appear @after-enter="renderMap">
        <div :key="mode" ref="chartEl" class="map-chart"></div>
      </transition>
      <div v-if="loading" class="map-loading font-data">MAP LOADING…</div>
      <div v-else-if="loadError" class="map-loading error font-data">{{ loadError }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 全国维度：[{province, sales, orders, users}]
  regionData: { type: Array, default: () => [] },
  // 陕西维度：[{city, sales, orders, users}]
  cityData: { type: Array, default: () => [] },
  // 商家脱敏模式：tooltip 金额显示 ***
  masked: { type: Boolean, default: false }
})

const mode = ref('shaanxi') // 默认陕西
const chartEl = ref(null)
const loading = ref(false)
const loadError = ref('')
let chart = null

/* 西安坐标（全国视图飞线终点） */
const XIAN_COORD = [108.948024, 34.263161]

/* geoJson 模块级缓存（避免重复 fetch） */
const geoCache = { shaanxi: null, china: null }
async function loadGeo(name) {
  if (geoCache[name]) return geoCache[name]
  const res = await fetch(`/${name}.json`)
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const json = await res.json()
  echarts.registerMap(name, json)
  geoCache[name] = json
  return json
}

/* 行政区名归一化：去 省/市/自治区 等后缀，便于数据名与地图名匹配 */
const NAME_SUFFIXES = ['维吾尔自治区', '壮族自治区', '回族自治区', '自治区', '特别行政区', '省', '市']
function normalizeName(name) {
  let n = String(name || '').trim()
  for (const s of NAME_SUFFIXES) {
    if (n.endsWith(s)) { n = n.slice(0, -s.length); break }
  }
  return n
}

function buildOption(mapName, geoJson) {
  const isSx = mapName === 'shaanxi'
  const list = (isSx ? props.cityData : props.regionData) || []
  const nameKey = isSx ? 'city' : 'province'

  // 数据名 → 记录
  const lookup = new Map()
  list.forEach(d => lookup.set(normalizeName(d[nameKey]), d))

  // geo 特征：地区名 → 中心坐标
  const features = geoJson.features || []
  const centerMap = new Map()
  features.forEach(f => {
    const p = f.properties || {}
    centerMap.set(normalizeName(p.name), p.center || p.centroid)
  })

  // 热力着色数据（visualMap 关联，名称与地图严格对齐）
  const heatData = features.map(f => {
    const name = f.properties.name
    const item = lookup.get(normalizeName(name))
    return {
      name,
      value: item ? Number(item.sales || 0) : 0,
      sales: item ? Number(item.sales || 0) : 0,
      orders: item ? Number(item.orders || 0) : 0,
      users: item ? Number(item.users || 0) : 0
    }
  })
  const salesValues = heatData.map(d => d.sales)
  const maxSales = Math.max(...salesValues, 1)

  // 涟漪点 & 飞线（地市 → 西安 / 省会 → 西安）
  const scatterData = []
  const lineData = []
  list.forEach(d => {
    const key = normalizeName(d[nameKey])
    const coord = centerMap.get(key)
    if (!coord) return
    scatterData.push({
      name: isSx ? d.city : d.province,
      value: [coord[0], coord[1], Number(d.sales || 0)],
      sales: Number(d.sales || 0),
      orders: Number(d.orders || 0),
      users: Number(d.users || 0),
      isXian: key === '西安'
    })
    // 全国视图跳过陕西（其中心即西安，避免零长飞线）；陕西视图跳过西安（终点）
    if (key !== '西安' && key !== '陕西') lineData.push({ coords: [coord, XIAN_COORD] })
  })
  const normalScatter = scatterData.filter(d => !d.isXian)
  const xianScatter = scatterData.filter(d => d.isXian)
  // 全国视图：西安为飞线汇聚点，补一个金色锚点（无销售数据，仅定位）
  if (!isSx) {
    xianScatter.push({
      name: '西安',
      value: [XIAN_COORD[0], XIAN_COORD[1], 0],
      anchorOnly: true
    })
  }

  function tipHtml(d, title) {
    // 商家脱敏模式：金额不可见（地图保留相对热力）
    const salesHtml = props.masked
      ? `销售额：<b style="color:#ffcc00">🔒 ***</b><br/>`
      : `销售额：<b style="color:#ffcc00">¥${Number(d.sales || 0).toLocaleString('zh-CN')}</b><br/>`
    return [
      `<div style="font-weight:600;margin-bottom:4px;color:#aef4ff">${title}</div>`,
      salesHtml,
      `订单数：<b>${Number(d.orders || 0).toLocaleString('zh-CN')}</b>　`,
      `用户数：<b>${Number(d.users || 0).toLocaleString('zh-CN')}</b>`
    ].join('')
  }

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(3, 11, 34, 0.94)',
      borderColor: 'rgba(0, 229, 255, 0.45)',
      textStyle: { color: '#c9e1ff', fontSize: 12 },
      formatter: params =>
        params.data && params.data.sales !== undefined
          ? tipHtml(params.data, params.data.name || params.name)
          : params.name
    },
    visualMap: {
      type: 'continuous',
      min: 0,
      max: maxSales,
      left: 12,
      bottom: 14,
      text: ['高', '低'],
      textStyle: { color: '#7a97c5', fontSize: 10 },
      calculable: false,
      itemWidth: 10,
      itemHeight: 80,
      inRange: { color: ['#08244a', '#0d4a8c', '#1173c4', '#009dff', '#00e5ff'] }
    },
    geo: {
      map: mapName,
      roam: false,
      zoom: isSx ? 1.16 : 1.1,
      scaleLimit: { min: 1, max: 5 },
      itemStyle: {
        areaColor: 'rgba(6, 22, 52, 0.9)',
        borderColor: 'rgba(0, 229, 255, 0.5)',
        borderWidth: 1,
        shadowColor: 'rgba(0, 165, 255, 0.35)',
        shadowBlur: 18,
        shadowOffsetY: 6
      },
      emphasis: {
        itemStyle: { areaColor: 'rgba(0, 165, 255, 0.4)' },
        label: { show: true, color: '#dff4ff', fontSize: 10 }
      },
      select: { disabled: true }
    },
    series: [
      // 热力着色（绑定 geo）
      { type: 'map', geoIndex: 0, data: heatData },
      // 普通涟漪点
      {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        zlevel: 2,
        rippleEffect: { brushType: 'stroke', scale: 2.6, period: 4 },
        symbolSize: val => 6 + Math.min(9, ((val[2] || 0) / maxSales) * 9),
        itemStyle: { color: '#00e5ff', shadowBlur: 6, shadowColor: 'rgba(0,229,255,0.8)' },
        data: normalScatter
      },
      // 西安高亮大涟漪（金色）
      {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        zlevel: 3,
        rippleEffect: { brushType: 'stroke', scale: 4.5, period: 3 },
        symbolSize: 14,
        itemStyle: { color: '#ffcc00', shadowBlur: 10, shadowColor: 'rgba(255,204,0,0.9)' },
        label: {
          show: true,
          position: 'right',
          formatter: '西安',
          color: '#ffe08a',
          fontSize: 11,
          fontWeight: 600
        },
        data: xianScatter
      },
      // 飞线 → 西安
      {
        type: 'lines',
        coordinateSystem: 'geo',
        zlevel: 4,
        effect: {
          show: true,
          period: 4,
          trailLength: 0.25,
          symbol: 'arrow',
          symbolSize: 5,
          color: '#00e5ff'
        },
        lineStyle: { color: '#00b0ff', width: 1, opacity: 0.28, curveness: 0.25 },
        data: lineData
      }
    ]
  }
}

/* 切换模式（由 toggle 触发，重挂载后经 after-enter 渲染） */
function setMode(m) {
  if (mode.value === m) return
  mode.value = m
}

/* 在当前 chartEl 上初始化并渲染（同模式数据更新仅 setOption，避免闪烁） */
async function renderMap() {
  const m = mode.value
  loading.value = true
  loadError.value = ''
  try {
    const geoJson = await loadGeo(m)
    await nextTick()
    if (!chartEl.value || mode.value !== m) return
    // 模式切换或容器重挂载 → 重建实例；同模式同容器 → 直接更新
    if (!chart || chart.__mode !== m || chart.__el !== chartEl.value) {
      if (chart) chart.dispose()
      chart = echarts.init(chartEl.value)
      chart.__mode = m
      chart.__el = chartEl.value
    }
    chart.setOption(buildOption(m, geoJson), { notMerge: true })
  } catch (e) {
    loadError.value = 'MAP DATA FAILED'
  } finally {
    if (mode.value === m) loading.value = false
  }
}

/* 数据更新时仅刷新 option（不重挂载） */
watch(
  () => [props.cityData, props.regionData],
  () => { if (chart) renderMap() },
  { deep: true }
)

/* 容器尺寸守卫：布局扰动瞬时 0 尺寸时跳过 resize（防止地图塌陷不自愈），恢复后重试 */
let resizeRetryTimer = null
const onResize = () => {
  if (!chart) return
  const el = chartEl.value
  if (!el || el.clientWidth === 0 || el.clientHeight === 0) {
    clearTimeout(resizeRetryTimer)
    resizeRetryTimer = setTimeout(() => onResize(), 200)
    return
  }
  try { chart.resize() } catch (e) { /* 0 尺寸瞬时异常兜底 */ }
}

onMounted(() => {
  // appear 首次入场也会触发 after-enter → renderMap
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  clearTimeout(resizeRetryTimer)
  if (chart) { chart.dispose(); chart = null }
})
</script>

<style scoped>
.dual-map {
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

.map-toggle {
  position: absolute;
  top: 2px;
  right: 4px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(2, 10, 26, 0.6);
  border: 1px solid rgba(0, 229, 255, 0.22);
  border-radius: 3px;
  padding: 3px 8px;
}
.toggle-btn {
  padding: 3px 12px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--text-muted);
  background: transparent;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  transition: all var(--dur-fast);
}
.toggle-btn:hover { color: var(--color-primary); }
.toggle-btn.active {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.28), rgba(123, 104, 238, 0.28));
  color: #fff;
  box-shadow: 0 0 10px rgba(0, 229, 255, 0.3);
}
.toggle-swap {
  font-size: 12px;
  color: var(--text-faint);
}

.map-stage {
  flex: 1;
  min-height: 0;
  position: relative;
}
.map-chart {
  width: 100%;
  height: 100%;
}

.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--text-faint);
  pointer-events: none;
}
.map-loading.error { color: var(--color-danger); }

/* 切换淡入淡出（仅 opacity） */
.map-fade-enter-active { transition: opacity 0.45s ease; }
.map-fade-leave-active { transition: opacity 0.2s ease; }
.map-fade-enter-from,
.map-fade-leave-to { opacity: 0; }
</style>
