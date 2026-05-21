package com.xue.k9track.controller;

import com.xue.k9track.entity.HourlyActivity;
import com.xue.k9track.entity.PhaseStat;
import com.xue.k9track.entity.ScatterData;
import com.xue.k9track.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/migration")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationService migrationService;

    /**
     * GET /api/migration/overview
     * 返回总览数据：总里程、总天数、最快时速、停歇点数量等
     */
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        return migrationService.getOverview();
    }

    /**
     * GET /api/migration/phase-stats
     * 返回各阶段（breeding / migration / stopover）的统计信息
     */
    @GetMapping("/phase-stats")
    public List<PhaseStat> getPhaseStats() {
        return migrationService.getPhaseStats();
    }

    /**
     * GET /api/migration/track
     * 返回抽稀后的轨迹点 [{lng, lat}, ...]
     */
    @GetMapping("/track")
    public List<Map<String, Double>> getTrack() {
        return migrationService.getTrack();
    }

    /**
     * GET /api/migration/stopovers
     * 返回停歇点列表 [{lat, lng, startTime, endTime, stayDays}, ...]
     */
    @GetMapping("/stopovers")
    public List<Map<String, Object>> getStopovers() {
        return migrationService.getStopovers();
    }

    /**
     * GET /api/migration/hourly-activity
     * 返回 0-23 小时的活动频次
     */
    @GetMapping("/hourly-activity")
    public List<HourlyActivity> getHourlyActivity() {
        return migrationService.getHourlyActivity();
    }

    /**
     * GET /api/migration/scatter
     * 返回速度-高度散点采样数据 [{speed, altitude}, ...]
     */
    @GetMapping("/scatter")
    public List<ScatterData> getScatter() {
        return migrationService.getScatterData();
    }

    /**
     * GET /api/migration/daily-speed
     * 返回每日平均速度 [{date, avgSpeed}, ...]
     */
    @GetMapping("/daily-speed")
    public List<Map<String, Object>> getDailySpeed() {
        return migrationService.getDailySpeed();
    }
}
