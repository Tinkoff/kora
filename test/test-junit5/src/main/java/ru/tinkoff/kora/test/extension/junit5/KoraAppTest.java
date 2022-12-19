package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.application.graph.Lifecycle;

import java.lang.annotation.*;

@ExtendWith(KoraJUnit5Extension.class)
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KoraAppTest {

    enum CompilationShareMode {
        PER_RUN,
        PER_CLASS,
        PER_METHOD
    }

    /**
     * @return class loader share mode between different test executions
     */
    CompilationShareMode shareMode() default CompilationShareMode.PER_CLASS;

    /**
     * @return class annotated with {@link ru.tinkoff.kora.common.KoraApp}
     */
    Class<?> application();

    @Language("HOCON")
    String configuration() default "";

    Class<? extends Lifecycle>[] classes();

    Class<? extends AbstractKoraProcessor>[] processors() default {};
}
