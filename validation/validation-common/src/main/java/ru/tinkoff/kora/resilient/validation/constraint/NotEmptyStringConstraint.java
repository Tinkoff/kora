package ru.tinkoff.kora.resilient.validation.constraint;

import ru.tinkoff.kora.resilient.validation.Constraint;
import ru.tinkoff.kora.resilient.validation.NotEmptyConstraint;
import ru.tinkoff.kora.resilient.validation.Violation;

/**
 * Please add Description Here.
 */
public class NotEmptyStringConstraint implements NotEmptyConstraint<String> {

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
