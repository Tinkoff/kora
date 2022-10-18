package ru.tinkoff.kora.resilient.validation.annotation;

import ru.tinkoff.kora.resilient.validation.NotEmptyConstraint;

import java.lang.annotation.*;

//TODO CHECK HOW TO WORK WITH IT


@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Constrainted(NotEmptyConstraint.class)
public @interface Range {

    int from();

    int to();
}
