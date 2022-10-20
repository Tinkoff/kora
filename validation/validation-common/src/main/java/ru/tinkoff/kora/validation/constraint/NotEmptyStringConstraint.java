package ru.tinkoff.kora.validation.constraint;

import ru.tinkoff.kora.validation.Constraint;
import ru.tinkoff.kora.validation.Violation;

/**
 * Please add Description Here.
 */
public final class NotEmptyStringConstraint implements Constraint<String> {

    @Override
    public Violation validate(String fieldValue) {
        if (fieldValue == null) {
            return Violation.of("Should be not empty, but was null");
        }

        if (fieldValue.isEmpty()) {
            return Violation.of("Should be not empty, but was empty");
        }

        return null;
    }
}
