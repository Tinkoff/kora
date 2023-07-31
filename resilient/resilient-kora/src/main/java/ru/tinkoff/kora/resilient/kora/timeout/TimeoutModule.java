package ru.tinkoff.kora.resilient.kora.timeout;

import ru.tinkoff.kora.common.DefaultComponent;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.config.common.Config;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;
import ru.tinkoff.kora.resilient.kora.Timeout;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface TimeoutModule {

    default TimeoutConfig koraTimeoutConfig(Config config, ConfigValueExtractor<TimeoutConfig> extractor) {
        var value = config.get("resilient");
        return extractor.extract(value);
    }

    default TimeoutManager koraTimeoutManager(@Tag(Timeout.class) ExecutorService executorService,
                                              TimeoutConfig config,
                                              @Nullable TimeoutMetrics metrics) {
        return new SimpleTimeoutManager(metrics == null
            ? new NoopTimeoutMetrics()
            : metrics,
            executorService, config);
    }

    @DefaultComponent
    @Tag(Timeout.class)
    default ExecutorService koraTimeoutExecutorService() {
        return Executors.newCachedThreadPool();
    }
}
