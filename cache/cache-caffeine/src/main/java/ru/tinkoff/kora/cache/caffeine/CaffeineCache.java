package ru.tinkoff.kora.cache.caffeine;

import ru.tinkoff.kora.cache.Cache;

import javax.annotation.Nonnull;
import java.util.function.Function;

public interface CaffeineCache<K, V> extends Cache<K, V> {

    V getOrCompute(@Nonnull K key, @Nonnull Function<K, V> mappingFunction);
}
