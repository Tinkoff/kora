package ru.tinkoff.kora.validation;

/**
 * Please add Description Here.
 */
record SimpleViolation(String message, ValidationContext.Path path) implements Violation {}
