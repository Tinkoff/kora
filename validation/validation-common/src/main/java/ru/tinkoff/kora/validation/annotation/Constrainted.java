package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.ConstraintFactory;

import java.lang.annotation.*;

/**
 * Please add Description Here.
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE})
public @interface Constrainted {

    Class<? extends ConstraintFactory> value();
}
