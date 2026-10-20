package com.hufng.report;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * SpringApplication.run is mocked so main() is exercised without starting the real
 * context (which would need Kafka/ClickHouse/Eureka).
 */
class HufngReportApplicationTest {

    @Test
    void main_delegatesToSpringApplicationRunWithArgs() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            HufngReportApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(HufngReportApplication.class, args));
        }
    }

    @Test
    void canBeInstantiatedAsConfigurationBean() {
        // Spring instantiates the @SpringBootApplication class itself as a @Configuration bean
        assertThat(new HufngReportApplication()).isNotNull();
    }

    @Test
    void scansWholeComHufngTree_soHufngCommonComponentsArePickedUp() {
        SpringBootApplication annotation = HufngReportApplication.class.getAnnotation(SpringBootApplication.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.scanBasePackages()).containsExactly("com.hufng");
    }
}
