package ru.tinkoff.kora.cache.testcache;

import reactor.core.publisher.Mono;
import ru.tinkoff.kora.cache.Cache;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DummyCache implements Cache<String, String> {

    private final Map<String, String> cache = new HashMap<>();

    public DummyCache(String name) {

    }

    @Override
    public String get(@Nonnull String key) {
        return cache.get(key);
    }

    @Nonnull
    @Override
    public String put(@Nonnull String key, @Nonnull String value) {
        cache.put(key, value);
        return value;
    }

    @Override
    public void invalidate(@Nonnull String key) {
        cache.remove(key);
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }

    @Nonnull
    @Override
    public Mono<String> getAsync(@Nonnull String key) {
        return Mono.justOrEmpty(get(key));
    }

    @Nonnull
    @Override
    public Mono<String> putAsync(@Nonnull String key, @Nonnull String value) {
        put(key, value);
        return Mono.just(value);
    }

    @Nonnull
    @Override
    public Mono<Boolean> invalidateAsync(@Nonnull String key) {
        invalidate(key);
        return Mono.just(true);
    }

    @Nonnull
    @Override
    public Mono<Boolean> invalidateAllAsync() {
        invalidateAll();
        return Mono.just(true);
    }

    @Override
    public void invalidate(@Nonnull Collection<String> keys) {
        for (String key : keys) {
            invalidate(key);
        }
    }

    @Override
    public Mono<Boolean> invalidateAsync(@Nonnull Collection<String> keys) {
        for (String key : keys) {
            invalidate(key);
        }
        return Mono.just(true);
    }

    @Nonnull
    @Override
    public Map<String, String> get(@Nonnull Collection<String> keys) {
        return keys.stream()
            .collect(Collectors.toMap(k -> k, cache::get));
    }

    @Nonnull
    @Override
    public Mono<Map<String, String>> getAsync(@Nonnull Collection<String> keys) {
        return Mono.just(get(keys));
    }
}
