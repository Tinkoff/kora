package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.Nullable;

/**
 * Please add Description Here.
 */
public interface Field<T> {

    String name();

    T value();

    default boolean isNotEmpty() {
        return value() != null;
    }

    static <T> Field<T> of(@Nullable T value) {
        return new SimpleField<>("", value);
    }

    static <T> Field<T> of(@Nullable T value, String name) {
        return new SimpleField<>(name, value);
    }

    static <T> Field<T> of(@Nullable T value, String name, String rootPathToField) {
        final String fieldNameWithPath = (rootPathToField == null || rootPathToField.isBlank())
            ? name
            : rootPathToField + "." + name;

        return new SimpleField<>(fieldNameWithPath, value);
    }
}
