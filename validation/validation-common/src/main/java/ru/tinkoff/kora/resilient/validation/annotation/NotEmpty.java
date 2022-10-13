package ru.tinkoff.kora.resilient.validation.annotation;

import ru.tinkoff.kora.resilient.validation.constraint.NotEmptyConstraint;

import java.lang.annotation.*;

/**
 * Please add Description Here.
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Constrainted(NotEmptyConstraint.class)
public @interface NotEmpty {
}
