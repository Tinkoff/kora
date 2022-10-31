package ru.tinkoff.kora.validation.annotation;

import org.intellij.lang.annotations.Language;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.PatternValidatorFactory;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ValidatedBy(PatternValidatorFactory.class)
public @interface Pattern {

    /**
     * @return RegEx pattern
     */
    @Language("RegExp")
    String pattern();

    /**
     * @return {@link java.util.regex.Pattern#flags()} for RegEx
     */
    int flags() default 0;
}
