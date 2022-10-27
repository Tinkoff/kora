package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ConstraintFactory<T> {

    @NotNull
    Constraint<T> create();
}
