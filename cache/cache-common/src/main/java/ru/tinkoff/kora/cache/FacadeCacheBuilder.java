package ru.tinkoff.kora.cache;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

final class FacadeCacheBuilder<K, V> implements CacheBuilder<K, V> {

    private final List<Cache<K, V>> facades = new ArrayList<>();

    FacadeCacheBuilder(@Nonnull Cache<K, V> cache) {
        facades.add(cache);
    }

    @Nonnull
    @Override
    public CacheBuilder<K, V> addCache(@Nonnull Cache<K, V> cache) {
        facades.add(cache);
        return this;
    }

    @Nonnull
    @Override
    public Cache<K, V> build() {
        if (facades.isEmpty()) {
            throw new IllegalArgumentException("Facades can't be empty for Facade Cache Builder!");
        }

        if (facades.size() == 1) {
            return facades.get(0);
        }

        return new FacadeSyncCache<>(facades);
    }

    private record FacadeSyncCache<K, V>(List<Cache<K, V>> facades) implements Cache<K, V> {

        @Nullable
        @Override
        public V get(@Nonnull K key) {
            for (var facade : facades) {
                final V v = facade.get(key);
                if (v != null) {
                    return v;
                }
            }

            return null;
        }

        @Nonnull
        @Override
        public Map<K, V> get(@Nonnull Collection<K> keys) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public V put(@Nonnull K key, @Nonnull V value) {
            for (var facade : facades) {
                facade.put(key, value);
            }

            return value;
        }

        @Override
        public void invalidate(@Nonnull K key) {
            for (var facade : facades) {
                facade.invalidate(key);
            }
        }

        @Override
        public void invalidate(@Nonnull Collection<K> keys) {
            for (var facade : facades) {
                facade.invalidate(keys);
            }
        }

        @Override
        public void invalidateAll() {
            for (var facade : facades) {
                facade.invalidateAll();
            }
        }

        @Nonnull
        @Override
        public Mono<V> getAsync(@Nonnull K key) {
            Mono<V> result = null;
            for (var facade : facades) {
                result = (result == null)
                    ? facade.getAsync(key)
                    : result.switchIfEmpty(facade.getAsync(key));
            }

            return result;
        }

        @Nonnull
        @Override
        public Mono<Map<K, V>> getAsync(@Nonnull Collection<K> keys) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Mono<V> putAsync(@Nonnull K key, @Nonnull V value) {
            final List<Mono<V>> operations = facades.stream()
                .map(cache -> cache.putAsync(key, value))
                .toList();

            return Flux.merge(operations).then(Mono.just(value));
        }

        @Nonnull
        @Override
        public Mono<Boolean> invalidateAsync(@Nonnull K key) {
            final List<Mono<Boolean>> operations = facades.stream()
                .map(cache -> cache.invalidateAsync(key))
                .toList();

            return Flux.merge(operations).reduce((v1, v2) -> v1 && v2);
        }

        @Override
        public Mono<Boolean> invalidateAsync(@Nonnull Collection<K> keys) {
            final List<Mono<Boolean>> operations = facades.stream()
                .map(cache -> cache.invalidateAsync(keys))
                .toList();

            return Flux.merge(operations).reduce((v1, v2) -> v1 && v2);
        }

        @Nonnull
        @Override
        public Mono<Boolean> invalidateAllAsync() {
            final List<Mono<Boolean>> operations = facades.stream()
                .map(Cache::invalidateAllAsync)
                .toList();

            return Flux.merge(operations).reduce((v1, v2) -> v1 && v2);
        }
    }
}
