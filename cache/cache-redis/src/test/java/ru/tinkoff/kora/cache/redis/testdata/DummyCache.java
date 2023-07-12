package ru.tinkoff.kora.cache.redis.testdata;

import ru.tinkoff.kora.cache.redis.AbstractRedisCache;
import ru.tinkoff.kora.cache.redis.RedisCacheConfig;
import ru.tinkoff.kora.cache.redis.RedisCacheKeyMapper;
import ru.tinkoff.kora.cache.redis.RedisCacheValueMapper;
import ru.tinkoff.kora.cache.redis.client.ReactiveRedisClient;
import ru.tinkoff.kora.cache.redis.client.SyncRedisClient;

public final class DummyCache extends AbstractRedisCache<String, String> {

    public DummyCache(RedisCacheConfig config,
                      SyncRedisClient syncClient,
                      ReactiveRedisClient reactiveClient,
                      CacheTelemetry telemetry,
                      RedisCacheKeyMapper<String> keyMapper,
                      RedisCacheValueMapper<String> valueMapper) {
        super("dummy", config, syncClient, reactiveClient, telemetry, keyMapper, valueMapper);
    }
}
