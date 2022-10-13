package ru.tinkoff.kora.resilient.validation.annotation;

import ru.tinkoff.kora.resilient.validation.constraint.NotNullConstraint;

import java.lang.annotation.*;

/**
 * Please add Description Here.
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Constrainted(NotNullConstraint.class)
public @interface NotNull {
}
