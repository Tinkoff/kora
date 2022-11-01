package ru.tinkoff.kora.validation;

record SimpleViolation(String message, ValidationContext.Path path) implements Violation {}
