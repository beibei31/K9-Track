<template>
  <div class="stats-row">
    <div class="stat-card">
      <div class="stat-value">{{ overview.totalDistanceKm ?? '-' }}</div>
      <div class="stat-label">总里程 (km)</div>
    </div>
    <div class="stat-card">
      <div class="stat-value">{{ overview.totalDays ?? '-' }}</div>
      <div class="stat-label">迁徙天数</div>
    </div>
    <div class="stat-card">
      <div class="stat-value">{{ overview.maxSpeedKmh ?? '-' }}</div>
      <div class="stat-label">最快时速 (km/h)</div>
    </div>
    <div class="stat-card">
      <div class="stat-value">{{ overview.stopoverCount ?? '-' }}</div>
      <div class="stat-label">有效停歇点</div>
    </div>
    <div class="stat-card">
      <div class="stat-value">{{ overview.totalRecords?.toLocaleString() ?? '-' }}</div>
      <div class="stat-label">GPS 记录总数</div>
    </div>
    <div class="stat-card date-range">
      <div class="stat-value date">{{ overview.startDate ?? '-' }}</div>
      <div class="stat-label">起止日期</div>
      <div class="stat-value date">{{ overview.endDate ?? '-' }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  overview: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: true }
})
</script>

<style scoped>
.stats-row {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  flex-wrap: wrap;
}
.stat-card {
  flex: 1;
  min-width: 140px;
  background: linear-gradient(135deg, #112236 0%, #162a44 100%);
  border: 1px solid #1a3a4a;
  border-radius: 8px;
  padding: 14px 18px;
  text-align: center;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #4fc3f7;
}
.stat-value.date {
  font-size: 13px;
  color: #81d4fa;
}
.stat-label {
  font-size: 12px;
  color: #8899aa;
  margin-top: 4px;
}
</style>
