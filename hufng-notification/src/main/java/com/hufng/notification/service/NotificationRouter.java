package com.hufng.notification.service;

import com.hufng.notification.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRouter {

    private final List<NotificationService> services;

    public void send(String message) {
        send(NotificationChannel.ALL, message);
    }

    public void send(NotificationChannel channel, String message) {
        List<NotificationService> targets = channel == NotificationChannel.ALL
                ? services
                : services.stream().filter(s -> s.channel() == channel).toList();

        if (targets.isEmpty()) {
            log.warn("No notification channel enabled for {}", channel);
            return;
        }
        targets.forEach(s -> {
            try {
                s.send(message);
            } catch (Exception e) {
                log.error("{} failed: {}", s.getClass().getSimpleName(), e.getMessage());
            }
        });
    }
}
