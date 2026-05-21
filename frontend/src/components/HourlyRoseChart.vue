<template>
  <div class="cht-wrap">
    <div class="cht-title">昼夜飞行节律</div>
    <div ref="chartRef" class="cht-body"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  hourlyData: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null
let ro = null

// 莫兰迪色系 —— 低饱和度
const COLORS = [
  '#9bbfd4','#a3c9c7','#c2cfa3','#dfcf9f',
  '#e2bd9c','#dba898','#cba3a3','#c2a6bf',
  '#b3b3cc','#a3bdd4','#a3c9c0','#bccfa3',
  '#d9cea3','#dfbfa3','#dba8a8','#cba3ba',
  '#c2a6cc','#b3a6cc','#a3b3cc','#a3c1cc',
  '#9bc4cc','#9bc4c0','#a9cc9b','#c2cc9b'
]

function render() {
  const el = chartRef.value
  if (!el || el.clientWidth === 0) return
  if (!chart) chart = echarts.init(el)

  let data
  if (props.hourlyData.length === 24) {
    data = props.hourlyData.map(h => ({ name: String(h.hourOfDay), value: h.activityCount || 0 }))
  } else {
    data = Array.from({length: 24}, (_, i) => ({ name: String(i), value: 30 + Math.random() * 50 }))
  }

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#fff',
      borderColor: '#e8e8e8',
      textStyle: { color: '#1a1a1a', fontSize: 12 },
      formatter: p => `<b>${p.name}:00</b><br/>飞行占比 ${p.value}%`
    },
    series: [{
      type: 'pie',
      radius: ['20%', '78%'],
      center: ['50%', '50%'],
      roseType: 'radius',
      itemStyle: { borderRadius: 2, borderColor: '#fff', borderWidth: 1 },
      data: data.map((d, i) => ({
        ...d,
        itemStyle: { color: COLORS[i % COLORS.length] }
      })),
      label: { color: '#8c8c8c', fontSize: 9, formatter: p => p.value > 40 ? `${p.name}h` : '' },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' }, scaleSize: 6 }
    }]
  }
  chart.clear()
  chart.setOption(option)
}

watch(() => props.hourlyData, () => render(), { deep: true })

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
