package ru.tinkoff.kora.resilient.validation.annotation;

import ru.tinkoff.kora.resilient.validation.LengthConstraintFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Constrainted(LengthConstraintFactory.class)
public @interface HasLength {
}

