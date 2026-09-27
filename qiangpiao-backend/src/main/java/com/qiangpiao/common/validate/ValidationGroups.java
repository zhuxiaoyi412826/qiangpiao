package com.qiangpiao.common.validate;

/**
 * JSR-303 分组校验标识。
 */
public final class ValidationGroups {

    private ValidationGroups() {
    }

    /** 新增 */
    public interface Create {
    }

    /** 修改 */
    public interface Update {
    }

    /** 查询 */
    public interface Query {
    }
}
