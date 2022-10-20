package ru.tinkoff.kora.validation.constraint;

import ru.tinkoff.kora.validation.Constraint;
import ru.tinkoff.kora.validation.Violation;

/**
 * Please add Description Here.
 */
public final class NotNullConstraint<T> implements Constraint<T>{

    @Override
    public Violation validate(T fieldValue) {
        if (fieldValue == null) {
            return Violation.of("Should be not null, but was null");
        }

        return null;
    }
}
