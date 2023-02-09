package ru.tinkoff.kora.validation.common.annotation;

import ru.tinkoff.kora.common.AopAnnotation;
import ru.tinkoff.kora.validation.common.Validator;

import java.lang.annotation.*;

/**
 * Indicates that Method Return value should be validated
 */
@AopAnnotation
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = {ElementType.METHOD})
public @interface ValidateOutput {

}
