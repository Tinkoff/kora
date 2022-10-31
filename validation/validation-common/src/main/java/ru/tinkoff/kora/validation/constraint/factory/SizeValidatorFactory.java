package ru.tinkoff.kora.validation.constraint.factory;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.ValidatorFactory;

/**
 * Please add Description Here.
 */
public interface SizeValidatorFactory<T> extends ValidatorFactory<T> {

    @NotNull
    @Override
    default Validator<T> create() {
        return create(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @NotNull
    Validator<T> create(long from, long to);
}
