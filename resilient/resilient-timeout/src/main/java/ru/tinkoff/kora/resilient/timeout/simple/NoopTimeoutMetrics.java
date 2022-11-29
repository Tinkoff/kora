package ru.tinkoff.kora.resilient.timeout.simple;

import ru.tinkoff.kora.resilient.timeout.telemetry.TimeoutMetrics;

final class NoopTimeoutMetrics implements TimeoutMetrics {

    static final TimeoutMetrics INSTANCE = new NoopTimeoutMetrics();

    @Override
    public void recordTimeout(String name) {
        // do nothing
    }
}
