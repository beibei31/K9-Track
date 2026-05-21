<template>
  <div class="app">
    <!-- ===== 标题卡片 ===== -->
    <header class="header-card">
      <h1 class="app-title">勺嘴鹬迁徙监测与分析平台 — K9-Track</h1>
      <p class="app-subtitle">Spoon-billed Sandpiper Migration Analysis</p>
    </header>

    <!-- ===== 5 统计卡片 ===== -->
    <div class="stats-row">
      <div class="stat-card" v-for="item in statCards" :key="item.label">
        <div class="stat-icon" v-html="item.icon"></div>
        <div class="stat-body">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- ===== 主体：地图 + 图表 ===== -->
    <div class="main-body">
      <!-- 左侧：ECharts Geo 地图 -->
      <div class="map-card">
        <div class="card-header">迁徙轨迹追踪</div>
        <div class="card-body">
          <div class="map-loading" v-if="mapLoading">
            <div class="loading-spinner"></div>
            <span>地图数据加载中...</span>
          </div>
          <div ref="mapChartRef" class="map-chart"></div>
        </div>
      </div>

      <!-- 右侧：3 个复用图表 -->
      <div class="charts-col">
        <div class="chart-card"><SpeedTimeChart :dailyData="dailySpeedData" /></div>
        <div class="chart-card"><HourlyRoseChart :hourlyData="hourlyRoseData" /></div>
        <div class="chart-card"><ScatterChart :scatterData="scatterPlotData" /></div>
      </div>
    </div>

    <!-- ===== 下方：数据简报表格 ===== -->
    <section class="full-card">
      <div class="section-header">
        <h3 class="section-title">关键停歇地一览</h3>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>停歇地点</th><th>到达日期</th><th>停留时间</th><th>核心行为</th></tr>
        </thead>
        <tbody>
          <tr v-for="row in tableData" :key="row.site">
            <td class="cell-site">{{ row.site }}</td>
            <td>{{ row.date }}</td>
            <td>{{ row.duration }}</td>
            <td><span class="behavior-tag" :class="row.tagClass">{{ row.behavior }}</span></td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOverview, getTrack, getStopovers, getDailySpeed, getHourlyActivity, getScatterData } from './api/index.js'
import SpeedTimeChart from './components/SpeedTimeChart.vue'
import HourlyRoseChart from './components/HourlyRoseChart.vue'
import ScatterChart from './components/ScatterChart.vue'

// ======================== Refs & State ========================
const mapChartRef = ref(null)
const mapLoading = ref(true)
let mapChart = null
let roMap = null

// —— 可从 API 刷新的数据（初始为 mock，API 成功后覆盖） ——
const statCards = ref([
  { label: '总里程 (km)', value: '—', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10"/></svg>' },
  { label: '有效天数', value: '—', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/></svg>' },
  { label: '最高时速 (km/h)', value: '—', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>' },
  { label: '停歇点数量', value: '—', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>' },
  { label: 'GPS 记录总数', value: '—', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2.5" stroke-linecap="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>' },
])

const tableData = ref([])
const routeData = ref([])   // 从 API 来的轨迹坐标 [[lng,lat],...]
const stopData = ref([])   // 从 API 来的停歇点
const dailySpeedData = ref([])   // 每日平均速度
const hourlyRoseData = ref([])   // 每小时活动占比
const scatterPlotData = ref([])  // 速度-高度散点

// ======================== 已知锚点（用于坐标→地名+行为匹配） ========================
const ANCHORS = [
  { name: '楚科奇半岛', lng: 177.5, lat: 64.5, behavior: '繁殖、育雏、换羽', tag: 'tag-blue' },
  { name: '堪察加沿岸', lng: 159.0, lat: 56.0, behavior: '中转、浅滩补给', tag: 'tag-amber' },
  { name: '鄂霍次克海', lng: 143.0, lat: 53.0, behavior: '快速补给、避风', tag: 'tag-amber' },
  { name: '库页岛',     lng: 142.5, lat: 48.0, behavior: '跨海前最后补给', tag: 'tag-amber' },
  { name: '辽东湾',     lng: 121.5, lat: 40.5, behavior: '浅滩摄食底栖生物', tag: 'tag-amber' },
  { name: '盐城滩涂',   lng: 120.9, lat: 32.8, behavior: '深度停歇、换羽、增重', tag: 'tag-green' },
  { name: '闽江口湿地', lng: 119.6, lat: 26.1, behavior: '海岸线迁徙关键点', tag: 'tag-amber' },
  { name: '北部湾',     lng: 108.5, lat: 21.5, behavior: '跨国境前最后加油站', tag: 'tag-amber' },
  { name: '马塔班湾',   lng: 97.5,  lat: 16.2, behavior: '抵达越冬地边缘', tag: 'tag-amber' },
  { name: '泰国湾',     lng: 100.5, lat: 13.5, behavior: '长途越冬、躲避极寒', tag: 'tag-green' },
]

function matchAnchor(lng, lat) {
  let best = null, bestD = Infinity
  for (const a of ANCHORS) {
    const d = (a.lng - lng) ** 2 + (a.lat - lat) ** 2
    if (d < bestD) { bestD = d; best = a }
  }
  return bestD < 4 ? best : null   // 阈值 ~2°
}

// ======================== Mock 兜底 ========================
function mockRoute() {
  return [
    [177.5, 64.5], [168, 63], [159, 56], [152, 54.5],
    [143, 53], [142.5, 50], [135, 47], [128, 44],
    [121.5, 40.5], [121, 37], [120.9, 32.8], [120, 29],
    [119.6, 26.1], [115, 24], [108.5, 21.5], [103, 18.5],
    [97.5, 16.2], [99, 15], [100.5, 13.5]
  ]
}

function mockStops() {
  return ANCHORS.map(a => ({
    name: a.name, coord: [a.lng, a.lat],
    days: a.name === '泰国湾' ? 28 : a.name === '盐城滩涂' ? 15 : a.name === '楚科奇半岛' ? 23 : a.name === '辽东湾' ? 7 : a.name === '北部湾' ? 5 : a.name === '堪察加沿岸' ? 4 : a.name === '马塔班湾' ? 4 : 3,
    behavior: a.behavior
  }))
}

// ==================== Douglas-Peucker 轨迹抽稀 ====================
function perpendicularDist(pt, lineStart, lineEnd) {
  const [x, y] = pt
  const [x1, y1] = lineStart
  const [x2, y2] = lineEnd
  const dx = x2 - x1, dy = y2 - y1
  const lenSq = dx * dx + dy * dy
  if (lenSq === 0) return Math.hypot(x - x1, y - y1)
  const t = Math.max(0, Math.min(1, ((x - x1) * dx + (y - y1) * dy) / lenSq))
  return Math.hypot(x - (x1 + t * dx), y - (y1 + t * dy))
}

function douglasPeucker(points, epsilon) {
  if (points.length <= 2) return points.slice()
  let dmax = 0, index = 0
  const end = points.length - 1
  for (let i = 1; i < end; i++) {
    const d = perpendicularDist(points[i], points[0], points[end])
    if (d > dmax) { dmax = d; index = i }
  }
  if (dmax > epsilon) {
    const left = douglasPeucker(points.slice(0, index + 1), epsilon)
    const right = douglasPeucker(points.slice(index), epsilon)
    return left.slice(0, -1).concat(right)
  }
  return [points[0], points[end]]
}

function simplifyRoute(points, epsilon = 0.5) {
  if (points.length <= 50) return points
  return douglasPeucker(points, epsilon)
}

// 滑动窗口平滑：去除 GPS 抖动，N 个点取均值
function smoothPoints(points, windowSize = 12) {
  if (points.length <= windowSize) return points
  const result = []
  const half = Math.floor(windowSize / 2)
  for (let i = 0; i < points.length; i++) {
    const start = Math.max(0, i - half)
    const end = Math.min(points.length - 1, i + half)
    let sx = 0, sy = 0, cnt = 0
    for (let j = start; j <= end; j++) {
      sx += points[j][0]; sy += points[j][1]; cnt++
    }
    result.push([sx / cnt, sy / cnt])
  }
  return result
}

// ==================== 地图渲染 ====================
async function initMap(geoLoaded) {
  const el = mapChartRef.value
  if (!el || el.clientWidth === 0) return

  let geoJSON = geoLoaded
  if (!geoJSON) {
    try {
      const res = await fetch('https://cdn.jsdelivr.net/npm/echarts/map/json/world.json')
      geoJSON = await res.json()
      echarts.registerMap('world', geoJSON)
    } catch (e) { console.warn('GeoJSON 加载失败:', e) }
  }
  mapLoading.value = false

  mapChart = echarts.init(el)
  const rawRoute = routeData.value.length > 0 ? routeData.value : mockRoute()

  // 先平滑去抖 → 再 Douglas-Peucker 抽稀：保留路径整体形态，去掉 GPS 锯齿
  const smoothed = smoothPoints(rawRoute, 12)
  const route = simplifyRoute(smoothed, 0.25)

  // 分段构建（配合 curveness 形成光滑贝塞尔曲线）
  const segments = []
  for (let i = 0; i < route.length - 1; i++) {
    segments.push({ coords: [route[i], route[i + 1]] })
  }

  // 10 个关键锚点（涟漪特效），合并 API 停歇天数
  const stopMap = new Map(stopData.value.map(s => [s.name, s.days]))
  const anchorPoints = ANCHORS.map(a => {
    const days = stopMap.get(a.name)
    return {
      name: a.name, value: [a.lng, a.lat], behavior: a.behavior,
      days: days !== undefined ? days : null
    }
  })

  const option = {
    backgroundColor: '#FFFFFF',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#fff',
      borderColor: '#E5E7EB',
      textStyle: { color: '#1F2937', fontSize: 12 },
      formatter: p => {
        if ((p.seriesType === 'effectScatter' || p.seriesType === 'scatter') && p.data && p.data.name) {
          const b = p.data.behavior || ''
          const d = p.data.days ? `<br/>停歇 ${p.data.days} 天` : ''
          return `<b>${p.data.name}</b>${d}${b ? '<br/>' + b : ''}`
        }
        return ''
      }
    },
    geo: geoJSON ? {
      map: 'world',
      roam: false,
      center: [118, 40],
      zoom: 3.8,
      aspectScale: 0.75,
      layoutCenter: ['50%', '52%'],
      layoutSize: '100%',
      itemStyle: { areaColor: '#E5E7EB', borderColor: '#FFFFFF', borderWidth: 0.8 },
      emphasis: { disabled: true }
    } : undefined,
    series: [
      {
        type: 'lines', coordinateSystem: geoJSON ? 'geo' : 'cartesian2d',
        polyline: false, data: segments,
        lineStyle: { color: '#93C5FD', width: 5, curveness: 0.3, opacity: 0.7 },
        effect: { show: false }, zlevel: 1
      },
      {
        type: 'lines', coordinateSystem: geoJSON ? 'geo' : 'cartesian2d',
        polyline: false, data: segments,
        lineStyle: { color: '#2563EB', width: 2.2, curveness: 0.3, opacity: 1 },
        effect: { show: true, period: 5, trailLength: 0.15, symbol: 'arrow', symbolSize: 7, color: '#1D4ED8' },
        zlevel: 2
      },
      {
        type: 'effectScatter', coordinateSystem: geoJSON ? 'geo' : 'cartesian2d',
        data: anchorPoints,
        rippleEffect: { scale: 3, brushType: 'stroke' },
        showEffectOn: 'render',
        symbol: 'circle', symbolSize: 8,
        itemStyle: { color: '#DC2626' },
        label: { show: true, position: 'right', distance: 8, color: '#374151', fontSize: 11, fontWeight: 500,
          formatter: p => p.data.days ? `${p.data.name}\n停歇${p.data.days}天` : p.data.name
        },
        emphasis: { scale: 1.8 }, zlevel: 10
      }
    ]
  }

  if (!geoJSON) {
    option.geo = undefined
    option.xAxis = { show: false, min: 95, max: 180 }
    option.yAxis = { show: false, min: 10, max: 70 }
    option.backgroundColor = '#FAFBFC'
    option.series.forEach(s => { s.coordinateSystem = 'cartesian2d' })
  }

  mapChart.setOption(option)
}

// ==================== 数据拉取 ====================
async function fetchFromAPI() {
  console.log('[K9] 尝试连接后端 API ...')
  try {
    const [overview, track, stopovers, dailySpeed, hourlyActivity, scatter] = await Promise.all([
      getOverview(), getTrack(), getStopovers(), getDailySpeed(), getHourlyActivity(), getScatterData()
    ])

    // 1. 统计卡片
    const ov = overview.data
    statCards.value[0].value = (ov.totalDistanceKm || 0).toLocaleString()
    statCards.value[1].value = ov.totalDays || '—'
    statCards.value[2].value = (ov.maxSpeedKmh || 0).toFixed(1)
    statCards.value[3].value = ov.stopoverCount || 0
    statCards.value[4].value = (ov.totalRecords || 0).toLocaleString()

    // 2. 轨迹路线
    const trackArr = track.data || []
    if (trackArr.length > 0) {
      routeData.value = trackArr.map(p => [p.lng, p.lat])
    }

    // 3. 停歇点（坐标匹配锚点名称 + 行为）
    const stopArr = stopovers.data || []
    if (stopArr.length > 0) {
      stopData.value = stopArr.map(s => {
        const anchor = matchAnchor(s.lng, s.lat)
        return {
          name: anchor ? anchor.name : `停歇点(${s.lng?.toFixed(1)},${s.lat?.toFixed(1)})`,
          coord: [s.lng, s.lat],
          days: Math.round(s.stayDays || 0),
          behavior: anchor ? anchor.behavior : ''
        }
      })
      // 同步表格
      tableData.value = stopArr.map(s => {
        const anchor = matchAnchor(s.lng, s.lat)
        return {
          site: anchor ? `${anchor.name} (${s.lng.toFixed(2)}, ${s.lat.toFixed(2)})` : `(${s.lng.toFixed(2)}, ${s.lat.toFixed(2)})`,
          date: (s.startTime || '').substring(0, 10),
          duration: `${Math.round(s.stayDays || 0)} 天`,
          behavior: anchor ? anchor.behavior : '—',
          tagClass: anchor ? anchor.tag : 'tag-amber'
        }
      })
    }

    // 4. 每日平均速度 → SpeedTimeChart
	    const dsData = dailySpeed.data || []
	    if (dsData.length > 0) {
	      dailySpeedData.value = dsData.map(d => ({
	        date: d.date ? d.date.substring(5) : '',  // MM-DD
	        avgSpeed: d.avgSpeed
	      }))
	    }

	    // 5. 每小时活动占比 → HourlyRoseChart
	    const haData = hourlyActivity.data || []
	    if (haData.length > 0) {
	      hourlyRoseData.value = haData.map(h => ({
	        hourOfDay: h.hourOfDay,
	        activityCount: h.activityCount
	      }))
	    }

	    // 6. 速度-高度散点 → ScatterChart
	    const scData = scatter.data || []
	    if (scData.length > 0) {
	      scatterPlotData.value = scData.map(s => ({
	        speedKmh: s.speedKmh,
	        altitude: s.altitude
	      }))
	    }

	    console.log('[K9] API 数据加载完成')
  } catch (e) {
    console.warn('[K9] API 不可用，使用 Mock 数据:', e.message)
  }
}

// ==================== ResizeObserver ====================
function observe(el, fn) {
  const ro = new ResizeObserver(() => { if (el.clientWidth > 0) fn() })
  ro.observe(el)
  return ro
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await nextTick()

  // 并行加载 GeoJSON + API 数据
  const geoPromise = (async () => {
    try {
      const res = await fetch('https://cdn.jsdelivr.net/npm/echarts/map/json/world.json')
      const json = await res.json()
      echarts.registerMap('world', json)
      return json
    } catch (e) { console.warn('GeoJSON 加载失败:', e); return null }
  })()

  const apiPromise = fetchFromAPI()

  const geoLoaded = await geoPromise
  await apiPromise

  // 初始化图表（用已加载的数据或 mock）
  if (mapChartRef.value) {
    initMap(geoLoaded)
    roMap = observe(mapChartRef.value, () => initMap(geoLoaded))
  }
})

onUnmounted(() => {
  roMap?.disconnect()
  mapChart?.dispose()
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: #F7F9FC; color: #1F2937; font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.app { max-width: 1440px; margin: 0 auto; padding: 24px 32px 48px; display: flex; flex-direction: column; gap: 16px; }
</style>

<style scoped>
/* ===== 标题卡片 ===== */
.header-card {
  background: #FFFFFF; border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  padding: 22px 32px; text-align: center;
}
.app-title { font-size: 22px; font-weight: 700; color: #111827; letter-spacing: 1px; }
.app-subtitle { font-size: 12px; color: #9CA3AF; margin-top: 4px; }

/* ===== 统计卡片 ===== */
.stats-row { display: flex; gap: 14px; }
.stat-card {
  flex: 1; display: flex; align-items: center; gap: 12px;
  background: #FFFFFF; border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  padding: 16px 20px; transition: box-shadow 0.2s;
}
.stat-card:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.06); }
.stat-icon { flex-shrink: 0; display: flex; align-items: center; }
.stat-value { font-size: 22px; font-weight: 700; color: #111827; }
.stat-label { font-size: 12px; color: #9CA3AF; margin-top: 2px; }

/* ===== 主体：地图 + 图表 ===== */
.main-body { display: flex; gap: 14px; height: 640px; }
.map-card {
  flex: 0 0 65%; background: #FFFFFF; border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  display: flex; flex-direction: column; overflow: hidden;
}
.card-header {
  padding: 14px 20px 0; font-size: 14px; font-weight: 600; color: #111827; flex-shrink: 0;
}
.card-body { flex: 1; margin: 10px 16px 16px; border-radius: 8px; position: relative; overflow: hidden; }
.map-chart { width: 100%; height: 100%; }
.map-loading {
  position: absolute; inset: 0; z-index: 5; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px;
  background: #F3F4F6; color: #9CA3AF; font-size: 13px;
}
.loading-spinner {
  width: 28px; height: 28px; border: 3px solid #E5E7EB;
  border-top: 3px solid #2563EB; border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 右侧图表列 ===== */
.charts-col { flex: 1; display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.chart-card {
  flex: 1; background: #FFFFFF; border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  overflow: hidden; padding: 10px 14px; min-height: 0;
}

/* ===== 下方全宽卡片 ===== */
.full-card {
  background: #FFFFFF; border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  padding: 18px 22px 20px;
}
.section-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.section-title { font-size: 15px; font-weight: 700; color: #111827; }
.section-badge { font-size: 11px; color: #2563EB; background: #EFF6FF; padding: 2px 8px; border-radius: 10px; }
.chart-body { width: 100%; height: 280px; }

/* ===== 表格 ===== */
.data-table { width: 100%; border-collapse: collapse; }
.data-table th {
  text-align: left; padding: 10px 16px; font-size: 12px;
  font-weight: 500; color: #9CA3AF; border-bottom: 1px solid #F3F4F6;
}
.data-table td {
  padding: 12px 16px; font-size: 13px; color: #374151;
  border-bottom: 1px solid #F9FAFB;
}
.data-table tbody tr:last-child td { border-bottom: none; }
.data-table tbody tr:hover { background: #F9FAFB; }
.cell-site { font-weight: 600; color: #111827; }
.behavior-tag { font-size: 11px; padding: 3px 10px; border-radius: 12px; font-weight: 500; }
.tag-blue  { background: #EFF6FF; color: #2563EB; }
.tag-amber { background: #FFFBEB; color: #D97706; }
.tag-green { background: #F0FDF4; color: #16A34A; }
</style>
