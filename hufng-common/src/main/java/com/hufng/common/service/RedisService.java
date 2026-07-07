package com.hufng.common.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis operation service interface
 */
public interface RedisService {
    
    /**
     * Save a property
     */
    void set(String key, Object value, long time);

    /**
     * Save a property
     */
    void set(String key, Object value);

    /**
     * Get a property
     */
    Object get(String key);

    /**
     * Delete a property
     */
    Boolean del(String key);

    /**
     * Batch delete properties
     */
    Long del(List<String> keys);

    /**
     * Set expiration time
     */
    Boolean expire(String key, long time);

    /**
     * Get expiration time
     */
    Long getExpire(String key);

    /**
     * Check whether a property exists
     */
    Boolean hasKey(String key);

    /**
     * Increment by delta
     */
    Long incr(String key, long delta);

    /**
     * Decrement by delta
     */
    Long decr(String key, long delta);

    /**
     * Get a property from a Hash structure
     */
    Object hGet(String key, String hashKey);

    /**
     * Put a property into a Hash structure
     */
    Boolean hSet(String key, String hashKey, Object value, long time);

    /**
     * Put a property into a Hash structure
     */
    void hSet(String key, String hashKey, Object value);

    /**
     * Get the entire Hash structure
     */
    Map<Object, Object> hGetAll(String key);

    /**
     * Set the entire Hash structure
     */
    Boolean hSetAll(String key, Map<String, Object> map, long time);

    /**
     * Set the entire Hash structure
     */
    void hSetAll(String key, Map<String, ?> map);

    /**
     * Delete a property from a Hash structure
     */
    void hDel(String key, Object... hashKey);

    /**
     * Check whether a property exists in a Hash structure
     */
    Boolean hHasKey(String key, String hashKey);

    /**
     * Increment a property in a Hash structure
     */
    Long hIncr(String key, String hashKey, Long delta);

    /**
     * Decrement a property in a Hash structure
     */
    Long hDecr(String key, String hashKey, Long delta);

    /**
     * Get all members of a Set structure
     */
    Set<Object> sMembers(String key);

    /**
     * Add a property to a Set structure
     */
    Long sAdd(String key, Object... values);

    /**
     * Add a property to a Set structure
     */
    Long sAdd(String key, long time, Object... values);

    /**
     * Check whether a value is a member of a Set
     */
    Boolean sIsMember(String key, Object value);

    /**
     * Get the size of a Set structure
     */
    Long sSize(String key);

    /**
     * Remove a property from a Set structure
     */
    Long sRemove(String key, Object... values);

    /**
     * Get a range of elements from a List structure
     */
    List<Object> lRange(String key, long start, long end);

    /**
     * Get the length of a List structure
     */
    Long lSize(String key);

    /**
     * Get an element from a List by index
     */
    Object lIndex(String key, long index);

    /**
     * Append a property to a List structure
     */
    Long lPush(String key, Object value);

    /**
     * Append a property to a List structure
     */
    Long lPush(String key, Object value, long time);

    /**
     * Batch append properties to a List structure
     */
    Long lPushAll(String key, Object... values);

    /**
     * Batch append properties to a List structure
     */
    Long lPushAll(String key, Long time, Object... values);

    /**
     * Remove a property from a List structure
     */
    Long lRemove(String key, long count, Object value);
}