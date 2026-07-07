package com.hufng.notification.util;

import java.util.ArrayList;
import java.util.List;

public final class MessageUtil {

    private MessageUtil() {}

    public static List<String> split(String message, int maxLength) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(start + maxLength, message.length());
            // avoid cutting mid-line
            if (end < message.length()) {
                int lastNewline = message.lastIndexOf('\n', end);
                if (lastNewline > start) end = lastNewline + 1;
            }
            chunks.add(message.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
