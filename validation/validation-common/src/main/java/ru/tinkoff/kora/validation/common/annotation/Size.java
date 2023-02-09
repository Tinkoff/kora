package ru.tinkoff.kora.validation.common.annotation;

import org.jetbrains.annotations.Range;
import ru.tinkoff.kora.validation.common.constraint.factory.SizeValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = {ElementType.METHOD,ElementType.FIELD, ElementType.PARAMETER})
@ValidatedBy(SizeValidatorFactory.class)
public @interface Size {

    /**
     * @return minimum value should have (inclusive)
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    int min() default 0;

    /**
     * @return maximum value should have (inclusive)
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    int max();
}
