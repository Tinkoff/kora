package ru.tinkoff.kora.resilient.annotation.processor.aop.testdata;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.resilient.timeout.annotation.Timeout;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class TimeoutTarget implements MockLifecycle {

    @Timeout("custom1")
    public String getValueSync() {
        try {
            Thread.sleep(2000);
            return "OK";
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Timeout("custom2")
    public Mono<String> getValueMono() {
        return Mono.fromCallable(() -> "OK")
            .delayElement(Duration.ofSeconds(2));
    }

    @Timeout("custom3")
    public Flux<String> getValueFlux() {
        return Flux.from(Mono.fromCallable(() -> "OK"))
            .delayElements(Duration.ofSeconds(2))
            .onErrorMap(e -> e.getCause() instanceof TimeoutException, e -> new ru.tinkoff.kora.resilient.timeout.TimeoutException(e.getCause().getMessage()));
    }
}
