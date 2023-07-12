package ru.tinkoff.kora.cache.caffeine;

import org.junit.jupiter.api.Assertions;
import ru.tinkoff.kora.cache.caffeine.testdata.DummyCache;

abstract class CacheRunner extends Assertions implements CaffeineCacheModule {

    protected DummyCache createCache() {
        try {
            return new DummyCache(new CaffeineCacheConfig(null, null, null, null),
                caffeineCacheFactory(), defaultCacheTelemetry(null, null));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
