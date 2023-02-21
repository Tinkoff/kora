package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

import java.lang.annotation.*;

@ExtendWith(KoraJUnit5Extension.class)
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KoraAppTest {

    enum CompilationMode {

        /**
         * {@link KoraAppTest#components()} instances are create one time only
         */
        PER_RUN,

        /**
         * {@link KoraAppTest#components()} instances are recreated each Test Class
         */
        PER_CLASS,

        /**
         * {@link KoraAppTest#components()} instances are recreated each Test Method
         */
        PER_METHOD
    }

    /**
     * @return class loader share mode between different test executions
     */
    CompilationMode compileMode() default CompilationMode.PER_CLASS;

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
     * @return classes that are applicable for Annotation Processing and are {@link ru.tinkoff.kora.common.Component}
     */
    Class<?>[] components();

    /**
     * @return annotation processors used to process {@link #components()}
     * @see KoraAppProcessor is included by default
     */
    Class<? extends AbstractKoraProcessor>[] processors() default {};
}
