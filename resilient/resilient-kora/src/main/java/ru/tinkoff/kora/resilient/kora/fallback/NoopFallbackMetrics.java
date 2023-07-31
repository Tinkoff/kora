package ru.tinkoff.kora.resilient.kora.fallback;

import javax.annotation.Nonnull;

final class NoopFallbackMetrics implements FallbackMetrics {

    static final NoopFallbackMetrics INSTANCE = new NoopFallbackMetrics();

    @Override
    public void recordExecute(@Nonnull String name, @Nonnull Throwable throwable) {
        // do nothing
    }
}
