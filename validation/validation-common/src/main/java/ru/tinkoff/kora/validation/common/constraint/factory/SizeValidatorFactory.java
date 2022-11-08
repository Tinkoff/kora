package ru.tinkoff.kora.validation.common.constraint.factory;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.common.Validator;
import ru.tinkoff.kora.validation.common.ValidatorFactory;

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
