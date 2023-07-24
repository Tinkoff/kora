package ru.tinkoff.kora.resilient.kora.fallback;

import ru.tinkoff.kora.resilient.kora.telemetry.FallbackMetrics;

import javax.annotation.Nonnull;

final class NoopFallbackMetrics implements FallbackMetrics {

    static final NoopFallbackMetrics INSTANCE = new NoopFallbackMetrics();

    @Override
    public void recordExecute(@Nonnull String name, @Nonnull Throwable throwable) {
        // do nothing
    }
}
