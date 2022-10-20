package ru.tinkoff.kora.validation;

@FunctionalInterface
public interface Constraint<T> {

    Violation validate(T fieldValue);
}
