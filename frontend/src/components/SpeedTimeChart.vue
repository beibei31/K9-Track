<template>
  <div class="cht-wrap">
    <div class="cht-title">迁徙速度时序分布</div>
    <div ref="chartRef" class="cht-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref(null)
let chart = null
let ro = null

function mock() {
  const days = []
  const speeds = []
  const start = new Date(2024, 3, 1)
  for (let i = 0; i < 118; i++) {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    days.push(`${d.getMonth() + 1}/${d.getDate()}`)
    let base = i < 20 ? 5 + Math.random() * 20 : i < 60 ? 35 + Math.random() * 30 : 15 + Math.random() * 25
    speeds.push(Math.round(base * 10) / 10)
  }
  return { days, speeds }
}

function render() {
  const el = chartRef.value
  if (!el || el.clientWidth === 0) return
  if (!chart) chart = echarts.init(el)
  const { days, speeds } = mock()
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e8e8e8',
      textStyle: { color: '#1a1a1a', fontSize: 12 },
      formatter: p => `${p[0].axisValue}<br/>速度 <b style="color:#1890ff">${p[0].value} km/h</b>`
    },
    grid: { top: 10, right: 16, bottom: 24, left: 40 },
    xAxis: {
      type: 'category', data: days,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8c8c8c', fontSize: 10, interval: 14 },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value', min: 0, max: 70,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8c8c8c', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [{
      type: 'line', data: speeds, smooth: true, symbol: 'none',
      lineStyle: { color: '#1890ff', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24,144,255,0.18)' },
          { offset: 1, color: 'rgba(24,144,255,0.02)' }
        ])
      }
    }]
  })
}

onMounted(() => {
  const el = chartRef.value
  if (!el) return
  setTimeout(render, 80)
  ro = new ResizeObserver(() => {
    if (el.clientWidth > 0) chart ? chart.resize() : render()
  })
  ro.observe(el)
})
onUnmounted(() => { ro?.disconnect(); chart?.dispose() })
</script>

<style scoped>
.cht-wrap { height: 100%; display: flex; flex-direction: column; }
.cht-title { font-size: 13px; font-weight: 600; color: #1a1a1a; padding-bottom: 4px; flex-shrink: 0; }
.cht-body { flex: 1; min-height: 0; min-width: 0; }
</style>
