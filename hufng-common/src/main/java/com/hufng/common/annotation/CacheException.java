package com.hufng.common.annotation;

import java.lang.annotation.*;

/**
 * Custom annotation; cache methods marked with this annotation will throw exceptions
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheException {
}
