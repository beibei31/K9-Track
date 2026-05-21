package com.xue.k9track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("hourly_activity")
public class HourlyActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer hourOfDay;
    private Integer activityCount;
}
