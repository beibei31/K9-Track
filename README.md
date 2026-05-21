# K9-Track：基于 Spark 的勺嘴鹬迁徙分析与可视化系统

## 项目概述

本项目是一个基于 **Apache Spark** 的大数据批处理应用系统，以极度濒危物种**勺嘴鹬 (Spoon-billed Sandpiper)** 的 GPS 追踪数据为核心，完成迁徙路线分析、停歇点识别、各阶段统计和昼夜飞行习性等分析任务。

系统采用**纯本地运行架构**，利用 Spark 本地模式处理复杂时空计算，避免了单机环境搭建 Hadoop 集群的开支，完全符合轻量级计算与前后端联调的业务需求。

## 系统架构

```
┌──────────────────────────────────────────────────┐
│                 前端 (Vue 3 + Vite)                │
│         高德地图 JS API 2.0 + ECharts 5            │
│              http://localhost:3000                 │
└──────────────────────┬───────────────────────────┘
                       │ Axios HTTP
┌──────────────────────▼───────────────────────────┐
│         spring-boot-api (Spring Boot 2.7)         │
│          MyBatis-Plus + JDBC 读取 MySQL            │
│          ⚠ 不包含任何 Spark 依赖                    │
│              http://localhost:8080                 │
└──────────────────────┬───────────────────────────┘
                       │ JDBC
┌──────────────────────▼───────────────────────────┐
│                 MySQL 8.0 (本地)                    │
│         数据库: k9track (5 张结果表)                │
│              127.0.0.1:3306                       │
└──────────────────────┬───────────────────────────┘
                       │ JDBC 写入
┌──────────────────────▼───────────────────────────┐
│           spark-processor (纯 Java 应用)            │
│          Spark 3.3.2 local[*] 模式               │
│          读取本地 CSV → 聚合计算 → 写入 MySQL       │
│          ⚠ 不涉及任何 Web 服务，仅运行一次          │
└──────────────────────┴───────────────────────────┘
```

## 项目结构

```
K9-Track/
├── spark-processor/               # 大数据离线处理工程 (独立 Maven 项目)
│   ├── pom.xml                    # Spark 3.3.2 + MySQL 驱动
│   └── src/main/
│       ├── java/com/xue/spark/
│       │   └── SparkAnalyzer.java # Spark 批处理主程序
│       └── resources/data/
│           └── spoonbill_k9_gps.csv  # 194,500 条 GPS 数据
│
├── spring-boot-api/               # Web API 工程 (独立 Maven 项目，无 Spark)
│   ├── pom.xml                    # Spring Boot 2.7.18 + MyBatis-Plus
│   └── src/main/
│       ├── java/com/xue/k9track/
│       │   ├── K9TrackApplication.java
│       │   ├── entity/            # 5 个实体类
│       │   ├── mapper/            # 5 个 MyBatis-Plus Mapper
│       │   ├── service/
│       │   │   └── MigrationService.java
│       │   ├── controller/
│       │   │   └── MigrationController.java  # 6 个 REST API
│       │   └── util/
│       │       └── HaversineUtil.java        # 半正矢距离工具
│       └── resources/
│           └── application.yml
│
├── frontend/                      # 前端工程 (Vue 3 + Vite)
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.js
│       ├── App.vue                # 主布局 (暗色大屏风格)
│       ├── api/index.js           # Axios 请求封装
│       ├── views/
│       │   └── MapView.vue        # 高德地图 (迁徙路线 + 停歇标记)
│       └── components/
│           ├── StatsCards.vue     # 核心统计卡片
│           ├── SpeedTimeChart.vue # ECharts 速度时序折线图
│           ├── HourlyRoseChart.vue # ECharts 南丁格尔玫瑰图
│           └── ScatterChart.vue   # ECharts 高度-速度散点图
│
├── input/
│   └── spoonbill_k9_gps_corrected.csv   # 原始数据源
└── README.md
```

## 数据集说明

| 属性 | 值 |
|------|-----|
| 文件名 | `spoonbill_k9_gps.csv` |
| 数据量 | **194,500 条** GPS 追踪记录 |
| 时间范围 | 2024-04-01 至 2024-08-14 |
| 地理范围 | 泰国 (12.96°N) 至 俄罗斯北极 (62.05°N) |
| 字段 | `point_id, timestamp, latitude, longitude, altitude, speed_kmh, phase, point_type` |
| 物候期 | breeding (繁殖), migration (迁徙), stopover (停歇) |

数据来源：通过模拟勺嘴鹬在东亚-澳大拉西亚迁飞区的迁徙路线生成，覆盖从泰国越冬地到俄罗斯北极繁殖地的完整迁徙周期。

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | **1.8** | Spark 和 Spring Boot 统一使用 |
| MySQL | **8.0** | 本地运行，端口 3306 |
| Maven | 3.6+ | 管理两个 Java 工程 |
| Node.js | 18+ | 前端构建 |
| IDEA | 任意版本 | 分别打开两个 Maven 工程 |

> **无需安装 Hadoop / Spark 集群** — Spark 以 `local[*]` 模式嵌入运行。

## 运行指南（纯本地 / 单机）

### 步骤 1：准备 MySQL

确保本地 MySQL 服务已启动，默认连接信息为：
- URL: `127.0.0.1:3306`
- 用户名: `root`
- 密码: `root`

> 如需修改，请同时更新 `spark-processor/.../SparkAnalyzer.java` 和 `spring-boot-api/.../application.yml` 中的数据库连接信息。

### 步骤 2：运行 Spark 批处理

在 IDEA 中打开 `spark-processor` 目录作为独立 Maven 项目：

```bash
# 1. Maven 加载依赖 (IDEA 自动完成)
# 2. 运行 SparkAnalyzer 的 main 方法
```

SparkAnalyzer 将自动完成：
1. 创建 `k9track` 数据库和 5 张结果表
2. 读取 `src/main/resources/data/spoonbill_k9_gps.csv`
3. 执行 5 个批处理算子（抽稀、网格聚合、阶段统计、小时活动、散点采样）
4. 通过 JDBC 将结果写入 MySQL

运行日志会显示处理进度和写入记录数。

### 步骤 3：启动 Spring Boot API

在 IDEA 中打开 `spring-boot-api` 目录作为独立 Maven 项目：

1. Maven 加载依赖
2. 运行 `K9TrackApplication` 的 main 方法
3. Spring Boot 启动在 **http://localhost:8080**

验证 API：
```bash
curl http://localhost:8080/api/migration/overview
curl http://localhost:8080/api/migration/track
curl http://localhost:8080/api/migration/stopovers
```

### 步骤 4：启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 **http://localhost:3000**，打开浏览器即可查看可视化大屏。

> Vite 已配置 `/api` 代理到 `localhost:8080`，开发时无跨域问题。

## 前后端联调说明

前端通过 Axios 直接请求 `http://localhost:8080`，后端 Controller 已添加 `@CrossOrigin(origins = "*")` 注解。前后端分离部署，通过 HTTP REST API 通信。

**API 接口清单：**

| 端点 | 说明 |
|------|------|
| `GET /api/migration/overview` | 总览数据（总里程、总天数、最快时速、停歇点数） |
| `GET /api/migration/phase-stats` | 各阶段统计（平均速度、最高速度、平均高度） |
| `GET /api/migration/track` | 抽稀轨迹点 `[{lng, lat}]` |
| `GET /api/migration/stopovers` | 停歇点列表（含坐标、起止时间、停留天数） |
| `GET /api/migration/hourly-activity` | 0-23 小时飞行活动频次 |
| `GET /api/migration/scatter` | 速度-高度散点采样数据（2000 点） |

## Spark 应用编程要点

### 配置说明 (`SparkAnalyzer.java`)

```java
SparkSession spark = SparkSession.builder()
    .appName("K9-Track-GPS-Analyzer")
    .master("local[*]")              // 利用所有 CPU 核心
    .config("spark.sql.adaptive.enabled", "true")   // AQE 自适应优化
    .config("spark.sql.shuffle.partitions", "8")    // 适配单机 (非默认200)
    .config("spark.driver.memory", "4g")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .getOrCreate();
```

### 批处理算子一览

| 算子 | Spark API | 输出表 | 记录数 |
|------|-----------|--------|--------|
| 轨迹抽稀 | `filter(col("point_id").mod(50).equalTo(0))` | `track_points` | ~3,890 |
| 网格聚合停歇点 | `groupBy(grid_lat, grid_lng).agg(min, max)` + 时间差过滤 | `stopovers` | ~10-20 |
| 各阶段统计 | `groupBy("point_type").agg(avg, max, count)` | `phase_stats` | 3 |
| 昼夜飞行习性 | `filter(speed>20)` + `hour(ts)` + `groupBy("hour")` | `hourly_activity` | 24 |
| 散点采样 | `filter(point_type='migration')` + `orderBy(rand()).limit(2000)` | `scatter_data` | 2,000 |

### 关键设计决策

1. **物理拆分两个 Maven 工程**：`spring-boot-api` 不依赖 Spark 相关 JAR，避免与 Spark 的 Guava/Jackson 等库产生版本冲突。
2. **Spark 使用 `point_type` 英文列进行过滤和分组**：CSV 中 `phase` 列的中文编码在跨平台时可能出现乱码（文件编码为 ISO-8859），因此统一使用 `point_type` (breeding/migration/stopover) 作为分组键。
3. **停歇点识别使用网格聚合而非窗口函数**：避免对 19.4 万条数据逐行计算大圆距离，改用经纬度 2 位小数（约 1km）网格聚合 + 时间差 > 48h 过滤，计算效率更高。
4. **总里程在 Spring Boot 端使用 Haversine 轻量计算**：Spark 只负责抽稀并写入 MySQL，由 Java 端的 `HaversineUtil` 对抽稀后的 ~4000 点累加距离，避免 Spark 中额外的 Shuffle 开销。
5. **Spark 以 subprocess/external 模式运行**：`spark-processor` 是完全独立的 `main` 方法程序，不嵌入 Spring Boot。运行一次后将结果持久化在 MySQL 中，供 Web API 反复读取。

## 前端页面截图说明

- **顶部**：6 个核心统计卡片（总里程、迁徙天数、最快时速、停歇点数、GPS 记录总数、起止日期）
- **左侧/中央**：高德地图深色主题，蓝色迁徙路线折线 + 红色停歇点标记（点击可查看停留天数）
- **右侧**：3 个 ECharts 图表
  - 速度时序折线图（展示迁徙过程中的移动步长变化）
  - 昼夜飞行南丁格尔玫瑰图（展示高速飞行在各小时的分布）
  - 高度-速度散点图（迁徙阶段 2000 采样点）

## 高德地图 API Key

前端 `MapView.vue` 中的 `YOUR_AMAP_KEY` 需替换为实际的高德 JS API Key。

申请地址：https://console.amap.com/dev/key/app

## 常见问题

**Q: Spark 运行报 `java.lang.ClassNotFoundException`**
A: 确保 `spark-processor` 的 pom.xml 中 spark-core 和 spark-sql 依赖已正确加载，且 scope 为 compile。

**Q: Spring Boot 启动报数据库连接失败**
A: 确认 MySQL 服务已启动，且 `application.yml` 中的 username/password 配置正确。

**Q: 前端地图无法加载**
A: 检查高德 API Key 是否正确配置，或使用 `http://localhost:3000` 访问（Vite 代理需要开发服务器运行）。
