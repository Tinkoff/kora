package ru.tinkoff.kora.validation.common.annotation;

import ru.tinkoff.kora.validation.common.constraint.factory.NotBlankValidatorFactory;

import java.lang.annotation.*;

/**
 * The annotated element must not be {@code null} and must contain at least one non-whitespace character
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = {ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@ValidatedBy(NotBlankValidatorFactory.class)
public @interface NotBlank {
}
