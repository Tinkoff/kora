package ru.tinkoff.kora.cache;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

final class AsyncCacheLoader<K, V> implements CacheLoader<K, V> {

    record Pair<K, V>(K key, V value) {}

    private final Function<K, Mono<V>> loader;
    private final Function<Collection<K>, Mono<Map<K, V>>> loaderMany;

    AsyncCacheLoader(Function<K, Mono<V>> loader) {
        this.loader = loader;
        this.loaderMany = (keys) -> {
            var valuesMonos = keys.stream()
                .map(key -> loader.apply(key)
                    .map(v -> new Pair<>(key, v)))
                .toList();

            return Flux.merge(valuesMonos)
                .collect(() -> new HashMap<K, V>(), (collector, value) -> collector.put(value.key(), value.value()));
        };
    }

    AsyncCacheLoader(Function<K, Mono<V>> loader, Function<Collection<K>, Mono<Map<K, V>>> loaderMany) {
        this.loader = loader;
        this.loaderMany = loaderMany;
    }

    @Nullable
    @Override
    public V load(@Nonnull K key) {
        return loader.apply(key).block();
    }

    @Nullable
    @Override
    public Map<K, V> load(@Nonnull Collection<K> keys) {
        return loaderMany.apply(keys).block();
    }

    @Nonnull
    @Override
    public Mono<V> loadAsync(@Nonnull K key) {
        return loader.apply(key);
    }

    @Override
    public Mono<Map<K, V>> loadAsync(@Nonnull Collection<K> keys) {
        return loaderMany.apply(keys);
    }
}
