package com.bonae.logistics.hub.infrastructure.config;

import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import com.fasterxml.jackson.databind.JavaType;
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
import java.util.List;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final String HUB_DETAIL_CACHE_NAME = "hubDetail";
    private static final String HUB_ROUTE_GRAPH_CACHE_NAME = "hubRouteGraph";

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
                .entryTtl(Duration.ofMinutes(10))
                // 논리적 캐시 이름(hubDetail)과 실제 Redis 키 prefix(hub:detail:)를 분리한다.
                .computePrefixWith(cacheName -> "hub:detail:");

        JavaType hubRouteGraphType = objectMapper.getTypeFactory().constructCollectionType(List.class, HubRouteEdge.class);
        Jackson2JsonRedisSerializer<List<HubRouteEdge>> hubRouteGraphSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, hubRouteGraphType);

        RedisCacheConfiguration hubRouteGraphConfig = defaultConfig
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(hubRouteGraphSerializer))
                .entryTtl(Duration.ofMinutes(5))
                // 활성 간선 전체를 하나의 키로 관리한다.
                .computePrefixWith(cacheName -> "hub:route-graph:");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(HUB_DETAIL_CACHE_NAME, hubDetailConfig)
                .withCacheConfiguration(HUB_ROUTE_GRAPH_CACHE_NAME, hubRouteGraphConfig)
                .transactionAware() // 커밋 이후에 캐시 반영 (조회는 즉시, 쓰기/삭제만 커밋 후로 미뤄짐)
                .build();
    }

    // CachingConfigurer errorHandler()를 통해 annotation-driven cache가 사용할 오류 처리기를 명시한다.
    @Override
    public CacheErrorHandler errorHandler() {
        return new HubCacheErrorHandler();
    }
}