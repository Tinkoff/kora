package ru.tinkoff.kora.resilient.kora.timeout;

import ru.tinkoff.kora.resilient.kora.telemetry.TimeoutMetrics;

import javax.annotation.Nonnull;

final class NoopTimeoutMetrics implements TimeoutMetrics {

    @Override
    public void recordTimeout(@Nonnull String name, long timeoutInNanos) {
        // do nothing
    }
}
