package ru.tinkoff.kora.validation.common.annotation;

import ru.tinkoff.kora.common.AopAnnotation;

import java.lang.annotation.*;

/**
 * Indicates that Method Arguments / Method Return Value should be validated
 */
@AopAnnotation
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = {ElementType.METHOD})
public @interface Validate {

}
