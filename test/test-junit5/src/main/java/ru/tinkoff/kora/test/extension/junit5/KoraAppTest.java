package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.common.Tag;

import java.lang.annotation.*;

@ExtendWith(KoraJUnit5Extension.class)
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KoraAppTest {

    enum InitializeMode {

        /**
         * {@link KoraAppTest#components()} instances are initialized one time only
         */
        PER_RUN,

        /**
         * {@link KoraAppTest#components()} instances are initialized each Test Class
         */
        PER_CLASS,

        /**
         * {@link KoraAppTest#components()} instances are initialized each Test Method
         */
        PER_METHOD
    }

    /**
     * @return Context Initialization mode between different test executions
     */
    InitializeMode initializeMode() default InitializeMode.PER_METHOD;

    /**
     * @return class annotated with {@link KoraApp}
     */
    Class<?> application();

    /**
     * @return classes that are {@link Component} and applicable for Annotation Processing and will be included in Context initialization
     */
    Class<?>[] components() default {};

    /**
     * @return classes that should be Mocked, types are matched with {@link Tag.Any} tag
     */
    Class<?>[] mocks() default {};
}
