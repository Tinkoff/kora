package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

import java.lang.annotation.*;

@ExtendWith(KoraJUnit5Extension.class)
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KoraAppTest {

    enum CompilationShareMode {

        /**
         * {@link KoraAppTest#classes()} instances are create one time only
         */
        PER_RUN,

        /**
         * {@link KoraAppTest#classes()} instances are recreated each Test Class
         */
        PER_CLASS,

        /**
         * {@link KoraAppTest#classes()} instances are recreated each Test Method
         */
        PER_METHOD
    }

    /**
     * @return class loader share mode between different test executions
     */
    CompilationShareMode shareMode() default CompilationShareMode.PER_METHOD;

    /**
     * @return class annotated with {@link ru.tinkoff.kora.common.KoraApp}
     */
    Class<?> application();

    /**
     * @return application configuration in HOCON format
     */
    @Language("HOCON")
    String config() default "";

    /**
     * @return classes that are applicable for Annotation Processing and are parts of {@link ru.tinkoff.kora.common.KoraApp}
     */
    Class<?>[] classes();

    /**
     * @return annotation processors used to process {@link #classes()}
     * @see KoraAppProcessor is included by default
     */
    Class<? extends AbstractKoraProcessor>[] processors() default {};
}
