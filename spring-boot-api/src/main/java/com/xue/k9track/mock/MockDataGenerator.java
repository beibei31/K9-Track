package com.xue.k9track.mock;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    static class Node {
        double lng, lat;
        String name;
        Node(String n, double l1, double l2) { name = n; lng = l1; lat = l2; }
    }

    public static void main(String[] args) {
        // 请确保该路径存在
        String outputFile = "E:/project/idea/K9-Track/input/spoonbill_k9_real_route.csv";
        Random random = new Random(42);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime currentTime = LocalDateTime.of(2024, 4, 3, 6, 0);

        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node("楚科奇", 177.5, 64.5));
        nodes.add(new Node("堪察加", 159.0, 56.0));
        nodes.add(new Node("鄂霍次克海", 143.0, 53.0));
        nodes.add(new Node("库页岛", 142.5, 48.0));
        nodes.add(new Node("辽东湾", 121.5, 40.5));
        nodes.add(new Node("条子泥", 120.9, 32.8));
        nodes.add(new Node("闽江口", 119.6, 26.1));
        nodes.add(new Node("北部湾", 108.5, 21.5));
        nodes.add(new Node("马塔班湾", 97.5, 16.2));
        nodes.add(new Node("泰国湾", 100.5, 13.5));

        // 核心停歇点索引: 条子泥(5), 马塔班湾(8)
        int[] stopoverNodes = {5, 8};

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println("point_id,timestamp,longitude,latitude,altitude,speed_kmh,phase,point_type");

            int pointId = 0;
            int flightPointsPerSegment = 18000;
            int stopoverPointsPerSite = 10000;

            for (int i = 0; i < nodes.size() - 1; i++) {
                Node start = nodes.get(i);
                Node end = nodes.get(i + 1);

                // === 飞行段（含昼夜节律） ===
                for (int j = 0; j < flightPointsPerSegment; j++) {
                    pointId++;
                    double t = (double) j / flightPointsPerSegment;
                    double lng = start.lng + (end.lng - start.lng) * t + (random.nextDouble() - 0.5) * 0.01;
                    double lat = start.lat + (end.lat - start.lat) * t + (random.nextDouble() - 0.5) * 0.01;

                    // 昼夜节律：飞行速度 + 高度随时刻变化
                    int hour = currentTime.getHour();
                    double speed, altitude;
                    if (hour >= 8 && hour <= 10) {
                        // 清晨出发高峰：全速爬升
                        speed = 45 + random.nextDouble() * 30;
                        altitude = 1200 + random.nextDouble() * 1300;
                    } else if (hour >= 16 && hour <= 18) {
                        // 黄昏降落前冲刺
                        speed = 45 + random.nextDouble() * 25;
                        altitude = 1000 + random.nextDouble() * 1200;
                    } else if (hour >= 11 && hour <= 15) {
                        // 白天巡航
                        speed = 35 + random.nextDouble() * 25;
                        altitude = 800 + random.nextDouble() * 1200;
                    } else if (hour >= 5 && hour <= 7) {
                        // 凌晨过渡期：逐渐爬升
                        speed = 15 + random.nextDouble() * 25;
                        altitude = 400 + random.nextDouble() * 600;
                    } else if (hour >= 19 && hour <= 21) {
                        // 傍晚过渡期：逐渐下降
                        speed = 15 + random.nextDouble() * 20;
                        altitude = 500 + random.nextDouble() * 700;
                    } else {
                        // 夜间 (22-4)：低频滑翔，速度极低
                        speed = random.nextDouble() * 18;
                        altitude = 200 + random.nextDouble() * 600;
                    }

                    currentTime = currentTime.plusSeconds(20 + random.nextInt(10));

                    writer.printf("%d,%s,%.6f,%.6f,%.2f,%.2f,%s,%s%n",
                            pointId, currentTime.format(formatter), lng, lat, altitude, speed,
                            "Migration", "flight");
                }

                // === 停歇段 ===
                if (contains(stopoverNodes, i + 1)) {
                    Node site = nodes.get(i + 1);
                    for (int j = 0; j < stopoverPointsPerSite; j++) {
                        pointId++;
                        // 聚类：在点附近极小幅度抖动
                        double lng = site.lng + (random.nextDouble() - 0.5) * 0.005;
                        double lat = site.lat + (random.nextDouble() - 0.5) * 0.005;
                        // 聚类：低空、低速
                        double altitude = random.nextDouble() * 5;
                        double speed = random.nextDouble() * 3;
                        currentTime = currentTime.plusMinutes(2 + random.nextInt(2));

                        writer.printf("%d,%s,%.6f,%.6f,%.2f,%.2f,%s,%s%n",
                                pointId, currentTime.format(formatter), lng, lat, altitude, speed,
                                "Stopover", "stopover");
                    }
                }
            }
            System.out.println("✅ 数据集构建完成，共生成 " + pointId + " 条记录。");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static boolean contains(int[] arr, int val) {
        for (int v : arr) if (v == val) return true;
        return false;
    }
}