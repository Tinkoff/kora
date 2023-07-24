package ru.tinkoff.kora.resilient.kora;

import ru.tinkoff.kora.common.AopAnnotation;
import ru.tinkoff.kora.resilient.kora.retry.RetryExhaustedException;

import java.lang.annotation.*;

/**
 * Annotation allow applying {@link ru.tinkoff.kora.resilient.kora.retry.Retry} to a specific method
 * When applied to method, method may throw {@link RetryExhaustedException} when all retry attempts are exhausted
 */
@AopAnnotation
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface Retry {

    /**
     * @return the name of the retryable and part of the config path
     */
    String value();
}
