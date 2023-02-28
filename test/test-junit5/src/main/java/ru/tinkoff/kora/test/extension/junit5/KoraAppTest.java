package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

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
     * @return Context initialization share mode between different test executions
     */
    InitializeMode initializeMode() default InitializeMode.PER_CLASS;

    /**
     * @return class annotated with {@link KoraApp}
     */
    Class<?> application();

    /**
     * @return application configuration in HOCON format
     */
    @Language("HOCON")
    String config() default "";

    /**
     * @return classes that are {@link Component} and applicable for Annotation Processing and will be included in Context initialization
     */
    Class<?>[] components();

    /**
     * @return classes that should be Mocked
     */
    MockComponent[] mocks() default {};

    /**
     * @return annotation processors used to process {@link #components()}
     * @see KoraAppProcessor is included by default
     */
    Class<? extends AbstractKoraProcessor>[] processors() default {};
}
