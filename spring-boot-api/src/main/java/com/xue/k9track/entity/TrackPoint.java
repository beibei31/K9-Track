package com.xue.k9track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("track_points")
public class TrackPoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer pointId;
    private Double lng;
    private Double lat;
    private String timestamp;
}
