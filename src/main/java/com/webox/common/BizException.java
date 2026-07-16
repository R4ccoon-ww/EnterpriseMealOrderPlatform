package com.webox.common;

import lombok.Getter;

/**
 * 业务异常。code 约定：400x 参数/业务错误，401 未登录，403 无权限，404 不存在。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(4000, message);
    }
}
