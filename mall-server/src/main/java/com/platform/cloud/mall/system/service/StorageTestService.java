package com.platform.cloud.mall.system.service;

import com.platform.cloud.mall.system.vo.StorageTestVO;

/**
 * 对象存储连通性测试。
 */
public interface StorageTestService {

    /** 用当前已保存的配置上传一个小文件再删除，失败抛出带错误码的提示 */
    StorageTestVO test();
}
