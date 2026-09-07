<template>
  <div class="profile-wrap">
    <div class="profile-block">
      <div class="block-title">性别分布</div>
      <div ref="genderEl" class="mini-chart"></div>
    </div>
    <div class="profile-block">
      <div class="block-title">年龄分布</div>
      <div ref="ageEl" class="mini-chart"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/* 新契约：userProfiles 为扁平数组 [{profileType, profileName, userCount, ratio}]
   兼容旧结构 { gender: [], age: [] } */
const props = defineProps({
  data: {
    type: [Array, Object],
    default: () => []
  }
})

/* 按 profileType 分组 → { gender: [], age: [] } */
const groups = computed(() => {
  if (Array.isArray(props.data)) {
    const map = {}
    ;(props.data || []).forEach(d => {
      const t = d.profileType || '其他'
      ;(map[t] = map[t] || []).push(d)
    })
    const keys = Object.keys(map)
    const genderKey = keys.find(k => /gender|性别/i.test(k))
    const ageKey = keys.find(k => /age|年龄/i.test(k))
    // 未匹配到语义键时按顺序兜底
    if (!genderKey && !ageKey && keys.length >= 2) {
      return { gender: map[keys[0]], age: map[keys[1]] }
    }
    if (!genderKey && !ageKey && keys.length === 1) {
      return { gender: [], age: map[keys[0]] }
    }
    return { gender: genderKey ? map[genderKey] : [], age: ageKey ? map[ageKey] : [] }
  }
  return { gender: props.data?.gender || [], age: props.data?.age || [] }
})

const genderEl = ref(null)
const ageEl = ref(null)
let gChart = null
let aChart = null

function renderGender() {
  if (!gChart) return
  const list = groups.value.gender
  gChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(3, 11, 34, 0.92)',
      borderColor: 'rgba(0, 212, 255, 0.4)',
      textStyle: { color: '#c9e1ff', fontSize: 11 },
      formatter: p => `${p.name}: ${Number(p.value).toLocaleString()}人 (${p.percent}%)`
    },
    legend: {
      bottom: 0,
      itemWidth: 8, itemHeight: 8,
      textStyle: { color: '#a8c4e8', fontSize: 10 }
    },
    series: [{
      type: 'pie',
      radius: ['42%', '62%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: false,
      label: {
        show: true,
        position: 'center',
        formatter: p => p ? [
          `{v|${Number(p.value||0).toLocaleString()}}`,
          `{l|${p.name}占比 ${p.percent}%}`
        ].join('\n') : '',
        rich: {
          v: { color: '#fff', fontSize: 15, fontWeight: 700, lineHeight: 22, fontFamily: 'monospace' },
          l: { color: '#7a97c5', fontSize: 10, lineHeight: 14 }
        }
      },
      labelLine: { show: false },
      itemStyle: {
        borderColor: 'rgba(3, 11, 34, 0.9)',
        borderWidth: 2
      },
      data: list.map((d, i) => ({
        name: d.profileName,
        value: Number(d.userCount || 0),
        itemStyle: { color: i === 0 ? '#00a5ff' : '#ff6b9d' }
      }))
    }]
  })
}

function renderAge() {
  if (!aChart) return
  const list = groups.value.age
  aChart.setOption({
    grid: { left: 10, right: 10, top: 10, bottom: 20, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(3, 11, 34, 0.92)',
      borderColor: 'rgba(0, 212, 255, 0.4)',
      textStyle: { color: '#c9e1ff', fontSize: 11 },
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(0, 212, 255, 0.06)' } }
    },
    xAxis: {
      type: 'category',
      data: list.map(d => d.profileName),
      axisLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.2)' } },
      axisLabel: { color: '#7a97c5', fontSize: 9, interval: 0, rotate: 20 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#7a97c5', fontSize: 9, formatter: v => (v/10000).toFixed(0)+'万' },
      splitLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.08)' } }
    },
    series: [{
      type: 'bar',
      data: list.map((d, i) => ({
        value: Number(d.userCount || 0),
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: ['#00d4ff','#7b68ee','#00ffa3','#ffcc00','#ff6b9d','#ff9f43'][i % 6] },
            { offset: 1, color: 'rgba(123, 104, 238, 0.2)' }
          ])
        }
      })),
      barWidth: 16,
      label: {
        show: true,
        position: 'top',
        color: '#a8c4e8',
        fontSize: 9,
        formatter: p => (p.value / 10000).toFixed(1) + '万'
      }
    }]
  })
}

function render() {
  renderGender()
  renderAge()
}

const resizeHandler = () => {
  gChart && gChart.resize()
  aChart && aChart.resize()
}

watch(() => props.data, () => nextTick(render), { deep: true })

onMounted(async () => {
  await nextTick()
  gChart = echarts.init(genderEl.value)
  aChart = echarts.init(ageEl.value)
  render()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  if (gChart) { gChart.dispose(); gChart = null }
  if (aChart) { aChart.dispose(); aChart = null }
})
</script>

<style scoped>
.profile-wrap {
  height: 100%;
  display: flex;
  gap: 10px;
}

.profile-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.block-title {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 4px;
  padding-left: 6px;
  border-left: 2px solid rgba(0, 212, 255, 0.5);
}

.mini-chart {
  flex: 1;
  min-height: 0;
}
</style>
