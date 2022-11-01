package ru.tinkoff.kora.validation;

record SimpleValidationContext(Path path, boolean isFailFast) implements ValidationContext {

    record SimpleFieldPath(Path root, String value) implements ValidationContext.Path {

        static final Path EMPTY = new SimpleFieldPath(null, "");

        @Override
        public String toString() {
            return (root == null)
                ? value
                : root + "." + value;
        }
    }

    record SimpleIndexPath(Path root, int index) implements ValidationContext.Path {

        @Override
        public String value() {
            return "[" + index + "]";
        }

        @Override
        public String toString() {
            return (root == null)
                ? value()
                : root + "." + value();
        }
    }
}
