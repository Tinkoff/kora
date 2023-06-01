package ru.tinkoff.kora.cache.caffeine;

import ru.tinkoff.kora.cache.Cache;

import javax.annotation.Nonnull;
import java.util.function.Function;

public interface CaffeineCache<K, V> extends Cache<K, V> {

    /**
     * @param key             to look for value or compute and put if absent
     * @param mappingFunction to use for value computing
     * @return existing or computed value
     */
    V putIfAbsent(@Nonnull K key, @Nonnull Function<K, V> mappingFunction);
}
