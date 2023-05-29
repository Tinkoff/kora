package ru.tinkoff.kora.cache.caffeine;

import reactor.core.publisher.Mono;
import ru.tinkoff.kora.cache.Cache;
import ru.tinkoff.kora.cache.telemetry.CacheTelemetry;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

public abstract class AbstractCaffeineCache<K, V> implements Cache<K, V> {

    private static final String ORIGIN = "caffeine";

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<K, V> caffeine;
    private final CacheTelemetry telemetry;

    protected AbstractCaffeineCache(String name,
                                    CaffeineCacheConfig config,
                                    CaffeineCacheFactory factory,
                                    CacheTelemetry telemetry) {
        this.name = name;
        this.caffeine = factory.build(config);
        this.telemetry = telemetry;
    }

    @Override
    public V get(@Nonnull K key) {
        var telemetryContext = telemetry.create(CacheTelemetry.Operation.Type.GET, name, ORIGIN);
        telemetryContext.startRecording();
        var value = caffeine.getIfPresent(key);
        telemetryContext.recordSuccess(value);
        return value;
    }

    @Nonnull
    @Override
    public Map<K, V> get(@Nonnull Collection<K> keys) {
        var telemetryContext = telemetry.create(CacheTelemetry.Operation.Type.GET, name, ORIGIN);
        telemetryContext.startRecording();
        var values = caffeine.getAllPresent(keys);
        telemetryContext.recordSuccess();
        return values;
    }

    @Nonnull
    public V put(@Nonnull K key, @Nonnull V value) {
        var telemetryContext = telemetry.create(CacheTelemetry.Operation.Type.PUT, name, ORIGIN);
        telemetryContext.startRecording();
        caffeine.put(key, value);
        telemetryContext.recordSuccess();
        return value;
    }

    @Override
    public void invalidate(@Nonnull K key) {
        var telemetryContext = telemetry.create(CacheTelemetry.Operation.Type.INVALIDATE, name, ORIGIN);
        telemetryContext.startRecording();
        caffeine.invalidate(key);
        telemetryContext.recordSuccess();
    }

    @Override
    public void invalidate(@Nonnull Collection<K> keys) {
        var telemetryContext = telemetry.create(CacheTelemetry.Operation.Type.INVALIDATE, name, ORIGIN);
        telemetryContext.startRecording();
        caffeine.invalidateAll(keys);
        telemetryContext.recordSuccess();
    }

    @Override
    public void invalidateAll() {
        var telemetryContext = telemetry.create(CacheTelemetry.Operation.Type.INVALIDATE_ALL, name, ORIGIN);
        telemetryContext.startRecording();
        caffeine.invalidateAll();
        telemetryContext.recordSuccess();
    }

    @Nonnull
    @Override
    public Mono<V> getAsync(@Nonnull K key) {
        return Mono.fromCallable(() -> get(key));
    }

    @Nonnull
    @Override
    public Mono<Map<K, V>> getAsync(@Nonnull Collection<K> keys) {
        return Mono.fromCallable(() -> get(keys));
    }

    @Nonnull
    @Override
    public Mono<V> putAsync(@Nonnull K key, @Nonnull V value) {
        return Mono.fromCallable(() -> put(key, value));
    }

    @Nonnull
    @Override
    public Mono<Boolean> invalidateAsync(@Nonnull K key) {
        return Mono.fromCallable(() -> {
            invalidate(key);
            return true;
        });
    }

    @Override
    public Mono<Boolean> invalidateAsync(@Nonnull Collection<K> keys) {
        return Mono.fromCallable(() -> {
            invalidate(keys);
            return true;
        });
    }

    @Nonnull
    @Override
    public Mono<Boolean> invalidateAllAsync() {
        return Mono.fromCallable(() -> {
            invalidateAll();
            return true;
        });
    }
}
