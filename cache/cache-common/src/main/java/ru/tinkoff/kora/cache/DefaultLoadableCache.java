package ru.tinkoff.kora.cache;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

final class DefaultLoadableCache<K, V> implements LoadableCache<K, V> {

    private final Cache<K, V> cache;
    private final CacheLoader<K, V> cacheLoader;

    DefaultLoadableCache(Cache<K, V> cache, CacheLoader<K, V> cacheLoader) {
        this.cache = cache;
        this.cacheLoader = cacheLoader;
    }

    @Nullable
    @Override
    public V get(@Nonnull K key) {
        var fromCache = cache.get(key);
        if (fromCache != null) {
            return fromCache;
        }

        var value = cacheLoader.load(key);
        if (value != null) {
            cache.put(key, value);
        }

        return value;
    }

    @Nonnull
    @Override
    public Map<K, V> get(@Nonnull Collection<K> keys) {
        var fromCache = cache.get(keys);
        if (fromCache.size() == keys.size()) {
            return fromCache;
        }

        var missingKeys = keys.stream()
            .filter(k -> !fromCache.containsKey(k))
            .toList();

        var value = cacheLoader.load(missingKeys);
        if (value != null) {
            value.forEach(cache::put);
        }

        var result = new HashMap<>(fromCache);
        result.putAll(value);
        return result;
    }

    @Nonnull
    @Override
    public Mono<V> getAsync(@Nonnull K key) {
        return cache.getAsync(key)
            .switchIfEmpty(
                cacheLoader.loadAsync(key)
                    .flatMap(value -> cache.putAsync(key, value).thenReturn(value))
            );
    }

    @Nonnull
    @Override
    public Mono<Map<K, V>> getAsync(@Nonnull Collection<K> keys) {
        return cache.getAsync(keys)
            .flatMap(fromCache -> {
                if (fromCache.size() == keys.size()) {
                    return Mono.just(fromCache);
                }

                var missingKeys = keys.stream()
                    .filter(k -> !fromCache.containsKey(k))
                    .toList();

                return cacheLoader.loadAsync(missingKeys)
                    .flatMap(loaded -> {
                        var putMonos = loaded.entrySet().stream()
                            .map(e -> cache.putAsync(e.getKey(), e.getValue()))
                            .toList();

                        final Map<K, V> result;
                        if (fromCache.isEmpty()) {
                            result = loaded;
                        } else {
                            result = new HashMap<>();
                            result.putAll(fromCache);
                            result.putAll(loaded);
                        }

                        return Flux.merge(putMonos)
                            .then(Mono.just(result));
                    });
            });
    }
}
