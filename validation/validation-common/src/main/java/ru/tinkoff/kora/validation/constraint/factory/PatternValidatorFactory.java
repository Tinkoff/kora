package ru.tinkoff.kora.validation.constraint.factory;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.ValidatorFactory;

public interface PatternValidatorFactory<T> extends ValidatorFactory<T> {

    @NotNull
    @Override
    default Validator<T> create() {
        throw new UnsupportedOperationException("Doesn't support creation without Pattern!");
    }

    @NotNull
    default Validator<T> create(String pattern) {
        return create(pattern, 0);
    }

    @NotNull
    Validator<T> create(String pattern, int flags);
}
