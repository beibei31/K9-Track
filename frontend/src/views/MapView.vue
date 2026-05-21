<template>
  <div class="map-wrap">
    <div ref="mapContainer" class="map-inner"></div>
    <div class="map-loading" v-if="loading">
      <div class="loading-spinner"></div>
      <span>地图加载中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const mapContainer = ref(null)
const loading = ref(true)
let map = null
let polyline = null
let markers = []

// Mock 轨迹数据：勺嘴鹬迁徙路线（泰国 → 俄罗斯北极）
function mockTrack() {
  const points = []
  // 泰国越冬地 → 缅甸 → 云南 → 四川 → 甘肃 → 蒙古 → 俄罗斯
  const waypoints = [
    [100.5, 13.7], [100.8, 15.0], [99.5, 17.5], [98.0, 19.0],
    [100.2, 20.5], [101.5, 21.5], [103.0, 23.0], [104.5, 24.5],
    [106.0, 26.0], [107.5, 27.5], [109.0, 29.0], [111.0, 30.5],
    [113.0, 32.0], [115.0, 33.5], [117.0, 35.0], [119.0, 36.5],
    [120.5, 38.0], [122.0, 39.5], [124.0, 41.5], [126.0, 43.5],
    [129.0, 45.5], [132.0, 47.5], [135.0, 49.5], [138.0, 51.5],
    [142.0, 53.5], [146.0, 55.5], [150.0, 57.5], [154.0, 59.5],
    [158.0, 60.5], [162.0, 61.5]
  ]
  // 在每个航点之间插值生成更多点（模拟连续轨迹）
  for (let i = 0; i < waypoints.length - 1; i++) {
    const [lng1, lat1] = waypoints[i]
    const [lng2, lat2] = waypoints[i + 1]
    const steps = 8
    for (let s = 0; s < steps; s++) {
      const t = s / steps
      const lng = lng1 + (lng2 - lng1) * t + (Math.random() - 0.5) * 0.8
      const lat = lat1 + (lat2 - lat1) * t + (Math.random() - 0.5) * 0.5
      points.push([lng, lat])
    }
  }
  points.push(waypoints[waypoints.length - 1])
  return points
}

// Mock 停歇点（对应统计卡片中的 12 个）
function mockStopovers() {
  return [
    { lng: 100.5, lat: 13.7, name: '泰国越冬地', days: 28 },
    { lng: 99.2, lat: 17.8, name: '缅甸萨尔温江口', days: 5 },
    { lng: 101.5, lat: 21.5, name: '云南西双版纳', days: 3 },
    { lng: 106.0, lat: 26.0, name: '贵州草海', days: 4 },
    { lng: 111.0, lat: 30.5, name: '洞庭湖', days: 8 },
    { lng: 115.5, lat: 33.8, name: '江苏盐城湿地', days: 15 },
    { lng: 119.0, lat: 36.5, name: '黄河三角洲', days: 10 },
    { lng: 121.5, lat: 39.5, name: '辽东湾', days: 7 },
    { lng: 129.0, lat: 45.5, name: '兴凯湖', days: 6 },
    { lng: 138.0, lat: 51.5, name: '鄂霍次克海沿岸', days: 5 },
    { lng: 150.0, lat: 57.5, name: '堪察加半岛', days: 4 },
    { lng: 162.0, lat: 61.5, name: '楚科奇繁殖地', days: 23 }
  ]
}

onMounted(async () => {
  try {
    const AMap = await AMapLoader.load({
      key: import.meta.env.VITE_AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.Marker', 'AMap.Polyline', 'AMap.InfoWindow']
    })

    map = new AMap.Map(mapContainer.value, {
      zoom: 4.5,
      center: [117, 36],
      mapStyle: 'amap://styles/light',    // 浅色地图配白底UI
      viewMode: '2D',
      resizeEnable: true
    })

    drawRoute(AMap)
    drawStopovers(AMap)
    loading.value = false
  } catch (e) {
    console.error('高德地图加载失败:', e)
    loading.value = false
  }
})

function drawRoute(AMap) {
  const path = mockTrack()
  polyline = new AMap.Polyline({
    path,
    strokeColor: '#1890ff',
    strokeWeight: 3,
    strokeOpacity: 0.85,
    showDir: true,
    dirColor: '#ff7a45',
    lineJoin: 'round',
    lineCap: 'round'
  })
  map.add(polyline)
  map.setFitView([polyline], false, [60, 60, 60, 60])
}

function drawStopovers(AMap) {
  const stopovers = mockStopovers()
  stopovers.forEach((s, i) => {
    const marker = new AMap.Marker({
      position: [s.lng, s.lat],
      title: s.name,
      icon: new AMap.Icon({
        size: new AMap.Size(22, 28),
        imageSize: new AMap.Size(22, 28),
        image: 'data:image/svg+xml,' + encodeURIComponent(
          `<svg xmlns="http://www.w3.org/2000/svg" width="22" height="28">
            <path d="M11 0C5.5 0 1 4.2 1 9.5c0 5 10 18 10 18s10-13 10-18C21 4.2 16.5 0 11 0z" fill="#ff5252" stroke="#fff" stroke-width="1.5"/>
            <circle cx="11" cy="9" r="3" fill="#fff"/>
          </svg>`
        )
      }),
      offset: new AMap.Pixel(-11, -28),
      zIndex: 100 + i
    })

    const infoWindow = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -32),
      content: `
        <div style="padding:8px 12px;font-size:13px;line-height:1.7;color:#1a1a1a;min-width:140px">
          <b style="font-size:14px;color:#1890ff">${s.name}</b><br/>
          停留: <b>${s.days} 天</b>
        </div>`
    })

    marker.on('click', () => {
      infoWindow.open(map, marker.getPosition())
    })

    map.add(marker)
    markers.push(marker)
  })
}

onUnmounted(() => {
  markers.forEach(m => m?.setMap(null))
  polyline?.setMap(null)
  map?.destroy()
})
</script>

<style scoped>
.map-wrap {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
}
.map-inner {
  width: 100%;
  height: 100%;
}
.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #eef2f5;
  color: #8c8c8c;
  font-size: 13px;
  z-index: 5;
}
.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e0e0e0;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
