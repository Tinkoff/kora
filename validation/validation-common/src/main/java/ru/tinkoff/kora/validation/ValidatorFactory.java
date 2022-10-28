package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ValidatorFactory<T> {

    @NotNull
    Validator<T> create();
}
