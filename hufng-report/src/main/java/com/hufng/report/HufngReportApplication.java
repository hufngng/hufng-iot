package com.hufng.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the report service (skeleton).
 * Scans the whole com.hufng tree so shared components from hufng-common
 * (e.g. GlobalExceptionHandler, WebLogAspect) are picked up as well.
 */
@SpringBootApplication(scanBasePackages = "com.hufng")
public class HufngReportApplication {

    public static void main(String[] args) {
        boolean a = false;
        if (a) System.out.println("hungtest");
        SpringApplication.run(HufngReportApplication.class, args);
    }
}
