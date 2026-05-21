package com.xue.k9track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("stopover")
public class Stopover {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Double longitude;
    private Double latitude;
    private String startTime;
    private String endTime;
    private Double stayDays;
}
