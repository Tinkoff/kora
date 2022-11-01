package ru.tinkoff.kora.validation.constraint.factory;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.ValidatorFactory;
import ru.tinkoff.kora.validation.annotation.Range;

public interface RangeValidatorFactory<T> extends ValidatorFactory<T> {

    @NotNull
    @Override
    default Validator<T> create() {
        return create(Long.MIN_VALUE, Long.MAX_VALUE, Range.Boundary.INCLUSIVE_INCLUSIVE);
    }

    @NotNull
    default Validator<T> create(double from, double to) {
        return create(from, to, Range.Boundary.INCLUSIVE_INCLUSIVE);
    }

    @NotNull
    Validator<T> create(double from, double to, Range.Boundary boundary);
}
