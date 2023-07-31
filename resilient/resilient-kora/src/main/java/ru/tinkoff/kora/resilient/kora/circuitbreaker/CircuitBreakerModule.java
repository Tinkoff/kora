package ru.tinkoff.kora.resilient.kora.circuitbreaker;

import ru.tinkoff.kora.application.graph.All;
import ru.tinkoff.kora.config.common.Config;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;

import javax.annotation.Nullable;

public interface CircuitBreakerModule {
    default CircuitBreakerConfig koraCircuitBreakerConfig(Config config, ConfigValueExtractor<CircuitBreakerConfig> extractor) {
        var resilient = config.get("resilient");
        return extractor.extract(resilient);
    }

    default CircuitBreakerManager koraCircuitBreakerManager(CircuitBreakerConfig config,
                                                            All<CircuitBreakerPredicate> failurePredicates,
                                                            @Nullable CircuitBreakerMetrics metrics) {
        return new SimpleCircuitBreakerManager(config, failurePredicates,
            (metrics == null)
                ? new NoopCircuitBreakerMetrics()
                : metrics);
    }

    default CircuitBreakerPredicate koraDefaultCircuitBreakerFailurePredicate() {
        return new SimpleCircuitBreakerPredicate();
    }
}
