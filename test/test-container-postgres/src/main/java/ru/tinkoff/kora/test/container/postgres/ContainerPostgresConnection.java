package ru.tinkoff.kora.test.container.postgres;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerPostgresConnection {
}
