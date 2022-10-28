package ru.tinkoff.kora.validation;

/**
 * Please add Description Here.
 */
record SimpleValidationContext(Path path, boolean isFailFast) implements ValidationContext {

    record SimplePath(Path root, String value) implements ValidationContext.Path {

        static final Path EMPTY = new SimplePath(null, "");

        @Override
        public String toString() {
            return (root == null)
                ? value
                : root + "." + value;
        }
    }
}
