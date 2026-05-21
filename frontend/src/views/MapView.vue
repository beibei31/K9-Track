<template>
  <div class="map-wrapper">
    <div v-if="loading" class="map-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>地图加载中...</span>
    </div>
    <div id="amap-container" ref="mapContainer"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  track: { type: Array, default: () => [] },
  stopovers: { type: Array, default: () => [] },
  loading: { type: Boolean, default: true }
})

const mapContainer = ref(null)
let mapInstance = null
let polylineInstance = null

onMounted(async () => {
  await initMap()
})

watch(() => props.track, (newTrack) => {
  if (newTrack.length > 0 && mapInstance) {
    nextTick(() => drawRoute(newTrack))
  }
})

watch(() => props.stopovers, (newStopovers) => {
  if (newStopovers.length > 0 && mapInstance) {
    nextTick(() => drawStopovers(newStopovers))
  }
})

async function initMap() {
  try {
    const AMap = await AMapLoader.load({
      key: import.meta.env.VITE_AMAP_KEY,  // 从 .env 文件读取高德 Key
      version: '2.0',
      plugins: ['AMap.Marker', 'AMap.Polyline', 'AMap.InfoWindow']
    })

    mapInstance = new AMap.Map('amap-container', {
      zoom: 5,
      center: [116, 35],
      mapStyle: 'amap://styles/darkblue',
      viewMode: '3D'
    })

    // 如果数据已经加载，立即绘制
    if (props.track.length > 0) drawRoute(props.track)
    if (props.stopovers.length > 0) drawStopovers(props.stopovers)
  } catch (e) {
    console.error('高德地图加载失败:', e)
  }
}

function drawRoute(track) {
  if (!mapInstance || !window.AMap) return
  const AMap = window.AMap

  if (polylineInstance) {
    mapInstance.remove(polylineInstance)
  }

  const path = track.map(p => [p.lng, p.lat])
  polylineInstance = new AMap.Polyline({
    path,
    strokeColor: '#4fc3f7',
    strokeWeight: 3,
    strokeOpacity: 0.8,
    showDir: true,
    dirColor: '#ff9800'
  })
  mapInstance.add(polylineInstance)
  mapInstance.setFitView([polylineInstance])
}

function drawStopovers(stopovers) {
  if (!mapInstance || !window.AMap) return
  const AMap = window.AMap

  stopovers.forEach(s => {
    const marker = new AMap.Marker({
      position: [s.lng, s.lat],
      title: `停歇点 (${s.stayDays}天)`,
      icon: new AMap.Icon({
        size: new AMap.Size(20, 20),
        imageSize: new AMap.Size(20, 20),
        image: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"><circle cx="10" cy="10" r="8" fill="%23ff5252" stroke="%23fff" stroke-width="2"/></svg>'
      })
    })

    const infoWindow = new AMap.InfoWindow({
      content: `
        <div style="padding:8px;font-size:12px;line-height:1.6">
          <b>停歇点</b><br/>
          开始: ${s.startTime}<br/>
          结束: ${s.endTime}<br/>
          停留: <b>${s.stayDays} 天</b>
        </div>`,
      offset: new AMap.Pixel(0, -25)
    })

    marker.on('click', () => {
      infoWindow.open(mapInstance, marker.getPosition())
    })

    mapInstance.add(marker)
  })
}
</script>

<style scoped>
.map-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}
#amap-container {
  width: 100%;
  height: 100%;
}
.map-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #8899aa;
  z-index: 10;
}
</style>
