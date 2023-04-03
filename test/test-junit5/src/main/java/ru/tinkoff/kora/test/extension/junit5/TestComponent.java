package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.Component;

import java.lang.annotation.*;

/**
 * Indicate {@link Component} is expected to be injected from {@link KoraAppTest} graph
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestComponent {

}
