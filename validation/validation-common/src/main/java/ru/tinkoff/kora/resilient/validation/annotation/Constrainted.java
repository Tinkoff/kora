package ru.tinkoff.kora.resilient.validation.annotation;

import ru.tinkoff.kora.resilient.validation.Constraint;

import java.lang.annotation.*;

/**
 * Please add Description Here.
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE})
public @interface Constrainted {

    Class<? extends Constraint<?>> value();
}
