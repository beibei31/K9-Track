<template>
  <div class="app-container">
    <!-- 顶部标题栏 -->
    <header class="app-header">
      <h1>🦆 勺嘴鹬迁徙监测与分析平台 — K9-Track</h1>
      <span class="subtitle">Spoon-billed Sandpiper Migration Analysis</span>
    </header>

    <!-- 核心统计卡片 -->
    <StatsCards :overview="overview" :loading="loading" />

    <!-- 主体区域 -->
    <div class="main-content">
      <div class="map-panel">
        <MapView :track="track" :stopovers="stopovers" :loading="loading" />
      </div>
      <div class="charts-panel">
        <SpeedTimeChart :track="track" :loading="loading" />
        <HourlyRoseChart :data="hourlyActivity" :loading="loading" />
        <ScatterChart :data="scatterData" :loading="loading" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverview, getTrack, getStopovers, getHourlyActivity, getScatterData } from './api/index.js'
import StatsCards from './components/StatsCards.vue'
import MapView from './views/MapView.vue'
import SpeedTimeChart from './components/SpeedTimeChart.vue'
import HourlyRoseChart from './components/HourlyRoseChart.vue'
import ScatterChart from './components/ScatterChart.vue'

const overview = ref({})
const track = ref([])
const stopovers = ref([])
const hourlyActivity = ref([])
const scatterData = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [ov, tr, st, ha, sd] = await Promise.all([
      getOverview(), getTrack(), getStopovers(), getHourlyActivity(), getScatterData()
    ])
    overview.value = ov.data
    track.value = tr.data
    stopovers.value = st.data
    hourlyActivity.value = ha.data
    scatterData.value = sd.data
  } catch (e) {
    console.error('数据加载失败:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style>
.app-container {
  min-height: 100vh;
  background: #0a1628;
  color: #e0e6ed;
}
.app-header {
  text-align: center;
  padding: 18px 20px;
  background: linear-gradient(135deg, #0d2137 0%, #132a42 100%);
  border-bottom: 1px solid #1a3a4a;
}
.app-header h1 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
}
.subtitle {
  font-size: 13px;
  color: #8899aa;
}
.main-content {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  height: calc(100vh - 170px);
}
.map-panel {
  flex: 0 0 60%;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #1a3a4a;
}
.charts-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}
</style>
