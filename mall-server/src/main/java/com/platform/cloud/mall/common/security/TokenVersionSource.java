package com.platform.cloud.mall.common.security;

/**
 * 令牌版本号的权威来源（数据库），由各领域实现：system 提供管理员，member 提供会员。
 * 放在 common 定义接口，是为了让版本号缓存与校验逻辑不依赖具体领域的 Mapper。
 */
public interface TokenVersionSource {

    UserType userType();

    /**
     * 从数据库读取当前版本号。
     *
     * @return 用户不存在（含已删除）返回 null，此时其所有令牌都视为无效
     */
    Integer loadVersion(Long userId);

    /** 版本号 +1（改密、冻结、停用、调整角色时调用），由实现方在自己的事务里执行 UPDATE */
    void increment(Long userId);
}
