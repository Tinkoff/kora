package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.NegativeOrZeroValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ValidatedBy(NegativeOrZeroValidatorFactory.class)
public @interface NegativeOrZero {
}
