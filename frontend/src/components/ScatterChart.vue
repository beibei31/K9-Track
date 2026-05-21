<template>
  <div class="chart-card">
    <div class="chart-title">高度-速度散点分布 (迁徙阶段采样)</div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: true }
})

const chartRef = ref(null)
let chartInstance = null

onMounted(() => {
  chartInstance = echarts.init(chartRef.value)
  window.addEventListener('resize', () => chartInstance?.resize())
})

watch(() => props.data, (val) => {
  if (val.length > 0) nextTick(() => renderChart(val))
})

function renderChart(data) {
  if (!chartInstance) return
  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: p => `速度: ${p.value[0]} km/h<br/>高度: ${p.value[1]} m`
    },
    grid: { top: 16, right: 24, bottom: 32, left: 56 },
    xAxis: {
      name: '速度 (km/h)',
      nameTextStyle: { color: '#8899aa' },
      axisLabel: { color: '#8899aa' }
    },
    yAxis: {
      name: '高度 (m)',
      nameTextStyle: { color: '#8899aa' },
      axisLabel: { color: '#8899aa' }
    },
    series: [{
      type: 'scatter',
      data: data.map(d => [d.speed, d.altitude]),
      symbolSize: 4,
      itemStyle: {
        color: new echarts.graphic.RadialGradient(0.4, 0.3, 0.8, [
          { offset: 0, color: 'rgba(79,195,247,0.9)' },
          { offset: 1, color: 'rgba(79,195,247,0.1)' }
        ])
      }
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
  min-height: 220px;
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
  min-height: 180px;
}
</style>
