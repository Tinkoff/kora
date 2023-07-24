package ru.tinkoff.kora.resilient.kora;

import ru.tinkoff.kora.resilient.kora.circuitbreaker.CircuitBreakerModule;
import ru.tinkoff.kora.resilient.kora.fallback.FallbackModule;
import ru.tinkoff.kora.resilient.kora.retry.RetryModule;
import ru.tinkoff.kora.resilient.kora.timeout.TimeoutModule;

public interface ResilientModule extends CircuitBreakerModule, RetryModule, TimeoutModule, FallbackModule {

}
