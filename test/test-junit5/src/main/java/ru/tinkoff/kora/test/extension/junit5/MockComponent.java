package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.Component;

import java.lang.annotation.*;

/**
 * Indicate that {@link Component} from {@link KoraAppTest#components()} will be Mocked and injected
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MockComponent {

}
