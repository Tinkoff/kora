package ru.tinkoff.kora.resilient.retry.simple;

import com.typesafe.config.Config;
import ru.tinkoff.kora.application.graph.All;
import ru.tinkoff.kora.common.DefaultComponent;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;
import ru.tinkoff.kora.resilient.retry.RetrierFailurePredicate;
import ru.tinkoff.kora.resilient.retry.RetrierManager;
import ru.tinkoff.kora.resilient.retry.annotation.Retryable;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface RetryableModule {

    default SimpleRetrierConfig simpleRetryableConfig(Config config, ConfigValueExtractor<SimpleRetrierConfig> extractor) {
        return !config.hasPath("resilient")
            ? new SimpleRetrierConfig(Map.of())
            : extractor.extract(config.getValue("resilient"));
    }

    default RetrierManager simpleRetryableManager(SimpleRetrierConfig config,
                                                  All<RetrierFailurePredicate> failurePredicates,
                                                  @Tag(Retryable.class) ExecutorService executorService) {
        return new SimpleRetrierManager(executorService, config, failurePredicates);
    }

    @DefaultComponent
    @Tag(Retryable.class)
    default ExecutorService simpleRetryableExecutorService() {
        return Executors.newCachedThreadPool();
    }

    default RetrierFailurePredicate simpleRetrierFailurePredicate() {
        return new SimpleRetrierFailurePredicate();
    }
}
