package com.sky.exception;

/**
 * 自建异常，表示获取地址信息失败异常
 */
public class AddressNotFoundException extends BaseException {
    public AddressNotFoundException(String message) {
        super(message);
    }
}
