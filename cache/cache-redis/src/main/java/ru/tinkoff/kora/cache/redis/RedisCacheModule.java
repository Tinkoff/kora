package ru.tinkoff.kora.cache.redis;

import ru.tinkoff.kora.application.graph.TypeRef;
import ru.tinkoff.kora.cache.CacheKey;
import ru.tinkoff.kora.cache.redis.client.LettuceModule;
import ru.tinkoff.kora.cache.telemetry.CacheMetrics;
import ru.tinkoff.kora.cache.telemetry.CacheTelemetry;
import ru.tinkoff.kora.cache.telemetry.CacheTracer;
import ru.tinkoff.kora.cache.telemetry.DefaultCacheTelemetry;
import ru.tinkoff.kora.common.DefaultComponent;
import ru.tinkoff.kora.json.common.JsonCommonModule;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.tinkoff.kora.json.common.JsonWriter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public interface RedisCacheModule extends JsonCommonModule, LettuceModule {

    @DefaultComponent
    default CacheTelemetry defaultCacheTelemetry(@Nullable CacheMetrics metrics, @Nullable CacheTracer tracer) {
        return new DefaultCacheTelemetry(metrics, tracer);
    }

    @DefaultComponent
    default <V> RedisCacheValueMapper<V> redisValueMapper(JsonWriter<V> jsonWriter, JsonReader<V> jsonReader, TypeRef<V> valueRef) {
        return new RedisCacheValueMapper<>() {
            @Override
            public byte[] write(V value) {
                try {
                    return jsonWriter.toByteArray(value);
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }

            @Override
            public V read(byte[] serializedValue) {
                try {
                    return (serializedValue == null) ? null : jsonReader.read(serializedValue);
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        };
    }

    @DefaultComponent
    default <T extends CacheKey> RedisCacheKeyMapper<T> redisKeyMapper(TypeRef<T> keyRef) {
        return c -> c.values().stream().map(String::valueOf).collect(Collectors.joining("-")).getBytes(StandardCharsets.UTF_8);
    }
}
