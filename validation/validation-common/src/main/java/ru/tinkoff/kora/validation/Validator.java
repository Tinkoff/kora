package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Validator<T> {

    @NotNull
    List<Violation> validate(@Nullable T value, @NotNull ValidationContext context);

    @NotNull
    default List<Violation> validate(@Nullable T value) {
        return validate(value, new SimpleValidationContext(SimpleValidationContext.SimpleFieldPath.ROOT, false));
    }

    default void validateAndThrow(@Nullable T value, @NotNull ValidationContext context) throws ViolationException {
        final List<Violation> violations = validate(value, context);
        if (!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }

    default void validateAndThrow(@Nullable T value) throws ViolationException {
        final List<Violation> violations = validate(value);
        if (!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }
}
