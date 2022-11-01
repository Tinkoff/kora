package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

public interface ValidationContext {

    interface Path {

        String value();

        Path root();

        default String full() {
            return toString();
        }
    }

    Path path();

    boolean isFailFast();

    default ValidationContext addPath(@NotNull String path) {
        return new SimpleValidationContext(new SimpleValidationContext.SimpleFieldPath(path(), path), isFailFast());
    }

    default ValidationContext addPath(int pathIndex) {
        return new SimpleValidationContext(new SimpleValidationContext.SimpleIndexPath(path(), pathIndex), isFailFast());
    }

    default Violation violates(@NotNull String message) {
        return new SimpleViolation(message, path());
    }
}
