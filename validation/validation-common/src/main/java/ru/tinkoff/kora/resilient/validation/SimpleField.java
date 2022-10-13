package ru.tinkoff.kora.resilient.validation;

/**
 * Please add Description Here.
 */
record SimpleField<T>(String name, T value) implements Field<T> { }
