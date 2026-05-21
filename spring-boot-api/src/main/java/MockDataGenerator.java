import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class MockDataGenerator {
    public static void main(String[] args) {
        String outputFile = "spoonbill_k9_gps_corrected.csv";
        int totalPoints = 194500;
        Random random = new Random();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 初始时间设定为 4 月 3 日
        LocalDateTime currentTime = LocalDateTime.of(2024, 4, 3, 6, 0);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // 写入表头
            writer.println("point_id,timestamp,longitude,latitude,altitude,speed_kmh,phase,point_type");

            for (int i = 1; i <= totalPoints; i++) {
                double lng, lat, altitude, speed;
                String phase, pointType;

                // 阶段1：繁殖地 (俄罗斯楚科奇) - 5000条数据，停留约10天
                if (i <= 5000) {
                    lng = 177.5 + random.nextGaussian() * 0.05;
                    lat = 64.5 + random.nextGaussian() * 0.05;
                    altitude = 10 + random.nextDouble() * 20;
                    speed = Math.abs(random.nextGaussian() * 3);
                    phase = "Breeding";
                    pointType = "habitat";
                    currentTime = currentTime.plusMinutes(random.nextInt(5) + 1);
                }
                // 阶段2：第一段南下迁徙 (沿海岸线狂飙) - 7000条数据，历时约5天
                else if (i <= 12000) {
                    double progress = (i - 5000) / 7000.0;
                    // 加入弧线偏移，模拟沿着东亚海岸线飞行
                    lng = 177.5 - (177.5 - 120.9) * progress - Math.sin(progress * Math.PI) * 5.0;
                    lat = 64.5 - (64.5 - 32.8) * progress;
                    altitude = 2000 + random.nextGaussian() * 300;
                    speed = 45 + random.nextGaussian() * 15; // 巡航速度快
                    phase = "Migration";
                    pointType = "flight";
                    currentTime = currentTime.plusMinutes(1);
                }
                // 阶段3：超级停歇地 (江苏条子泥) - 165000条海量数据，停留长达1个月
                else if (i <= 177000) {
                    lng = 120.9 + random.nextGaussian() * 0.01; // 高斯噪声极小，密集聚类
                    lat = 32.8 + random.nextGaussian() * 0.01;
                    altitude = random.nextDouble() * 5; // 在滩涂觅食，海拔极低
                    speed = Math.abs(random.nextGaussian() * 2); // 速度极慢
                    phase = "Stopover";
                    pointType = "stopover";
                    currentTime = currentTime.plusSeconds(15); // 时间走得慢，数据打点密
                }
                // 阶段4：第二段南下迁徙 - 7500条数据，飞往东南亚
                else if (i <= 184500) {
                    double progress = (i - 177000) / 7500.0;
                    lng = 120.9 - (120.9 - 97.5) * progress - Math.sin(progress * Math.PI) * 2.0;
                    lat = 32.8 - (32.8 - 16.2) * progress;
                    altitude = 1500 + random.nextGaussian() * 200;
                    speed = 50 + random.nextGaussian() * 10;
                    phase = "Migration";
                    pointType = "flight";
                    currentTime = currentTime.plusMinutes(1);
                }
                // 阶段5：越冬地 (缅甸马塔班湾) - 剩下的10000条数据
                else {
                    lng = 97.5 + random.nextGaussian() * 0.03;
                    lat = 16.2 + random.nextGaussian() * 0.03;
                    altitude = 5 + random.nextDouble() * 10;
                    speed = Math.abs(random.nextGaussian() * 4);
                    phase = "Wintering";
                    pointType = "habitat";
                    currentTime = currentTime.plusMinutes(random.nextInt(10) + 2);
                }

                // 极端值保护
                if (speed < 0) speed = 0;
                if (altitude < 0) altitude = 0;

                // 写入 CSV，保留合适的小数位
                writer.printf("%d,%s,%.6f,%.6f,%.2f,%.2f,%s,%s%n",
                        i, currentTime.format(formatter), lng, lat, altitude, speed, phase, pointType);
            }
            System.out.println("✅ 完美生态飞路数据生成完毕！文件位置: " + outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}