package com.xue.k9track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("scatter_data")
public class ScatterData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Double speed;
    private Double altitude;
}
