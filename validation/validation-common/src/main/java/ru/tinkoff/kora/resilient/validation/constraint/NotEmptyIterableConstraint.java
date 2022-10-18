package ru.tinkoff.kora.resilient.validation.constraint;

import ru.tinkoff.kora.resilient.validation.Constraint;
import ru.tinkoff.kora.resilient.validation.NotEmptyConstraint;
import ru.tinkoff.kora.resilient.validation.Violation;

/**
 * Please add Description Here.
 */
public class NotEmptyIterableConstraint implements NotEmptyConstraint<Iterable> {

    @Override
    public Violation validate(Iterable fieldValue) {
        if (fieldValue == null) {
            return Violation.of("Should be not empty, but was null");
        }

        if (!fieldValue.iterator().hasNext()) {
            return Violation.of("Should be not empty, but was empty");
        }

        return null;
    }
}
