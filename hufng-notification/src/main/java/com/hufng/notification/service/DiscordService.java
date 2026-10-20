package com.hufng.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hufng.notification.enums.NotificationChannel;
import com.hufng.notification.util.MessageUtil;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "notification.discord.enabled", havingValue = "true")
public class DiscordService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);
    private static final int MAX_LENGTH = 2000;

    @Value("${notification.discord.webhook-url}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    public DiscordService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.DISCORD;
    }

    @Override
    public void send(String message) {
        List<String> chunks = MessageUtil.split(message, MAX_LENGTH);
        for (int i = 0; i < chunks.size(); i++) {
            sendChunk(chunks.get(i));
            if (chunks.size() > 1) {
                log.info("Discord: sent part {}/{}", i + 1, chunks.size());
            }
        }
    }

    private void sendChunk(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl, new HttpEntity<>(Map.of("content", content), headers), String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Discord: sent successfully");
            } else {
                log.error("Discord: error {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Discord: exception {}", e.getMessage());
        }
    }
}
