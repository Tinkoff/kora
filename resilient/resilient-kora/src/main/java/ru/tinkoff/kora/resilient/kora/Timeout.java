package ru.tinkoff.kora.resilient.kora;

import ru.tinkoff.kora.common.AopAnnotation;
import ru.tinkoff.kora.resilient.kora.timeout.TimeoutExhaustedException;

import java.lang.annotation.*;

/**
 * Annotation allow applying {@link ru.tinkoff.kora.resilient.kora.timeout.Timeout} to a specific method
 * When applied to method, method may throw {@link TimeoutExhaustedException} when all timeout occured
 */
@AopAnnotation
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface Timeout {

    /**
     * @return the name of the Timeout config path
     */
    String value();
}
