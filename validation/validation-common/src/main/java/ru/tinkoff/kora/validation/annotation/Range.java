package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.NotEmptyConstraintFactory;

import java.lang.annotation.*;

//TODO CHECK HOW TO WORK WITH IT


@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Constrainted(NotEmptyConstraintFactory.class)
public @interface Range {

    long from();

    long to();
}
