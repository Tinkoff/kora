package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.Nullable;

/**
 * Please add Description Here.
 */
public interface Field<T> {

    String name();

    T value();

    static <T> Field<T> of(@Nullable T value) {
        return new SimpleField<>("", value);
    }

    static <T> Field<T> of(@Nullable T value, String name) {
        return new SimpleField<>(name, value);
    }

    static <T> Field<T> of(@Nullable T value, String name, String pathToField) {
        final String fieldNameWithPath = (pathToField == null || pathToField.isBlank())
            ? name
            : pathToField + "." + name;

        return new SimpleField<>(fieldNameWithPath, value);
    }
}
