# K9-Track 项目完整说明文档

## 一、项目概述

K9-Track 是一个基于 **Apache Spark** 的勺嘴鹬 (Spoon-billed Sandpiper) GPS 迁徙数据分析与可视化系统。勺嘴鹬是极度濒危物种，全球数量不足 500 只。本项目通过模拟其 GPS 追踪数据，完成从数据生成、批处理分析到前端大屏可视化的完整数据流水线。

### 核心功能

- **迁徙路线追踪**：Douglas-Peucker 抽稀 + 滑动窗口平滑，在世界地图上绘制完整迁徙轨迹
- **关键停歇点识别**：网格聚合算法识别停留超 48 小时的停歇地
- **昼夜飞行节律**：24 小时飞行活动占比，南丁格尔玫瑰图呈现
- **多维度统计**：总里程、有效天数、最高时速、阶段统计、速度-高度分布

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + Vite | 3.4 / 5.4 |
| 图表库 | ECharts | 5.5 |
| HTTP 客户端 | Axios | 1.7 |
| 后端框架 | Spring Boot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3 |
| 数据库 | MySQL | 8.0 |
| 大数据引擎 | Apache Spark (local[*]) | 3.3.2 |
| 构建工具 | Maven | 3.6+ |

---

## 二、系统架构

```
┌─────────────────────────────────────────────────────────┐
│                   前端 (Vue 3 + Vite)                     │
│            ECharts Geo 世界地图 + 3 个图表组件             │
│                  http://localhost:3000                    │
└────────────────────────┬────────────────────────────────┘
                         │ Axios HTTP (6 个 REST API)
┌────────────────────────▼────────────────────────────────┐
│              spring-boot-api (Spring Boot 2.7)            │
│              MyBatis-Plus 读取 MySQL 结果表               │
│              含 MockDataGenerator 模拟数据生成器            │
│                  http://localhost:8080                    │
└────────────────────────┬────────────────────────────────┘
                         │ JDBC
┌────────────────────────▼────────────────────────────────┐
│                    MySQL 8.0 (Docker)                     │
│              数据库: k9track (5 张结果表)                  │
│                  127.0.0.1:3307                           │
└────────────────────────┬────────────────────────────────┘
                         │ JDBC 写入
┌────────────────────────▼────────────────────────────────┐
│              spark-processor (Spark 3.3.2)                │
│             local[*] 模式，纯 Java API 编程               │
│          CSV 原始数据 → 5 个批处理算子 → MySQL            │
└─────────────────────────────────────────────────────────┘
```

**架构说明：**

- `spring-boot-api` 不包含任何 Spark 依赖，避免与 Spark 的 Guava/Jackson 产生 Jar 冲突，也大幅减小 JAR 体积（Spark 相关 JAR 约 200MB）
- `spark-processor` 是纯批处理应用，不涉及 Web 服务，运行一次后即可退出
- 前后端通过 REST API 通信，后端已配置 `@CrossOrigin`
- 两条数据路径可独立运作：MockDataGenerator（快速体验）或 Spark（完整流水线）

---

## 三、项目目录结构

```
K9-Track/
│
├── spark-processor/                    # Spark 批处理工程 (独立 Maven 项目)
│   ├── pom.xml                         # Spark 3.3.2 + MySQL Connector, JDK 8
│   └── src/main/java/com/xue/spark/
│       └── SparkAnalyzer.java          # 主程序 (5 个批处理算子)
│
├── spring-boot-api/                    # Web API 工程 (独立 Maven 项目)
│   ├── pom.xml                         # Spring Boot 2.7.18 + MyBatis-Plus, JDK 17
│   └── src/main/
│       ├── java/com/xue/k9track/
│       │   ├── K9TrackApplication.java       # Spring Boot 启动类
│       │   ├── controller/
│       │   │   └── MigrationController.java  # REST API 控制器 (6 个接口)
│       │   ├── service/
│       │   │   └── MigrationService.java     # 业务逻辑层
│       │   ├── entity/                       # 5 个实体类
│       │   │   ├── MigrationSummary.java     # 迁徙总览
│       │   │   ├── TrackPoint.java           # 轨迹点
│       │   │   ├── Stopover.java             # 停歇点
│       │   │   ├── HourlyActivity.java       # 小时活动
│       │   │   └── ScatterData.java          # 散点数据
│       │   ├── mapper/                       # 5 个 MyBatis-Plus Mapper 接口
│       │   ├── util/HaversineUtil.java       # Haversine 距离计算工具
│       │   └── mock/MockDataGenerator.java   # 模拟数据生成器
│       └── resources/application.yml         # 数据库连接配置
│
├── frontend/                           # 前端工程 (Vue 3 + Vite)
│   ├── package.json                    # Vue 3.4, ECharts 5.5, Axios 1.7
│   ├── vite.config.js                  # Vite 配置 (含 API 代理)
│   └── src/
│       ├── main.js                     # Vue 应用入口
│       ├── App.vue                     # 主布局 (白底极简大屏风格, 含 ECharts Geo 地图)
│       ├── api/index.js                # Axios 封装 (6 个 API 函数)
│       └── components/
│           ├── SpeedTimeChart.vue      # 迁徙速度时序折线图
│           ├── HourlyRoseChart.vue     # 昼夜飞行南丁格尔玫瑰图
│           └── ScatterChart.vue        # 高度-速度散点图
│
├── sql/init.sql                        # 数据库完整 DDL (5 张表 + 建库)
├── docker-compose.yml                  # Docker MySQL 8.0 一键启动
├── input/                              # 原始 GPS CSV 数据
│   └── spoonbill_k9_gps_corrected.csv  # 194,500 条记录
├── README.md                           # 快速开始指南
└── PROJECT.md                          # 本文件 (项目完整说明)
```

---

## 四、数据流水线

### 4.1 路径 A：MockDataGenerator 快速体验

```
MockDataGenerator.java
  → JDBC 直连 MySQL
  → 模拟 80 天迁徙数据 (182,000 GPS 点)
  → 写入 5 张表 (含停歇点聚类、昼夜节律等)
  → Spring Boot API 读取
  → 前端可视化
```

**优势**：无需 CSV 文件，无需 Spark，30 秒内获得完整可用的数据库。

### 4.2 路径 B：Spark 完整批处理流水线

```
CSV 原始数据 (194,500 行)
  → Spark DataFrame 加载
  → 算子 1: 轨迹抽稀 (filter mod 50) → track_point 表
  → 算子 2: 网格聚合停歇点 (groupBy + 时间过滤) → stopover 表
  → 算子 3: 昼夜飞行占比 (flight/total × 100) → hourly_activity 表
  → 算子 4: 随机散点采样 (orderBy rand limit) → scatter_data 表
  → 算子 5: Haversine 总里程 + 总览 → migration_summary 表
  → Spring Boot API 读取
  → 前端可视化
```

**优势**：展示完整的大数据批处理流程，包含 UDF 注册、窗口函数、分区调优。

---

## 五、数据库设计

数据库：`k9track`，字符集：`utf8mb4`，共 5 张表。

### 5.1 migration_summary（迁徙总览表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK) | 自增主键 |
| total_distance | DOUBLE | 总迁徙里程 (km) |
| avg_speed | DOUBLE | 全程平均时速 (km/h) |
| max_speed | DOUBLE | 最高时速 (km/h) |
| total_stopover_days | DOUBLE | 累计停歇天数 |
| update_time | DATETIME | 数据更新时间 |

数据量：1 行。

### 5.2 track_point（轨迹点表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK) | 自增主键 |
| point_id | INT | 原始 CSV 中的点 ID |
| timestamp | VARCHAR(50) | 时间戳 (yyyy-MM-dd HH:mm:ss) |
| longitude | DOUBLE | 经度 |
| latitude | DOUBLE | 纬度 |
| altitude | DOUBLE | 海拔 (m) |
| speed_kmh | DOUBLE | 瞬时速度 (km/h) |
| phase | VARCHAR(50) | 所属阶段 (中文) |
| point_type | VARCHAR(50) | 记录类型 (英文) |

数据量：~3,890 行（point_id % 50 == 0 抽稀）。

### 5.3 stopover（停歇点表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK) | 自增主键 |
| longitude | DOUBLE | 中心经度 |
| latitude | DOUBLE | 中心纬度 |
| start_time | VARCHAR(50) | 抵达时间 |
| end_time | VARCHAR(50) | 离开时间 |
| stay_days | DOUBLE | 停留天数 |

数据量：2~15 行（取决于数据源）。

### 5.4 hourly_activity（昼夜活动频次表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK) | 自增主键 |
| hour_of_day | INT | 小时 (0-23) |
| activity_count | INT | 该小时飞行记录占比 (%) |

数据量：24 行。Spark 计算逻辑：`flight_pct = flight_count / total_count × 100`，只统计 speed > 20 km/h 的飞行记录。

### 5.5 scatter_data（散点采样表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK) | 自增主键 |
| speed_kmh | DOUBLE | 瞬时速度 (km/h) |
| altitude | DOUBLE | 海拔 (m) |

数据量：2,000 行（从迁徙阶段随机采样）。

---

## 六、API 接口文档

Base URL: `http://localhost:8080`

所有接口使用 GET 方法，返回 JSON，已配置 `@CrossOrigin(origins = "*")`。

### 6.1 迁徙总览

```
GET /api/migration/overview
```

**响应示例：**

```json
{
  "totalDistanceKm": 9710.5,
  "avgSpeedKmh": 38.2,
  "maxSpeedKmh": 72.6,
  "totalStopoverDays": 37,
  "stopoverCount": 2,
  "totalRecords": 182000,
  "startDate": "2024-04-03",
  "endDate": "2024-06-22",
  "totalDays": 80,
  "updateTime": "2026-05-21 22:10:36"
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| totalDistanceKm | Number | 总迁徙里程 (km) |
| avgSpeedKmh | Number | 全程平均时速 (km/h) |
| maxSpeedKmh | Number | 最高瞬时速度 (km/h) |
| totalStopoverDays | Number | 累计停歇天数 |
| stopoverCount | Number | 有效停歇点数量 |
| totalRecords | Number | GPS 记录总数 |
| startDate | String | 迁徙起始日期 |
| endDate | String | 迁徙结束日期 |
| totalDays | Number | 迁徙总天数 |
| updateTime | String | 数据更新时间 |

---

### 6.2 轨迹坐标

```
GET /api/migration/track
```

**响应示例：**

```json
[
  { "lng": 177.5, "lat": 64.5 },
  { "lng": 168.0, "lat": 63.0 },
  { "lng": 159.0, "lat": 56.0 }
]
```

数据量约 3,890 个坐标点，前端经过滑动窗口平滑 + Douglas-Peucker 抽稀后再渲染。

---

### 6.3 停歇点

```
GET /api/migration/stopovers
```

**响应示例：**

```json
[
  {
    "lat": 32.8,
    "lng": 120.9,
    "startTime": "2024-04-28 18:29:04",
    "endTime": "2024-05-15 12:15:22",
    "stayDays": 17.4
  },
  {
    "lat": 16.2,
    "lng": 97.5,
    "startTime": "2024-05-31 11:03:08",
    "endTime": "2024-06-18 08:45:11",
    "stayDays": 17.3
  }
]
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| lat | Number | 停歇点纬度 |
| lng | Number | 停歇点经度 |
| startTime | String | 抵达时间 |
| endTime | String | 离开时间 |
| stayDays | Number | 停留天数 |

前端通过坐标匹配预定义的 10 个锚点（楚科奇半岛 → 泰国湾），自动关联地名和行为描述。

---

### 6.4 昼夜活动频次

```
GET /api/migration/hourly-activity
```

**响应示例：**

```json
[
  { "hourOfDay": 0, "activityCount": 0 },
  { "hourOfDay": 5, "activityCount": 71 },
  { "hourOfDay": 9, "activityCount": 89 },
  { "hourOfDay": 15, "activityCount": 89 },
  { "hourOfDay": 22, "activityCount": 1 }
]
```

数据量 24 条，`activityCount` 为该小时处于飞行状态（speed > 20 km/h）的记录占比（%）。后端会自动补全 DB 中缺失的小时（值为 0）。

---

### 6.5 速度-高度散点

```
GET /api/migration/scatter
```

**响应示例：**

```json
[
  { "speedKmh": 33.03, "altitude": 888.39 },
  { "speedKmh": 52.17, "altitude": 1452.61 }
]
```

数据量 2,000 点，从迁徙阶段随机采样，前端渲染为 ECharts 散点图（x = 速度，y = 高度）。

---

### 6.6 每日平均速度

```
GET /api/migration/daily-speed
```

**响应示例：**

```json
[
  { "date": "2024-04-03", "avgSpeed": 40.5 },
  { "date": "2024-04-04", "avgSpeed": 38.2 }
]
```

数据量约 80 条（等于迁徙天数），后端按日期分组计算 `avg(speed_kmh)`。前端渲染为时序折线图，X 轴为 MM-DD，Y 轴为 km/h。

---

## 七、前端架构

### 7.1 页面布局

```
┌──────────────────────────────────────────────────────────┐
│           勺嘴鹬迁徙监测与分析平台 — K9-Track               │
├─────────┬─────────┬─────────┬─────────┬─────────────────┤
│ 总里程   │ 有效天数 │ 最高时速 │ 停歇点数 │ GPS 记录总数     │  ← 5 统计卡片
├───────────────────────────────┬───────────────────────────┤
│                               │  迁徙速度时序分布 (折线图)   │
│    ECharts Geo 世界地图        ├───────────────────────────┤
│    + 蓝色迁徙贝塞尔曲线         │  昼夜飞行节律 (玫瑰图)      │
│    + 红色涟漪停歇标记           ├───────────────────────────┤
│    + 地名 + 停留天数标签        │  高度-速度散点 (散点图)     │
├───────────────────────────────┴───────────────────────────┤
│                  关键停歇地一览 (表格)                      │
└──────────────────────────────────────────────────────────┘
```

### 7.2 地图渲染流程

```
API 返回 ~3890 坐标点
  → 滑动窗口平滑 (windowSize=12, 去 GPS 抖动)
  → Douglas-Peucker 抽稀 (epsilon=0.25, 保留关键拐点)
  → 分段构建 {coords: [[start], [end]]} 数组
  → ECharts lines 系列 (curveness=0.3, 蓝色渐变 + 箭头动效)
  → 10 个锚点 effectScatter (红色涟漪, 匹配停歇天数)
```

### 7.3 组件说明

| 组件 | Props | 数据来源 | 图表类型 |
|------|-------|---------|---------|
| `SpeedTimeChart.vue` | `dailyData: [{date, avgSpeed}]` | `/api/migration/daily-speed` | ECharts 折线 + 渐变面积 |
| `HourlyRoseChart.vue` | `hourlyData: [{hourOfDay, activityCount}]` | `/api/migration/hourly-activity` | ECharts 南丁格尔玫瑰图 |
| `ScatterChart.vue` | `scatterData: [{speedKmh, altitude}]` | `/api/migration/scatter` | ECharts 散点图 |

三个组件均支持 props 驱动：传入有效数据时使用 API 数据，为空时自动降级为 Mock 数据以保证开发阶段也能看到图表效果。

### 7.4 配色方案

- **整体风格**：白底极简 (`#FFFFFF` + `#F7F9FC` 背景)
- **地图**：陆地 `#E5E7EB`（浅灰）、国界线白色
- **迁徙线**：底色 `#93C5FD` (5px) + 上层 `#2563EB` (2.2px) + 蓝色箭头动效
- **停歇标记**：红色涟漪 `#DC2626`
- **玫瑰图**：莫兰迪低饱和色系（灰蓝 → 灰绿 → 灰粉 → 灰紫，24 段过渡）
- **折线图**：`#1890FF` 蓝色 + 渐变面积
- **散点图**：半透明蓝 `rgba(24,144,255,0.55)`

---

## 八、Spark 批处理详解

### 8.1 环境配置

```java
SparkSession spark = SparkSession.builder()
    .appName("K9-Track-GPS-Analyzer")
    .master("local[*]")                              // 使用所有 CPU 核心
    .config("spark.sql.adaptive.enabled", "true")     // AQE 自适应查询优化
    .config("spark.sql.shuffle.partitions", "8")      // 适配单机 (默认 200 为集群设计)
    .config("spark.driver.memory", "4g")              // Driver 内存
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .getOrCreate();
```

### 8.2 五个批处理算子

| # | 算子 | Spark API | 输出表 | 关键技术 |
|---|------|-----------|--------|---------|
| 1 | 轨迹抽稀 | `filter(mod(point_id, 50) = 0)` | track_point | 均匀间隔采样，保留时间分布特征 |
| 2 | 停歇点识别 | `round(lat,2).groupBy` + 时间差 >48h | stopover | 空间网格分箱 + Haversine 距离过滤 |
| 3 | 昼夜飞行 | `hour(ts).groupBy` + flight/total × 100 | hourly_activity | 双 groupBy 后 join 计算百分比 |
| 4 | 散点采样 | `filter(migration).orderBy(rand()).limit(2000)` | scatter_data | 随机采样 |
| 5 | 总览计算 | Window `lag()` + Haversine UDF + `sum()` | migration_summary | 注册 UDF，在抽稀数据上逐行算邻点距离 |

### 8.3 Haversine UDF

```java
spark.udf().register("haversine", (Double lat1, Double lng1, Double lat2, Double lng2) -> {
    final double R = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}, DataTypes.DoubleType);
```

总里程计算通过窗口函数 `lag()` 获取相邻坐标后再套用 Haversine UDF 求和。

### 8.4 Java 17 兼容性

Spark 3.3.2 在 JDK 17 上运行需要添加 `--add-opens` 参数：

```
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
```

在 IDEA 中配置于 `Run → Edit Configurations → VM options`。

---

## 九、前端关键算法

### 9.1 Douglas-Peucker 轨迹抽稀

```javascript
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
```

epsilon = 0.25（约 28km 垂直距离阈值），在保持路径形态的前提下将 ~3800 点压缩到 ~30 段。

### 9.2 滑动窗口平滑

```javascript
function smoothPoints(points, windowSize = 12) {
  const result = []
  const half = Math.floor(windowSize / 2)
  for (let i = 0; i < points.length; i++) {
    const start = Math.max(0, i - half)
    const end = Math.min(points.length - 1, i + half)
    let sx = 0, sy = 0, cnt = 0
    for (let j = start; j <= end; j++) { sx += points[j][0]; sy += points[j][1]; cnt++ }
    result.push([sx / cnt, sy / cnt])
  }
  return result
}
```

窗口大小 12，去除 GPS 模拟数据中的随机抖动。

### 9.3 锚点坐标匹配

前端预定义了 10 个关键锚点（从楚科奇半岛到泰国湾），API 返回的停歇点通过欧几里得距离匹配到最近锚点（阈值 2°），自动关联地名、行为和颜色标签。

---

## 十、路径配置说明

项目中有 **2 个文件** 使用了硬编码的绝对路径 `E:/project/idea/K9-Track/`，克隆到本地后需要改成你自己的项目路径。

### 需修改的文件

| 文件 | 行号 | 硬编码路径 | 说明 |
|------|------|-----------|------|
| `spark-processor/.../SparkAnalyzer.java` | 第 74 行 | `E:/project/idea/K9-Track/input/spoonbill_k9_real_route.csv` | Spark 读取的 CSV 输入路径 |
| `spring-boot-api/.../mock/MockDataGenerator.java` | 第 20 行 | `E:/project/idea/K9-Track/input/spoonbill_k9_real_route.csv` | MockDataGenerator 输出的 CSV 路径 |

### 修改方式

假设你的项目放在 `D:/workspace/K9-Track`，将上述两个文件中的路径改为：

```java
String csvPath = "D:/workspace/K9-Track/input/spoonbill_k9_real_route.csv";
```

> 注意：路径分隔符使用正斜杠 `/`（Java 跨平台兼容），不要用反斜杠 `\`。

### 其他不用改的

- **`docker-compose.yml`**：无硬编码路径，只有端口映射 `3307:3306`
- **`application.yml`**：数据库连接使用 `127.0.0.1:3307`，无本地路径
- **前端 `api/index.js`**：`baseURL: 'http://localhost:8080'`，无需修改
- **`pom.xml`**：Maven 坐标均为相对/远程依赖，无本地路径
- **文档中的 `cd` 命令**：那是终端操作示例，不是配置文件，按你的实际目录执行即可

---

## 十一、运行指南

### 11.1 环境准备

| 组件 | 版本要求 | 验证命令 |
|------|----------|---------|
| JDK | 17 | `java -version` |
| Maven | 3.6+ | `mvn -v` |
| Node.js | 18+ | `node -v` |
| Docker Desktop | 最新版 | `docker -v` |

### 11.2 启动步骤

**1. 启动 MySQL**

```powershell
cd &lt;你的项目路径&gt;
docker compose up -d
```

**2. 生成数据（二选一）**

快速体验：
```
IDEA 打开 spring-boot-api → 运行 MockDataGenerator.java
```

完整流水线：
```
IDEA 打开 spark-processor → 运行 SparkAnalyzer.java
```

**3. 启动后端 API**

```
IDEA 打开 spring-boot-api → 运行 K9TrackApplication.java
```

验证：
```powershell
curl http://localhost:8080/api/migration/overview
```

**4. 启动前端**

```powershell
cd frontend
npm install
npm run dev
```

打开 http://localhost:3000

### 11.3 验证清单

- [ ] 5 个统计卡片显示真实数据（非 "—"）
- [ ] 世界地图轮廓可见，蓝色迁徙曲线从右上（楚科奇）连到左下（泰国）
- [ ] 红色涟漪标记显示地名 + 停歇天数
- [ ] 右侧三个图表正常渲染
- [ ] 底部停歇地表格有数据
- [ ] 整体白底风格统一

---

## 十二、常见问题

**Q: Spring Boot 启动报 `Port 8080 is already in use`**

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Q: 前端地图不显示世界轮廓**

ECharts Geo 从 `cdn.jsdelivr.net` 异步加载世界地图 GeoJSON。若网络不通，页面降级为笛卡尔坐标系，迁徙线仍可见。

**Q: 图表数据为空（显示 Mock 数据）**

确认：1) API 已启动 2) MySQL 有数据。打开 F12 → Network 检查接口返回。

**Q: Spark 报 `cannot access class sun.nio.ch.DirectBuffer`**

JDK 17 兼容性问题，在 IDEA Run Configuration 的 VM options 中添加 `--add-opens` 参数（见 8.4 节）。

**Q: MySQL 容器启动失败**

检查 3307 端口是否被占用：`netstat -ano | findstr :3307`。可修改 `docker-compose.yml` 中的端口映射。

**Q: MockDataGenerator 生成的停歇点太多或太少**

修改 `MockDataGenerator.java` 中的 `stopoverPointsPerSite` 和 `flightTimeDays/stopTimeDays` 参数。
