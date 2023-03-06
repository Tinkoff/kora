package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.common.Tag;
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
     * @return Context Initialization mode between different test executions
     */
    InitializeMode initializeMode() default InitializeMode.PER_METHOD;

    /**
     * @return class annotated with {@link KoraApp}
     */
    Class<?> application();

    /**
     * Order configs are merged:
     * 1) {@link KoraConfigModification}
     * 2) {@link KoraAppTest#configFiles()}
     * 3) {@link KoraAppTest#config()}
     *
     * @return application configuration files relative to resources directory
     */
    String[] configFiles() default {};

    /**
     * Order configs are merged:
     * 1) {@link KoraConfigModification}
     * 2) {@link KoraAppTest#configFiles()}
     * 3) {@link KoraAppTest#config()}
     *
     * @return application configuration in HOCON format
     */
    @Language("HOCON")
    String config() default "";

    /**
     * @return classes that are {@link Component} and applicable for Annotation Processing and will be included in Context initialization
     */
    Class<?>[] components();

    /**
     * @return classes that should be Mocked, types are matched with {@link Tag.Any} tag
     */
    Class<?>[] mocks() default {};

    /**
     * @return annotation processors used to process {@link #components()}
     * @see KoraAppProcessor is included by default
     */
    Class<? extends AbstractKoraProcessor>[] processors() default {};
}
