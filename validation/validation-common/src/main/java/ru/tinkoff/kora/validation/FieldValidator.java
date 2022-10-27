package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Please add Description Here.
 */
public interface FieldValidator<T> extends Validator<T> {

    @NotNull
    List<Violation> validate(@NotNull Field<T> field, @NotNull ValidationOptions options);

    @NotNull
    @Override
    default List<Violation> validate(@Nullable T value, @NotNull ValidationOptions options) {
        return validate(Field.of(value), options);
    }
}
