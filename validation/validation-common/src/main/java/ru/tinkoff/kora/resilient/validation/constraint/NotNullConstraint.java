package ru.tinkoff.kora.resilient.validation.constraint;

import ru.tinkoff.kora.resilient.validation.Constraint;
import ru.tinkoff.kora.resilient.validation.Field;
import ru.tinkoff.kora.resilient.validation.Violation;

import java.util.List;

/**
 * Please add Description Here.
 */
public class NotNullConstraint implements Constraint{

    @Override
    public Violation validate(Object fieldValue) {
        if (fieldValue == null) {
            return Violation.of("Should be not null, but was null");
        }

        return null;
    }
}
