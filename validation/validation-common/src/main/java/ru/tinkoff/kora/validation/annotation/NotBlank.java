package ru.tinkoff.kora.validation.annotation;

import ru.tinkoff.kora.validation.constraint.factory.NotBlankValidatorFactory;

import java.lang.annotation.*;

/**
 * The annotated element must not be {@code null} and must contain at least one non-whitespace character
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
@ValidatedBy(NotBlankValidatorFactory.class)
public @interface NotBlank {
}
