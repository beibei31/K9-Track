package com.xue.spark;

import org.apache.spark.sql.*;
import org.apache.spark.sql.api.java.UDF4;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import static org.apache.spark.sql.functions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 勺嘴鹬 GPS 数据批处理分析器
 * 以 local[*] 模式运行，读取本地 CSV，通过 JDBC 将结果写入 MySQL。
 * 该程序作为批处理脚本运行一次，不涉及任何 Web 服务。
 *
 * 输出表：
 *   migration_summary  - 迁徙总览
 *   track_point        - 抽稀轨迹点 (约4000条)
 *   stopover           - 网格聚合停歇点
 *   hourly_activity    - 昼夜飞行频次
 *   scatter_data       - 速度-高度散点采样
 */
public class SparkAnalyzer {

    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3307/k9track"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "root";

    /** 半正矢公式：地球半径 (km) */
    private static final double EARTH_RADIUS_KM = 6371.0;

    public static void main(String[] args) throws Exception {
        // 0. 初始化数据库和表结构
        initDatabase();

        // 1. 创建 SparkSession
        SparkSession spark = SparkSession.builder()
                .appName("K9-Track-GPS-Analyzer")
                .master("local[*]")
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .config("spark.sql.shuffle.partitions", "8")
                .config("spark.driver.memory", "4g")
                .config("spark.ui.enabled", "false")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        System.out.println("===========================================");
        System.out.println("  K9-Track Spark Analyzer 启动");
        System.out.println("  Spark 版本: " + spark.version());
        System.out.println("  Master: " + spark.sparkContext().master());
        System.out.println("===========================================");

        // 注册 Haversine UDF
        spark.udf().register("haversine", (UDF4<Double, Double, Double, Double, Double>)
                (lat1, lng1, lat2, lng2) -> {
                    if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return 0.0;
                    double dLat = Math.toRadians(lat2 - lat1);
                    double dLng = Math.toRadians(lng2 - lng1);
                    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
                    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                    return EARTH_RADIUS_KM * c;
                }, DataTypes.DoubleType);

        // 2. 读取 CSV
        String csvPath = "E:/project/idea/K9-Track/input/spoonbill_k9_real_route.csv";

        StructType schema = new StructType()
                .add("point_id", DataTypes.IntegerType)
                .add("timestamp", DataTypes.StringType)
                .add("longitude", DataTypes.DoubleType)
                .add("latitude", DataTypes.DoubleType)
                .add("altitude", DataTypes.DoubleType)
                .add("speed_kmh", DataTypes.DoubleType)
                .add("phase", DataTypes.StringType)
                .add("point_type", DataTypes.StringType);

        Dataset<Row> df = spark.read()
                .option("header", "true")
                .option("charset", "UTF-8")
                .schema(schema)
                .csv(csvPath);

        df = df.withColumn("ts", to_timestamp(col("timestamp"), "yyyy-MM-dd HH:mm:ss"));
        df.cache();

        long totalCount = df.count();
        System.out.println("总记录数: " + totalCount);

        // ========== 处理1: 轨迹抽稀 → track_point ==========
        System.out.println("\n[处理1] 轨迹抽稀 (point_id % 50 == 0) → track_point");
        Dataset<Row> trackDF = df.filter(col("point_id").mod(50).equalTo(0))
                .orderBy("timestamp")
                .withColumn("id", monotonically_increasing_id().plus(1))
                .select(
                        col("id"),
                        col("point_id"),
                        col("timestamp"),
                        col("longitude"),
                        col("latitude"),
                        col("altitude"),
                        col("speed_kmh"),
                        col("phase"),
                        col("point_type")
                );

        long trackCount = trackDF.count();
        System.out.println("抽稀后轨迹点数: " + trackCount);
        writeToMySQL(trackDF, "track_point");

        // ========== 处理2: 网格聚合停歇点 → stopover ==========
        System.out.println("\n[处理2] 网格聚合停歇点 → stopover");
        Dataset<Row> stopoverDF = df.filter(col("point_type").equalTo("stopover"))
                .withColumn("grid_lat", round(col("latitude"), 1))
                .withColumn("grid_lng", round(col("longitude"), 1));

        Dataset<Row> gridAgg = stopoverDF.groupBy("grid_lat", "grid_lng")
                .agg(
                        min("ts").as("start_time"),
                        max("ts").as("end_time"),
                        count("point_id").as("point_count")
                )
                .withColumn("diff_hours",
                        col("end_time").cast("long").minus(col("start_time").cast("long")).divide(3600))
                .filter(col("diff_hours").gt(24))  // 停留超过24小时视为有效停歇
                .withColumn("stay_days", round(col("diff_hours").divide(24), 1))
                .withColumn("id", monotonically_increasing_id().plus(1))
                .select(
                        col("id"),
                        col("grid_lng").as("longitude"),
                        col("grid_lat").as("latitude"),
                        col("start_time"),
                        col("end_time"),
                        col("stay_days")
                )
                .orderBy(col("stay_days").desc());

        long stopoverCount = gridAgg.count();
        System.out.println("有效停歇点 (停留>6h): " + stopoverCount);
        writeToMySQL(gridAgg, "stopover");

        // ========== 处理4: 昼夜飞行习性（百分比） → hourly_activity ==========
        System.out.println("\n[处理4] 昼夜飞行习性(飞行占比%) → hourly_activity");

        // 每小时总记录数
        Dataset<Row> hourlyTotal = df
                .withColumn("hour_of_day", hour(col("ts")))
                .groupBy("hour_of_day")
                .agg(count("point_id").as("total_count"));

        // 每小时飞行记录数 (speed > 20)
        Dataset<Row> hourlyFlight = df
                .filter(col("speed_kmh").gt(20))
                .withColumn("hour_of_day", hour(col("ts")))
                .groupBy("hour_of_day")
                .agg(count("point_id").as("flight_count"));

        Dataset<Row> hourlyDF = hourlyTotal
                .join(hourlyFlight,
                        hourlyTotal.col("hour_of_day").equalTo(hourlyFlight.col("hour_of_day")),
                        "left_outer")
                .drop(hourlyFlight.col("hour_of_day"))
                .na().fill(0)
                .withColumn("flight_pct",
                        round(col("flight_count").multiply(100).divide(col("total_count")), 0))
                .withColumn("id", monotonically_increasing_id().plus(1))
                .select(col("id"), col("hour_of_day"), col("flight_pct").as("activity_count"))
                .orderBy("hour_of_day");

        hourlyDF.show(24, false);
        writeToMySQL(hourlyDF, "hourly_activity");

        // ========== 处理5: 高度-速度散点采样 → scatter_data ==========
        System.out.println("\n[处理5] 高度-速度散点采样 → scatter_data");
        Dataset<Row> scatterDF = df
                .filter(col("point_type").equalTo("flight"))
                .select(col("speed_kmh"), col("altitude"))
                .orderBy(rand())
                .limit(2000)
                .withColumn("id", monotonically_increasing_id().plus(1))
                .select(col("id"), col("speed_kmh"), col("altitude"));

        long scatterCount = scatterDF.count();
        System.out.println("散点采样数: " + scatterCount);
        writeToMySQL(scatterDF, "scatter_data");

        // ========== 处理6: 迁徙总览 → migration_summary ==========
        System.out.println("\n[处理6] 迁徙总览 → migration_summary");

        // 总里程：对抽稀后的轨迹点，用 Haversine 计算相邻点距离并累加
        Dataset<Row> trackWithLag = trackDF
                .withColumn("prev_lat", lag("latitude", 1).over(org.apache.spark.sql.expressions.Window.orderBy("timestamp")))
                .withColumn("prev_lng", lag("longitude", 1).over(org.apache.spark.sql.expressions.Window.orderBy("timestamp")));

        Row distRow = trackWithLag
                .filter(col("prev_lat").isNotNull())
                .agg(sum(callUDF("haversine", col("prev_lat"), col("prev_lng"), col("latitude"), col("longitude"))))
                .head();
        double totalDistance = distRow.isNullAt(0) ? 0.0 : distRow.getDouble(0);

        // 平均速度和最高速度
        Row speedRow = df.agg(avg("speed_kmh"), max("speed_kmh")).head();
        double avgSpeed = speedRow.isNullAt(0) ? 0.0 : speedRow.getDouble(0);
        double maxSpeed = speedRow.isNullAt(1) ? 0.0 : speedRow.getDouble(1);

        // 总停歇天数：统计不重复的停歇日期数（避免网格重叠导致重复计数）
        long totalStopoverDays = getCount(df);

        Dataset<Row> summaryDF = spark.createDataFrame(
                java.util.Arrays.asList(
                        org.apache.spark.sql.RowFactory.create(
                                1L,
                                Math.round(totalDistance * 10.0) / 10.0,
                                Math.round(avgSpeed * 100.0) / 100.0,
                                Math.round(maxSpeed * 100.0) / 100.0,
                                (double) totalStopoverDays,
                                new java.sql.Timestamp(System.currentTimeMillis()).toString()
                        )
                ),
                new StructType()
                        .add("id", DataTypes.LongType)
                        .add("total_distance", DataTypes.DoubleType)
                        .add("avg_speed", DataTypes.DoubleType)
                        .add("max_speed", DataTypes.DoubleType)
                        .add("total_stopover_days", DataTypes.DoubleType)
                        .add("update_time", DataTypes.StringType)
        );

        summaryDF.show(false);
        writeToMySQL(summaryDF, "migration_summary");

        // 清理缓存
        df.unpersist();
        spark.stop();

        System.out.println("\n===========================================");
        System.out.println("  处理完成!");
        System.out.println("  总里程: " + Math.round(totalDistance * 10.0) / 10.0 + " km");
        System.out.println("  平均时速: " + Math.round(avgSpeed * 100.0) / 100.0 + " km/h");
        System.out.println("  最快时速: " + Math.round(maxSpeed * 100.0) / 100.0 + " km/h");
        System.out.println("  总停歇天数: " + totalStopoverDays + " 天");
        System.out.println("  抽稀轨迹点: " + trackCount);
        System.out.println("  有效停歇点: " + stopoverCount);
        System.out.println("  散点采样数: " + scatterCount);
        System.out.println("===========================================");
    }

    private static long getCount(Dataset<Row> df) {
        return df.filter(col("point_type").equalTo("stopover"))
                .select(date_format(col("ts"), "yyyy-MM-dd"))
                .distinct()
                .count();
    }

    /** 将 DataFrame 通过 JDBC 写入 MySQL */
    private static void writeToMySQL(Dataset<Row> df, String tableName) {
        java.util.Properties props = new java.util.Properties();
        props.put("user", JDBC_USER);
        props.put("password", JDBC_PASSWORD);
        props.put("driver", "com.mysql.cj.jdbc.Driver");
        props.put("characterEncoding", "UTF-8");
        props.put("useUnicode", "true");

        df.write()
                .mode("overwrite")
                .jdbc(JDBC_URL, tableName, props);
        System.out.println("  已写入表: " + tableName + " (" + df.count() + " 条)");
    }

    /** 初始化 MySQL 数据库和表结构 */
    private static void initDatabase() throws Exception {
        String baseUrl = "jdbc:mysql://127.0.0.1:3307"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

        try (Connection conn = DriverManager.getConnection(baseUrl, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS k9track "
                    + "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("数据库 k9track 已就绪");
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS migration_summary ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "total_distance DOUBLE COMMENT '总里程(km)', "
                    + "avg_speed DOUBLE COMMENT '平均时速(km/h)', "
                    + "max_speed DOUBLE COMMENT '最快时速(km/h)', "
                    + "total_stopover_days DOUBLE COMMENT '总停歇天数', "
                    + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁徙总览表'");

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS track_point ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "point_id INT COMMENT '原始点ID', "
                    + "timestamp VARCHAR(50) COMMENT '时间戳', "
                    + "longitude DOUBLE COMMENT '经度', "
                    + "latitude DOUBLE COMMENT '纬度', "
                    + "altitude DOUBLE COMMENT '海拔', "
                    + "speed_kmh DOUBLE COMMENT '速度(km/h)', "
                    + "phase VARCHAR(50) COMMENT '所属阶段', "
                    + "point_type VARCHAR(50) COMMENT '记录类型'"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽稀轨迹表'");

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS stopover ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "longitude DOUBLE COMMENT '中心经度', "
                    + "latitude DOUBLE COMMENT '中心纬度', "
                    + "start_time VARCHAR(50) COMMENT '抵达时间', "
                    + "end_time VARCHAR(50) COMMENT '离开时间', "
                    + "stay_days DOUBLE COMMENT '停留天数'"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格聚合停歇点表'");

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS hourly_activity ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "hour_of_day INT COMMENT '小时(0-23)', "
                    + "activity_count DOUBLE COMMENT '飞行占比(%)'"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='昼夜活动频次表(百分比)'");

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS scatter_data ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "speed_kmh DOUBLE COMMENT '速度(km/h)', "
                    + "altitude DOUBLE COMMENT '海拔(m)'"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='速度-高度散点采样表'");

            System.out.println("数据表已就绪: migration_summary, track_point, stopover, hourly_activity, scatter_data");
        }
    }
}
