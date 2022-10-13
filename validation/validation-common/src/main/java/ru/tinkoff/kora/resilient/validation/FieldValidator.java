package ru.tinkoff.kora.resilient.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Please add Description Here.
 */
public interface FieldValidator<T> extends Validator<T> {

    @NotNull
    List<Violation> validate(Field<T> field);

    @NotNull
    @Override
    default List<Violation> validate(@Nullable T value) {
        return validate(Field.of(value));
    }
}
