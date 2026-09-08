<template>
  <div class="profile-page">
    <!-- 背景层 -->
    <div class="bg-effects">
      <div class="grid-bg"></div>
      <div class="radial-bg"></div>
      <div class="vignette"></div>
    </div>

    <!-- 顶部栏（sticky） -->
    <header class="profile-header">
      <div class="header-left">
        <router-link to="/cockpit" class="back-link" title="返回数据大屏">🖥 返回大屏</router-link>
        <router-link v-if="!isAdmin" to="/admin/dashboard" class="back-link back-sub" title="返回管理后台">⬡ 后台</router-link>
        <router-link to="/trend" class="back-link back-sub" title="销售趋势分析">📈 趋势分析</router-link>
        <router-link to="/hotmodels" class="back-link back-sub" title="热销机型分析">🏆 热销机型</router-link>
        <router-link to="/traffic" class="back-link back-sub" title="流量来源分析">🛰 流量分析</router-link>
      </div>
      <div class="header-center">
        <h1 class="page-title">
          <span class="title-zh">用户画像分析</span>
          <span class="title-en">USER&nbsp;PROFILE&nbsp;ANALYSIS</span>
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
      <div class="state-text">画像数据加载中…</div>
    </div>
    <div v-else-if="failed" class="state-box">
      <div class="state-icon">⚠️</div>
      <div class="state-text">数据加载失败：{{ errorMsg }}</div>
      <button class="retry-btn" @click="loadData">重新加载</button>
    </div>
    <div v-else-if="ready && !ages.length" class="state-box">
      <div class="state-icon">📭</div>
      <div class="state-text">暂无用户画像数据</div>
      <div class="state-sub">可由管理员在「数据导入」页面上传 user_profile 表 Excel</div>
    </div>

    <!-- 主体：可滚动长页 -->
    <main v-else-if="ready" class="profile-body">
      <!-- 信息条 -->
      <div class="info-bar">
        <span class="info-chip">
          <span class="chip-icon">👥</span>
          总用户 <b>{{ fmtNum(totalUsers) }}</b> 人 · 覆盖 <b>{{ ages.length }}</b> 个年龄段
          <span class="chip-sep">·</span> 数据来源：user_profile 榜单
        </span>
        <span v-if="!isAdmin" class="info-chip chip-lock">
          <span class="chip-icon">🔒</span>
          商家视图：消费分层/客单价/ARPU 等经营指标不可见，以下为人群结构分析
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

      <!-- 01 典型用户画像卡（创意核心） -->
      <section class="panel panel-persona">
        <span class="corner-tl"></span><span class="corner-tr"></span>
        <span class="corner-bl"></span><span class="corner-br"></span>
        <div class="panel-title">
          <span class="panel-no">01</span>
          典型用户画像 · Persona
          <span class="title-en">Persona Card</span>
          <span class="title-tip">基于主力人群（占比最高年龄段 × 主导性别）自动生成</span>
        </div>
        <div class="persona-card">
          <div class="persona-avatar">
            <div class="avatar-ring">
              <span class="avatar-emoji">{{ personaEmoji }}</span>
            </div>
            <div class="persona-name">{{ persona.gender }} · {{ persona.ageRange }}</div>
            <div class="persona-share">占整体用户 {{ persona.sharePct }}%</div>
          </div>
          <div class="persona-attrs">
            <div class="attr-row">
              <div class="attr-item">
                <span class="attr-icon">🕘</span>
                <div class="attr-text">
                  <div class="attr-label">活跃时段</div>
                  <div class="attr-value">{{ persona.activeHours }}</div>
                </div>
              </div>
              <div class="attr-item">
                <span class="attr-icon">📱</span>
                <div class="attr-text">
                  <div class="attr-label">偏好机型</div>
                  <div class="attr-value">{{ persona.devicePref }}</div>
                </div>
              </div>
              <div class="attr-item">
                <span class="attr-icon">💴</span>
                <div class="attr-text">
                  <div class="attr-label">主流价位</div>
                  <div class="attr-value">{{ persona.priceBand }}</div>
                </div>
              </div>
              <div class="attr-item">
                <span class="attr-icon">👥</span>
                <div class="attr-text">
                  <div class="attr-label">人群规模</div>
                  <div class="attr-value">{{ fmtNum(persona.users) }} 人</div>
                </div>
              </div>
            </div>
            <div class="tag-cloud">
              <span v-for="t in personaTags" :key="t" class="tag-chip">{{ t }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 02 性别构成 + 03 年龄结构 -->
      <div class="panel-row">
        <section class="panel panel-half">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">02</span>
            性别构成
            <span class="title-en">Gender Split</span>
          </div>
          <div ref="genderEl" class="chart chart-gender"></div>
        </section>
        <section class="panel panel-half">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">03</span>
            年龄结构
            <span class="title-en">Age Distribution</span>
          </div>
          <div ref="ageEl" class="chart chart-age"></div>
        </section>
      </div>

      <!-- 04 年龄×性别 蝴蝶图 -->
      <section class="panel">
        <span class="corner-tl"></span><span class="corner-tr"></span>
        <span class="corner-bl"></span><span class="corner-br"></span>
        <div class="panel-title">
          <span class="panel-no">04</span>
          年龄 × 性别 · 蝴蝶图
          <span class="title-en">Butterfly Chart</span>
          <span class="title-tip">左男右女 · 悬停查看各年龄段性别构成</span>
        </div>
        <div ref="butterflyEl" class="chart chart-butterfly"></div>
      </section>

      <!-- 05 分龄活跃时钟（热力图） -->
      <section class="panel">
        <span class="corner-tl"></span><span class="corner-tr"></span>
        <span class="corner-bl"></span><span class="corner-br"></span>
        <div class="panel-title">
          <span class="panel-no">05</span>
          分龄活跃时钟
          <span class="title-en">Age × Hour Heatmap</span>
          <span class="title-tip">不同年龄段的 24 小时活跃节律：年轻人夜猫、中年双峰、银发晨型</span>
        </div>
        <div ref="heatmapEl" class="chart chart-heatmap"></div>
      </section>

      <!-- 管理员专属：06 消费分层金字塔 + 07 年龄价值气泡 -->
      <template v-if="isAdmin">
        <div class="panel-row">
          <section class="panel panel-half">
            <span class="corner-tl"></span><span class="corner-tr"></span>
            <span class="corner-bl"></span><span class="corner-br"></span>
            <div class="panel-title">
              <span class="panel-no">06</span>
              消费能力分层
              <span class="title-en">Value Tiers</span>
            </div>
            <div ref="pyramidEl" class="chart chart-pyramid"></div>
          </section>
          <section class="panel panel-half">
            <span class="corner-tl"></span><span class="corner-tr"></span>
            <span class="corner-bl"></span><span class="corner-br"></span>
            <div class="panel-title">
              <span class="panel-no">07</span>
              年龄价值矩阵
              <span class="title-en">Age Value Bubble</span>
            </div>
            <div ref="bubbleEl" class="chart chart-bubble"></div>
          </section>
        </div>

        <!-- 08 分层明细表 -->
        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">08</span>
            消费分层明细 · 经营指标
            <span class="title-en">Tier Matrix</span>
          </div>
          <div class="value-table-wrap">
            <table class="value-table">
              <thead>
                <tr>
                  <th>分层</th>
                  <th class="ta-r">用户数</th>
                  <th class="ta-r">占比</th>
                  <th class="ta-r">ARPU</th>
                  <th class="ta-r">复购率</th>
                  <th class="ta-r">分层 GMV 贡献</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="t in tierRows" :key="t.tier">
                  <td>
                    <span class="tier-dot" :style="{ background: t.color }"></span>
                    {{ t.tier }}
                  </td>
                  <td class="ta-r">{{ fmtNum(t.users) }}</td>
                  <td class="ta-r">{{ t.ratio }}%</td>
                  <td class="ta-r hl-gmv">¥ {{ fmtNum(t.arpu) }}</td>
                  <td class="ta-r">{{ t.repurchase }}%</td>
                  <td class="ta-r">
                    <div class="gmv-bar-track">
                      <div class="gmv-bar" :style="{ width: t.gmvPct + '%', background: t.color }"></div>
                    </div>
                    <span class="gmv-pct">{{ t.gmvPct }}%</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>

      <!-- 商家专属：06 兴趣雷达 + 07 运营洞察 -->
      <template v-else>
        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">06</span>
            分龄兴趣偏好 · 雷达
            <span class="title-en">Interest Radar</span>
            <span class="title-tip">各年龄段兴趣倾向（满分 100），指导内容与选品方向</span>
          </div>
          <div ref="radarEl" class="chart chart-radar"></div>
        </section>

        <section class="panel">
          <span class="corner-tl"></span><span class="corner-tr"></span>
          <span class="corner-bl"></span><span class="corner-br"></span>
          <div class="panel-title">
            <span class="panel-no">07</span>
            人群运营洞察 · 商家版
            <span class="title-en">Audience Insights</span>
            <span class="title-tip">基于人群结构生成的运营建议</span>
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
      </template>
    </main>

    <!-- 页脚 -->
    <footer class="profile-footer">
      HUAWEI DATA COCKPIT · USER PROFILE · 数据仅供参考
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
const genders = ref([])
const ages = ref([])
const ageGender = ref([])
const persona = ref({})
const ageHourly = ref([])
const interestDims = ref([])
const ageInterests = ref([])
const consumptionTiers = ref([])
const ageValue = ref([])

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
    const data = await adminApi.getProfile()
    genders.value = (data?.genders || []).map(g => ({
      profileName: g.profileName || '',
      userCount: Number(g.userCount || 0),
      ratio: Number(g.ratio || 0)
    }))
    ages.value = (data?.ages || []).map(a => ({
      profileName: a.profileName || '',
      userCount: Number(a.userCount || 0),
      ratio: Number(a.ratio || 0)
    }))
    ageGender.value = (data?.ageGender || []).map(r => ({
      ageRange: r.ageRange || '',
      users: Number(r.users || 0),
      maleCount: Number(r.maleCount || 0),
      femaleCount: Number(r.femaleCount || 0),
      malePct: Number(r.malePct || 0),
      femalePct: Number(r.femalePct || 0)
    }))
    persona.value = data?.persona || {}
    ageHourly.value = (data?.ageHourly || []).map(r => ({
      ageRange: r.ageRange || '',
      hours: (r.hours || []).map(Number)
    }))
    interestDims.value = data?.interestDims || []
    ageInterests.value = (data?.ageInterests || []).map(r => ({
      ageRange: r.ageRange || '',
      scores: (r.scores || []).map(Number)
    }))
    consumptionTiers.value = (data?.consumptionTiers || []).map(t => ({
      tier: t.tier || '',
      users: Number(t.users || 0),
      ratio: Number(t.ratio || 0),
      arpu: Number(t.arpu || 0),
      repurchase: Number(t.repurchase || 0)
    }))
    ageValue.value = (data?.ageValue || []).map(v => ({
      ageRange: v.ageRange || '',
      users: Number(v.users || 0),
      conversionRate: Number(v.conversionRate || 0),
      aov: Number(v.aov || 0),
      gmv: Number(v.gmv || 0)
    }))
  } catch (e) {
    failed.value = true
    errorMsg.value = e?.response?.data?.message || e?.message || '服务异常'
  } finally {
    loading.value = false
  }
  if (failed.value) return
  ready.value = true
  await nextTick()
  renderAll()
}

/* ===== 通用格式化 ===== */
function fmtNum(v) { return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 }) }
function fmtWan(v) { return (Number(v || 0) / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 1 }) }

/* ===== 派生指标 ===== */
const totalUsers = computed(() => genders.value.reduce((a, b) => a + b.userCount, 0))
const mainAge = computed(() => [...ages.value].sort((a, b) => b.ratio - a.ratio)[0] || {})
const maleRow = computed(() => genders.value.find(g => g.profileName.includes('男')) || {})
const femaleRow = computed(() => genders.value.find(g => g.profileName.includes('女')) || {})
const malePct = computed(() => maleRow.value.ratio || 0)

/* Persona 表现层 */
const personaEmoji = computed(() => {
  const g = persona.value.gender || ''
  const a = persona.value.ageRange || ''
  if (g.includes('男')) {
    if (a.includes('55') || a.includes('45')) return '👨‍🦳'
    if (a.includes('18岁以下') || a === '18-24岁') return '🧑‍💻'
    return '👨‍💼'
  }
  if (a.includes('55') || a.includes('45')) return '👩‍🦳'
  if (a.includes('18岁以下') || a === '18-24岁') return '👩‍🎓'
  return '👩‍💼'
})
const personaTags = computed(() => persona.value.tags || [])

/* ===== KPI（角色差异化） ===== */
const kpis = computed(() => {
  if (!ages.value.length) return []
  const main = mainAge.value
  const youth = ages.value.filter(a => a.profileName.includes('18') || a.profileName.includes('24'))
    .reduce((s, a) => s + a.ratio, 0)
  if (isAdmin.value) {
    const diamond = consumptionTiers.value[0] || {}
    const high = consumptionTiers.value.slice(0, 3).reduce((s, t) => s + t.ratio, 0)
    const totalGmv = ageValue.value.reduce((s, v) => s + v.gmv, 0)
    return [
      { icon: '👥', color: '#00d4ff', label: '总用户规模', value: fmtWan(totalUsers.value), unit: '万', sub: `${ages.value.length} 个年龄段合计` },
      { icon: '💎', color: '#7b68ee', label: '高价值用户占比', value: high.toFixed(1), unit: '%', sub: '钻石+铂金+黄金三层合计' },
      { icon: '💰', color: '#00ffa3', label: '预估总 GMV', value: '¥ ' + fmtWan(totalGmv), unit: '万', sub: '按年龄转化模型推算' },
      { icon: '🔄', color: '#ffcc00', label: '钻石层复购率', value: diamond.repurchase ?? '—', unit: '%', sub: `${fmtWan(diamond.users)} 万核心用户` }
    ]
  }
  return [
    { icon: '👥', color: '#00d4ff', label: '总用户规模', value: fmtWan(totalUsers.value), unit: '万', sub: `${ages.value.length} 个年龄段合计` },
    { icon: '🎯', color: '#ffcc00', label: '主力人群占比', value: main.ratio, unit: '%', sub: main.profileName },
    { icon: '⚖️', color: '#00ffa3', label: '男性用户占比', value: malePct.value, unit: '%', sub: `女性 ${100 - malePct.value}%` },
    { icon: '🌱', color: '#7b68ee', label: 'Z 世代占比', value: youth.toFixed(1), unit: '%', sub: '18-24 岁年轻客群' }
  ]
})

/* ===== 分层表衍生（管理员） ===== */
const TIER_COLORS = ['#c9a1ff', '#7b68ee', '#00d4ff', '#00ffa3', '#5f8bb0']
const tierRows = computed(() => {
  const totalGmv = consumptionTiers.value.reduce((s, t) => s + t.users * t.arpu, 0) || 1
  return consumptionTiers.value.map((t, i) => {
    const gmvShare = (t.users * t.arpu / totalGmv) * 100
    return { ...t, color: TIER_COLORS[i % TIER_COLORS.length], gmvPct: gmvShare.toFixed(1) }
  })
})

/* ===== 商家洞察（基于人群结构推导） ===== */
const insights = computed(() => {
  if (!ages.value.length) return []
  const main = mainAge.value
  const youth = ages.value.find(a => a.profileName.includes('25-34'))
  const silver = ages.value.find(a => a.profileName.includes('55'))
  const young = ages.value.find(a => a.profileName.includes('18-24'))
  const list = []
  if (main.profileName) {
    list.push({
      icon: '🎯', color: '#ffcc00',
      title: `主力客群：${main.profileName}（${main.ratio}%）`,
      text: `${fmtWan(main.userCount)} 万用户构成基本盘，偏好旗舰机型与影像功能，建议将新品首发与以旧换新资源优先投向该人群。`
    })
  }
  if (young) {
    list.push({
      icon: '🌙', color: '#7b68ee',
      title: `夜活力客群：18-24 岁占 ${young.ratio}%`,
      text: '该人群活跃高峰在 20:00-次日 02:00，游戏与社交属性强，建议晚间时段投放短视频内容并主推 nova 系列性价比组合。'
    })
  }
  if (silver) {
    list.push({
      icon: '🌅', color: '#00ffa3',
      title: `银发潜力：55 岁以上占 ${silver.ratio}%`,
      text: '清晨 5-8 点为活跃高峰，关注健康与性价比，建议以门店体验+耐用续航为卖点，配合家人推荐场景做口碑营销。'
    })
  }
  const male = maleRow.value
  if (male.profileName) {
    list.push({
      icon: '⚖️', color: '#00d4ff',
      title: `性别结构：男性 ${male.ratio}% / 女性 ${(100 - male.ratio).toFixed(0)}%`,
      text: '男性偏商务科技属性，女性偏影像设计与颜值，建议内容素材分双线制作：Mate 商务叙事 + nova/影像美学叙事。'
    })
  }
  return list
})

/* ===== 图表实例 ===== */
const genderEl = ref(null)
const ageEl = ref(null)
const butterflyEl = ref(null)
const heatmapEl = ref(null)
const pyramidEl = ref(null)
const bubbleEl = ref(null)
const radarEl = ref(null)
let charts = {}

const tooltipStyle = {
  backgroundColor: 'rgba(3, 11, 34, 0.92)',
  borderColor: 'rgba(0, 212, 255, 0.4)',
  textStyle: { color: '#c9e1ff', fontSize: 12 }
}

/* ===== 02 性别构成：对比环 ===== */
function genderOption() {
  const data = genders.value.map(g => ({
    name: g.profileName,
    value: g.userCount,
    itemStyle: { color: g.profileName.includes('男') ? '#00a5ff' : '#ff6b9d' }
  }))
  return {
    tooltip: { ...tooltipStyle, trigger: 'item', formatter: p => `${p.name}：<b>${fmtNum(p.value)}</b> 人（${p.percent}%）` },
    legend: {
      bottom: 6, left: 'center', icon: 'circle',
      itemWidth: 10, itemHeight: 10,
      textStyle: { color: '#7a97c5', fontSize: 11 }
    },
    series: [{
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: false,
      itemStyle: { borderColor: '#060f2a', borderWidth: 3, borderRadius: 5 },
      label: {
        show: true, position: 'center',
        formatter: () => '性别\n构成',
        color: '#7a97c5', fontSize: 13, lineHeight: 18
      },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 700, color: '#e8f6ff' },
        itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0, 212, 255, 0.4)' }
      },
      labelLine: { show: false },
      data
    }]
  }
}

/* ===== 03 年龄结构：横向条形 ===== */
function ageOption() {
  const sorted = [...ages.value]
  const colors = ['#00d4ff', '#7b68ee', '#00ffa3', '#ffcc00', '#ff6b9d', '#ff9f43']
  return {
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => `${p[0].name}<br/>用户数：<b>${fmtNum(p[0].value)}</b>（${sorted[p[0].dataIndex].ratio}%）` },
    grid: { left: 76, right: 56, top: 8, bottom: 8 },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category',
      data: sorted.map(a => a.profileName),
      inverse: true,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#c9e1ff', fontSize: 11, margin: 10 }
    },
    series: [{
      type: 'bar',
      data: sorted.map((a, i) => ({
        value: a.userCount,
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(6, 30, 66, 0.9)' },
            { offset: 0.4, color: colors[i % colors.length] + 'cc' },
            { offset: 1, color: colors[i % colors.length] }
          ])
        }
      })),
      barWidth: '56%',
      showBackground: true,
      backgroundStyle: { color: 'rgba(0, 212, 255, 0.05)', borderRadius: [0, 4, 4, 0] },
      label: {
        show: true, position: 'right', color: '#8fd8ff', fontSize: 11,
        fontFamily: 'Rajdhani, monospace',
        formatter: p => sorted[p.dataIndex].ratio + '%'
      },
      animationDelay: i => i * 60
    }]
  }
}

/* ===== 04 蝴蝶图：男左女右 ===== */
function butterflyOption() {
  const rows = ageGender.value
  return {
    tooltip: {
      ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: p => {
        const r = rows[p[0].dataIndex] || {}
        return `${r.ageRange}<br/>男性：<b>${fmtNum(r.maleCount)}</b>（${r.malePct}%）<br/>女性：<b>${fmtNum(r.femaleCount)}</b>（${r.femalePct}%）`
      }
    },
    legend: {
      top: 0, left: 'center', icon: 'circle',
      itemWidth: 9, itemHeight: 9,
      textStyle: { color: '#7a97c5', fontSize: 11 }
    },
    grid: { left: 8, right: 8, top: 30, bottom: 10, containLabel: true },
    xAxis: [
      { type: 'value', max: v => Math.ceil(v.max * 1.15), axisLabel: { show: false }, splitLine: { show: false } },
      { type: 'value', max: v => Math.ceil(v.max * 1.15), axisLabel: { show: false }, splitLine: { show: false } }
    ],
    yAxis: {
      type: 'category',
      data: rows.map(r => r.ageRange),
      inverse: true,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#c9e1ff', fontSize: 11 }
    },
    series: [
      {
        name: '男性', type: 'bar', xAxisIndex: 0,
        data: rows.map(r => r.maleCount),
        barWidth: '52%',
        itemStyle: {
          borderRadius: [4, 0, 0, 4],
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: 'rgba(0, 165, 255, 0.25)' },
            { offset: 1, color: '#00a5ff' }
          ])
        },
        label: { show: true, position: 'left', color: '#69b7ff', fontSize: 10, formatter: p => rows[p.dataIndex].malePct + '%' }
      },
      {
        name: '女性', type: 'bar', xAxisIndex: 1,
        data: rows.map(r => r.femaleCount),
        barWidth: '52%',
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#ff6b9d' },
            { offset: 1, color: 'rgba(255, 107, 157, 0.25)' }
          ])
        },
        label: { show: true, position: 'right', color: '#ff9dc0', fontSize: 10, formatter: p => rows[p.dataIndex].femalePct + '%' }
      }
    ],
    animationDuration: 700
  }
}

/* ===== 05 分龄活跃热力时钟 ===== */
function heatmapOption() {
  const hours = Array.from({ length: 24 }, (_, h) => pad(h) + ':00')
  const data = []
  ageHourly.value.forEach((row, y) => {
    row.hours.forEach((v, x) => data.push([x, y, v]))
  })
  return {
    tooltip: {
      ...tooltipStyle,
      formatter: p => `${ageHourly.value[p.value[1]].ageRange} · ${hours[p.value[0]]}<br/>活跃度：<b>${p.value[2]}</b>`
    },
    grid: { left: 76, right: 66, top: 10, bottom: 42 },
    xAxis: {
      type: 'category', data: hours,
      splitArea: { show: true, areaStyle: { color: ['rgba(0,212,255,0.02)', 'rgba(0,212,255,0.05)'] } },
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#7a97c5', fontSize: 10, interval: 2 }
    },
    yAxis: {
      type: 'category',
      data: ageHourly.value.map(r => r.ageRange),
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#c9e1ff', fontSize: 11 }
    },
    visualMap: {
      min: 0, max: 100, orient: 'vertical', right: 0, top: 'center',
      itemHeight: 90, itemWidth: 12,
      calculable: false,
      inRange: { color: ['#0a1d42', '#123a7a', '#0066cc', '#00a5ff', '#7ce7ff'] },
      textStyle: { color: '#7a97c5', fontSize: 10 }
    },
    series: [{
      type: 'heatmap',
      data,
      label: { show: false },
      itemStyle: { borderColor: '#060f2a', borderWidth: 2, borderRadius: 3 },
      emphasis: { itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0, 212, 255, 0.5)' } },
      animationDuration: 600
    }]
  }
}

/* ===== 06 消费分层金字塔（管理员） ===== */
function pyramidOption() {
  const colors = ['#c9a1ff', '#7b68ee', '#00d4ff', '#00ffa3', '#5f8bb0']
  const tiers = consumptionTiers.value
  return {
    tooltip: {
      ...tooltipStyle,
      formatter: p => {
        const t = tiers[p.dataIndex] || {}
        return `<b>${t.tier}</b><br/>用户数：${fmtNum(t.users)}（${t.ratio}%）<br/>ARPU：¥ ${fmtNum(t.arpu)}<br/>复购率：${t.repurchase}%`
      }
    },
    series: [{
      type: 'funnel',
      left: '6%', right: '6%', top: 10, bottom: 10,
      minSize: '30%', maxSize: '100%',
      sort: 'descending',
      gap: 4,
      label: {
        show: true, position: 'inside',
        color: '#e8f6ff', fontSize: 11.5,
        formatter: p => `${tiers[p.dataIndex].tier}  ${tiers[p.dataIndex].ratio}%`
      },
      itemStyle: { borderColor: '#060f2a', borderWidth: 1, borderRadius: 3 },
      emphasis: { label: { fontSize: 12.5 } },
      data: tiers.map((t, i) => ({
        name: t.tier, value: t.ratio,
        itemStyle: { color: colors[i % colors.length] }
      })),
      animationDuration: 800,
      animationDelay: i => i * 90
    }]
  }
}

/* ===== 07 年龄价值气泡图（管理员） ===== */
function bubbleOption() {
  const rows = ageValue.value
  const maxGmv = Math.max(...rows.map(r => r.gmv), 1)
  const symbols = rows.map(r => 14 + 46 * (r.gmv / maxGmv))
  return {
    tooltip: {
      ...tooltipStyle,
      formatter: p => {
        const r = rows[p.dataIndex] || {}
        return `<b>${r.ageRange}</b><br/>客单价：¥ ${fmtNum(r.aov)}<br/>转化率：${r.conversionRate}%<br/>GMV：¥ ${fmtWan(r.gmv)} 万`
      }
    },
    grid: { left: 60, right: 30, top: 30, bottom: 34 },
    xAxis: {
      type: 'category', data: rows.map(r => r.ageRange),
      axisLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.5)' } },
      axisTick: { show: false },
      axisLabel: { color: '#7a97c5', fontSize: 10, interval: 0, rotate: 18 }
    },
    yAxis: {
      type: 'value', name: '客单价（元）',
      nameTextStyle: { color: '#7a97c5', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.15)' } },
      axisLabel: { color: '#7a97c5', fontSize: 10, formatter: v => '¥' + (v / 1000).toFixed(0) + 'k' }
    },
    series: [{
      type: 'scatter',
      data: rows.map((r, i) => ({
        value: r.aov,
        symbolSize: symbols[i],
        itemStyle: {
          color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.8, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.35)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.05)' }
          ]),
          borderColor: '#00d4ff', borderWidth: 1.5
        },
        emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0, 212, 255, 0.6)' } }
      })),
      label: {
        show: true, position: 'top', color: '#aef4ff', fontSize: 10.5,
        fontFamily: 'Rajdhani, monospace',
        formatter: p => '¥' + fmtNum(rows[p.dataIndex].aov)
      },
      animationDelay: i => i * 80
    }]
  }
}

/* ===== 06 兴趣雷达（商家） ===== */
function radarOption() {
  const colors = ['#00d4ff', '#7b68ee', '#00ffa3', '#ffcc00', '#ff6b9d', '#ff9f43']
  const indicators = interestDims.value.map(d => ({ name: d, max: 100 }))
  const seriesData = ageInterests.value.map((r, i) => ({
    name: r.ageRange,
    value: r.scores,
    lineStyle: { color: colors[i % colors.length], width: 2 },
    itemStyle: { color: colors[i % colors.length] },
    areaStyle: { color: colors[i % colors.length] + '22' },
    symbolSize: 4
  }))
  return {
    tooltip: { ...tooltipStyle },
    legend: {
      bottom: 0, left: 'center', icon: 'circle',
      itemWidth: 9, itemHeight: 9,
      textStyle: { color: '#7a97c5', fontSize: 10 }
    },
    radar: {
      indicator: indicators,
      center: ['50%', '47%'],
      radius: '60%',
      splitNumber: 4,
      axisName: { color: '#a8c4e8', fontSize: 10.5 },
      splitLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.35)' } },
      splitArea: { areaStyle: { color: ['rgba(0,212,255,0.02)', 'rgba(0,212,255,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(58, 90, 140, 0.35)' } }
    },
    series: [{ type: 'radar', data: seriesData }],
    animationDuration: 800
  }
}

/* ===== 初始化图表 ===== */
function initCharts() {
  if (charts.gender) return
  if (genderEl.value) charts.gender = echarts.init(genderEl.value)
  if (ageEl.value) charts.age = echarts.init(ageEl.value)
  if (butterflyEl.value) charts.butterfly = echarts.init(butterflyEl.value)
  if (heatmapEl.value) charts.heatmap = echarts.init(heatmapEl.value)
  if (pyramidEl.value && isAdmin.value) charts.pyramid = echarts.init(pyramidEl.value)
  if (bubbleEl.value && isAdmin.value) charts.bubble = echarts.init(bubbleEl.value)
  if (radarEl.value && !isAdmin.value) charts.radar = echarts.init(radarEl.value)
}

/* ===== 渲染全部图表 ===== */
function renderAll() {
  if (!ages.value.length) return
  initCharts()
  if (charts.gender) charts.gender.setOption(genderOption(), true)
  if (charts.age) charts.age.setOption(ageOption(), true)
  if (charts.butterfly && ageGender.value.length) charts.butterfly.setOption(butterflyOption(), true)
  if (charts.heatmap && ageHourly.value.length) charts.heatmap.setOption(heatmapOption(), true)
  if (charts.pyramid && isAdmin.value && consumptionTiers.value.length) charts.pyramid.setOption(pyramidOption(), true)
  if (charts.bubble && isAdmin.value && ageValue.value.length) charts.bubble.setOption(bubbleOption(), true)
  if (charts.radar && !isAdmin.value && ageInterests.value.length) charts.radar.setOption(radarOption(), true)
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
.profile-page {
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
    linear-gradient(rgba(0, 212, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.04) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, #000 30%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, #000 30%, transparent 100%);
}
.radial-bg {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse 60% 45% at 50% 0%, rgba(0, 212, 255, 0.10), transparent 70%),
              radial-gradient(ellipse 50% 40% at 85% 60%, rgba(123, 104, 238, 0.08), transparent 70%);
}
.vignette {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse 120% 90% at 50% 50%, transparent 60%, rgba(2, 6, 20, 0.8) 100%);
}

/* ===== 顶部栏 ===== */
.profile-header {
  position: sticky; top: 0; z-index: 20;
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px;
  padding: 14px 22px;
  background: rgba(4, 12, 36, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 212, 255, 0.18);
}
.header-left, .header-right { display: flex; align-items: center; gap: 8px; flex: 1; }
.header-right { justify-content: flex-end; }
.header-center { text-align: center; }

.back-link {
  padding: 5px 12px;
  font-size: 12px; color: #8fb8e8; text-decoration: none;
  border: 1px solid rgba(58, 90, 140, 0.5);
  border-radius: 4px;
  background: rgba(0, 212, 255, 0.04);
  white-space: nowrap;
  transition: all 0.2s;
}
.back-link:hover { color: #00d4ff; border-color: rgba(0, 212, 255, 0.5); box-shadow: 0 0 12px rgba(0, 212, 255, 0.2); }

.page-title { display: flex; flex-direction: column; gap: 2px; margin: 0; }
.title-zh {
  font-size: 21px; font-weight: 700; letter-spacing: 4px;
  background: linear-gradient(90deg, #aef4ff, #00d4ff 45%, #7b68ee);
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
  border: 3px solid rgba(0, 212, 255, 0.15);
  border-top-color: #00d4ff;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.retry-btn {
  padding: 7px 22px;
  font-size: 13px; color: #00d4ff;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.45);
  border-radius: 4px; cursor: pointer;
  transition: all 0.2s;
}
.retry-btn:hover { background: rgba(0, 212, 255, 0.18); box-shadow: 0 0 14px rgba(0, 212, 255, 0.3); }

/* ===== 主体 ===== */
.profile-body {
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
.info-chip b { color: #00d4ff; font-family: 'Rajdhani', monospace; font-size: 13px; }
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
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

/* 四角装饰 */
.corner-tl, .corner-tr, .corner-bl, .corner-br {
  position: absolute; width: 10px; height: 10px;
  border-color: rgba(0, 212, 255, 0.55); border-style: solid;
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
  font-size: 13px; color: #00d4ff; opacity: 0.85;
}
.title-en { font-size: 10px; letter-spacing: 2px; color: #4d6fa3; }
.title-tip { font-size: 11px; color: #5f7ca8; font-weight: 400; margin-left: auto; }

/* ===== Persona 画像卡 ===== */
.persona-card {
  display: flex; gap: 26px; align-items: stretch;
  padding: 20px 22px;
  background:
    radial-gradient(ellipse 40% 80% at 12% 50%, rgba(0, 212, 255, 0.10), transparent 70%),
    radial-gradient(ellipse 40% 80% at 88% 50%, rgba(123, 104, 238, 0.10), transparent 70%),
    rgba(6, 15, 42, 0.55);
  border: 1px solid rgba(0, 212, 255, 0.22);
  border-radius: 12px;
}
.persona-avatar {
  flex-shrink: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 8px;
  padding: 0 18px;
  border-right: 1px dashed rgba(58, 90, 140, 0.45);
}
.avatar-ring {
  width: 108px; height: 108px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  border: 2px solid rgba(0, 212, 255, 0.5);
  background: radial-gradient(circle, rgba(0, 212, 255, 0.15), rgba(6, 15, 42, 0.9) 70%);
  box-shadow: 0 0 26px rgba(0, 212, 255, 0.25), inset 0 0 18px rgba(0, 212, 255, 0.15);
}
.avatar-emoji { font-size: 54px; line-height: 1; }
.persona-name {
  font-size: 15px; font-weight: 700; color: #e8f6ff;
  letter-spacing: 1px;
}
.persona-share {
  font-size: 11.5px; color: #00d4ff;
  font-family: 'Rajdhani', monospace;
}
.persona-attrs { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 16px; min-width: 0; }
.attr-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.attr-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  background: rgba(10, 24, 56, 0.65);
  border: 1px solid rgba(58, 90, 140, 0.35);
  border-radius: 8px;
}
.attr-icon { font-size: 20px; flex-shrink: 0; }
.attr-label { font-size: 11px; color: #6f8cb8; margin-bottom: 2px; }
.attr-value { font-size: 12.5px; color: #e8f6ff; font-weight: 600; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip {
  padding: 5px 14px;
  font-size: 12px; color: #aef4ff;
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.12), rgba(123, 104, 238, 0.12));
  border: 1px solid rgba(0, 212, 255, 0.35);
  border-radius: 16px;
  animation: tagPulse 3s ease-in-out infinite;
}
.tag-chip:nth-child(2n) { animation-delay: 0.6s; border-color: rgba(123, 104, 238, 0.45); }
.tag-chip:nth-child(3n) { animation-delay: 1.2s; border-color: rgba(0, 255, 163, 0.4); }
@keyframes tagPulse {
  0%, 100% { box-shadow: 0 0 0 rgba(0, 212, 255, 0); }
  50% { box-shadow: 0 0 14px rgba(0, 212, 255, 0.25); }
}

/* 图表容器（显式高度，防止被 flex 折叠为 0） */
.chart { width: 100%; }
.chart-gender { height: 330px; }
.chart-age { height: 330px; }
.chart-butterfly { height: 320px; }
.chart-heatmap { height: 300px; }
.chart-pyramid { height: 340px; }
.chart-bubble { height: 340px; }
.chart-radar { height: 380px; }

/* ===== 分层明细表（管理员） ===== */
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
.value-table tr:hover td { background: rgba(0, 212, 255, 0.035); }
.ta-r { text-align: right; }
.ta-c { text-align: center; }
.hl-gmv { color: #00ffa3; font-weight: 700; font-family: 'Rajdhani', monospace; }
.tier-dot {
  display: inline-block; width: 9px; height: 9px;
  border-radius: 50%; margin-right: 7px; vertical-align: 1px;
}
.gmv-bar-track {
  display: inline-block; vertical-align: middle;
  width: 130px; height: 6px; margin-right: 8px;
  background: rgba(58, 90, 140, 0.18);
  border-radius: 3px; overflow: hidden;
}
.gmv-bar { height: 100%; border-radius: 3px; }
.gmv-pct { font-size: 11px; color: #8fb8e8; font-family: 'Rajdhani', monospace; }

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
.profile-footer {
  position: relative; z-index: 1;
  text-align: center;
  padding: 18px 0 22px;
  font-size: 10.5px; letter-spacing: 3px; color: #3a5a8c;
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .panel-row { grid-template-columns: 1fr; }
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .attr-row { grid-template-columns: repeat(2, 1fr); }
  .insight-grid { grid-template-columns: 1fr; }
  .persona-card { flex-direction: column; }
  .persona-avatar { border-right: none; border-bottom: 1px dashed rgba(58, 90, 140, 0.45); padding: 0 0 14px; }
  .header-center .title-zh { font-size: 16px; letter-spacing: 2px; }
}
@media (max-width: 700px) {
  .kpi-grid { grid-template-columns: 1fr; }
  .attr-row { grid-template-columns: 1fr; }
  .profile-header { flex-wrap: wrap; }
}
</style>
