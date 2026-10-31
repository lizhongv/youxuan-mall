package com.platform.cloud.mall.trade.logistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 一条物流轨迹 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTraceVO {

    @Schema(description = "时间 yyyy-MM-dd HH:mm:ss（快递公司给出的原始时间文本）")
    private String time;

    @Schema(description = "轨迹描述")
    private String context;

    @Schema(description = "所在地，可能为空")
    private String location;
}
