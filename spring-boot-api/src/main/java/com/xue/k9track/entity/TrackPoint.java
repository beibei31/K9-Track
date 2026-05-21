package com.xue.k9track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("track_point")
public class TrackPoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer pointId;
    private String timestamp;
    private Double longitude;
    private Double latitude;
    private Double altitude;
    private Double speedKmh;
    private String phase;
    private String pointType;
}
