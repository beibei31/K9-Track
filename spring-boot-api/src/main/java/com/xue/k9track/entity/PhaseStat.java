package com.xue.k9track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("phase_stats")
public class PhaseStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phase;
    private Double avgSpeed;
    private Double maxSpeed;
    private Double avgAltitude;
    private Integer recordCount;
}
