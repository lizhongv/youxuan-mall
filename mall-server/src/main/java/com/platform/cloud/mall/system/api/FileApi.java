package com.platform.cloud.mall.system.api;

import java.util.Collection;

/**
 * 文件能力对其他领域开放：校验会员提交的图片地址确实来自本站上传。
 */
public interface FileApi {

    /**
     * 评价图、售后凭证等会员提交的图片，必须是本站上传接口返回的地址（本地 /uploads/ 或当前对象存储域名）；
     * 否则抛业务异常。null 或空集合直接通过。
     */
    void requireUploadedImages(Collection<String> urls);
}
