package ru.tinkoff.kora.cache.redis;

import org.jetbrains.annotations.ApiStatus.Internal;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.cache.Cache;
import ru.tinkoff.kora.cache.redis.client.ReactiveRedisClient;
import ru.tinkoff.kora.cache.redis.client.SyncRedisClient;
import ru.tinkoff.kora.cache.telemetry.CacheTelemetry;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

@Internal
public abstract class AbstractRedisCache<K, V> implements Cache<K, V> {

    private final String name;
    private final SyncRedisClient syncClient;
    private final ReactiveRedisClient reactiveClient;
    private final CacheTelemetry telemetry;

    private final RedisCacheKeyMapper<K> keyMapper;
    private final RedisCacheValueMapper<V> valueMapper;

    private final Long expireAfterAccessMillis;
    private final Long expireAfterWriteMillis;

    protected AbstractRedisCache(String name,
                                 RedisCacheConfig config,
                                 SyncRedisClient syncClient,
                                 ReactiveRedisClient reactiveClient,
                                 CacheTelemetry telemetry,
                                 RedisCacheKeyMapper<K> keyMapper,
                                 RedisCacheValueMapper<V> valueMapper) {
        this.name = name;
        this.syncClient = syncClient;
        this.reactiveClient = reactiveClient;
        this.telemetry = telemetry;
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
        this.expireAfterAccessMillis = (config.expireAfterAccess() == null)
            ? null
            : config.expireAfterAccess().toMillis();
        this.expireAfterWriteMillis = (config.expireAfterWrite() == null)
            ? null
            : config.expireAfterWrite().toMillis();
    }

    @Nonnull
    String origin() {
        return "redis";
    }

    @Override
    public V get(@Nonnull K key) {
        if(key == null) {
            return null;
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("GET", name, origin());
        telemetryContext.startRecording();

        try {
            final byte[] keyAsBytes = keyMapper.apply(key);
            final byte[] jsonAsBytes = (expireAfterAccessMillis == null)
                ? syncClient.get(keyAsBytes)
                : syncClient.getExpire(keyAsBytes, expireAfterAccessMillis);

            final V value = valueMapper.read(jsonAsBytes);
            telemetryContext.recordSuccess(value);
            return value;
        } catch (Exception e) {
            telemetryContext.recordFailure(e);
            return null;
        }
    }

    @Nonnull
    @Override
    public Map<K, V> get(@Nonnull Collection<K> keys) {
        if(keys == null || keys.isEmpty()) {
            return null;
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("GET_MANY", name, origin());

        try {
            telemetryContext.startRecording();
            final Map<K, byte[]> keysByKeyBytes = keys.stream()
                .collect(Collectors.toMap(k -> k, keyMapper, (v1, v2) -> v2));

            final byte[][] keysByBytes = keysByKeyBytes.values().toArray(byte[][]::new);
            final Map<byte[], byte[]> valueByKeys = (expireAfterAccessMillis == null)
                ? syncClient.get(keysByBytes)
                : syncClient.getExpire(keysByBytes, expireAfterAccessMillis);

            final Map<K, V> keyToValue = new HashMap<>();
            for (var entry : keysByKeyBytes.entrySet()) {
                valueByKeys.forEach((k, v) -> {
                    if (Arrays.equals(entry.getValue(), k)) {
                        var value = valueMapper.read(v);
                        keyToValue.put(entry.getKey(), value);
                    }
                });
            }

            telemetryContext.recordSuccess(keyToValue);
            return keyToValue;
        } catch (Exception e) {
            telemetryContext.recordFailure(e);
            return Collections.emptyMap();
        }
    }

    @Nonnull
    @Override
    public V put(@Nonnull K key, @Nonnull V value) {
        if(key == null || value == null) {
            return null;
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("PUT", name, origin());
        telemetryContext.startRecording();

        try {
            final byte[] keyAsBytes = keyMapper.apply(key);
            final byte[] valueAsBytes = valueMapper.write(value);
            if (expireAfterWriteMillis == null) {
                syncClient.set(keyAsBytes, valueAsBytes);
            } else {
                syncClient.setExpire(keyAsBytes, valueAsBytes, expireAfterWriteMillis);
            }
            telemetryContext.recordSuccess();
            return value;
        } catch (Exception e) {
            telemetryContext.recordFailure(e);
            return value;
        }
    }

    @Override
    public void invalidate(@Nonnull K key) {
        if(key != null) {
            final byte[] keyAsBytes = keyMapper.apply(key);
            final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("INVALIDATE", name, origin());

            try {
                telemetryContext.startRecording();
                syncClient.del(keyAsBytes);
                telemetryContext.recordSuccess();
            } catch (Exception e) {
                telemetryContext.recordFailure(e);
            }
        }
    }

    @Override
    public void invalidate(@Nonnull Collection<K> keys) {
        if(keys != null && !keys.isEmpty()) {
            final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("INVALIDATE_MANY", name, origin());

            try {
                final byte[][] keysAsBytes = keys.stream()
                    .map(keyMapper)
                    .toArray(byte[][]::new);

                telemetryContext.startRecording();
                syncClient.del(keysAsBytes);
                telemetryContext.recordSuccess();
            } catch (Exception e) {
                telemetryContext.recordFailure(e);
            }
        }
    }

    @Override
    public void invalidateAll() {
        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("INVALIDATE_ALL", name, origin());

        try {
            telemetryContext.startRecording();
            syncClient.flushAll();
            telemetryContext.recordSuccess();
        } catch (Exception e) {
            telemetryContext.recordFailure(e);
        }
    }

    @Nonnull
    @Override
    public Mono<V> getAsync(@Nonnull K key) {
        if(key == null) {
            return Mono.empty();
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("GET", name, origin());
        return Mono.defer(() -> {
                telemetryContext.startRecording();
                final byte[] keyAsBytes = keyMapper.apply(key);
                return (expireAfterAccessMillis == null)
                    ? reactiveClient.get(keyAsBytes)
                    : reactiveClient.getExpire(keyAsBytes, expireAfterAccessMillis);
            })
            .map(jsonAsBytes -> {
                final V value = valueMapper.read(jsonAsBytes);
                telemetryContext.recordSuccess(value);
                return value;
            })
            .onErrorResume(e -> {
                telemetryContext.recordFailure(e);
                return Mono.empty();
            });
    }

    @Nonnull
    @Override
    public Mono<Map<K, V>> getAsync(@Nonnull Collection<K> keys) {
        if(keys == null || keys.isEmpty()) {
            return Mono.just(Collections.emptyMap());
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("GET_MANY", name, origin());
        return Mono.defer(() -> {
                telemetryContext.startRecording();
                var keysByKeyByte = keys.stream()
                    .collect(Collectors.toMap(k -> k, keyMapper, (v1, v2) -> v2));

                var keysAsBytes = keysByKeyByte.values().toArray(byte[][]::new);
                var getMono = (expireAfterAccessMillis == null)
                    ? reactiveClient.get(keysAsBytes)
                    : reactiveClient.getExpire(keysAsBytes, expireAfterAccessMillis);

                return getMono
                    .map(valuesByKeys -> {
                        final Map<K, V> keyToValue = new HashMap<>();
                        for (var entry : keysByKeyByte.entrySet()) {
                            valuesByKeys.forEach((k, v) -> {
                                if (Arrays.equals(entry.getValue(), k)) {
                                    var value = valueMapper.read(v);
                                    keyToValue.put(entry.getKey(), value);
                                }
                            });
                        }
                        telemetryContext.recordSuccess(keyToValue);
                        return keyToValue;
                    });
            })
            .onErrorResume(e -> {
                telemetryContext.recordFailure(e);
                return Mono.just(Collections.emptyMap());
            });
    }

    @Nonnull
    @Override
    public Mono<V> putAsync(@Nonnull K key, @Nonnull V value) {
        if(key == null) {
            return Mono.justOrEmpty(value);
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("PUT", name, origin());
        return Mono.defer(() -> {
                telemetryContext.startRecording();
                final byte[] keyAsBytes = keyMapper.apply(key);
                final byte[] valueAsBytes = valueMapper.write(value);
                return (expireAfterWriteMillis == null)
                    ? reactiveClient.set(keyAsBytes, valueAsBytes)
                    : reactiveClient.setExpire(keyAsBytes, valueAsBytes, expireAfterWriteMillis);
            })
            .map(r -> value)
            .switchIfEmpty(Mono.fromCallable(() -> {
                telemetryContext.recordSuccess();
                return value;
            }))
            .onErrorResume(e -> {
                telemetryContext.recordFailure(e);
                return Mono.just(value);
            });
    }

    @Nonnull
    @Override
    public Mono<Boolean> invalidateAsync(@Nonnull K key) {
        if(key == null) {
            return Mono.just(false);
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("INVALIDATE", name, origin());
        return Mono.defer(() -> {
                telemetryContext.startRecording();
                final byte[] keyAsBytes = keyMapper.apply(key);
                return reactiveClient.del(keyAsBytes)
                    .then(Mono.just(true));
            })
            .doOnSuccess(r -> telemetryContext.recordSuccess())
            .onErrorResume(e -> {
                telemetryContext.recordFailure(e);
                return Mono.just(false);
            });
    }

    @Override
    public Mono<Boolean> invalidateAsync(@Nonnull Collection<K> keys) {
        if(keys == null || keys.isEmpty()) {
            return Mono.just(false);
        }

        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("INVALIDATE_MANY", name, origin());
        return Mono.defer(() -> {
                telemetryContext.startRecording();
                final byte[][] keyAsBytes = keys.stream()
                    .distinct()
                    .map(keyMapper)
                    .toArray(byte[][]::new);

                return reactiveClient.del(keyAsBytes)
                    .then(Mono.just(true));
            })
            .doOnSuccess(r -> telemetryContext.recordSuccess())
            .onErrorResume(e -> {
                telemetryContext.recordFailure(e);
                return Mono.just(false);
            });
    }

    @Nonnull
    @Override
    public Mono<Boolean> invalidateAllAsync() {
        final CacheTelemetry.TelemetryContext telemetryContext = telemetry.create("INVALIDATE_ALL", name, origin());
        return Mono.defer(() -> {
            telemetryContext.startRecording();
            return reactiveClient.flushAll()
                .then(Mono.just(true))
                .doOnSuccess(r -> telemetryContext.recordSuccess())
                .onErrorResume(e -> {
                    telemetryContext.recordFailure(e);
                    return Mono.just(false);
                });
        });
    }
}
