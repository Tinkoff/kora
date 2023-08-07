package ru.tinkoff.kora.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ru.tinkoff.kora.cache.telemetry.CacheMetrics;
import ru.tinkoff.kora.cache.telemetry.CacheTracer;
import ru.tinkoff.kora.common.DefaultComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CaffeineCacheModule {

    //TODO refactor telemetry to separate impls for redis and caffeine
    @DefaultComponent
    default CaffeineCacheTelemetry caffeineCacheTelemetry(@Nullable CacheMetrics metrics, @Nullable CacheTracer tracer) {
        return new CaffeineCacheTelemetry(metrics, tracer);
    }

    @DefaultComponent
    default CaffeineCacheFactory caffeineCacheFactory() {
        return new CaffeineCacheFactory() {
            @Nonnull
            @Override
            public <K, V> Cache<K, V> build(@Nonnull CaffeineCacheConfig config) {
                final Caffeine<K, V> builder = (Caffeine<K, V>) Caffeine.newBuilder();
                if (config.expireAfterWrite() != null)
                    builder.expireAfterWrite(config.expireAfterWrite());
                if (config.expireAfterAccess() != null)
                    builder.expireAfterAccess(config.expireAfterAccess());
                if (config.initialSize() != null)
                    builder.initialCapacity(config.initialSize());
                if (config.maximumSize() != null)
                    builder.maximumSize(config.maximumSize());
                return builder.recordStats().build();
            }
        };
    }
}
