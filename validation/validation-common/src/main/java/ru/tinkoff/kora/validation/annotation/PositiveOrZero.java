package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.PositiveOrZeroValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ValidatedBy(PositiveOrZeroValidatorFactory.class)
public @interface PositiveOrZero {
}
