package com.xue.spark;

import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.Window;
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
 */
public class SparkAnalyzer {

    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/k9track"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        // 0. 初始化 MySQL 数据库和表结构
        initDatabase();

        // 1. 创建 SparkSession —— local[*] 利用本地所有 CPU 核心
        SparkSession spark = SparkSession.builder()
                .appName("K9-Track-GPS-Analyzer")
                .master("local[*]")
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .config("spark.sql.shuffle.partitions", "8")
                .config("spark.driver.memory", "4g")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        System.out.println("===========================================");
        System.out.println("  K9-Track Spark Analyzer 启动");
        System.out.println("  Spark 版本: " + spark.version());
        System.out.println("  Master: " + spark.sparkContext().master());
        System.out.println("===========================================");

        // 2. 读取 CSV 数据
        String csvPath = SparkAnalyzer.class.getClassLoader()
                .getResource("data/spoonbill_k9_gps.csv").getPath();

        StructType schema = new StructType()
                .add("point_id", DataTypes.IntegerType)
                .add("timestamp", DataTypes.StringType)
                .add("latitude", DataTypes.DoubleType)
                .add("longitude", DataTypes.DoubleType)
                .add("altitude", DataTypes.IntegerType)
                .add("speed_kmh", DataTypes.DoubleType)
                .add("phase", DataTypes.StringType)
                .add("point_type", DataTypes.StringType);

        Dataset<Row> df = spark.read()
                .option("header", "true")
                .option("charset", "UTF-8")
                .schema(schema)
                .csv(csvPath);

        // 解析时间戳
        df = df.withColumn("ts", to_timestamp(col("timestamp"), "yyyy-MM-dd HH:mm:ss"));
        df.cache();

        long totalCount = df.count();
        System.out.println("总记录数: " + totalCount);

        // ========== 处理1: 轨迹抽稀 (Downsampling) ==========
        System.out.println("\n[处理1] 轨迹抽稀 (point_id % 50 == 0)...");
        Dataset<Row> trackDF = df.filter(col("point_id").mod(50).equalTo(0))
                .orderBy("timestamp")
                .select(col("point_id"),
                        col("longitude").as("lng"),
                        col("latitude").as("lat"),
                        col("timestamp"));

        long trackCount = trackDF.count();
        System.out.println("抽稀后轨迹点数: " + trackCount);
        writeToMySQL(trackDF, "track_points");

        // ========== 处理2: 网格聚合识别停歇点 ==========
        System.out.println("\n[处理2] 网格聚合识别停歇点...");
        // 过滤 stopover 类型，按经纬度保留两位小数 (约1km网格) 分组
        Dataset<Row> stopoverDF = df.filter(col("point_type").equalTo("stopover"))
                .withColumn("grid_lat", round(col("latitude"), 2))
                .withColumn("grid_lng", round(col("longitude"), 2));

        // 计算每个网格内最大/最小时间戳，停留时间 = 最大 - 最小 (小时)
        Dataset<Row> gridAgg = stopoverDF.groupBy("grid_lat", "grid_lng")
                .agg(
                        min("ts").as("start_time"),
                        max("ts").as("end_time"),
                        count("point_id").as("point_count")
                )
                .withColumn("diff_hours",
                        (col("end_time").cast("long").minus(col("start_time").cast("long")).divide(3600)))
                // 只保留停留时间 > 48 小时的有效停歇点
                .filter(col("diff_hours").gt(48))
                .withColumn("stay_days", round(col("diff_hours").divide(24), 1))
                .select(
                        col("grid_lat").as("lat"),
                        col("grid_lng").as("lng"),
                        col("start_time"),
                        col("end_time"),
                        col("stay_days")
                )
                .orderBy(col("stay_days").desc());

        long stopoverCount = gridAgg.count();
        System.out.println("有效停歇点数量 (停留>48h): " + stopoverCount);
        writeToMySQL(gridAgg, "stopovers");

        // ========== 处理3: 各阶段统计 ==========
        System.out.println("\n[处理3] 各阶段统计...");
        Dataset<Row> phaseStats = df.groupBy("point_type")
                .agg(
                        round(avg("speed_kmh"), 2).as("avg_speed"),
                        round(max("speed_kmh"), 2).as("max_speed"),
                        round(avg("altitude"), 1).as("avg_altitude"),
                        count("point_id").as("record_count")
                )
                .withColumnRenamed("point_type", "phase")
                .orderBy(col("phase"));

        phaseStats.show(false);
        writeToMySQL(phaseStats, "phase_stats");

        // ========== 处理4: 昼夜飞行习性 (按小时统计高速飞行) ==========
        System.out.println("\n[处理4] 昼夜飞行习性分析...");
        Dataset<Row> hourlyActivity = df
                .filter(col("speed_kmh").gt(20))
                .withColumn("hour", hour(col("ts")))
                .groupBy("hour")
                .agg(count("point_id").as("count"))
                .orderBy("hour");

        hourlyActivity.show(24, false);
        writeToMySQL(hourlyActivity, "hourly_activity");

        // ========== 处理5: 高度-速度散点采样 ==========
        System.out.println("\n[处理5] 高度-速度散点采样...");
        Dataset<Row> scatterData = df
                .filter(col("point_type").equalTo("migration"))
                .select(col("speed_kmh").as("speed"),
                        col("altitude").as("altitude"))
                .orderBy(rand())
                .limit(2000);

        long scatterCount = scatterData.count();
        System.out.println("散点采样数: " + scatterCount);
        writeToMySQL(scatterData, "scatter_data");

        // 清理缓存
        df.unpersist();
        spark.stop();

        System.out.println("\n===========================================");
        System.out.println("  处理完成!");
        System.out.println("  抽稀轨迹点: " + trackCount);
        System.out.println("  有效停歇点: " + stopoverCount);
        System.out.println("  散点采样数: " + scatterCount);
        System.out.println("===========================================");
    }

    /**
     * 将 DataFrame 通过 JDBC 写入 MySQL
     */
    private static void writeToMySQL(Dataset<Row> df, String tableName) {
        java.util.Properties props = new java.util.Properties();
        props.put("user", JDBC_USER);
        props.put("password", JDBC_PASSWORD);
        props.put("driver", "com.mysql.cj.jdbc.Driver");
        // 解决中文编码问题
        props.put("characterEncoding", "UTF-8");
        props.put("useUnicode", "true");

        df.write()
                .mode("overwrite")
                .jdbc(JDBC_URL, tableName, props);
        System.out.println("  已写入表: " + tableName + " (" + df.count() + " 条)");
    }

    /**
     * 初始化 MySQL 数据库和表结构
     */
    private static void initDatabase() throws Exception {
        String baseUrl = "jdbc:mysql://127.0.0.1:3306"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

        // 先连接无数据库的 URL 来创建数据库
        try (Connection conn = DriverManager.getConnection(baseUrl, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS k9track "
                    + "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("数据库 k9track 已就绪");
        }

        // 连接到 k9track 数据库创建表
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {

            // 轨迹抽稀点表
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS track_points ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "point_id INT, "
                    + "lng DOUBLE, "
                    + "lat DOUBLE, "
                    + "timestamp VARCHAR(50)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 停歇点表
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS stopovers ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "lat DOUBLE, "
                    + "lng DOUBLE, "
                    + "start_time VARCHAR(50), "
                    + "end_time VARCHAR(50), "
                    + "stay_days DOUBLE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 各阶段统计表
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS phase_stats ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "phase VARCHAR(50), "
                    + "avg_speed DOUBLE, "
                    + "max_speed DOUBLE, "
                    + "avg_altitude DOUBLE, "
                    + "record_count INT"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 每小时活动频次表
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS hourly_activity ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "hour INT, "
                    + "count INT"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 散点数据表
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS scatter_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "speed DOUBLE, "
                    + "altitude DOUBLE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            System.out.println("数据表已就绪: track_points, stopovers, phase_stats, hourly_activity, scatter_data");
        }
    }
}
