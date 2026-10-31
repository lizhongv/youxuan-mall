package com.platform.cloud.mall.trade.logistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单物流。查询失败、未开通、未发货时 traces 为空数组并在 message 中给出提示，前端统一按「有轨迹 / 无轨迹 + 提示」渲染。
 */
@Data
public class LogisticsVO {

    @Schema(description = "快递公司编码（快递100 编码，如 shunfeng），无法识别时为空")
    private String company;

    @Schema(description = "快递公司名称")
    private String companyName;

    @Schema(description = "运单号")
    private String trackingNo;

    @Schema(description = "物流状态 NONE / IN_TRANSIT / COLLECTED / PROBLEM / SIGNED / RETURN_SIGNED / DELIVERING / "
            + "RETURNING / TRANSFERRED / CUSTOMS / REJECTED")
    private String state;

    @Schema(description = "物流状态文案")
    private String stateText;

    @Schema(description = "轨迹，时间倒序（最新在前）")
    private List<LogisticsTraceVO> traces = new ArrayList<>();

    @Schema(description = "提示信息：未开通、未发货、查询失败等，正常有轨迹时为空")
    private String message;
}
