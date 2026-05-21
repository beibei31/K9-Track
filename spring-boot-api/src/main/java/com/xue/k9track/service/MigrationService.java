package com.xue.k9track.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xue.k9track.entity.*;
import com.xue.k9track.mapper.*;
import com.xue.k9track.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MigrationService {

    private final TrackPointMapper trackPointMapper;
    private final StopoverMapper stopoverMapper;
    private final PhaseStatMapper phaseStatMapper;
    private final HourlyActivityMapper hourlyActivityMapper;
    private final ScatterDataMapper scatterDataMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取总览数据：总里程、总天数、最快时速、停歇点数量
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // 总里程：按 point_id 排序的抽稀轨迹点，累加 Haversine 距离
        List<TrackPoint> points = trackPointMapper.selectList(
                new QueryWrapper<TrackPoint>().orderByAsc("point_id"));

        double totalDistance = 0;
        for (int i = 1; i < points.size(); i++) {
            TrackPoint prev = points.get(i - 1);
            TrackPoint curr = points.get(i);
            totalDistance += HaversineUtil.distance(prev.getLat(), prev.getLng(), curr.getLat(), curr.getLng());
        }
        overview.put("totalDistanceKm", Math.round(totalDistance * 10.0) / 10.0);

        // 总天数：从轨迹点时间戳的时间跨度
        if (!points.isEmpty()) {
            LocalDateTime first = LocalDateTime.parse(points.get(0).getTimestamp(), FMT);
            LocalDateTime last = LocalDateTime.parse(points.get(points.size() - 1).getTimestamp(), FMT);
            overview.put("totalDays", Duration.between(first, last).toDays());
            overview.put("startDate", points.get(0).getTimestamp().substring(0, 10));
            overview.put("endDate", points.get(points.size() - 1).getTimestamp().substring(0, 10));
        } else {
            overview.put("totalDays", 0);
            overview.put("startDate", "");
            overview.put("endDate", "");
        }

        // 最快时速：从各阶段统计中取最大值
        Double maxSpeed = phaseStatMapper.selectList(null).stream()
                .map(PhaseStat::getMaxSpeed)
                .max(Double::compare).orElse(0.0);
        overview.put("maxSpeedKmh", maxSpeed);

        // 停歇点数量
        Long stopoverCount = stopoverMapper.selectCount(null);
        overview.put("stopoverCount", stopoverCount);

        // 总 GPS 记录数
        Integer totalRecords = phaseStatMapper.selectList(null).stream()
                .map(PhaseStat::getRecordCount)
                .reduce(0, Integer::sum);
        overview.put("totalRecords", totalRecords);

        return overview;
    }

    /**
     * 获取各阶段统计数据
     */
    public List<PhaseStat> getPhaseStats() {
        return phaseStatMapper.selectList(null);
    }

    /**
     * 获取抽稀轨迹点 (供前端绘制迁徙路线)
     */
    public List<Map<String, Double>> getTrack() {
        List<TrackPoint> points = trackPointMapper.selectList(
                new QueryWrapper<TrackPoint>().orderByAsc("point_id"));

        return points.stream().map(p -> {
            Map<String, Double> coord = new LinkedHashMap<>();
            coord.put("lng", p.getLng());
            coord.put("lat", p.getLat());
            return coord;
        }).collect(Collectors.toList());
    }

    /**
     * 获取停歇点列表
     */
    public List<Map<String, Object>> getStopovers() {
        List<Stopover> list = stopoverMapper.selectList(
                new QueryWrapper<Stopover>().orderByDesc("stay_days"));

        return list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("lat", s.getLat());
            m.put("lng", s.getLng());
            m.put("startTime", s.getStartTime());
            m.put("endTime", s.getEndTime());
            m.put("stayDays", s.getStayDays());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 获取每小时活动频次
     */
    public List<HourlyActivity> getHourlyActivity() {
        return hourlyActivityMapper.selectList(
                new QueryWrapper<HourlyActivity>().orderByAsc("hour"));
    }

    /**
     * 获取速度-高度散点数据
     */
    public List<ScatterData> getScatterData() {
        return scatterDataMapper.selectList(null);
    }
}
