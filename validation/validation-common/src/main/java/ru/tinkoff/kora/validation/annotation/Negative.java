package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.NegativeValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ValidatedBy(NegativeValidatorFactory.class)
public @interface Negative {
}
