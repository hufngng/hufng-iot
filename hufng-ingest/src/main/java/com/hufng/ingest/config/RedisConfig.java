package com.hufng.ingest.config;

import com.hufng.common.config.BaseRedisConfig;
import org.springframework.context.annotation.Configuration;

/**
 * Activates the shared Redis beans (redisTemplate, redisSerializer, redisCacheManager,
 * redisService) from hufng-common — the intended pattern for consuming BaseRedisConfig.
 */
@Configuration
public class RedisConfig extends BaseRedisConfig {
}
