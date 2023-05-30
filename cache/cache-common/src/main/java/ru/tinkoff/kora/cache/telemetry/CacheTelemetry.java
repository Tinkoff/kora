package ru.tinkoff.kora.cache.telemetry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CacheTelemetry {

    record Operation(@Nonnull String name, @Nonnull String cacheName, @Nonnull String origin) {}

    interface TelemetryContext {
        void startRecording();

        void recordSuccess();

        void recordSuccess(@Nullable Object valueFromCache);

        void recordFailure(@Nullable Throwable throwable);
    }

    @Nonnull
    TelemetryContext create(@Nonnull String operationName, @Nonnull String cacheName, @Nonnull String origin);
}
