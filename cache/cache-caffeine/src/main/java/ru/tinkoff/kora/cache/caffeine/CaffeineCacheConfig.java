package ru.tinkoff.kora.cache.caffeine;


import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;

public record CaffeineCacheConfig(@Nullable Duration expireAfterWrite,
                                  @Nullable Duration expireAfterAccess,
                                  @Nullable Long maximumSize,
                                  @Nullable Integer initialSize) {

    private static final long DEFAULT_MAXIMUM_SIZE = 100_000L;

    public CaffeineCacheConfig(@Nullable Duration expireAfterWrite, @Nullable Duration expireAfterAccess, @Nullable Long maximumSize, @Nullable Integer initialSize) {
        this.expireAfterWrite = expireAfterWrite;
        this.expireAfterAccess = expireAfterAccess;
        this.initialSize = initialSize;
        this.maximumSize = (maximumSize == null) ? DEFAULT_MAXIMUM_SIZE : maximumSize;
    }
}
