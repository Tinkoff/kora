package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Please add Description Here.
 */
public interface FieldValidator<T> extends Validator<T> {

    @NotNull
    List<Violation> validate(Field<T> field, Options options);

    @NotNull
    @Override
    default List<Violation> validate(@Nullable T value) {
        return validate(Field.of(value), new SimpleOptions(false));
    }
}
