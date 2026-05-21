<template>
  <div class="chart-card">
    <div class="chart-title">迁徙速度时序分布</div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  track: { type: Array, default: () => [] },
  loading: { type: Boolean, default: true }
})

const chartRef = ref(null)
let chartInstance = null

onMounted(() => {
  chartInstance = echarts.init(chartRef.value)
  window.addEventListener('resize', () => chartInstance?.resize())
})

watch(() => props.track, (val) => {
  if (val.length > 0) nextTick(() => renderChart(val))
})

function renderChart(track) {
  if (!chartInstance) return
  // 用轨迹点的顺序作为时间轴代理，展示迁徙过程
  const xData = track.map((_, i) => i)
  // 计算相邻点之间的步长作为速度代理
  const speeds = []
  for (let i = 1; i < track.length; i++) {
    const prev = track[i - 1]
    const curr = track[i]
    const dlat = curr.lat - prev.lat
    const dlng = curr.lng - prev.lng
    speeds.push(Math.sqrt(dlat * dlat + dlng * dlng) * 100)
  }
  speeds.unshift(speeds[0] || 0)

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 16, right: 16, bottom: 28, left: 48 },
    xAxis: {
      type: 'category',
      data: xData,
      name: '轨迹序号',
      axisLabel: { color: '#8899aa' }
    },
    yAxis: {
      type: 'value',
      name: '步长 (×100°)',
      axisLabel: { color: '#8899aa' }
    },
    series: [{
      type: 'line',
      data: speeds,
      smooth: true,
      symbol: 'none',
      lineStyle: { color: '#4fc3f7', width: 1.5 },
      areaStyle: { color: 'rgba(79,195,247,0.15)' }
    }]
  })
}
</script>

<style scoped>
.chart-card {
  flex: 1;
  background: #112236;
  border: 1px solid #1a3a4a;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  min-height: 200px;
}
.chart-title {
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #b0bec5;
  border-bottom: 1px solid #1a3a4a;
}
.chart-body {
  flex: 1;
  min-height: 160px;
}
</style>
