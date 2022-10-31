package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.PositiveValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ValidatedBy(PositiveValidatorFactory.class)
public @interface Positive {
}
