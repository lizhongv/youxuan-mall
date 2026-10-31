package com.platform.cloud.mall.trade.freight.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 运费模板（trd_freight_template）。is_default 全局最多一个，由服务层在保存时互斥维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trd_freight_template")
public class FreightTemplate extends BaseEntity {

    private String name;
    /** 见 {@link com.platform.cloud.mall.trade.freight.support.FreightCalculator.ChargeType} */
    private String chargeType;
    private Integer isDefault;
    private Integer status;
}
