package com.platform.cloud.mall.trade.review.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 评价 VO；公开接口不返回 memberId，避免匿名评价被反查 */
@Data
public class ReviewVO {

    private Long id;
    private Long orderId;
    private Long goodsId;
    private String goodsName;
    private String goodsPic;
    private Long memberId;
    private String nickname;
    private String avatar;
    private String specText;
    private Integer rating;
    private String content;
    private List<String> images;
    private Integer anonymous;
    private String reply;
    private LocalDateTime replyTime;
    private Integer status;
    private LocalDateTime createdAt;
}
