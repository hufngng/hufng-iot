package com.hufng.common.exception;

import com.hufng.common.api.IErrorCode;

/**
 * Assertion utility class for throwing various API exceptions
 * Created by macro on 2020/2/27.
 */
public class Asserts {
    public static void fail(String message) {
        throw new ApiException(message);
    }

    public static void fail(IErrorCode errorCode) {
        throw new ApiException(errorCode);
    }
}
