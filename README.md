# K9-Track：勺嘴鹬迁徙监测与分析平台

> **克隆后必读**：项目中有 2 个 Java 文件包含硬编码的 `E:/project/idea/K9-Track/` 路径，需要改成你的实际项目路径，详见 [PROJECT.md](PROJECT.md) 第十章。

基于 **Apache Spark** 的勺嘴鹬 (Spoon-billed Sandpiper) GPS 迁徙数据批处理与可视化系统。涵盖迁徙路线追踪、停歇点识别、昼夜飞行节律和速度-高度分布等多维分析。

## 系统架构

```
┌──────────────────────────────────────────────────┐
│              前端 (Vue 3 + Vite + ECharts 5)       │
│       ECharts Geo 世界地图 + 迁徙线 + 图表组件       │
│              http://localhost:3000                 │
└──────────────────────┬───────────────────────────┘
                       │ Axios HTTP
┌──────────────────────▼───────────────────────────┐
│         spring-boot-api (Spring Boot 2.7)         │
│          MyBatis-Plus + JDBC 读取 MySQL            │
│          包含 MockDataGenerator 可生成模拟数据       │
│              http://localhost:8080                 │
└──────────────────────┬───────────────────────────┘
                       │ JDBC
┌──────────────────────▼───────────────────────────┐
│                 MySQL 8.0 (Docker)                 │
│      数据库: k9track (6 张结果表)                   │
│      127.0.0.1:3307                                │
└──────────────────────┬───────────────────────────┘
                       │ JDBC 写入
┌──────────────────────▼───────────────────────────┐
│           spark-processor (Spark 3.3.2)            │
│          local[*] 模式，纯 Java API                 │
│          CSV → 聚合计算 → 写入 MySQL                │
└──────────────────────┴───────────────────────────┘
```

## 项目结构

```
K9-Track/
├── spark-processor/               # Spark 离线批处理 (独立 Maven 项目)
│   ├── pom.xml                    # Spark 3.3.2, JDK 8
│   └── src/main/java/com/xue/spark/
│       └── SparkAnalyzer.java     # 6 个批处理算子
│
├── spring-boot-api/               # Web API 工程 (独立 Maven 项目)
│   ├── pom.xml                    # Spring Boot 2.7.18 + MyBatis-Plus, JDK 17
│   └── src/main/java/com/xue/k9track/
│       ├── K9TrackApplication.java
│       ├── controller/MigrationController.java  # 7 个 REST API
│       ├── service/MigrationService.java
│       ├── entity/                # 6 个实体类
│       ├── mapper/                # 6 个 MyBatis-Plus Mapper
│       └── mock/MockDataGenerator.java  # 模拟数据生成器（可独立运行）
│
├── frontend/                      # 前端工程 (Vue 3 + Vite)
│   ├── package.json               # Vue 3.4, ECharts 5.5, Axios
│   └── src/
│       ├── main.js
│       ├── App.vue                # 主布局（白底极简大屏风格）
│       ├── api/index.js           # Axios 请求封装 (7 个接口)
│       └── components/
│           ├── SpeedTimeChart.vue     # 迁徙速度时序折线图
│           ├── HourlyRoseChart.vue    # 昼夜飞行南丁格尔玫瑰图
│           └── ScatterChart.vue       # 高度-速度散点图
│
├── sql/init.sql                   # 数据库建表脚本
├── docker-compose.yml             # Docker MySQL 8.0
├── input/                         # 原始 GPS 数据
│   └── spoonbill_k9_gps_corrected.csv
└── README.md
```

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | **17** (API) / **8** (Spark) | API 用 17，Spark 用 8 |
| MySQL | **8.0** | Docker 一键启动 |
| Maven | 3.6+ | 或用 IDEA 内置 Maven |
| Node.js | 18+ | 前端开发 |
| Docker Desktop | 可选 | 用于 MySQL，免手动安装 |

## 快速开始（两种路径）

### 路径 A：MockDataGenerator 快速体验（跳过 Spark）

适用场景：只需要前端可视化效果，不需要真实 Spark 分析流程。

**1. 启动 MySQL**

```powershell
cd <你的项目路径>
docker compose up -d
```

**2. 运行 MockDataGenerator 生成模拟数据**

在 IDEA 中打开 `spring-boot-api`：

```
File → Open → spring-boot-api → OK
```

运行 `MockDataGenerator.java` 的 main 方法：

```
src/main/java/com/xue/k9track/mock/MockDataGenerator.java → Run
```

控制台输出约 30 秒后显示 6 张表写入完成，数据规模：182,000 GPS 点、80 天迁徙、~9,700 km。

**3. 启动 Spring Boot API**

```
src/main/java/com/xue/k9track/K9TrackApplication.java → Run
```

**4. 启动前端**

```powershell
cd <你的项目路径>\frontend
npm install
npm run dev
```

打开 http://localhost:3000

### 路径 B：Spark 完整流水线（从 CSV 到可视化）

适用场景：需要展示完整的 Spark 批处理流程。

**1-2. 同上（启动 MySQL）**

**3. 运行 Spark 批处理**

在 IDEA 中打开 `spark-processor`：

```
File → Open → spark-processor → OK
```

运行 `SparkAnalyzer.java`：

```
src/main/java/com/xue/spark/SparkAnalyzer.java → Run
```

首次运行需下载 Spark 依赖（约 200MB），之后秒开。控制台输出 6 个处理步骤及统计指标。

**4-5. 同上（启动 API + 前端）**

## API 接口清单

所有接口均以 `GET /api/migration/` 为前缀，返回 JSON。

| 端点 | 说明 | 数据量 |
|------|------|--------|
| `/overview` | 迁徙总览（总里程、时速、天数、记录数） | 1 组 |
| `/track` | 轨迹坐标 `[{lng, lat}]` | 3,890 点 |
| `/stopovers` | 停歇点 `[{lat, lng, startTime, endTime, stayDays}]` | 2+ 个 |
| `/phase-stats` | 各阶段统计（breeding/migration/stopover） | 3 条 |
| `/hourly-activity` | 0-23 小时飞行占比 `[{hourOfDay, activityCount}]` | 24 条 |
| `/scatter` | 速度-高度散点 `[{speedKmh, altitude}]` | 2,000 点 |
| `/daily-speed` | 每日平均速度 `[{date, avgSpeed}]` | ~80 条 |

## MySQL 表结构

共 6 张表，DDL 见 [sql/init.sql](sql/init.sql)：

| 表名 | 说明 | 数据量 | 对应前端组件 |
|------|------|--------|-------------|
| `migration_summary` | 迁徙总览 | 1 行 | 统计卡片 |
| `phase_stat` | 各阶段统计 | 3 行 | — |
| `track_point` | 轨迹点 | ~3,890 行 | Geo 地图迁徙线 |
| `stopover` | 停歇点 | 2+ 行 | Geo 地图标记 + 表格 |
| `hourly_activity` | 小时活动占比 | 24 行 | 南丁格尔玫瑰图 |
| `scatter_data` | 速度-高度采样 | 2,000 行 | 散点图 |

## 前端大屏布局

```
┌──────────────────────────────────────────────┐
│              标题：K9-Track 迁徙监测平台        │
├────┬────┬────┬────┬────┬─────────────────────┤
│ 总里程 │ 天数 │ 时速 │ 停歇点│ GPS记录        │  ← 5 统计卡片
├──────────────────────┬───────────────────────┤
│                      │ 迁徙速度时序分布 (折线)  │
│   ECharts Geo 世界地图 ├───────────────────────┤
│   世界轮廓 + 蓝色迁徙线 │ 昼夜飞行节律 (玫瑰图)   │
│   + 红色停歇标记       ├───────────────────────┤
│                      │ 高度-速度散点 (散点图)   │
├──────────────────────┴───────────────────────┤
│              关键停歇地一览 (表格)              │
└──────────────────────────────────────────────┘
```

- 地图底图：ECharts Geo 组件，加载世界 GeoJSON（CDN），注册为 `world` 地图
- 迁徙线：Douglas-Peucker 抽稀 + 滑动窗口平滑 + 分段贝塞尔曲线
- 图表配色：莫兰迪低饱和色系
- 整体风格：白底极简，无暗黑元素

## Spark 批处理要点

### 6 个处理算子

| # | 算子 | 核心 API | 说明 |
|---|------|---------|------|
| 1 | 轨迹抽稀 | `filter(mod(point_id, 50) = 0)` | 约简到 ~3,890 点 |
| 2 | 停歇点识别 | 网格聚合 + 时间差 >48h 过滤 | 空间分箱减少 shuffle |
| 3 | 阶段统计 | `groupBy(point_type).agg(avg, max, count)` | 3 个阶段聚合 |
| 4 | 昼夜飞行 | 飞行占比 = flight / total × 100 | 24 小时百分比 |
| 5 | 散点采样 | `orderBy(rand()).limit(2000)` | 随机采样 |
| 6 | 总览计算 | Haversine UDF + Window lag() + sum() | 在抽稀数据上计算总里程 |

### 关键配置

```java
SparkSession spark = SparkSession.builder()
    .master("local[*]")
    .config("spark.sql.shuffle.partitions", "8")   // 适配单机
    .config("spark.driver.memory", "4g")
    .getOrCreate();
```

## 常见问题

**Q: Spring Boot 启动报 `Port 8080 is already in use`**

A: 端口被占用，找到并关闭占用进程：
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Q: 前端地图不显示世界轮廓**

A: ECharts Geo 需要从 CDN 异步加载世界地图 GeoJSON。检查网络能访问 `cdn.jsdelivr.net`。若 CDN 不可用，页面会自动降级为空白坐标轴模式，迁徙线仍可见。

**Q: 图表数据为空**

A: 确认 API 已启动且 MySQL 有数据。浏览器 F12 → Network 检查 `/api/migration/overview` 是否返回 200 和有效 JSON。

**Q: IDEA 打开 Spring Boot 后 Maven 依赖下载慢**

A: `pom.xml` 已配置阿里云 Maven 镜像，首次下载约 1-2 分钟。

**Q: Spark 报 `java.lang.ClassNotFoundException: com.mysql.cj.jdbc.Driver`**

A: 确认 `spark-processor/pom.xml` 中 `mysql-connector-java` 依赖未被排除。

**Q: Docker MySQL 端口冲突**

A: Docker 映射到宿主机 `3307`，与本地 MySQL（3306）不冲突。若 3307 也被占用，修改 `docker-compose.yml` 和 `application.yml` 中的端口号。
