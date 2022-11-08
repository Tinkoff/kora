package ru.tinkoff.kora.validation.common.annotation;

import ru.tinkoff.kora.validation.common.ValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE})
public @interface ValidatedBy {

    Class<? extends ValidatorFactory> value();
}
