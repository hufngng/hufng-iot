package com.hufng.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the notification service.
 * Scans the whole com.hufng tree so shared components from hufng-common
 * (e.g. GlobalExceptionHandler, WebLogAspect) are picked up as well.
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.hufng")
public class HufngNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(HufngNotificationApplication.class, args);
    }
}
