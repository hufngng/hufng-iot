package com.hufng.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Log wrapper class for the Controller layer
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class WebLog {
    /**
     * Operation description
     */
    private String description;

    /**
     * Operating user
     */
    private String username;

    /**
     * Operation timestamp
     */
    private Long startTime;

    /**
     * Time elapsed (ms)
     */
    private Integer spendTime;

    /**
     * Base path
     */
    private String basePath;

    /**
     * URI
     */
    private String uri;

    /**
     * URL
     */
    private String url;

    /**
     * HTTP method type
     */
    private String method;

    /**
     * IP address
     */
    private String ip;

    /**
     * Request parameters
     */
    private Object parameter;

    /**
     * Response result
     */
    private Object result;

}
