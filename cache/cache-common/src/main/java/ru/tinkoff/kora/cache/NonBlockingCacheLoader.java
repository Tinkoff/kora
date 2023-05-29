package ru.tinkoff.kora.cache;

import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

final class NonBlockingCacheLoader<K, V> implements CacheLoader<K, V> {

    private final Function<K, V> loader;
    private final Function<Collection<K>, Map<K, V>> loaderMany;

    NonBlockingCacheLoader(Function<K, V> loader) {
        this.loader = loader;
        this.loaderMany = (keys) -> {
            final Map<K, V> values = new HashMap<>();
            for (K key : keys) {
                values.put(key, loader.apply(key));
            }
            return values;
        };
    }

    NonBlockingCacheLoader(Function<K, V> loader, Function<Collection<K>, Map<K, V>> loaderMany) {
        this.loader = loader;
        this.loaderMany = loaderMany;
    }

    @Nullable
    @Override
    public V load(@Nonnull K key) {
        return loader.apply(key);
    }

    @Nullable
    @Override
    public Map<K, V> load(@Nonnull Collection<K> keys) {
        return loaderMany.apply(keys);
    }

    @Nonnull
    @Override
    public Mono<V> loadAsync(@Nonnull K key) {
        return Mono.fromCallable(() -> loader.apply(key));
    }

    @Override
    public Mono<Map<K, V>> loadAsync(@Nonnull Collection<K> keys) {
        return Mono.fromCallable(() -> loaderMany.apply(keys));
    }
}
