package ru.tinkoff.kora.resilient.kora.telemetry;

import javax.annotation.Nonnull;

public interface FallbackMetrics {

    void recordExecute(@Nonnull String name, @Nonnull Throwable throwable);
}
