package com.retailpulse.config;

import com.retailpulse.dto.response.BusinessEntityResponseDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Base config: key serializer + TTL, do not cache nulls
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        ObjectMapper om = JsonMapper.builder().findAndAddModules().build();

        JacksonJsonRedisSerializer<BusinessEntityResponseDto> entitySer = serializer(om, BusinessEntityResponseDto.class);
        JavaType listType = om.getTypeFactory().constructCollectionType(List.class, BusinessEntityResponseDto.class);
        JacksonJsonRedisSerializer<Object> entityListSer = serializer(om, listType);

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("businessEntity", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(entitySer))
        );
        cacheConfigs.put("businessEntityList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(entityListSer))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base) // default if any other cache is added later
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private static <T> JacksonJsonRedisSerializer<T> serializer(ObjectMapper objectMapper, Class<T> type) {
        return new JacksonJsonRedisSerializer<>(objectMapper, type);
    }

    private static JacksonJsonRedisSerializer<Object> serializer(ObjectMapper objectMapper, JavaType javaType) {
        return new JacksonJsonRedisSerializer<>(objectMapper, javaType);
    }
}
