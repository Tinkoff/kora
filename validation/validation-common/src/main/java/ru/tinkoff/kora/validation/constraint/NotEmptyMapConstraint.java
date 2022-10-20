package ru.tinkoff.kora.validation.constraint;

import ru.tinkoff.kora.validation.Constraint;
import ru.tinkoff.kora.validation.Violation;

import java.util.Map;

/**
 * Please add Description Here.
 */
public class NotEmptyMapConstraint<K, V> implements Constraint<Map<K, V>> {

    @Override
    public Violation validate(Map<K, V> fieldValue) {
        if (fieldValue == null) {
            return Violation.of("Should be not empty, but was null");
        }

        if (fieldValue.isEmpty()) {
            return Violation.of("Should be not empty, but was empty");
        }

        return null;
    }
}
