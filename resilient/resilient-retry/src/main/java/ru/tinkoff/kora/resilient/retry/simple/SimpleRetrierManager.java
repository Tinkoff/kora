package ru.tinkoff.kora.resilient.retry.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.resilient.retry.Retrier;
import ru.tinkoff.kora.resilient.retry.RetrierFailurePredicate;
import ru.tinkoff.kora.resilient.retry.RetrierManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

final class SimpleRetrierManager implements RetrierManager {

    private static final Logger logger = LoggerFactory.getLogger(SimpleRetrierManager.class);

    private final ExecutorService executors;

    private final Map<String, Retrier> retryableByName = new ConcurrentHashMap<>();
    private final SimpleRetrierConfig config;
    private final List<RetrierFailurePredicate> failurePredicates;

    SimpleRetrierManager(ExecutorService executors, SimpleRetrierConfig config, List<RetrierFailurePredicate> failurePredicates) {
        this.executors = executors;
        this.config = config;
        this.failurePredicates = failurePredicates;
    }

    @Nonnull
    @Override
    public Retrier get(@Nonnull String name) {
        return retryableByName.computeIfAbsent(name, (k) -> {
            final SimpleRetrierConfig.NamedConfig config = this.config.getNamedConfig(name);
            final RetrierFailurePredicate failurePredicate = getFailurePredicate(config);
            logger.debug("Creating Repeater named '{}' with config {}", name, config);
            return new SimpleRetrier(config, failurePredicate, executors);
        });
    }

    private RetrierFailurePredicate getFailurePredicate(SimpleRetrierConfig.NamedConfig config) {
        return failurePredicates.stream()
            .filter(p -> p.name().equals(config.failurePredicateName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("FailurePredicateClassName " + config.failurePredicateName() + " is not present as bean, please declare it as bean"));
    }
}
