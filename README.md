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
│                 MySQL 8.0 (容器/本地)               │
│      数据库: k9track (6 张结果表)                   │
│      127.0.0.1:3307 (Docker) / 3306 (本地)        │
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
│   ├── pom.xml                    # Spark 3.3.2 + MySQL 驱动, JDK 17
│   └── src/main/
│       ├── java/com/xue/spark/
│       │   └── SparkAnalyzer.java # Spark 批处理主程序 (6个算子)
│       └── resources/data/
│           └── spoonbill_k9_gps.csv  # 194,500 条 GPS 数据
│
├── spring-boot-api/               # Web API 工程 (独立 Maven 项目，无 Spark)
│   ├── pom.xml                    # Spring Boot 2.7.18 + MyBatis-Plus, JDK 17
│   └── src/main/
│       ├── java/com/xue/k9track/
│       │   ├── K9TrackApplication.java
│       │   ├── entity/            # 6 个实体类
│       │   ├── mapper/            # 6 个 MyBatis-Plus Mapper
│       │   ├── service/
│       │   │   └── MigrationService.java
│       │   ├── controller/
│       │   │   └── MigrationController.java  # 6 个 REST API
│       │   └── util/
│       │       └── HaversineUtil.java        # 半正矢距离工具(备用)
│       └── resources/
│           └── application.yml
│
├── frontend/                      # 前端工程 (Vue 3 + Vite)
│   ├── package.json
│   ├── vite.config.js
│   ├── .env                       # 高德地图 API Key
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
├── sql/
│   └── init.sql                   # 数据库初始化脚本 (可独立执行)
├── docker-compose.yml             # Docker MySQL 8.0 一键启动
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
| 物候期 | breeding (繁殖, 74,400条), migration (迁徙, 72,600条), stopover (停歇, 47,500条) |

数据来源：通过模拟勺嘴鹬在东亚-澳大拉西亚迁飞区的迁徙路线生成，覆盖从泰国越冬地到俄罗斯北极繁殖地的完整迁徙周期。

## 环境要求

### 你需要安装的

| 组件 | 版本要求 | 如何检查 |
|------|----------|----------|
| JDK | **17** | `java -version` |
| MySQL | **8.0** | `mysql --version`（或使用 Docker） |
| Maven | 3.6+ | `mvn -v`（IDEA 内置 Maven 也可） |
| Node.js | 18+ | `node -v` |
| Docker Desktop | 可选 | `docker -v`（免手动安装 MySQL） |

### 你不需要安装的

| 组件 | 说明 |
|------|------|
| **Spark** | Maven 自动下载 `spark-core_2.12:3.3.2` + `spark-sql_2.12:3.3.2` 到本地 `.m2` 仓库 |
| **Hadoop** | Spark 以 `local[*]` 模式在单 JVM 内运行，不需要任何 Hadoop 组件 |
| **Scala** | 本项目使用纯 Java API (`Dataset<Row>` / `spark.udf()`)，不需要写 Scala 代码 |

## 运行指南

### 步骤 1：配置高德地图 Key

编辑 [frontend/.env](frontend/.env)：

```env
VITE_AMAP_KEY=你的高德JS API Key
```

申请地址：https://console.amap.com/dev/key/app（选择「Web端(JS API)」平台）

### 步骤 2：启动 MySQL

**方式 A：Docker（推荐，一键启动）**

```powershell
cd E:\project\idea\K9-Track
docker compose up -d
```

验证 MySQL 是否启动成功：

```powershell
docker ps
docker exec -it k9track-mysql mysql -u root -proot -e "SHOW DATABASES;"
```

Docker 映射到宿主机 `127.0.0.1:3307`，与本地已有的 MySQL（3306）互不冲突。

**方式 B：本地 MySQL**

如果本机已安装 MySQL 8.0，直接创建数据库：

```powershell
mysql -u root -p

# 进入 MySQL 后执行
CREATE DATABASE IF NOT EXISTS k9track
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

然后修改以下两个文件中的端口号 `3307` → `3306`：
- [SparkAnalyzer.java](spark-processor/src/main/java/com/xue/spark/SparkAnalyzer.java) 中的 `JDBC_URL`
- [application.yml](spring-boot-api/src/main/resources/application.yml) 中的 `datasource.url`

若 MySQL 用户名/密码不是 `root`/`root`，也一并修改。

### 步骤 3：运行 Spark 批处理

在 IDEA 中打开 `spark-processor` 子目录作为独立 Maven 项目：

```
File → Open → 选择 E:\project\idea\K9-Track\spark-processor → OK
```

等待 IDEA 右下角的 Maven 依赖下载完成（首次约 3-5 分钟，之后秒开）。

然后运行 `SparkAnalyzer.java` 的 main 方法：

```
src/main/java/com/xue/spark/SparkAnalyzer.java → 右键 → Run 'SparkAnalyzer.main()'
```

控制台输出示例：

```
===========================================
  K9-Track Spark Analyzer 启动
  Spark 版本: 3.3.2
  Master: local[*]
===========================================
数据库 k9track 已就绪
数据表已就绪: migration_summary, phase_stat, track_point, stopover, hourly_activity, scatter_data
总记录数: 194500

[处理1] 轨迹抽稀 (point_id % 50 == 0) → track_point
抽稀后轨迹点数: 3890
  已写入表: track_point (3890 条)

[处理2] 网格聚合停歇点 → stopover
有效停歇点 (停留>48h): 15
  已写入表: stopover (15 条)

[处理3] 各阶段统计 → phase_stat
  已写入表: phase_stat (3 条)

[处理4] 昼夜飞行习性 → hourly_activity
  已写入表: hourly_activity (24 条)

[处理5] 高度-速度散点采样 → scatter_data
散点采样数: 2000
  已写入表: scatter_data (2000 条)

[处理6] 迁徙总览 → migration_summary
  已写入表: migration_summary (1 条)

===========================================
  处理完成!
  总里程: 8423.5 km
  平均时速: 12.73 km/h
  最快时速: 58.6 km/h
  总停歇天数: 186.3 天
  抽稀轨迹点: 3890
  有效停歇点: 15
  散点采样数: 2000
===========================================
```

**验证 Spark 结果**：打开 MySQL 客户端查看数据是否写入：

```sql
USE k9track;
SELECT * FROM migration_summary;
SELECT COUNT(*) FROM track_point;
SELECT COUNT(*) FROM stopover;
SELECT * FROM phase_stat;
```

### 步骤 4：启动 Spring Boot API

在 IDEA 中打开 `spring-boot-api` 子目录作为独立 Maven 项目：

```
File → Open → 选择 E:\project\idea\K9-Track\spring-boot-api → OK
```

等待 Maven 依赖加载完毕（首次约 1-2 分钟）。然后运行：

```
src/main/java/com/xue/k9track/K9TrackApplication.java → 右键 → Run
```

控制台输出看到以下日志表示启动成功：

```
Tomcat started on port(s): 8080 (http)
Started K9TrackApplication in 3.456 seconds
```

**验证 API**（浏览器直接访问或命令行）：

```powershell
# 总览数据
curl http://localhost:8080/api/migration/overview

# 轨迹点（供地图绘制）
curl http://localhost:8080/api/migration/track

# 停歇点
curl http://localhost:8080/api/migration/stopovers

# 阶段统计
curl http://localhost:8080/api/migration/phase-stats

# 每小时活动频次
curl http://localhost:8080/api/migration/hourly-activity

# 散点数据
curl http://localhost:8080/api/migration/scatter
```

每个端点都应返回 JSON 数据。

### 步骤 5：启动前端

```powershell
cd E:\project\idea\K9-Track\frontend
npm install
npm run dev
```

控制台输出：

```
  VITE v5.x.x  ready in xxx ms
  ➜  Local:   http://localhost:3000/
```

打开浏览器访问 **http://localhost:3000**，应能看到完整的大屏可视化页面：

- 顶部：6 个统计卡片（总里程、天数、最快时速、停歇点数等）
- 左中：高德地图，蓝色迁徙路线 + 红色停歇点标记（点击标记查看停留天数）
- 右侧：3 个 ECharts 图表
  - 速度时序折线图
  - 昼夜飞行南丁格尔玫瑰图
  - 高度-速度散点图

## 前后端联调说明

- 前端 Axios baseURL 指向 `http://localhost:8080`，同时 Vite dev server 配置了 `/api` 代理
- 后端所有 Controller 已添加 `@CrossOrigin(origins = "*")`
- 开发时直接 `npm run dev` + Spring Boot 同时运行即可，无跨域问题

### API 接口清单

| 请求方式 | 端点 | 说明 |
|----------|------|------|
| GET | `/api/migration/overview` | 总览数据（总里程、平均速度、最快时速、停歇点数量、总停歇天数） |
| GET | `/api/migration/phase-stats` | 各阶段统计（breeding/migration/stopover 的平均/最高速度、海拔） |
| GET | `/api/migration/track` | 抽稀轨迹点 `[{lng, lat}]`，约 3,890 条 |
| GET | `/api/migration/stopovers` | 停歇点列表 `[{lat, lng, startTime, endTime, stayDays}]` |
| GET | `/api/migration/hourly-activity` | 0-23 小时飞行活动频次 `[{hourOfDay, activityCount}]` |
| GET | `/api/migration/scatter` | 速度-高度散点采样 `[{speedKmh, altitude}]`，2,000 条 |

## MySQL 表结构

共 **6 张表**，DDL 见 [sql/init.sql](sql/init.sql)：

| 表名 | 说明 | 数据量 | 对应前端组件 |
|------|------|--------|-------------|
| `migration_summary` | 迁徙总览 | 1 行 | StatsCards |
| `phase_stat` | 各阶段统计 | 3 行 | （可扩展饼图） |
| `track_point` | 抽稀轨迹点 | ~3,890 行 | 高德地图 Polyline |
| `stopover` | 网格聚合停歇点 | ~10-20 行 | 高德地图 Marker |
| `hourly_activity` | 昼夜活动频次 | 24 行 | 南丁格尔玫瑰图 |
| `scatter_data` | 速度-高度采样 | 2,000 行 | 散点图 |

## Spark 应用编程要点

### SparkSession 配置

```java
SparkSession spark = SparkSession.builder()
    .appName("K9-Track-GPS-Analyzer")
    .master("local[*]")              // 利用所有 CPU 核心
    .config("spark.sql.adaptive.enabled", "true")   // AQE 自适应查询优化
    .config("spark.sql.shuffle.partitions", "8")    // 适配单机环境（默认200为集群设计）
    .config("spark.driver.memory", "4g")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    // JDK 17 模块访问兼容
    .config("spark.driver.extraJavaOptions", "--add-opens=...")
    .getOrCreate();
```

### 6 个批处理算子

| # | 算子 | 核心 Spark API | 关键技术点 |
|----|------|---------------|-----------|
| 1 | 轨迹抽稀 | `filter(mod())` + `orderBy()` | 约简数据集，保留均匀时间分布 |
| 2 | 停歇点网格聚合 | `round(lat,2)` + `groupBy().agg(min,max)` + 时间差 >48h 过滤 | 空间分箱减少 shuffle |
| 3 | 各阶段统计 | `groupBy("point_type").agg(avg,max,count)` | 标准聚合算子 |
| 4 | 昼夜飞行习性 | `filter(speed>20)` + `hour(ts)` + `groupBy` | 时间维度下钻 |
| 5 | 散点采样 | `filter()` + `orderBy(rand()).limit(2000)` | 随机采样避免数据倾斜 |
| 6 | 迁徙总览 | `Window + lag()` + `Haversine UDF` + `sum()` | UDF 注册 + 窗口函数（仅在 3890 行上执行） |

### 关键设计决策

1. **物理拆分两个 Maven 工程**：`spring-boot-api` 不依赖 Spark 相关 JAR（spark-core/spark-sql 总大小约 200MB），避免与 Spark 的 Guava/Jackson 等库产生版本冲突，也加快 Spring Boot 启动速度。
2. **使用 `point_type` 英文列过滤分组**：CSV 中 `phase` 列的中文编码为 ISO-8859，跨平台可能乱码，统一使用 `point_type`（breeding/migration/stopover）作为分组键。
3. **停歇点网格聚合而非逐行 Window 算距离**：经纬度保留 2 位小数（约 1km 网格）对 19.4 万条数据 groupBy + 时间差 >48h 过滤，避免在原始数据上用 Window 逐行计算大圆距离的开销。
4. **总里程在抽稀后（~4000 行）用 Haversine UDF 计算**：避免在 19.4 万行上做窗口操作。Spark 注册 Haversine UDF，对抽稀轨迹点执行 `lag()` 获取上一行坐标后计算相邻距离并求和，写入 `migration_summary` 表。
5. **local[*] 模式**：Spark Driver 和 Executor 运行在同一 JVM 进程内，利用本机所有 CPU 核心并行处理，无需分布式集群。

## 常见问题

**Q: IDEA 打开项目后 Maven 依赖下载失败**

A: 检查网络是否能访问 Maven Central。国内用户可以在 `pom.xml` 中添加阿里云镜像，或在 IDEA 设置 `Build → Build Tools → Maven → User settings file` 中配置镜像。

**Q: Spark 运行报 `java.lang.ClassNotFoundException: com.mysql.cj.jdbc.Driver`**

A: 确认 `spark-processor/pom.xml` 中 `mysql-connector-java` 依赖存在且 scope 为 compile（默认）。

**Q: Spring Boot 启动报 `Communications link failure`**

A: MySQL 未启动。Docker 用户执行 `docker compose up -d`；本地 MySQL 用户确认服务已启动且端口号正确（Docker 用 3307，本地用 3306）。

**Q: 前端页面空白 / 地图不显示**

A: 检查 `frontend/.env` 中的高德 API Key 是否已配置。或在浏览器 F12 Console 查看具体错误信息。

**Q: API 返回空数据**

A: 先确认步骤 3（Spark 批处理）已成功运行并将数据写入 MySQL。可在 MySQL 客户端中 `SELECT COUNT(*) FROM track_point` 验证。
