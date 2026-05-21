<template>
  <div class="dashboard">
    <!-- ==================== 标题卡片 ==================== -->
    <header class="header-card">
      <h1 class="app-title">勺嘴鹬迁徙监测与分析平台 — K9-Track</h1>
      <p class="app-subtitle">Spoon-billed Sandpiper Migration Analysis</p>
    </header>

    <!-- ==================== 指标卡片行 ==================== -->
    <div class="stats-row">
      <div class="stat-card" v-for="item in statCards" :key="item.label">
        <div class="stat-icon" v-html="item.icon"></div>
        <div class="stat-body">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- ==================== 主体：地图 + 图表 ==================== -->
    <div class="main-body">
      <!-- 左侧：地图卡片 -->
      <div class="map-card">
        <div class="card-header">迁徙轨迹追踪</div>
        <div class="card-body">
          <MapView />
        </div>
      </div>

      <!-- 右侧：3 个图表卡片 -->
      <div class="charts-col">
        <div class="chart-card">
          <SpeedTimeChart />
        </div>
        <div class="chart-card">
          <HourlyRoseChart />
        </div>
        <div class="chart-card">
          <ScatterChart />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import MapView from './views/MapView.vue'
import SpeedTimeChart from './components/SpeedTimeChart.vue'
import HourlyRoseChart from './components/HourlyRoseChart.vue'
import ScatterChart from './components/ScatterChart.vue'

const statCards = [
  { label: '总里程 (km)', value: '23,085.4', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10"/></svg>' },
  { label: '有效天数', value: '118', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>' },
  { label: '最高时速 (km/h)', value: '65.0', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>' },
  { label: '停歇点数量', value: '12', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>' },
  { label: 'GPS 记录总数', value: '194,500', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>' },
]
</script>

<style>
/* ==================== 全局 ==================== */
.dashboard {
  min-height: 100vh;
  background: #f4f7f9;
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1920px;
  margin: 0 auto;
}

/* ==================== 标题卡片 ==================== */
.header-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  padding: 20px 32px;
  text-align: center;
}
.app-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 1px;
}
.app-subtitle {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  letter-spacing: 0.5px;
}

/* ==================== 指标卡片行 ==================== */
.stats-row {
  display: flex;
  gap: 16px;
}
.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  padding: 16px 20px;
  transition: box-shadow 0.25s;
}
.stat-card:hover {
  box-shadow: 0 6px 28px rgba(0, 0, 0, 0.08);
}
.stat-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

/* ==================== 主体 ==================== */
.main-body {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

/* 地图卡片 (65%) */
.map-card {
  flex: 0 0 65%;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.map-card .card-header {
  padding: 14px 20px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}
.map-card .card-body {
  flex: 1;
  margin: 12px 20px 20px;
  border-radius: 8px;
  overflow: hidden;
}

/* 图表列 (35%) */
.charts-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.chart-card {
  flex: 1;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  min-height: 0;
}
</style>
