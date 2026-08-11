package com.bonae.logistics.hub.infrastructure.config;

import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final String HUB_DETAIL_CACHE_NAME = "hubDetail";

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();

        Jackson2JsonRedisSerializer<HubDetailResponse> hubDetailSerializer =
                new Jackson2JsonRedisSerializer<>(
                        objectMapper,
                        HubDetailResponse.class
                );

        RedisCacheConfiguration hubDetailConfig = defaultConfig
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                hubDetailSerializer
                        )
                )
                .entryTtl(Duration.ofHours(24))
                // 논리적 캐시 이름(hubDetail)과 실제 Redis 키 prefix(hub:detail:)를 분리
                .computePrefixWith(cacheName -> "hub:detail:");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(HUB_DETAIL_CACHE_NAME, hubDetailConfig)
                .transactionAware() // 커밋 이후에 캐시 반영 (조회는 즉시, 쓰기/삭제만 커밋 후로 미뤄짐)
                .build();
    }

    //CachingConfigurer errorHandler()를 통해 annotation-driven cache가 사용할 오류 처리기를 명시
    @Override
    public CacheErrorHandler errorHandler() {
        return new HubCacheErrorHandler();
    }
}