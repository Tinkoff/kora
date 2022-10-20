package ru.tinkoff.kora.validation;

/**
 * Please add Description Here.
 */
record SimpleViolation(String message, String fieldPath) implements Violation { }
