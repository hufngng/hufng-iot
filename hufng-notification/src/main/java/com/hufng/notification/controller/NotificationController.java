package com.hufng.notification.controller;

import com.hufng.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sanity endpoint proving the service is up, registered with Eureka, and
 * reachable through hufng-gateway at /api/notification/**.
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @GetMapping("/ping")
    public CommonResult<String> ping() {
        return CommonResult.success("pong");
    }
}
