package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

/**
 * Factory that is responsible for creating new {@link Validator<T>} implementations
 */
@FunctionalInterface
public interface ValidatorFactory<T> {

    @NotNull
    Validator<T> create();
}
