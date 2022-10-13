package ru.tinkoff.kora.resilient.validation.constraint;

import ru.tinkoff.kora.resilient.validation.Constraint;
import ru.tinkoff.kora.resilient.validation.Field;
import ru.tinkoff.kora.resilient.validation.Violation;

import java.util.List;
import java.util.Map;

/**
 * Please add Description Here.
 */
public class NotEmptyConstraint implements Constraint {

    @Override
    public Violation validate(Object fieldValue) {
        if (fieldValue == null) {
            return Violation.of("Should be not empty, but was null");
        }

        if (fieldValue instanceof String s && s.isEmpty()) {
            return Violation.of("Should be not empty, but was empty");
        } else if (fieldValue instanceof Iterable iterable && !iterable.iterator().hasNext()) {
            return Violation.of("Should be not empty, but was empty");
        } else if (fieldValue instanceof Map map && map.isEmpty()) {
            return Violation.of("Should be not empty, but was empty");
        }

        return null;
    }
}
