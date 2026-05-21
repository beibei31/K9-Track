<template>
  <div class="chart-card">
    <div class="chart-title">昼夜飞行习性 (南丁格尔玫瑰图)</div>
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
  const hours = data.map(d => `${d.hour}:00`)
  const counts = data.map(d => d.count)

  chartInstance.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#8899aa', fontSize: 10 } },
    series: [{
      type: 'pie',
      radius: ['20%', '75%'],
      roseType: 'area',
      data: hours.map((h, i) => ({ name: h, value: counts[i] })),
      label: {
        color: '#8899aa',
        fontSize: 10,
        formatter: p => p.value > 200 ? p.name : ''
      },
      itemStyle: {
        borderRadius: 2,
        borderColor: '#1a3a4a',
        borderWidth: 1
      },
      emphasis: {
        label: { fontSize: 14, fontWeight: 'bold' }
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
