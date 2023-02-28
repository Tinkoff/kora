package ru.tinkoff.kora.test.extension.junit5;

import java.lang.annotation.*;

/**
 * Indicate component is expected to be injected from {@link KoraAppTest} graph
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestComponent {

}
