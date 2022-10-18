package ru.tinkoff.kora.resilient.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Please add Description Here.
 */
public interface Validator<T> {

    @NotNull
    List<Violation> validate(@Nullable T value);

    default void validateAndThrow(@Nullable T value) throws ViolationException {
        final List<Violation> violations = validate(value);
        if(!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }
}
