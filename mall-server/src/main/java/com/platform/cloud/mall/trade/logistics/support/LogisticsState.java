package com.platform.cloud.mall.trade.logistics.support;

/**
 * 物流状态。取值对应快递100 实时查询返回的 state（resultv2=1 时的基础状态）：
 * 0 在途、1 揽收、2 疑难、3 签收、4 退签、5 派件、6 退回、7 转投、8 清关、10~13 清关相关、14 拒签。
 * <p>
 * 对外输出枚举名而不是快递100 的数字，换物流服务商时前端不用改。
 */
public enum LogisticsState {
    NONE("暂无物流信息"),
    IN_TRANSIT("运输中"),
    COLLECTED("已揽收"),
    PROBLEM("疑难件"),
    SIGNED("已签收"),
    RETURN_SIGNED("退件签收"),
    DELIVERING("派送中"),
    RETURNING("退回中"),
    TRANSFERRED("转投中"),
    CUSTOMS("清关中"),
    REJECTED("已拒签");

    private final String text;

    LogisticsState(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    /** 快递100 的 state 字符串转枚举；无法识别的新状态按「运输中」处理，比报错对用户更友好 */
    public static LogisticsState fromKuaidi100(String state) {
        if (state == null || state.isBlank()) {
            return IN_TRANSIT;
        }
        return switch (state.trim()) {
            case "0" -> IN_TRANSIT;
            case "1" -> COLLECTED;
            case "2" -> PROBLEM;
            case "3" -> SIGNED;
            case "4" -> RETURN_SIGNED;
            case "5" -> DELIVERING;
            case "6" -> RETURNING;
            case "7" -> TRANSFERRED;
            case "8", "10", "11", "12", "13" -> CUSTOMS;
            case "14" -> REJECTED;
            default -> IN_TRANSIT;
        };
    }
}
