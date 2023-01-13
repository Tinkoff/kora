package ru.tinkoff.kora.resilient.annotation.processor.aop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.tinkoff.kora.resilient.annotation.processor.aop.testdata.CircuitBreakerFallbackTarget;
import ru.tinkoff.kora.resilient.annotation.processor.aop.testdata.CircuitBreakerTarget;
import ru.tinkoff.kora.resilient.circuitbreaker.CallNotPermittedException;

import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CircuitBreakerTests extends TestAppRunner {

    @Test
    void syncCircuitBreaker() {
        // given
        var service = getServicesFromGraph(CircuitBreakerTarget.class, CircuitBreakerFallbackTarget.class).first();

        // when
        try {
            service.getValueSync();
            fail("Should not happen");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        // then
        try {
            service.getValueSync();
            fail("Should not happen");
        } catch (CallNotPermittedException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void monoCircuitBreaker() {
        // given
        var service = getServicesFromGraph(CircuitBreakerTarget.class, CircuitBreakerFallbackTarget.class).first();

        // when
        try {
            service.getValueMono().block(Duration.ofSeconds(5));
            fail("Should not happen");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        // then
        try {
            service.getValueMono().block(Duration.ofSeconds(5));
            fail("Should not happen");
        } catch (CallNotPermittedException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void fluxCircuitBreaker() {
        // given
        var service = getServicesFromGraph(CircuitBreakerTarget.class, CircuitBreakerFallbackTarget.class).first();

        // when
        try {
            service.getValueFlux().blockFirst(Duration.ofSeconds(5));
            fail("Should not happen");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        // then
        try {
            service.getValueFlux().blockFirst(Duration.ofSeconds(5));
            fail("Should not happen");
        } catch (CallNotPermittedException ex) {
            assertNotNull(ex.getMessage());
        }
    }
}
