package ru.tinkoff.kora.resilient.kora;

import ru.tinkoff.kora.common.AopAnnotation;
import ru.tinkoff.kora.resilient.kora.circuitbreaker.CircuitBreakerNotPermittedException;

import java.lang.annotation.*;

/**
 * Annotation allow applying {@link ru.tinkoff.kora.resilient.kora.circuitbreaker.CircuitBreaker} to a specific method
 * When applied to method, method may throw {@link CircuitBreakerNotPermittedException} when all CircuitBreaker in OPEN state
 */
@AopAnnotation
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface CircuitBreaker {

    /**
     * @return the name of CircuitBreaker config path
     */
    String value();
}
