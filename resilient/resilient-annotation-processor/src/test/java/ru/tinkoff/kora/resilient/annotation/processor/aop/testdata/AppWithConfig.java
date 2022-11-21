package ru.tinkoff.kora.resilient.annotation.processor.aop.testdata;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.resilient.circuitbreaker.fast.CircuitBreakerModule;
import ru.tinkoff.kora.resilient.fallback.simple.FallbackModule;

@KoraApp
public interface AppWithConfig extends CircuitBreakerModule, FallbackModule, ConfigModule {

    @Override
    default Config config() {
        return ConfigFactory.parseString(
            """
                resilient {
                  circuitbreaker {
                    default {
                      slidingWindowSize = 1
                      minimumRequiredCalls = 1
                      failureRateThreshold = 100
                      permittedCallsInHalfOpenState = 1
                      waitDurationInOpenState = 1s
                    }
                  }
                }
                """
        ).resolve();
    }
}
