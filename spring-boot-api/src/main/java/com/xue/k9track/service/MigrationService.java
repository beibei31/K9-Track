package com.xue.k9track.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xue.k9track.entity.*;
import com.xue.k9track.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MigrationService {

    private final MigrationSummaryMapper migrationSummaryMapper;
    private final TrackPointMapper trackPointMapper;
    private final StopoverMapper stopoverMapper;
    private final HourlyActivityMapper hourlyActivityMapper;
    private final ScatterDataMapper scatterDataMapper;

    /**
     * 总览数据：直接从 migration_summary 表读取（Spark 已预计算）
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        MigrationSummary summary = migrationSummaryMapper.selectOne(null);

        if (summary != null) {
            overview.put("totalDistanceKm", summary.getTotalDistance());
            overview.put("avgSpeedKmh", summary.getAvgSpeed());
            overview.put("maxSpeedKmh", summary.getMaxSpeed());
            overview.put("totalStopoverDays", summary.getTotalStopoverDays());
            overview.put("updateTime", summary.getUpdateTime());
        }

        // 补充从其他表获取的统计信息
        long stopoverCount = stopoverMapper.selectCount(null);
        overview.put("stopoverCount", stopoverCount);

        long totalRecords = trackPointMapper.selectCount(null);
        overview.put("totalRecords", totalRecords);

        // 起止日期从轨迹点表获取
        List<TrackPoint> points = trackPointMapper.selectList(
                new QueryWrapper<TrackPoint>().orderByAsc("point_id"));
        if (!points.isEmpty()) {
            overview.put("startDate", points.get(0).getTimestamp().substring(0, 10));
            overview.put("endDate", points.get(points.size() - 1).getTimestamp().substring(0, 10));

            long days = java.time.Duration.between(
                    java.time.LocalDateTime.parse(points.get(0).getTimestamp(),
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    java.time.LocalDateTime.parse(points.get(points.size() - 1).getTimestamp(),
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ).toDays();
            overview.put("totalDays", days);
        }

        return overview;
    }

    /**
     * 抽稀轨迹点 [{lng, lat}]
     */
    public List<Map<String, Double>> getTrack() {
        List<TrackPoint> points = trackPointMapper.selectList(
                new QueryWrapper<TrackPoint>().orderByAsc("point_id"));

        return points.stream().map(p -> {
            Map<String, Double> coord = new LinkedHashMap<>();
            coord.put("lng", p.getLongitude());
            coord.put("lat", p.getLatitude());
            return coord;
        }).collect(Collectors.toList());
    }

    /**
     * 停歇点列表
     */
    public List<Map<String, Object>> getStopovers() {
        List<Stopover> list = stopoverMapper.selectList(
                new QueryWrapper<Stopover>().orderByDesc("stay_days"));

        return list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("lat", s.getLatitude());
            m.put("lng", s.getLongitude());
            m.put("startTime", s.getStartTime());
            m.put("endTime", s.getEndTime());
            m.put("stayDays", s.getStayDays());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 每小时活动频次
     */
    public List<HourlyActivity> getHourlyActivity() {
        List<HourlyActivity> dbList = hourlyActivityMapper.selectList(
                new QueryWrapper<HourlyActivity>().orderByAsc("hour_of_day"));
        // 补全缺失的小时（夜间无飞行记录时 DB 只有 18 行，前端需要 24 行）
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        for (HourlyActivity h : dbList) {
            map.put(h.getHourOfDay(), h.getActivityCount());
        }
        List<HourlyActivity> result = new java.util.ArrayList<>();
        for (int h = 0; h < 24; h++) {
            HourlyActivity item = new HourlyActivity();
            item.setHourOfDay(h);
            item.setActivityCount(map.getOrDefault(h, 0));
            result.add(item);
        }
        return result;
    }

    /**
     * 速度-高度散点数据
     */
    public List<ScatterData> getScatterData() {
        return scatterDataMapper.selectList(null);
    }

    /**
     * 每日平均速度
     */
    public List<Map<String, Object>> getDailySpeed() {
        List<TrackPoint> points = trackPointMapper.selectList(
                new QueryWrapper<TrackPoint>().orderByAsc("point_id"));

        // 按日期分组求平均速度
        Map<String, List<Double>> dateMap = new LinkedHashMap<>();
        for (TrackPoint p : points) {
            String date = p.getTimestamp().substring(0, 10);
            dateMap.computeIfAbsent(date, k -> new ArrayList<>()).add(p.getSpeedKmh());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : dateMap.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", e.getKey());
            m.put("avgSpeed", Math.round(avg * 10.0) / 10.0);
            result.add(m);
        }
        return result;
    }
}
