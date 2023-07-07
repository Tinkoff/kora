package ru.tinkoff.kora.test.container.postgres;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerPostgres {
}
