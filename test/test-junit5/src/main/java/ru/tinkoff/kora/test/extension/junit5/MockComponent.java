package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.Tag;

import java.lang.annotation.*;

/**
 * Indicate component tha should be Mocked with {@link org.mockito.Mockito}
 */
@Documented
@Target({ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MockComponent {

    /**
     * @return component type to Mock
     */
    Class<?> value();

    /**
     * @return tags {@link Tag}
     */
    Class<?>[] tags() default {};
}
