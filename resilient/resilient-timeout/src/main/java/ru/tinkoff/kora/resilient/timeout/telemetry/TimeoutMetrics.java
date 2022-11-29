package ru.tinkoff.kora.resilient.timeout.telemetry;

public interface TimeoutMetrics {

    void recordTimeout(String name);
}
