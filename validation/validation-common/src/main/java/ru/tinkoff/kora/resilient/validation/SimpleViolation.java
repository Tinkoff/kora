package ru.tinkoff.kora.resilient.validation;

/**
 * Please add Description Here.
 */
record SimpleViolation(String message, String fieldPath) implements Violation { }
