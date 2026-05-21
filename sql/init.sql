-- ========================================
-- K9-Track 数据库初始化脚本
-- 可直接在 MySQL 中执行: source init.sql
-- ========================================

CREATE DATABASE IF NOT EXISTS k9track
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE k9track;

-- 清理旧表（慎用，仅开发阶段）
DROP TABLE IF EXISTS migration_summary;
DROP TABLE IF EXISTS phase_stat;
DROP TABLE IF EXISTS track_point;
DROP TABLE IF EXISTS stopover;
DROP TABLE IF EXISTS hourly_activity;
DROP TABLE IF EXISTS scatter_data;

-- -------------------------------------------------------
-- 1. 迁徙总览表
-- 说明: Spark 预计算的总里程、平均速度、最高速度、总停歇天数
-- 前端: 顶部统计卡片
-- -------------------------------------------------------
CREATE TABLE migration_summary (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    total_distance  DOUBLE        COMMENT '总里程(km)',
    avg_speed       DOUBLE        COMMENT '平均时速(km/h)',
    max_speed       DOUBLE        COMMENT '最快时速(km/h)',
    total_stopover_days DOUBLE    COMMENT '总停歇天数',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁徙总览表';

-- -------------------------------------------------------
-- 2. 各阶段统计表
-- 说明: 按 breeding / migration / stopover 分组聚合
-- 前端: 阶段分布饼图
-- -------------------------------------------------------
CREATE TABLE phase_stat (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    phase        VARCHAR(50)  COMMENT '阶段名称',
    point_count  INT          COMMENT '数据点数',
    avg_speed    DOUBLE       COMMENT '平均时速(km/h)',
    max_speed    DOUBLE       COMMENT '最高时速(km/h)',
    avg_altitude DOUBLE       COMMENT '平均海拔(m)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶段统计表';

-- -------------------------------------------------------
-- 3. 抽稀轨迹点表
-- 说明: point_id % 50 == 0 抽稀，约 3890 条
-- 前端: 高德地图 Polyline 迁徙路线
-- -------------------------------------------------------
CREATE TABLE track_point (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    point_id   INT          COMMENT '原始点ID',
    timestamp  VARCHAR(50)  COMMENT '时间戳 (yyyy-MM-dd HH:mm:ss)',
    longitude  DOUBLE       COMMENT '经度',
    latitude   DOUBLE       COMMENT '纬度',
    altitude   DOUBLE       COMMENT '海拔(m)',
    speed_kmh  DOUBLE       COMMENT '速度(km/h)',
    phase      VARCHAR(50)  COMMENT '所属阶段(中文)',
    point_type VARCHAR(50)  COMMENT '记录类型 (breeding/migration/stopover)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽稀轨迹表(约4000条)';

-- -------------------------------------------------------
-- 4. 停歇点表
-- 说明: 经纬度保留两位小数网格聚合，停留 > 48h 视为有效停歇点
-- 前端: 高德地图 Marker 标记，点击查看停留信息
-- -------------------------------------------------------
CREATE TABLE stopover (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    longitude  DOUBLE       COMMENT '中心经度',
    latitude   DOUBLE       COMMENT '中心纬度',
    start_time VARCHAR(50)  COMMENT '抵达时间',
    end_time   VARCHAR(50)  COMMENT '离开时间',
    stay_days  DOUBLE       COMMENT '停留天数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格聚合停歇点表';

-- -------------------------------------------------------
-- 5. 昼夜活动频次表
-- 说明: 筛选 speed_kmh > 20 的高速飞行记录，按小时(0-23)分组计数
-- 前端: ECharts 南丁格尔玫瑰图
-- -------------------------------------------------------
CREATE TABLE hourly_activity (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    hour_of_day    INT  COMMENT '小时(0-23)',
    activity_count INT  COMMENT '处于飞行状态的记录数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='昼夜活动频次表';

-- -------------------------------------------------------
-- 6. 速度-高度散点采样表
-- 说明: 从迁徙阶段随机抽取 2000 个点
-- 前端: ECharts 散点图 (x=speed_kmh, y=altitude)
-- -------------------------------------------------------
CREATE TABLE scatter_data (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    speed_kmh DOUBLE COMMENT '速度(km/h)',
    altitude  DOUBLE COMMENT '海拔(m)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='速度-高度散点采样表(2000条)';
