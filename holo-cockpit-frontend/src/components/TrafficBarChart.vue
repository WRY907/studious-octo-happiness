<template>
  <div ref="chartEl" class="chart-box"></div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * 流量来源横向条形图
 * 数据：[{category, sourceName, visits, ratio}]（两级结构）
 * 展示：TOP10 渠道按访问量降序横向条形 + 未上榜渠道合并为"其他"条
 *       （固定 11 行，保证每个渠道名标签完整可见、不拥挤不被遮挡）；
 *       条形按一级分类着色；条尾标注百分比；隐藏 x 轴保持清爽
 */
const props = defineProps({
  data: { type: Array, default: () => [] },
  topN: { type: Number, default: 10 }
})

const chartEl = ref(null)
let chart = null

/* 一级分类配色 */
const CAT_COLORS = {
  '搜索引擎': '#00e5ff',
  '社交媒体': '#7b68ee',
  '电商平台广告': '#00ffa3',
  '直接访问': '#ffcc00',
  '线下引流': '#ff4d6a'
}
const FALLBACK = ['#00e5ff', '#7b68ee', '#00ffa3', '#ffcc00', '#ff4d6a']
const OTHER_COLOR = '#5f8bb0' // "其他"合并条的中性色

/* TOP N + "其他"合并行（访问量降序，最大在顶部） */
const rows = computed(() => {
  const sorted = [...(props.data || [])]
    .filter(d => d.sourceName)
    .sort((a, b) => Number(b.visits || 0) - Number(a.visits || 0))
  if (sorted.length <= props.topN) return sorted

  const top = sorted.slice(0, props.topN)
  const rest = sorted.slice(props.topN)
  const restVisits = rest.reduce((s, d) => s + Number(d.visits || 0), 0)
  const restRatio = rest.reduce((s, d) => s + Number(d.ratio || 0), 0)
  top.push({
    category: '其他',
    sourceName: '其他',
    visits: restVisits,
    ratio: restRatio,
    merged: rest.map(d => d.sourceName) // tooltip 展示合并明细
  })
  return top
})

const totalVisits = computed(() =>
  (props.data || []).reduce((s, d) => s + Number(d.visits || 0), 0)
)

/* 分类 → 颜色（自动分配未预置的分类） */
const catColorMap = computed(() => {
  const map = { ...CAT_COLORS }
  let i = 0
  rows.value.forEach(r => {
    if (!map[r.category]) {
      map[r.category] = FALLBACK[i % FALLBACK.length]
      i++
    }
  })
  return map
})

function catColor(cat) {
  if (cat === '其他') return OTHER_COLOR
  return catColorMap.value[cat] || '#00e5ff'
}

function fmtVisits(v) {
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString('zh-CN')
}

function render() {
  if (!chart || !rows.value.length) return
  const list = rows.value
  const names = list.map(d => d.sourceName)
  const colorMap = catColorMap.value

  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(3, 11, 34, 0.94)',
      borderColor: 'rgba(0, 229, 255, 0.45)',
      textStyle: { color: '#c9e1ff', fontSize: 11 },
      formatter: p => {
        const d = list[p.dataIndex] || {}
        const head = `<b style="color:#aef4ff">${d.sourceName}</b> <span style="color:#6f8cb8">(${d.category || '-'})</span>`
        const stat = `<br/>访问量：${Number(d.visits || 0).toLocaleString('zh-CN')}<br/>占比：${Number(d.ratio || 0).toFixed(2)}%`
        // "其他"合并条：展开明细渠道
        if (d.merged && d.merged.length) {
          return head + stat + `<br/><span style="color:#6f8cb8">含：${d.merged.join('、')}</span>`
        }
        return head + stat
      }
    },
    grid: {
      left: 96,          // 左侧留足中文渠道名
      right: 52,         // 右侧留给百分比标签
      top: 6,
      bottom: 6,
      containLabel: false
    },
    xAxis: {
      type: 'value',
      show: false        // 隐藏刻度轴，靠条尾百分比传达量级
    },
    yAxis: {
      type: 'category',
      data: names,
      inverse: true,     // 最大值在顶部
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#c9e1ff',
        fontSize: 11,
        margin: 10,
        interval: 0     // 强制显示每一行标签（TOP10+其他 共11行，不会重叠）
      }
    },
    series: [{
      type: 'bar',
      data: list.map(d => ({
        value: Number(d.visits || 0),
        itemStyle: {
          borderRadius: [0, 2, 2, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(6, 30, 66, 0.9)' },
            { offset: 0.35, color: catColor(d.category) + 'cc' },
            { offset: 1, color: catColor(d.category) }
          ])
        }
      })),
      barWidth: '55%',   // 自适应类目高度，留出行间距不拥挤
      showBackground: true,
      backgroundStyle: {
        color: 'rgba(0, 229, 255, 0.05)',
        borderRadius: [0, 2, 2, 0]
      },
      label: {
        show: true,
        position: 'right',
        color: '#8fd8ff',
        fontSize: 10.5,
        fontFamily: 'Rajdhani, monospace',
        formatter: p => {
          const d = list[p.dataIndex] || {}
          const ratio = Number(d.ratio || 0)
          return (ratio > 0 ? ratio.toFixed(1) : '0.0') + '%'
        }
      },
      emphasis: {
        itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0, 229, 255, 0.35)' }
      },
      animationDuration: 800,
      animationDelay: i => i * 40 // 条形逐条入场
    }]
  })
}

const onResize = () => chart && chart.resize()
watch(() => props.data, () => nextTick(render), { deep: true })

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartEl.value)
  render()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chart) { chart.dispose(); chart = null }
})
</script>

<style scoped>
.chart-box {
  width: 100%;
  height: 100%;
  min-height: 0;
}
</style>
