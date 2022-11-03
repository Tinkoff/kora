package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

public interface ValidationContext {

    interface Path {

        String value();

        Path root();

        default Path add(@NotNull String field) {
            return new SimpleValidationContext.SimpleFieldPath(this, field);
        }

        default Path add(int index) {
            return new SimpleValidationContext.SimpleIndexPath(this, index);
        }

        static Path of(@NotNull String path) {
            return new SimpleValidationContext.SimpleFieldPath(null, path);
        }

        @NotNull
        default String full() {
            return toString();
        }
    }

    @NotNull
    Path path();

    boolean isFailFast();

    default ValidationContext addPath(@NotNull String path) {
        return new SimpleValidationContext(path().add(path), isFailFast());
    }

    default ValidationContext addPath(int pathIndex) {
        return new SimpleValidationContext(path().add(pathIndex), isFailFast());
    }

    @NotNull
    default Violation violates(@NotNull String message) {
        return new SimpleViolation(message, path());
    }

    static Builder builder() {
        return new SimpleValidationContext.SimpleBuilder(SimpleValidationContext.SimpleFieldPath.ROOT, false);
    }

    interface Builder {

        @NotNull
        Builder path(@NotNull Path path);

        @NotNull
        Builder failFast(boolean isFailFast);

        @NotNull
        ValidationContext build();
    }
}
