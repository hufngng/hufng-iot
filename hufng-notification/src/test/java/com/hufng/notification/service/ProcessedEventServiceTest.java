package com.hufng.notification.service;

import com.hufng.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ProcessedEventService.class)
class ProcessedEventServiceTest {

    @Autowired
    private ProcessedEventService processedEventService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void markProcessedIfNew_firstTime_returnsTrueAndPersists() {
        boolean result = processedEventService.markProcessedIfNew("event-1", "hufng-notification");

        assertThat(result).isTrue();
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void markProcessedIfNew_duplicate_returnsFalseAndDoesNotDoubleInsert() {
        processedEventService.markProcessedIfNew("event-1", "hufng-notification");

        boolean secondAttempt = processedEventService.markProcessedIfNew("event-1", "hufng-notification");

        assertThat(secondAttempt).isFalse();
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void markProcessedIfNew_sameEventDifferentConsumer_isTreatedAsNew() {
        processedEventService.markProcessedIfNew("event-1", "hufng-notification");

        boolean otherConsumer = processedEventService.markProcessedIfNew("event-1", "some-other-consumer");

        assertThat(otherConsumer).isTrue();
        assertThat(processedEventRepository.count()).isEqualTo(2);
    }
}
