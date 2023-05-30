package ru.tinkoff.kora.cache;

import javax.annotation.Nonnull;

/**
 * Analog of Caffeine LoadableCache, require {@link CacheLoader} associated
 */
public interface LoadableCache<K, V> extends ReadableCache<K, V> {

    /**
     * Create default loadable cache implementation
     *
     * @param cache       Cache to store loaded value
     * @param cacheLoader Cache loader
     * @param <K>         Cache key
     * @param <V>         Cache value
     * @return default loadable cache instance
     */
    @Nonnull
    static <K, V> LoadableCache<K, V> create(@Nonnull Cache<K, V> cache, @Nonnull CacheLoader<K, V> cacheLoader) {
        return new LoadableCacheImpl<>(cache, cacheLoader);
    }
}
