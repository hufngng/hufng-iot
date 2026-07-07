package com.hufng.notification.service;

import com.hufng.notification.enums.NotificationChannel;

public interface NotificationService {
    NotificationChannel channel();

    void send(String message);
}
