package ru.tinkoff.kora.validation;

import java.util.List;

/**
 * Please add Description Here.
 */
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

    default ValidationContext addPath(String path) {
        return new SimpleValidationContext(new SimpleValidationContext.SimplePath(path(), path), isFailFast());
    }

    default Violation erase(String message) {
        return new SimpleViolation(message, path());
    }

    default List<Violation> eraseAsList(String message) {
        return List.of(new SimpleViolation(message, path()));
    }
}
