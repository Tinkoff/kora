package ru.tinkoff.kora.validation;

@FunctionalInterface
public interface ConstraintFactory<T> {

    Constraint<T> create();
}
