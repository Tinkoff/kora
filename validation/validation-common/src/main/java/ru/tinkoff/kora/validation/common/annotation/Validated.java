package ru.tinkoff.kora.validation.common.annotation;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.FIELD})
public @interface Validated {

}
