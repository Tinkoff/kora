package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Please add Description Here.
 */
public interface Validator<T> {

    @NotNull
    List<Violation> validate(@Nullable T value, @NotNull ValidationOptions options);

    @NotNull
    default List<Violation> validate(@Nullable T value) {
        return validate(value, new SimpleValidationOptions(false));
    }

    default void validateAndThrow(@Nullable T value, @NotNull ValidationOptions options) throws ViolationException {
        final List<Violation> violations = validate(value, options);
        if(!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }

    default void validateAndThrow(@Nullable T value) throws ViolationException {
        final List<Violation> violations = validate(value);
        if(!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }
}
