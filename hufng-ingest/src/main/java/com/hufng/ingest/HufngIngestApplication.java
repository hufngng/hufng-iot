package com.hufng.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the MQTT ingest bridge.
 * Scans the whole com.hufng tree so shared components from hufng-common
 * (e.g. GlobalExceptionHandler, WebLogAspect) are picked up as well.
 */
@SpringBootApplication(scanBasePackages = "com.hufng")
public class HufngIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(HufngIngestApplication.class, args);
    }
}
