package ru.tinkoff.kora.resilient.validation;

import java.util.List;

/**
 * Please add Description Here.
 */
public interface Constraint {

    Violation validate(Object fieldValue);
}
