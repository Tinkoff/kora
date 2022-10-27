package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.NotEmptyConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.RangeConstraintFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Constrainted(RangeConstraintFactory.class)
public @interface Range {

    long from();

    long to();
}
