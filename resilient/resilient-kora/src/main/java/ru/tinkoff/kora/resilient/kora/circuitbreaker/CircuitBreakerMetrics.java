package ru.tinkoff.kora.resilient.kora.circuitbreaker;

import ru.tinkoff.kora.resilient.kora.circuitbreaker.CircuitBreaker;

import javax.annotation.Nonnull;

/**
 * Records circuit breaker metrics
 */
public interface CircuitBreakerMetrics {

    void recordState(@Nonnull String name, @Nonnull CircuitBreaker.State state);
}
