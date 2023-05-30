package ru.tinkoff.kora.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;

import javax.annotation.Nonnull;

public interface CaffeineCacheFactory {

    @Nonnull
    <K, V> Cache<K, V> build(@Nonnull CaffeineCacheConfig config);
}
