package ru.tinkoff.kora.validation;

/**
 * Please add Description Here.
 */
record SimpleField<T>(String name, T value) implements Field<T> { }
