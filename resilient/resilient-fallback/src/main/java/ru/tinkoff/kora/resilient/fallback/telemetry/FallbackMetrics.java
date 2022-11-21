package ru.tinkoff.kora.resilient.fallback.telemetry;

import javax.annotation.Nonnull;

public interface FallbackMetrics {

    void recordFallback(@Nonnull String name, @Nonnull Throwable throwable);

    void recordSkip(@Nonnull String name, @Nonnull Throwable throwable);
}
