package ru.tinkoff.kora.validation.constraint.factory;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.ValidatorFactory;

public interface SizeValidatorFactory<T> extends ValidatorFactory<T> {

    @NotNull
    @Override
    default Validator<T> create() {
        return create(0, Integer.MAX_VALUE);
    }

    @NotNull
    default Validator<T> create(int to) {
        return create(0, to);
    }

    @NotNull
    Validator<T> create(int from, int to);
}
