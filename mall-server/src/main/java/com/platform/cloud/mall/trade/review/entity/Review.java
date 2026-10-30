package com.platform.cloud.mall.trade.review.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/** 商品评价（trd_review）。昵称、头像在评价时快照，会员改名不影响历史评价展示。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "trd_review", autoResultMap = true)
public class Review extends BaseEntity {

    private Long orderId;
    private Long orderItemId;
    private Long goodsId;
    private Long memberId;
    private String nickname;
    private String avatar;
    private String specText;
    private Integer rating;
    private String content;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;
    private Integer anonymous;
    private String reply;
    private LocalDateTime replyTime;
    /** 1 显示 0 隐藏 */
    private Integer status;
}
