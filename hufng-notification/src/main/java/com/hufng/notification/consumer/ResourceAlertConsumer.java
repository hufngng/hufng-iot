package com.hufng.notification.consumer;

import com.hufng.notification.dto.ResourceAlertMessage;
import com.hufng.notification.service.NotificationRouter;
import com.hufng.notification.service.ProcessedEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * At-least-once (manual ack) means this message can be read again after a
 * crash/rebalance before it was acked — dedupe via processed_event before the
 * side-effect (Telegram/Discord call), which can't itself be made
 * transactional with the DB write.
 */
@Component
public class ResourceAlertConsumer {

    private static final String CONSUMER_NAME = "hufng-notification";

    private final NotificationRouter notificationRouter;
    private final ProcessedEventService processedEventService;

    public ResourceAlertConsumer(NotificationRouter notificationRouter,
                                  ProcessedEventService processedEventService) {
        this.notificationRouter = notificationRouter;
        this.processedEventService = processedEventService;
    }

    @KafkaListener(topics = "RESOURCE_ALERT", groupId = "hufng-notification")
    public void consume(ResourceAlertMessage alert, Acknowledgment ack) {
        try {
            if (!processedEventService.markProcessedIfNew(alert.eventId(), CONSUMER_NAME)) {
                return;
            }
            notificationRouter.send(alert.channel(), alert.content());
        } finally {
            ack.acknowledge();
        }
    }
}
