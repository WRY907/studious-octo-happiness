<template>
  <div ref="chartEl" class="chart-box"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ data: { type: Array, default: () => [] } })
const chartEl = ref(null)
let chart = null

function render() {
  if (!chart || !props.data?.length) return
  const dates = props.data.map(d => (d.statDate || '').slice(5))
  const sales = props.data.map(d => Number(d.sales || 0) / 10000)
  const orders = props.data.map(d => Number(d.orders || 0))
  const visits = props.data.map(d => Number(d.visits || 0) / 1000)

  chart.setOption({
    grid: { left: 50, right: 50, top: 40, bottom: 30 },
    legend: {
      top: 0, right: 10,
      textStyle: { color: '#a8c4e8', fontSize: 11 },
      itemWidth: 10, itemHeight: 8,
      data: ['销售额(万)', '订单数', '访问量(千)']
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(3, 11, 34, 0.92)',
      borderColor: 'rgba(0, 212, 255, 0.4)',
      textStyle: { color: '#c9e1ff', fontSize: 12 },
      axisPointer: { lineStyle: { color: 'rgba(0, 212, 255, 0.5)' } }
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.25)' } },
      axisLabel: { color: '#7a97c5', fontSize: 10, interval: 3 },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        nameTextStyle: { color: '#7a97c5', fontSize: 10 },
        axisLine: { show: false },
        axisLabel: { color: '#7a97c5', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(122, 151, 197, 0.08)' } }
      }
    ],
    series: [
      {
        name: '销售额(万)',
        type: 'line',
        data: sales,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: false,
        lineStyle: { width: 2.5, color: '#00d4ff', shadowColor: 'rgba(0, 212, 255, 0.5)', shadowBlur: 10 },
        itemStyle: { color: '#00d4ff', borderColor: '#fff', borderWidth: 1 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.4)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.02)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'line',
        data: orders,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: false,
        yAxisIndex: 0,
        lineStyle: { width: 2, color: '#00ffa3' },
        itemStyle: { color: '#00ffa3' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 255, 163, 0.25)' },
            { offset: 1, color: 'rgba(0, 255, 163, 0.02)' }
          ])
        }
      },
      {
        name: '访问量(千)',
        type: 'line',
        data: visits,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: false,
        lineStyle: { width: 2, color: '#ffcc00', type: 'dashed' },
        itemStyle: { color: '#ffcc00' }
      }
    ]
  })
}

const resizeHandler = () => chart && chart.resize()

watch(() => props.data, () => nextTick(render), { deep: true })

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartEl.value)
  render()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  if (chart) { chart.dispose(); chart = null }
})
</script>

<style scoped>
.chart-box { width: 100%; height: 100%; min-height: 240px; }
</style>
