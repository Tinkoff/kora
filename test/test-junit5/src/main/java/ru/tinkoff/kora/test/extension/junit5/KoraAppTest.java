package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.KoraApp;

import java.lang.annotation.*;

@ExtendWith(KoraJUnit5Extension.class)
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KoraAppTest {

    enum InitializeMode {

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
    Class<?> value();

    /**
     * @return classes that are {@link Component} and will be included in Context initialization or all classes from {@link #value()} if empty
     */
    Class<?>[] components() default {};
}
