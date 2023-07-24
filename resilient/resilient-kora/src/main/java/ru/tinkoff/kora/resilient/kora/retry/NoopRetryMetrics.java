package ru.tinkoff.kora.resilient.kora.retry;


import ru.tinkoff.kora.resilient.kora.telemetry.RetryMetrics;

import javax.annotation.Nonnull;

final class NoopRetryMetrics implements RetryMetrics {

    @Override
    public void recordAttempt(@Nonnull String name, long delayInNanos) {
        // do nothing
    }

    @Override
    public void recordExhaustedAttempts(@Nonnull String name, int totalAttempts) {
        // do nothing
    }
}
