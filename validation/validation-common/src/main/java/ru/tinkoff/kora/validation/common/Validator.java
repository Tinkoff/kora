package ru.tinkoff.kora.validation.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Validates instances
 *
 * @param <T> type of instance it supports
 */
public interface Validator<T> {

    /**
     * Validates value and return validation failures if occurred
     *
     * @param value   to validate
     * @param context context of validation and its options {@link ValidationContext}
     * @return validation violations
     */
    @NotNull
    List<Violation> validate(@Nullable T value, @NotNull ValidationContext context);

    /**
     * Validates value and return validation failures if occurred
     *
     * @param value to validate
     * @return validation violations
     */
    @NotNull
    default List<Violation> validate(@Nullable T value) {
        return validate(value, new SimpleValidationContext(SimpleValidationContext.SimpleFieldPath.ROOT, false));
    }

    /**
     * Validates value and return validation failures if occurred
     *
     * @param value   to validate
     * @param context context of validation and its options {@link ValidationContext}
     * @throws ViolationException is thrown if any violations occur
     */
    default void validateAndThrow(@Nullable T value, @NotNull ValidationContext context) throws ViolationException {
        final List<Violation> violations = validate(value, context);
        if (!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }

    /**
     * Validates value and return validation failures if occurred
     *
     * @param value to validate
     * @throws ViolationException is thrown if any violations occur
     */
    default void validateAndThrow(@Nullable T value) throws ViolationException {
        final List<Violation> violations = validate(value);
        if (!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
    }
}
