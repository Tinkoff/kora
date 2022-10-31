package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.IsFalseValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ValidatedBy(IsFalseValidatorFactory.class)
public @interface IsFalse {
}
