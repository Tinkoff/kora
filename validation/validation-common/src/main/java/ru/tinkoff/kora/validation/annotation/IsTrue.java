package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.IsTrueValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ValidatedBy(IsTrueValidatorFactory.class)
public @interface IsTrue {
}
