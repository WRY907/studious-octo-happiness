<template>
  <div class="traffic-page">
    <!-- 背景层 -->
    <div class="bg-effects">
      <div class="grid-bg"></div>
      <div class="radial-bg"></div>
      <div class="vignette"></div>
    </div>

    <!-- 顶部栏（sticky） -->
    <header class="traffic-header">
      <div class="header-left">
        <router-link to="/" class="back-link" title="返回数据大屏">🖥 返回大屏</router-link>
        <router-link v-if="!isAdmin" to="/admin/dashboard" class="back-link back-sub" title="返回管理后台">⬡ 后台</router-link>
        <router-link to="/trend" class="back-link back-sub" title="销售趋势分析">📈 趋势分析</router-link>
        <router-link to="/hotmodels" class="back-link back-sub" title="热销机型分析">🏆 热销机型</router-link>
      </div>
      <div class="header-center">
        <h1 class="page-title">
          <span class="title-zh">流量来源分析</span>
          <span class="title-en">TRAFFIC&nbsp;SOURCE&nbsp;ANALYSIS</span>
        </h1>
      </div>
      <div class="header-right">
        <span class="role-badge" :class="isAdmin ? 'role-admin' : 'role-merchant'">
          {{ isAdmin ? '👑 管理员视图' : '🏪 商家视图' }}
        </span>
        <span class="header-time">{{ currentTime }}</span>
      </div>
    </header>

    <!-- 加载/错误状态 -->
    <div v-if="loading" class="state-box">
      <div class="loading-dots"></div>
      <div class="state-text">流量数据加载中…</div>
    </div>
    <div v-else-if="failed" class="state-box">
      <div class="state-icon">⚠️</div>
      <div class="state-text">数据加载失败：{{ errorMsg }}</div>
      <button class="retry-btn" @click="loadData">重新加载</button>
    </div>
    <div v-else-if="ready && !sources.length" class="state-box">
      <div class="state-icon">📭</div>
      <div class="state-text">暂无流量来源数据</div>
      <div class="state-sub">可由管理员在「数据导入」页面上传 traffic_source 表 Excel</div>
    </div>

    <!-- 主体：可滚动长页 -->
    <main v-else-if="ready" class="traffic-body">
      <!-- 信息条 -->
      <div class="info-bar">
        <span class="info-chip">
          <span class="chip-icon">🛰</span>
          覆盖渠道 <b>{{ sources.length }}</b> 个 · <b>{{ categories.length }}</b> 大分类
          <span class="chip-sep">·</span> 数据来源：traffic_source 榜单
        </span>
        <span v-if="!isAdmin" class="info-chip chip-lock">
          <span class="chip-icon">🔒</span>
          商家视图：转化率/客单价/获客成本等经营指标不可见，以下为流量结构分析
        </span>
      </div>

      <!-- KPI -->
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

      <!-- 01 渠道全景旭日图 + 02 分类玫瑰图 -->
      <div class="panel-row">
        <section class="panel panel-wide">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">01</span>
            渠道全景 · 旭日图
            <span class="title-en">Channel Sunburst</span>
            <span class="title-tip">内环=分类 / 外环=渠道 · 悬停查看明细</span>
          </div>
          <div ref="sunburstEl" class="chart chart-sunburst"></div>
        </section>
        <section class="panel panel-narrow">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">02</span>
            分类占比 · 玫瑰图
            <span class="title-en">Category Rose</span>
          </div>
          <div ref="roseEl" class="chart chart-rose"></div>
        </section>
      </div>

      <!-- 03 24小时流量脉搏 -->
      <section class="panel">
        <span class="corner-tl"></span><span class="corner-tr"></span>
        <span class="corner-bl"></span><span class="corner-br"></span>
        <div class="panel-title">
          <span class="panel-no">03</span>
          24 小时流量脉搏
          <span class="title-en">Hourly Pulse</span>
          <span class="title-tip">晚间 19:00-22:00 为黄金时段，建议优先安排内容投放与直播</span>
        </div>
        <div ref="pulseEl" class="chart chart-pulse"></div>
      </section>

      <!-- 04 渠道排行榜 -->
      <section class="panel">
        <span class="corner-tl"></span><span class="corner-tr"></span>
        <span class="corner-bl"></span><span class="corner-br"></span>
        <div class="panel-title">
          <span class="panel-no">04</span>
          渠道排行榜 · 全量 {{ sources.length }} 强
          <span class="title-en">Channel Ranking</span>
          <span class="cat-legend">
            <span v-for="c in categories" :key="c.category" class="cat-dot" :style="{ background: catColor(c.category) }">{{ c.category }}</span>
          </span>
        </div>
        <div class="rank-list">
          <div v-for="(r, i) in ranked" :key="r.sourceName" class="rank-row" :title="r.sourceName + '（' + r.category + '）'">
            <span class="rank-no" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
            <span class="rank-logo">
              <img v-if="logoOf(r.sourceName)" :src="logoOf(r.sourceName)" :alt="r.sourceName" @error="onLogoError">
              <span v-else class="logo-emoji">{{ emojiOf(r.sourceName) }}</span>
            </span>
            <div class="rank-info">
              <div class="rank-name-line">
                <span class="rank-name">{{ r.sourceName }}</span>
                <span class="rank-cat" :style="{ color: catColor(r.category), borderColor: catColor(r.category) + '66', background: catColor(r.category) + '14' }">{{ r.category }}</span>
              </div>
              <div class="rank-bar-track">
                <div class="rank-bar" :style="{ width: r.barPct + '%', background: 'linear-gradient(90deg, ' + catColor(r.category) + '55, ' + catColor(r.category) + ')' }"></div>
              </div>
            </div>
            <div class="rank-value">
              <div class="rv-main">{{ fmtNum(r.visits) }}</div>
              <div class="rv-sub">{{ Number(r.ratio || 0).toFixed(1) }}%</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 管理员专属：05 漏斗 + 06 雷达 -->
      <template v-if="isAdmin">
        <div class="panel-row">
          <section class="panel panel-half">
            <span class="corner-tl"></span><span class="corner-tr"></span>
            <span class="corner-bl"></span><span class="corner-br"></span>
            <div class="panel-title">
              <span class="panel-no">05</span>
              全域转化漏斗
              <span class="title-en">Conversion Funnel</span>
            </div>
            <div ref="funnelEl" class="chart chart-funnel"></div>
          </section>
          <section class="panel panel-half">
            <span class="corner-tl"></span><span class="corner-tr"></span>
            <span class="corner-bl"></span><span class="corner-br"></span>
            <div class="panel-title">
              <span class="panel-no">06</span>
              渠道价值雷达
              <span class="title-en">Channel Value Radar</span>
            </div>
            <div ref="radarEl" class="chart chart-radar"></div>
          </section>
        </div>

        <!-- 07 渠道价值明细 -->
        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">07</span>
            渠道价值明细 · 经营指标
            <span class="title-en">Channel Value Matrix</span>
            <span class="title-tip">健康度 = 流量规模 / 转化率 / ROI 综合评分</span>
          </div>
          <div class="value-table-wrap">
            <table class="value-table">
              <thead>
                <tr>
                  <th>分类</th>
                  <th class="ta-r">访问量</th>
                  <th class="ta-r">占比</th>
                  <th class="ta-r">转化率</th>
                  <th class="ta-r">客单价</th>
                  <th class="ta-r">获客成本</th>
                  <th class="ta-r">预估 GMV</th>
                  <th class="ta-r">ROI</th>
                  <th class="ta-c">健康度</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in metrics" :key="m.category">
                  <td>
                    <span class="cat-dot-s" :style="{ background: catColor(m.category) }"></span>
                    {{ m.category }}
                  </td>
                  <td class="ta-r">{{ fmtNum(m.visits) }}</td>
                  <td class="ta-r">{{ shareOf(m.visits) }}%</td>
                  <td class="ta-r">{{ m.conversionRate }}%</td>
                  <td class="ta-r">¥ {{ fmtNum(m.aov) }}</td>
                  <td class="ta-r">{{ m.cac > 0 ? '¥ ' + m.cac : '免费' }}</td>
                  <td class="ta-r hl-gmv">¥ {{ fmtWan(m.gmv) }} 万</td>
                  <td class="ta-r">{{ m.roi != null ? m.roi : '—' }}</td>
                  <td class="ta-c">
                    <span class="health-badge" :class="'grade-' + healthOf(m).grade">{{ healthOf(m).grade }}</span>
                    <span class="health-score">{{ healthOf(m).score }}分</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>

      <!-- 商家专属：渠道洞察（公开维度） -->
      <section v-else class="panel">
        <span class="corner-tl"></span><span class="corner-tr"></span>
        <span class="corner-bl"></span><span class="corner-br"></span>
        <div class="panel-title">
          <span class="panel-no">05</span>
          渠道洞察 · 商家版
          <span class="title-en">Channel Insights</span>
          <span class="title-tip">基于公开流量结构生成的运营建议</span>
        </div>
        <div class="insight-grid">
          <div v-for="ins in insights" :key="ins.title" class="insight-card" :style="{ '--c': ins.color }">
            <div class="insight-icon">{{ ins.icon }}</div>
            <div class="insight-body">
              <div class="insight-title">{{ ins.title }}</div>
              <div class="insight-text">{{ ins.text }}</div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="traffic-footer">
      HUAWEI DATA COCKPIT · TRAFFIC SOURCE · 数据仅供参考
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'

/* ===== 角色 ===== */
const role = ref(localStorage.getItem('hw_role') || 'MERCHANT')
const isAdmin = computed(() => role.value === 'ADMIN')

/* ===== 状态 ===== */
const loading = ref(true)
const failed = ref(false)
const errorMsg = ref('')
const ready = ref(false)
const sources = ref([])          // 渠道明细
const categorySummary = ref([])  // 分类汇总
const hourlyPulse = ref([])      // 24h 脉搏
const funnelData = ref([])       // 漏斗（管理员）
const metrics = ref([])          // 渠道价值（管理员）

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
    const data = await adminApi.getTraffic()
    sources.value = (data?.sources || []).map(s => ({
      category: s.category || '未分类',
      sourceName: s.sourceName || '',
      visits: Number(s.visits || 0),
      ratio: Number(s.ratio || 0)
    }))
    categorySummary.value = (data?.categorySummary || []).map(c => ({
      category: c.category || '未分类',
      visits: Number(c.visits || 0),
      channels: Number(c.channels || 0),
      share: Number(c.share || 0)
    }))
    hourlyPulse.value = (data?.hourlyPulse || []).map(h => ({
      hour: Number(h.hour || 0),
      visits: Number(h.visits || 0)
    }))
    funnelData.value = (data?.funnel || []).map(f => ({
      stage: f.stage || '',
      count: Number(f.count || 0),
      rate: Number(f.rate || 0)
    }))
    metrics.value = (data?.channelMetrics || []).map(m => ({
      category: m.category || '',
      visits: Number(m.visits || 0),
      conversionRate: Number(m.conversionRate || 0),
      aov: Number(m.aov || 0),
      cac: Number(m.cac || 0),
      gmv: Number(m.gmv || 0),
      cost: Number(m.cost || 0),
      roi: m.roi == null ? null : Number(m.roi)
    }))
  } catch (e) {
    failed.value = true
    errorMsg.value = e?.response?.data?.message || e?.message || '服务异常'
    sources.value = []
  } finally {
    loading.value = false
  }
  if (failed.value) return
  ready.value = true
  await nextTick()
  renderAll()
}

/* ===== 渠道 logo 映射（爬取自各平台官网 favicon） ===== */
const LOGO_MAP = {
  '百度搜索': '/channels/baidu.ico',
  '抖音': '/channels/douyin.ico',
  '官网直达': '/channels/huawei.ico',
  '京东': '/channels/jd.ico',
  '微信视频号': '/channels/weixin.ico',
  '天猫': '/channels/tmall.ico',
  '拼多多': '/channels/pinduoduo.ico',
  'APP内访问': '/channels/huawei.ico',
  '360搜索': '/channels/so360.ico',
  '微博': '/channels/weibo.ico',
  '搜狗搜索': '/channels/sogou.ico',
  '小红书': '/channels/xiaohongshu.ico',
  '门店扫码': '/channels/huawei.ico',
  '体验店预约': '/channels/huawei.ico',
  'B站': '/channels/bilibili.ico',
  '必应': '/channels/bing.ico'
}
const EMOJI_MAP = {
  '必应': '🔍',
  '百度搜索': '🔍', '360搜索': '🔍', '搜狗搜索': '🔍',
  '抖音': '🎵', '微博': '💬', '小红书': '📕', 'B站': '📺', '微信视频号': '💬',
  '京东': '🛒', '天猫': '🐱', '拼多多': '🧧',
  '官网直达': '🌐', 'APP内访问': '📱', '门店扫码': '📇', '体验店预约': '🏪'
}
function logoOf(name) { return LOGO_MAP[name] || '' }
function emojiOf(name) { return EMOJI_MAP[name] || '🛰' }
function onLogoError(e) {
  const img = e.target
  const fb = document.createElement('span')
  fb.className = 'logo-emoji'
  fb.textContent = emojiOf(img.alt)
  img.replaceWith(fb)
}

/* ===== 分类配色（与大屏 TrafficBarChart 一致） ===== */
const CAT_COLORS = {
  '搜索引擎': '#00e5ff',
  '社交媒体': '#7b68ee',
  '电商平台广告': '#00ffa3',
  '直接访问': '#ffcc00',
  '线下引流': '#ff4d6a'
}
const FALLBACK_COLORS = ['#00e5ff', '#7b68ee', '#00ffa3', '#ffcc00', '#ff4d6a', '#38bdf8']
const catColorMap = computed(() => {
  const map = { ...CAT_COLORS }
  let i = 0
  sources.value.forEach(s => {
    if (!map[s.category]) { map[s.category] = FALLBACK_COLORS[i % FALLBACK_COLORS.length]; i++ }
  })
  return map
})
function catColor(cat) { return catColorMap.value[cat] || '#00e5ff' }

/* ===== 通用格式化 ===== */
function fmtNum(v) { return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 }) }
function fmtWan(v) { return (Number(v || 0) / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 1 }) }

/* ===== 排行榜（访问量降序） ===== */
const ranked = computed(() => {
  const sorted = [...sources.value].sort((a, b) => b.visits - a.visits)
  const maxV = Math.max(...sorted.map(r => r.visits), 1)
  return sorted.map(r => ({ ...r, barPct: Math.max(3, Math.round(r.visits / maxV * 100)) }))
})
const categories = computed(() =>
  [...categorySummary.value].sort((a, b) => b.visits - a.visits)
)
const totalVisits = computed(() => sources.value.reduce((a, b) => a + b.visits, 0))
function shareOf(visits) { return ((visits / (totalVisits.value || 1)) * 100).toFixed(1) }

/* ===== 峰值时段 ===== */
const peakHour = computed(() => {
  if (!hourlyPulse.value.length) return { hour: 20, visits: 0 }
  return [...hourlyPulse.value].sort((a, b) => b.visits - a.visits)[0]
})

/* ===== KPI（角色差异化） ===== */
const kpis = computed(() => {
  if (!sources.value.length) return []
  const top = ranked.value[0]
  const catCount = categories.value.length
  if (isAdmin.value) {
    const totalGmv = metrics.value.reduce((a, b) => a + (b.gmv || 0), 0)
    const paid = metrics.value.filter(m => m.cac > 0)
    const avgCac = paid.length ? Math.round(paid.reduce((a, b) => a + b.cac, 0) / paid.length) : 0
    const conv = funnelData.value.length ? funnelData.value[funnelData.value.length - 1].rate : 0
    return [
      { icon: '🛰', color: '#00e5ff', label: '总访问量', value: fmtNum(totalVisits.value), unit: '次', sub: `${sources.value.length} 个渠道合计` },
      { icon: '💰', color: '#00ffa3', label: '预估渠道 GMV', value: '¥ ' + fmtWan(totalGmv), unit: '万', sub: '按渠道转化模型推算' },
      { icon: '🎯', color: '#ffcc00', label: '全域支付转化率', value: conv, unit: '%', sub: '访问 → 支付全链路' },
      { icon: '💳', color: '#7b68ee', label: '平均获客成本', value: '¥ ' + avgCac, sub: `付费渠道均值（${paid.length} 类）` }
    ]
  }
  return [
    { icon: '🛰', color: '#00e5ff', label: '总访问量', value: fmtNum(totalVisits.value), unit: '次', sub: `${sources.value.length} 个渠道合计` },
    { icon: '🧭', color: '#7b68ee', label: '覆盖渠道分类', value: catCount, unit: '类', sub: `${sources.value.length} 个渠道入口` },
    { icon: '🏔', color: '#00ffa3', label: 'TOP 渠道占比', value: Number(top.ratio || 0).toFixed(1), unit: '%', sub: top.sourceName },
    { icon: '⏰', color: '#ffcc00', label: '流量峰值时段', value: pad(peakHour.value.hour) + ':00', sub: '晚间黄金流量窗口' }
  ]
})

/* ===== 健康度评分（管理员价值表） ===== */
function healthOf(m) {
  const maxVisits = Math.max(...metrics.value.map(x => x.visits), 1)
  const scaleScore = (m.visits / maxVisits) * 100
  const convScore = Math.min(100, (m.conversionRate / 3.6) * 100)
  const roiScore = m.roi != null ? Math.min(100, m.roi / 20 * 100) : 85
  const score = Math.round(scaleScore * 0.3 + convScore * 0.35 + roiScore * 0.35)
  const grade = score >= 85 ? 'S' : score >= 70 ? 'A' : score >= 55 ? 'B' : 'C'
  return { score, grade }
}

/* ===== 商家洞察（基于公开流量结构推导） ===== */
const insights = computed(() => {
  if (!ranked.value.length || !categories.value.length) return []
  const top = ranked.value[0]
  const social = categories.value.find(c => c.category === '社交媒体')
  const socialTop = sources.value.filter(s => s.category === '社交媒体').sort((a, b) => b.visits - a.visits)[0]
  const offline = categories.value.find(c => c.category === '线下引流')
  const ecom = categories.value.find(c => c.category === '电商平台广告')
  const list = []
  if (top) {
    list.push({
      icon: '🏔', color: '#00e5ff',
      title: `头部渠道：${top.sourceName}`,
      text: `以 ${Number(top.ratio || 0).toFixed(1)}% 的占比居首（${fmtNum(top.visits)} 次访问），建议保障该渠道的内容供给与预算稳定，守住基本盘。`
    })
  }
  if (social && socialTop) {
    list.push({
      icon: '🎵', color: '#7b68ee',
      title: `社交矩阵：${social.category}占 ${social.share}%`,
      text: `${socialTop.sourceName} 为社交第一大入口，结合 ${pad(peakHour.value.hour)}:00 流量峰值，建议将直播与短视频发布集中在晚间黄金时段。`
    })
  }
  if (ecom) {
    list.push({
      icon: '🛒', color: '#00ffa3',
      title: `电商阵地：占 ${ecom.share}%`,
      text: `京东/天猫/拼多多三大电商平台合计贡献 ${fmtNum(ecom.visits)} 次访问，大促节点前建议提前 2 周铺设商品详情页与评价内容。`
    })
  }
  if (offline) {
    list.push({
      icon: '🏪', color: '#ff4d6a',
      title: `线下反哺：仅占 ${offline.share}%`,
      text: `门店扫码与体验店预约是线下引流抓手，可与新品首发、以旧换新活动联动，把到店客流沉淀为私域复访。`
    })
  }
  return list
})

/* ===== 图表实例 ===== */
const sunburstEl = ref(null)
const roseEl = ref(null)
const pulseEl = ref(null)
const funnelEl = ref(null)
const radarEl = ref(null)
let charts = {}

const tooltipStyle = {
  backgroundColor: 'rgba(3, 11, 34, 0.92)',
  borderColor: 'rgba(0, 229, 255, 0.4)',
  textStyle: { color: '#c9e1ff', fontSize: 12 }
}

/* ===== 01 旭日图：分类 → 渠道两级结构 ===== */
function sunburstOption() {
  const colorMap = catColorMap.value
  const tree = categories.value.map(c => ({
    name: c.category,
    value: c.visits,
    itemStyle: { color: colorMap[c.category] },
    children: sources.value
      .filter(s => s.category === c.category)
      .sort((a, b) => b.visits - a.visits)
      .map(s => ({
        name: s.sourceName,
        value: s.visits,
        itemStyle: { color: colorMap[c.category] + '99' }
      }))
  }))
  return {
    tooltip: {
      ...tooltipStyle,
      formatter: p => {
        const pct = ((p.value / (totalVisits.value || 1)) * 100).toFixed(1)
        return `${p.name}<br/>访问量：<b>${fmtNum(p.value)}</b>（${pct}%）`
      }
    },
    series: [{
      type: 'sunburst',
      radius: ['16%', '92%'],
      center: ['50%', '50%'],
      data: tree,
      sort: null,
      nodeClick: 'rootToNode',
      minAngle: 2,
      label: {
        color: '#c9e1ff',
        fontSize: 10,
        minAngle: 8,
        rotate: 'radial'
      },
      levels: [
        {},
        { r0: '16%', r: '46%', label: { fontSize: 11, fontWeight: 600, rotate: 0 } },
        { r0: '48%', r: '92%', label: { fontSize: 9.5, align: 'right' } }
      ],
      itemStyle: { borderColor: '#060f2a', borderWidth: 1.5, borderRadius: 3 },
      emphasis: { focus: 'ancestor', itemStyle: { shadowBlur: 14, shadowColor: 'rgba(0, 229, 255, 0.45)' } }
    }],
    animationDuration: 900
  }
}

/* ===== 02 玫瑰图：分类占比 ===== */
function roseOption() {
  const colorMap = catColorMap.value
  const data = categories.value.map(c => ({
    name: c.category,
    value: c.visits,
    itemStyle: { color: colorMap[c.category] }
  }))
  return {
    tooltip: {
      ...tooltipStyle,
      formatter: p => {
        const pct = ((p.value / (totalVisits.value || 1)) * 100).toFixed(1)
        return `${p.name}<br/>访问量：<b>${fmtNum(p.value)}</b>（${pct}%）`
      }
    },
    legend: {
      bottom: 0, left: 'center', icon: 'circle',
      itemWidth: 9, itemHeight: 9,
      textStyle: { color: '#7a97c5', fontSize: 10 }
    },
    series: [{
      type: 'pie',
      roseType: 'radius',
      radius: ['18%', '76%'],
      center: ['50%', '46%'],
      itemStyle: { borderColor: '#060f2a', borderWidth: 2, borderRadius: 4 },
      label: {
        color: '#a8c4e8', fontSize: 10,
        formatter: p => p.name + '\n' + ((p.value / (totalVisits.value || 1)) * 100).toFixed(1) + '%'
      },
      labelLine: { length: 6, length2: 8, lineStyle: { color: '#3a5a8c' } },
      emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0, 229, 255, 0.4)' } },
      data
    }],
    animationDuration: 800
  }
}

/* ===== 03 24小时脉搏 ===== */
function pulseOption() {
  const hours = hourlyPulse.value.map(h => pad(h.hour) + ':00')
  const values = hourlyPulse.value.map(h => h.visits)
  const maxIdx = values.indexOf(Math.max(...values))
  return {
    tooltip: {
      ...tooltipStyle,
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: 'rgba(0,229,255,0.4)' } },
      formatter: p => `${p[0].name}<br/>访问量：<b>${fmtNum(p[0].value)}</b> 次`
    },
    grid: { left: 56, right: 24, top: 34, bottom: 30 },
    xAxis: {
      type: 'category',
      data: hours,
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.5)' } } ,
      axisTick: { show: false },
      axisLabel: { color: '#7a97c5', fontSize: 10, interval: 2 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.15)' } },
      axisLabel: {
        color: '#7a97c5', fontSize: 10,
        formatter: v => (v / 10000).toFixed(1) + '万'
      }
    },
    series: [{
      type: 'line',
      data: values,
      smooth: 0.45,
      symbol: 'circle',
      symbolSize: 5,
      showSymbol: false,
      lineStyle: { width: 2.2, color: '#00e5ff', shadowBlur: 10, shadowColor: 'rgba(0, 229, 255, 0.5)' },
      itemStyle: { color: '#00e5ff', borderColor: '#0a1830', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 229, 255, 0.35)' },
          { offset: 1, color: 'rgba(0, 229, 255, 0.02)' }
        ])
      },
      markPoint: {
        symbolSize: 46,
        itemStyle: { color: '#ffcc00', shadowBlur: 12, shadowColor: 'rgba(255, 204, 0, 0.5)' },
        label: { color: '#1a1400', fontSize: 10, fontWeight: 700 },
        data: [{ coord: maxIdx, value: '峰值' }]
      },
      markArea: {
        silent: true,
        itemStyle: { color: 'rgba(123, 104, 238, 0.10)' },
        label: { color: '#7b68ee', fontSize: 10, position: 'insideTop' },
        data: [[{ name: '晚间黄金时段', xAxis: '19:00' }, { xAxis: '22:00' }]]
      },
      animationDuration: 1000
    }]
  }
}

/* ===== 05 转化漏斗（管理员） ===== */
function funnelOption() {
  const colors = ['#00e5ff', '#38bdf8', '#7b68ee', '#ffcc00', '#00ffa3']
  return {
    tooltip: {
      ...tooltipStyle,
      formatter: p => `${p.name}<br/>人数：<b>${fmtNum(p.value)}</b><br/>转化率：${funnelData.value[p.dataIndex]?.rate ?? '—'}%`
    },
    series: [{
      type: 'funnel',
      left: '8%',
      right: '8%',
      top: 12,
      bottom: 12,
      minSize: '22%',
      maxSize: '100%',
      sort: 'none',
      gap: 3,
      label: {
        show: true,
        position: 'inside',
        color: '#e8f6ff',
        fontSize: 11,
        formatter: p => p.name + '  ' + (funnelData.value[p.dataIndex]?.rate ?? '') + '%'
      },
      itemStyle: { borderColor: '#060f2a', borderWidth: 1, borderRadius: 3 },
      emphasis: { label: { fontSize: 12 } },
      data: funnelData.value.map((f, i) => ({
        name: f.stage,
        value: f.count,
        itemStyle: { color: colors[i % colors.length] }
      })),
      animationDuration: 800,
      animationDelay: i => i * 90
    }]
  }
}

/* ===== 06 渠道价值雷达（管理员） ===== */
function radarOption() {
  const ms = metrics.value
  if (!ms.length) return {}
  const maxVisits = Math.max(...ms.map(m => m.visits), 1)
  const maxConv = Math.max(...ms.map(m => m.conversionRate), 0.1)
  const maxAov = Math.max(...ms.map(m => m.aov), 1)
  const maxGmv = Math.max(...ms.map(m => m.gmv), 1)
  const dims = ['流量规模', '转化能力', '客单价值', 'GMV 贡献', '成本效率']
  const indicators = dims.map(d => ({ name: d, max: 100 }))
  const seriesData = ms.map(m => {
    const colorMap = catColorMap.value
    const c = colorMap[m.category] || '#00e5ff'
    const cacScore = m.cac > 0 ? Math.min(100, Math.max(0, 100 - (m.cac / 45) * 100)) : 100
    return {
      name: m.category,
      value: [
        Math.round(m.visits / maxVisits * 100),
        Math.round(m.conversionRate / maxConv * 100),
        Math.round(m.aov / maxAov * 100),
        Math.round(m.gmv / maxGmv * 100),
        Math.round(cacScore)
      ],
      lineStyle: { color: c, width: 2 },
      itemStyle: { color: c },
      areaStyle: { color: c + '30' },
      symbolSize: 5
    }
  })
  return {
    tooltip: { ...tooltipStyle },
    legend: {
      bottom: 0, left: 'center', icon: 'circle',
      itemWidth: 9, itemHeight: 9,
      textStyle: { color: '#7a97c5', fontSize: 10 }
    },
    radar: {
      indicator: indicators,
      center: ['50%', '48%'],
      radius: '62%',
      splitNumber: 4,
      axisName: { color: '#a8c4e8', fontSize: 10.5 },
      splitLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.35)' } },
      splitArea: { areaStyle: { color: ['rgba(0,229,255,0.02)', 'rgba(0,229,255,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.35)' } }
    },
    series: [{ type: 'radar', data: seriesData }],
    animationDuration: 800
  }
}

/* ===== 初始化图表（ready 渲染出容器后调用） ===== */
function initCharts() {
  if (charts.sunburst) return
  if (sunburstEl.value) charts.sunburst = echarts.init(sunburstEl.value)
  if (roseEl.value) charts.rose = echarts.init(roseEl.value)
  if (pulseEl.value) charts.pulse = echarts.init(pulseEl.value)
  if (funnelEl.value && isAdmin.value) charts.funnel = echarts.init(funnelEl.value)
  if (radarEl.value && isAdmin.value) charts.radar = echarts.init(radarEl.value)
}

/* ===== 渲染全部图表 ===== */
function renderAll() {
  if (!sources.value.length) return
  initCharts()
  if (charts.sunburst) charts.sunburst.setOption(sunburstOption(), true)
  if (charts.rose) charts.rose.setOption(roseOption(), true)
  if (charts.pulse) charts.pulse.setOption(pulseOption(), true)
  if (charts.funnel && isAdmin.value && funnelData.value.length) charts.funnel.setOption(funnelOption(), true)
  if (charts.radar && isAdmin.value && metrics.value.length) charts.radar.setOption(radarOption(), true)
}

const resizeHandler = () => Object.values(charts).forEach(c => c && c.resize())

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  window.addEventListener('resize', resizeHandler)
  loadData()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', resizeHandler)
  Object.values(charts).forEach(c => c && c.dispose())
  charts = {}
})
</script>

<style scoped>
/* ===== 页面容器：可滚动长页 ===== */
.traffic-page {
  position: relative;
  height: 100vh;
  background: #060f2a;
  color: #c9e1ff;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ===== 背景层 ===== */
.bg-effects { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.grid-bg {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(0, 229, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 229, 255, 0.04) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, #000 30%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, #000 30%, transparent 100%);
}
.radial-bg {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse 60% 45% at 50% 0%, rgba(0, 229, 255, 0.10), transparent 70%),
              radial-gradient(ellipse 50% 40% at 85% 60%, rgba(123, 104, 238, 0.08), transparent 70%);
}
.vignette {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse 120% 90% at 50% 50%, transparent 60%, rgba(2, 6, 20, 0.8) 100%);
}

/* ===== 顶部栏 ===== */
.traffic-header {
  position: sticky; top: 0; z-index: 20;
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px;
  padding: 14px 22px;
  background: rgba(4, 12, 36, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 229, 255, 0.18);
}
.header-left, .header-right { display: flex; align-items: center; gap: 8px; flex: 1; }
.header-right { justify-content: flex-end; }
.header-center { text-align: center; }

.back-link {
  padding: 5px 12px;
  font-size: 12px; color: #8fb8e8; text-decoration: none;
  border: 1px solid rgba(58, 90, 140, 0.5);
  border-radius: 4px;
  background: rgba(0, 229, 255, 0.04);
  white-space: nowrap;
  transition: all 0.2s;
}
.back-link:hover { color: #00e5ff; border-color: rgba(0, 229, 255, 0.5); box-shadow: 0 0 12px rgba(0, 229, 255, 0.2); }

.page-title { display: flex; flex-direction: column; gap: 2px; margin: 0; }
.title-zh {
  font-size: 21px; font-weight: 700; letter-spacing: 4px;
  background: linear-gradient(90deg, #aef4ff, #00e5ff 45%, #7b68ee);
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
}
.title-en { font-size: 10px; letter-spacing: 6px; color: #4d6fa3; }

.role-badge {
  padding: 4px 10px;
  font-size: 12px; font-weight: 600;
  border-radius: 4px; white-space: nowrap;
}
.role-admin { color: #ffcc00; background: rgba(255, 204, 0, 0.10); border: 1px solid rgba(255, 204, 0, 0.4); }
.role-merchant { color: #00ffa3; background: rgba(0, 255, 163, 0.08); border: 1px solid rgba(0, 255, 163, 0.35); }

.header-time {
  font-size: 12px; color: #6f8cb8; font-family: 'Rajdhani', 'Consolas', monospace;
  white-space: nowrap;
}

/* ===== 状态 ===== */
.state-box {
  position: relative; z-index: 1;
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 12px;
  min-height: 60vh;
}
.state-icon { font-size: 42px; }
.state-text { color: #8fb8e8; font-size: 14px; }
.state-sub { color: #4d6fa3; font-size: 12px; }

.loading-dots {
  width: 44px; height: 44px;
  border: 3px solid rgba(0, 229, 255, 0.15);
  border-top-color: #00e5ff;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.retry-btn {
  padding: 7px 22px;
  font-size: 13px; color: #00e5ff;
  background: rgba(0, 229, 255, 0.08);
  border: 1px solid rgba(0, 229, 255, 0.45);
  border-radius: 4px; cursor: pointer;
  transition: all 0.2s;
}
.retry-btn:hover { background: rgba(0, 229, 255, 0.18); box-shadow: 0 0 14px rgba(0, 229, 255, 0.3); }

/* ===== 主体 ===== */
.traffic-body {
  position: relative; z-index: 1;
  width: min(1500px, 96vw);
  margin: 0 auto;
  padding: 18px 0 30px;
  display: flex; flex-direction: column; gap: 16px;
}

/* 信息条 */
.info-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.info-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 14px;
  font-size: 12px; color: #8fb8e8;
  border: 1px solid rgba(58, 90, 140, 0.4);
  border-radius: 20px;
  background: rgba(4, 12, 36, 0.6);
}
.info-chip b { color: #00e5ff; font-family: 'Rajdhani', monospace; font-size: 13px; }
.chip-icon { font-size: 13px; }
.chip-sep { color: #3a5a8c; }
.chip-lock { color: #ffd28a; border-color: rgba(255, 204, 0, 0.3); background: rgba(255, 204, 0, 0.05); }

/* KPI */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.kpi-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 18px;
  background: linear-gradient(135deg, rgba(10, 24, 56, 0.85), rgba(6, 15, 42, 0.9));
  border: 1px solid rgba(58, 90, 140, 0.35);
  border-left: 3px solid var(--c);
  border-radius: 10px;
  transition: transform 0.2s, box-shadow 0.2s;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35), 0 0 18px color-mix(in srgb, var(--c) 20%, transparent);
}
.kpi-icon {
  width: 46px; height: 46px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
  background: color-mix(in srgb, var(--c) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 35%, transparent);
  border-radius: 10px;
}
.kpi-label { font-size: 12px; color: #6f8cb8; margin-bottom: 3px; }
.kpi-value {
  font-size: 24px; font-weight: 700; color: #e8f6ff;
  font-family: 'Rajdhani', 'Consolas', monospace;
  line-height: 1.1;
}
.kpi-unit { font-size: 12px; font-weight: 400; color: #6f8cb8; margin-left: 4px; }
.kpi-sub { font-size: 11px; color: #4d6fa3; margin-top: 3px; }

/* 面板通用 */
.panel {
  position: relative;
  background: linear-gradient(160deg, rgba(10, 24, 56, 0.75), rgba(5, 13, 36, 0.92));
  border: 1px solid rgba(58, 90, 140, 0.35);
  border-radius: 10px;
  padding: 14px 16px 16px;
  display: flex; flex-direction: column;
  min-height: 0;
}
.panel-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
}
.panel-row.same { grid-template-columns: 1fr 1fr; }

/* 四角装饰 */
.corner-tl, .corner-tr, .corner-bl, .corner-br {
  position: absolute; width: 10px; height: 10px;
  border-color: rgba(0, 229, 255, 0.55); border-style: solid;
}
.corner-tl { top: -1px; left: -1px; border-width: 2px 0 0 2px; border-radius: 3px 0 0 0; }
.corner-tr { top: -1px; right: -1px; border-width: 2px 2px 0 0; border-radius: 0 3px 0 0; }
.corner-bl { bottom: -1px; left: -1px; border-width: 0 0 2px 2px; border-radius: 0 0 0 3px; }
.corner-br { bottom: -1px; right: -1px; border-width: 0 2px 2px 0; border-radius: 0 0 3px 0; }

.panel-title {
  display: flex; align-items: baseline; gap: 8px;
  font-size: 15px; font-weight: 600; color: #e8f6ff;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.panel-no {
  font-family: 'Rajdhani', monospace;
  font-size: 13px; color: #00e5ff; opacity: 0.85;
}
.title-en { font-size: 10px; letter-spacing: 2px; color: #4d6fa3; }
.title-tip { font-size: 11px; color: #5f7ca8; font-weight: 400; margin-left: auto; }

/* 分类图例 */
.cat-legend { display: inline-flex; gap: 10px; margin-left: auto; flex-wrap: wrap; }
.cat-dot {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; color: #8fb8e8; font-weight: 400;
}
.cat-dot::before { content: ''; width: 8px; height: 8px; border-radius: 50%; background: inherit; }
.cat-dot { padding-left: 12px; position: relative; }
.cat-dot::before { position: absolute; left: 0; }

/* 图表容器（显式高度，防止被 flex 折叠为 0） */
.chart { width: 100%; }
.chart-sunburst { height: 400px; }
.chart-rose { height: 400px; }
.chart-pulse { height: 300px; }
.chart-funnel { height: 340px; }
.chart-radar { height: 340px; }

/* ===== 排行榜 ===== */
.rank-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 22px;
}
.rank-row {
  display: flex; align-items: center; gap: 10px;
  padding: 7px 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  transition: all 0.15s;
}
.rank-row:hover {
  background: rgba(0, 229, 255, 0.04);
  border-color: rgba(0, 229, 255, 0.18);
}
.rank-no {
  width: 24px; text-align: center; flex-shrink: 0;
  font-family: 'Rajdhani', monospace;
  font-size: 14px; font-weight: 700; color: #4d6fa3;
}
.rank-no.top3 { color: #ffcc00; text-shadow: 0 0 8px rgba(255, 204, 0, 0.4); }
.rank-logo {
  width: 30px; height: 30px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 8px;
  overflow: hidden;
}
.rank-logo img { width: 20px; height: 20px; object-fit: contain; }
.logo-emoji { font-size: 16px; }

.rank-info { flex: 1; min-width: 0; }
.rank-name-line { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.rank-name { font-size: 13px; color: #e8f6ff; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rank-cat {
  font-size: 10px; padding: 1px 7px;
  border-radius: 10px; border: 1px solid; white-space: nowrap;
}
.rank-bar-track {
  height: 5px; border-radius: 3px;
  background: rgba(58, 90, 140, 0.18);
  overflow: hidden;
}
.rank-bar { height: 100%; border-radius: 3px; transition: width 0.6s ease; }

.rank-value { text-align: right; flex-shrink: 0; min-width: 74px; }
.rv-main {
  font-family: 'Rajdhani', 'Consolas', monospace;
  font-size: 15px; font-weight: 700; color: #aef4ff;
}
.rv-sub { font-size: 10.5px; color: #4d6fa3; }

/* ===== 价值明细表（管理员） ===== */
.value-table-wrap { overflow-x: auto; }
.value-table {
  width: 100%; border-collapse: collapse;
  font-size: 12.5px;
}
.value-table th {
  padding: 9px 12px;
  color: #6f8cb8; font-weight: 600; font-size: 11.5px;
  border-bottom: 1px solid rgba(58, 90, 140, 0.45);
  white-space: nowrap;
}
.value-table td {
  padding: 10px 12px;
  color: #c9e1ff;
  border-bottom: 1px solid rgba(58, 90, 140, 0.16);
  white-space: nowrap;
}
.value-table tr:hover td { background: rgba(0, 229, 255, 0.035); }
.ta-r { text-align: right; }
.ta-c { text-align: center; }
.hl-gmv { color: #00ffa3; font-weight: 700; font-family: 'Rajdhani', monospace; }
.cat-dot-s {
  display: inline-block; width: 8px; height: 8px;
  border-radius: 50%; margin-right: 7px; vertical-align: 1px;
}
.health-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px;
  font-weight: 800; font-size: 13px;
  border-radius: 6px;
}
.grade-S { color: #ffcc00; background: rgba(255, 204, 0, 0.12); border: 1px solid rgba(255, 204, 0, 0.55); }
.grade-A { color: #00ffa3; background: rgba(0, 255, 163, 0.10); border: 1px solid rgba(0, 255, 163, 0.45); }
.grade-B { color: #00e5ff; background: rgba(0, 229, 255, 0.10); border: 1px solid rgba(0, 229, 255, 0.45); }
.grade-C { color: #8fb8e8; background: rgba(143, 184, 232, 0.08); border: 1px solid rgba(143, 184, 232, 0.4); }
.health-score { font-size: 10.5px; color: #4d6fa3; margin-left: 6px; }

/* ===== 商家洞察卡 ===== */
.insight-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.insight-card {
  display: flex; gap: 14px;
  padding: 16px 18px;
  background: rgba(6, 15, 42, 0.6);
  border: 1px solid rgba(58, 90, 140, 0.35);
  border-left: 3px solid var(--c);
  border-radius: 10px;
  transition: transform 0.2s;
}
.insight-card:hover { transform: translateY(-2px); }
.insight-icon {
  width: 42px; height: 42px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
  background: color-mix(in srgb, var(--c) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 35%, transparent);
  border-radius: 10px;
}
.insight-title { font-size: 14px; font-weight: 700; color: #e8f6ff; margin-bottom: 6px; }
.insight-text { font-size: 12px; color: #8fb8e8; line-height: 1.75; }

/* ===== 页脚 ===== */
.traffic-footer {
  position: relative; z-index: 1;
  text-align: center;
  padding: 18px 0 22px;
  font-size: 10.5px; letter-spacing: 3px; color: #3a5a8c;
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .panel-row { grid-template-columns: 1fr; }
  .panel-row.same { grid-template-columns: 1fr; }
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .rank-list { grid-template-columns: 1fr; }
  .insight-grid { grid-template-columns: 1fr; }
  .header-center .title-zh { font-size: 16px; letter-spacing: 2px; }
}
@media (max-width: 700px) {
  .kpi-grid { grid-template-columns: 1fr; }
  .traffic-header { flex-wrap: wrap; }
  .chart-sunburst, .chart-rose { height: 320px; }
}
</style>
