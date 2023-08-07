package ru.tinkoff.kora.resilient.annotation.processor.aop.testdata;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.annotation.Root;
import ru.tinkoff.kora.resilient.kora.CircuitBreaker;

import java.io.IOException;

@Component
@Root
public class CircuitBreakerTarget {

    public boolean alwaysFail = true;

    @CircuitBreaker("custom1")
    public String getValueSync() {
        if (alwaysFail)
            throw new IllegalStateException("Failed");

        return "OK";
    }

    @CircuitBreaker("custom1")
    public void getValueSyncVoid() {
        if (alwaysFail)
            throw new IllegalStateException("Failed");
    }

    @CircuitBreaker("custom1")
    public void getValueSyncVoidCheckedException() throws IOException {
        if (alwaysFail)
            throw new IllegalStateException("Failed");
    }

    @CircuitBreaker("custom1")
    public String getValueSyncCheckedException() throws IOException {
        if (alwaysFail)
            throw new IllegalStateException("Failed");

        return "OK";
    }

    @CircuitBreaker("custom2")
    public Mono<String> getValueMono() {
        if (alwaysFail)
            return Mono.error(new IllegalStateException("Failed"));

        return Mono.just("OK");
    }

    @CircuitBreaker("custom3")
    public Flux<String> getValueFlux() {
        if (alwaysFail)
            return Flux.error(new IllegalStateException("Failed"));

        return Flux.just("OK");
    }
}
