package com.xue.k9track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("migration_summary")
public class MigrationSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Double totalDistance;
    private Double avgSpeed;
    private Double maxSpeed;
    private Double totalStopoverDays;
    private String updateTime;
}
