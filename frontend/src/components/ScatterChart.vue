<template>
  <div class="cht-wrap">
    <div class="cht-title">高度-速度散点采样</div>
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
  const pts = []
  for (let i = 0; i < 500; i++) {
    const speed = Math.round((20 + Math.random() * 45) * 10) / 10
    const altitude = Math.round(speed * 28 + (Math.random() - 0.5) * 1200)
    pts.push([speed, Math.max(50, Math.min(2900, altitude))])
  }
  return pts
}

function render() {
  const el = chartRef.value
  if (!el || el.clientWidth === 0) return
  if (!chart) chart = echarts.init(el)
  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: '#fff',
      borderColor: '#e8e8e8',
      textStyle: { color: '#1a1a1a', fontSize: 12 },
      formatter: p => `速度 ${p.value[0]} km/h<br/>高度 ${p.value[1]} m`
    },
    grid: { top: 10, right: 18, bottom: 24, left: 46 },
    xAxis: {
      type: 'value', min: 15, max: 70,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8c8c8c', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    yAxis: {
      type: 'value', min: 0, max: 3000,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8c8c8c', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [{
      type: 'scatter',
      data: mock(),
      symbolSize: 6,
      itemStyle: {
        color: 'rgba(24,144,255,0.55)',
        borderWidth: 0
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
