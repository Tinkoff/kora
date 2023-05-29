package ru.tinkoff.kora.cache.redis;

import org.junit.jupiter.api.Assertions;
import ru.tinkoff.kora.cache.redis.client.LettuceClientConfig;
import ru.tinkoff.kora.cache.redis.testdata.DummyCache;
import ru.tinkoff.kora.test.redis.RedisParams;

import java.time.Duration;

abstract class CacheRunner extends Assertions implements RedisCacheModule {

    protected DummyCache createCache(RedisParams redisParams) {
        var lettuceClientFactory = lettuceClientFactory();
        var lettuceClientConfig = new LettuceClientConfig(redisParams.uri().toString(), null, null, null, null, null, null);
        var lettuceCommander = lettuceCommander(lettuceClientFactory.build(lettuceClientConfig));
        lettuceCommander.init().block(Duration.ofMinutes(1));

        var syncRedisClient = lettuceCacheRedisClient(lettuceCommander);
        var reactiveRedisClient = lettuceReactiveCacheRedisClient(lettuceCommander);
        return new DummyCache(new RedisCacheConfig(null, null),
            syncRedisClient, reactiveRedisClient, defaultCacheTelemetry(null, null),
            stringRedisKeyMapper(), stringRedisValueMapper());
    }
}
