package com.hufng.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;
/**
 * Utility class for JSON serialization and deserialization using Jackson.
 * Wrapped to ensure thread-safety and consistent configuration across the application.
 */
public class JacksonUtils {

    // Singleton instance of ObjectMapper (Thread-safe)
    private static final ObjectMapper INSTANCE = new ObjectMapper();

    static {
        // Support for Java 8 Date/Time API (LocalDateTime, LocalDate, etc.)
        INSTANCE.registerModule(new JavaTimeModule());

        // Write dates in ISO-8601 format instead of timestamps
        INSTANCE.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Prevent crashing if JSON contains fields not present in the Java class
        INSTANCE.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Exclude null fields from serialized JSON strings to keep output clean
        INSTANCE.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // Private constructor to prevent instantiation
    private JacksonUtils() {}

    /**
     * @return The configured singleton ObjectMapper instance.
     */
    public static ObjectMapper mapper() {
        return INSTANCE;
    }

    /**
     * Converts a Java Object to a JSON String.
     */
    public static String toJson(Object object) {
        try {
            return INSTANCE.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("JSON encoding failed for object: " + object.getClass().getName(), e);
        }
    }

    /**
     * Converts a JSON String to a specific Java Object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return INSTANCE.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("JSON decoding failed for class: " + clazz.getName(), e);
        }
    }

    /**
     * Converts a JSON String to a Map.
     * Highly useful for dynamic Vault secret structures.
     */
    public static Map<String, Object> toMap(String json) {
        try {
            return INSTANCE.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to convert JSON string to Map", e);
        }
    }

    /**
     * Unchecked exception wrapping Jackson (de)serialization failures.
     */
    public static class JsonSerializationException extends RuntimeException {
        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}