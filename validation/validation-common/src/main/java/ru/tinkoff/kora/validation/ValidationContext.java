package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;

/**
 *
 */
@Immutable
public interface ValidationContext {

    /**
     * @return path where violation occured
     */
    @NotNull
    Path path();

    /**
     * @return {@link Boolean#TRUE} when should fail on first occurred violation
     */
    boolean isFailFast();

    @NotNull
    default ValidationContext addPath(@NotNull String path) {
        return new SimpleValidationContext(path().add(path), isFailFast());
    }

    @NotNull
    default ValidationContext addPath(int pathIndex) {
        return new SimpleValidationContext(path().add(pathIndex), isFailFast());
    }

    /**
     * @param message of violation
     * @return violation for current context
     */
    @NotNull
    default Violation violates(@NotNull String message) {
        return new SimpleViolation(message, path());
    }

    static Builder builder() {
        return new SimpleValidationContext.SimpleBuilder(SimpleValidationContext.SimpleFieldPath.ROOT, false);
    }

    /**
     * Indicates deep object path for violation and validation context
     */
    @Immutable
    interface Path {

        /**
         * @return current path value (field name or index in array)
         */
        String value();

        /**
         * @return root path if exist
         */
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

        /**
         * @return full path concatenated to string
         */
        @NotNull
        default String full() {
            return toString();
        }
    }

    /**
     * Context builder
     */
    interface Builder {

        /**
         * @param isFailFast {@link Boolean#TRUE} when should fail on first occurred violation
         * @return self
         */
        @NotNull
        Builder failFast(boolean isFailFast);

        @NotNull
        ValidationContext build();
    }
}
