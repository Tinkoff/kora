package ru.tinkoff.kora.resilient.validation.annotation;

import ru.tinkoff.kora.resilient.validation.NotEmptyConstraint;
import ru.tinkoff.kora.resilient.validation.constraint.NotEmptyStringConstraint;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Constrainted(NotEmptyConstraint.class)
public @interface NotEmpty {
}
