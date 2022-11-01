package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Validator<T> {

    @NotNull
    List<Violation> validate(T value, @NotNull ValidationContext context);

    @NotNull
    default List<Violation> validate(T value) {
        return validate(value, new SimpleValidationContext(SimpleValidationContext.SimpleFieldPath.EMPTY, false));
    }

    default void validateAndThrow(T value, @NotNull ValidationContext context) throws ViolationException {
        final List<Violation> violations = validate(value, context);
        if (!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }

    default void validateAndThrow(T value) throws ViolationException {
        final List<Violation> violations = validate(value);
        if (!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }
}
