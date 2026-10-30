package com.platform.cloud.mall.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 下拉选项用的字典项，只含标签与值 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO {

    private String label;
    private String value;
}
