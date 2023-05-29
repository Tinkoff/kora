package ru.tinkoff.kora.cache.redis;


import javax.annotation.Nullable;
import java.time.Duration;

public record RedisCacheConfig(@Nullable Duration expireAfterWrite,
                               @Nullable Duration expireAfterAccess) {

}
