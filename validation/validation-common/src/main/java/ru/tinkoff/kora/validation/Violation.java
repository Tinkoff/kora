package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

/**
 * Please add Description Here.
 */
public interface Violation {

    @NotNull
    String message();

    String path();

    static Violation of(String message) {
        return new SimpleViolation(message, null);
    }

    static Violation of(String message, String path) {
        return new SimpleViolation(message, path);
    }
}
