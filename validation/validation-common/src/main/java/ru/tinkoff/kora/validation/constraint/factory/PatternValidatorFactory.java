package ru.tinkoff.kora.validation.constraint.factory;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.ValidatorFactory;

import java.util.regex.Pattern;

/**
 * Please add Description Here.
 */
public interface PatternValidatorFactory<T> extends ValidatorFactory<T> {

    @NotNull
    @Override
    default Validator<T> create() {
        throw new UnsupportedOperationException("Doesn't support creation without Pattern!");
    }

    @NotNull
    Validator<T> create(String pattern, int flags);
}
