package com.retailpulse.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void cacheManagerCreatesConfiguredCaches() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = new RedisConfig().cacheManager(connectionFactory);

        assertThat(cacheManager.getCacheNames()).isEmpty();
        assertThat(cacheManager.getCache("businessEntity")).isNotNull();
        assertThat(cacheManager.getCache("businessEntityList")).isNotNull();
        assertThat(cacheManager.getCacheNames()).contains("businessEntity", "businessEntityList");
    }
}
