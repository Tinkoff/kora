package ru.tinkoff.kora.resilient.kora.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class SimpleRetryManager implements RetryManager {

    private static final Logger logger = LoggerFactory.getLogger(SimpleRetryManager.class);

    private final Map<String, Retry> retryableByName = new ConcurrentHashMap<>();
    private final List<RetryPredicate> failurePredicates;
    private final RetryConfig config;
    private final RetryMetrics metrics;

    SimpleRetryManager(RetryConfig config, List<RetryPredicate> failurePredicates, RetryMetrics metrics) {
        this.config = config;
        this.failurePredicates = failurePredicates;
        this.metrics = metrics;
    }

    @Nonnull
    @Override
    public Retry get(@Nonnull String name) {
        return retryableByName.computeIfAbsent(name, (k) -> {
            final RetryConfig.NamedConfig config = this.config.getNamedConfig(name);
            final RetryPredicate failurePredicate = getFailurePredicate(config);
            logger.debug("Creating Repeater named '{}' with config {}", name, config);
            return new SimpleRetry(name, config, failurePredicate, metrics);
        });
    }

    private RetryPredicate getFailurePredicate(RetryConfig.NamedConfig config) {
        return failurePredicates.stream()
            .filter(p -> p.name().equals(config.failurePredicateName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("FailurePredicateClassName " + config.failurePredicateName() + " is not present as bean, please declare it as bean"));
    }
}
