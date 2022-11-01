package ru.tinkoff.kora.validation.annotation;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE})
public @interface ValidatedBy {

    Class<? extends ru.tinkoff.kora.validation.ValidatorFactory> value();
}
