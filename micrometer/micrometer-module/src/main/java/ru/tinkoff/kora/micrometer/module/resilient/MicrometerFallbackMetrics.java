package ru.tinkoff.kora.micrometer.module.resilient;

import ru.tinkoff.kora.resilient.fallback.telemetry.FallbackMetrics;

import javax.annotation.Nonnull;

final class MicrometerFallbackMetrics implements FallbackMetrics {

    @Override
    public void recordExecute(@Nonnull String name, @Nonnull Throwable throwable) {
        // do nothing
    }

    @Override
    public void recordSkip(@Nonnull String name, @Nonnull Throwable throwable) {
        // do nothing
    }
}
